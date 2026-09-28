package com.familyguardian.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.familyguardian.app.data.local.WhatsAppDatabase
import com.familyguardian.app.data.models.DeviceRole
import com.familyguardian.app.data.models.LinkStatus
import com.familyguardian.app.data.models.SupervisionEventType
import com.familyguardian.app.data.remote.ClaimResult
import com.familyguardian.app.data.remote.EventDto
import com.familyguardian.app.data.remote.LinkDto
import com.familyguardian.app.data.remote.SupervisionRemoteSource
import com.familyguardian.app.data.repository.SupervisionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeSupervisionRemote : SupervisionRemoteSource {
    var available = true
    val links = mutableMapOf<String, LinkDto>()
    val events = mutableListOf<Pair<String, EventDto>>()
    val messages = mutableListOf<Pair<String, String>>()

    // Stands in for Firestore snapshot listeners: watchers must observe later changes to
    // a link, not just the value that happened to be stored when they subscribed.
    private val linkUpdates = MutableStateFlow(0)

    private fun notifyLinkChanged() {
        linkUpdates.value = linkUpdates.value + 1
    }

    override suspend fun isAvailable() = available

    override suspend fun createPendingLink(link: LinkDto): Boolean {
        if (links.containsKey(link.linkId)) return false
        links[link.linkId] = link
        notifyLinkChanged()
        return true
    }

    override suspend fun getLink(linkId: String) = links[linkId]

    override suspend fun claimLink(
        linkId: String,
        parentDeviceId: String,
        parentDisplayName: String
    ): ClaimResult {
        val current = links[linkId] ?: return ClaimResult.NotFound
        if (current.status != LinkStatus.PENDING.name) return ClaimResult.AlreadyClaimed
        val now = System.currentTimeMillis()
        val claimed = current.copy(
            status = LinkStatus.ACTIVE.name,
            parentDeviceId = parentDeviceId,
            parentDisplayName = parentDisplayName,
            activatedAt = now
        )
        links[linkId] = claimed
        notifyLinkChanged()
        return ClaimResult.Success(claimed)
    }

    override suspend fun setLinkStatus(linkId: String, status: String, revokedAt: Long?): Boolean {
        val current = links[linkId] ?: return false
        links[linkId] = current.copy(status = status, revokedAt = revokedAt)
        notifyLinkChanged()
        return true
    }

    override suspend fun pushEvent(linkId: String, event: EventDto) {
        events += linkId to event
    }

    override suspend fun pushChat(linkId: String, chat: com.familyguardian.app.data.remote.MirroredChatDto) = Unit

    override suspend fun pushMessage(
        linkId: String,
        message: com.familyguardian.app.data.remote.MirroredMessageDto
    ) {
        messages += linkId to message.text
    }

    override suspend fun deleteMirroredChat(linkId: String, chatId: String) = Unit

    override fun observeLinks(parentDeviceId: String): Flow<List<LinkDto>> =
        linkUpdates.map { links.values.filter { it.parentDeviceId == parentDeviceId } }

    override fun observeLink(linkId: String): Flow<LinkDto?> =
        linkUpdates.map { links[linkId] }.distinctUntilChanged()

    override fun observeEvents(linkId: String, limit: Int): Flow<List<EventDto>> = flowOf(emptyList())

    override fun observeChats(linkId: String): Flow<List<com.familyguardian.app.data.remote.MirroredChatDto>> =
        flowOf(emptyList())

    override fun observeAllMessages(linkId: String): Flow<List<com.familyguardian.app.data.remote.MirroredMessageDto>> =
        flowOf(emptyList())
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SupervisionRepositoryTest {

    private lateinit var context: Context
    private lateinit var remote: FakeSupervisionRemote
    private val openDatabases = mutableListOf<WhatsAppDatabase>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        remote = FakeSupervisionRemote()
    }

    @After
    fun tearDown() {
        openDatabases.forEach { it.close() }
        openDatabases.clear()
    }

    // Each simulated device gets its own in-memory database, so device identity and
    // local link state stay separate while both talk to the same fake remote.
    private fun newDeviceDatabase(): WhatsAppDatabase =
        Room.inMemoryDatabaseBuilder(context, WhatsAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also { openDatabases += it }

    private fun repository(db: WhatsAppDatabase = newDeviceDatabase()): SupervisionRepository =
        SupervisionRepository.newTestInstance(context, remote, db)

    // The exposed link state is a StateFlow fed by a Room Flow, so a local insert only
    // becomes visible after Room's invalidation tracker runs. Tests therefore wait for
    // the state they depend on instead of reading .value on the very next line.
    private suspend fun <T> awaitState(
        flow: StateFlow<T>,
        message: String,
        timeoutMs: Long = 5_000,
        predicate: (T) -> Boolean
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate(flow.value) && System.currentTimeMillis() < deadline) {
            delay(10)
        }
        assertTrue(message, predicate(flow.value))
    }

    // Pushing to the transport is deliberately fire-and-forget on the repository scope,
    // so a test that asserts on what reached the remote has to let that work run.
    private suspend fun awaitEvents(count: Int, timeoutMs: Long = 5_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (remote.events.size < count && System.currentTimeMillis() < deadline) {
            delay(10)
        }
        assertEquals(
            "expected $count supervision event(s) to reach the remote",
            count,
            remote.events.size
        )
    }

    @Test
    fun `pairing code is single use`() = runBlocking {
        val child = repository()
        child.initialise()
        child.setRole(DeviceRole.CHILD, "Kid phone")

        val outcome = child.issueChildPairingCode()
        val code = (outcome as com.familyguardian.app.data.repository.PairingOutcome.ChildCodeIssued).code

        val parent = repository()
        parent.initialise()
        parent.setRole(DeviceRole.PARENT, "Mum phone")

        val first = parent.claimChildCode(code)
        assertTrue("first claim should succeed", first is com.familyguardian.app.data.repository.PairingOutcome.Linked)

        val second = parent.claimChildCode(code)
        assertEquals(
            "second claim on the same code must be rejected",
            com.familyguardian.app.data.repository.PairingOutcome.CodeAlreadyUsed,
            second
        )
    }

    @Test
    fun `unknown pairing code is reported as not found`() = runBlocking {
        val parent = repository()
        parent.initialise()
        assertEquals(
            com.familyguardian.app.data.repository.PairingOutcome.CodeNotFound,
            parent.claimChildCode("ZZZZ-ZZZZ-ZZZZ-ZZZZ")
        )
    }

    @Test
    fun `parent device sees itself linked but child device reports no supervisor until claimed`() =
        runBlocking {
            val child = repository()
            child.initialise()
            child.setRole(DeviceRole.CHILD, "Kid phone")
            val code = (child.issueChildPairingCode() as
                com.familyguardian.app.data.repository.PairingOutcome.ChildCodeIssued).code

            assertTrue("child should not be supervised before pairing", child.supervisedBy.value.isEmpty())

            val parent = repository()
            parent.initialise()
            parent.setRole(DeviceRole.PARENT, "Mum phone")
            parent.claimChildCode(code)

            awaitState(
                parent.supervisedChildren,
                "parent should have one supervised child"
            ) { it.isNotEmpty() }
        }

    @Test
    fun `revoke clears the link and stops the child reporting a supervisor`() = runBlocking {
        val child = repository()
        child.initialise()
        child.setRole(DeviceRole.CHILD, "Kid phone")
        val code = (child.issueChildPairingCode() as
            com.familyguardian.app.data.repository.PairingOutcome.ChildCodeIssued).code

        val parent = repository()
        parent.initialise()
        parent.setRole(DeviceRole.PARENT, "Mum phone")
        parent.claimChildCode(code)

        child.refreshLinkStatuses()
        val link = child.localLink(code)
        assertNotNull("link should be stored locally on the child", link)
        assertEquals(LinkStatus.ACTIVE.name, link!!.status)

        parent.revokeLink(code)
        child.refreshLinkStatuses()

        awaitState(
            child.supervisedBy,
            "revoked link must not count as a supervisor"
        ) { it.isEmpty() }

        val revoked = child.localLink(code)
        assertEquals(LinkStatus.REVOKED.name, revoked?.status)
    }

    @Test
    fun `no events are pushed while the device has no active supervisor`() = runBlocking {
        val child = repository()
        child.initialise()
        child.setRole(DeviceRole.CHILD, "Kid phone")

        child.recordSimpleEvent(SupervisionEventType.STATUS_ADDED, "posted a status")

        assertTrue("nothing may leave an unpaired device", remote.events.isEmpty())
    }

    @Test
    fun `events reach the remote once a link is active`() = runBlocking {
        val child = repository()
        child.initialise()
        child.setRole(DeviceRole.CHILD, "Kid phone")
        val code = (child.issueChildPairingCode() as
            com.familyguardian.app.data.repository.PairingOutcome.ChildCodeIssued).code

        val parent = repository()
        parent.initialise()
        parent.setRole(DeviceRole.PARENT, "Mum phone")
        parent.claimChildCode(code)

        awaitState(
            child.supervisedBy,
            "child should report the claimed parent as its supervisor"
        ) { it.isNotEmpty() }

        child.recordSimpleEvent(SupervisionEventType.STATUS_ADDED, "posted a status")

        awaitEvents(1)
        assertEquals(code, remote.events.first().first)
        assertEquals(SupervisionEventType.STATUS_ADDED.name, remote.events.first().second.type)
    }

    @Test
    fun `pairing is refused outright when the transport is unavailable`() = runBlocking {
        remote.available = false
        val child = repository()
        child.initialise()
        child.setRole(DeviceRole.CHILD, "Kid phone")

        assertEquals(
            com.familyguardian.app.data.repository.PairingOutcome.RemoteUnavailable,
            child.issueChildPairingCode()
        )
    }

    @Test
    fun `link records survive a new repository instance for the same device`() = runBlocking {
        val db = newDeviceDatabase()
        val child = repository(db)
        child.initialise()
        child.setRole(DeviceRole.CHILD, "Kid phone")
        child.issueChildPairingCode()

        val reopened = repository(db)
        reopened.initialise()
        assertEquals(DeviceRole.CHILD, reopened.role.value)
        assertTrue("device id must be stable across restarts", reopened.deviceId == child.deviceId)
    }
}
