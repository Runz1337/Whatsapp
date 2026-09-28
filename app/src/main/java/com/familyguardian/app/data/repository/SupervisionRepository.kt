package com.familyguardian.app.data.repository

import android.content.Context
import com.familyguardian.app.data.local.WhatsAppDatabase
import com.familyguardian.app.capture.WhatsAppNotificationParser
import com.familyguardian.app.data.models.CapturedDirection
import com.familyguardian.app.data.models.CapturedMessageEntity
import com.familyguardian.app.data.models.DeviceIdentityEntity
import com.familyguardian.app.data.models.DeviceRole
import com.familyguardian.app.data.models.LinkStatus
import com.familyguardian.app.data.models.SupervisedEventEntity
import com.familyguardian.app.data.models.SupervisionEventType
import com.familyguardian.app.data.models.SupervisionLinkEntity
import com.familyguardian.app.data.remote.ClaimResult
import com.familyguardian.app.data.remote.EventDto
import com.familyguardian.app.data.remote.FirestoreSupervisionRemoteSource
import com.familyguardian.app.data.remote.LinkDto
import com.familyguardian.app.data.remote.MirroredChatDto
import com.familyguardian.app.data.remote.MirroredMessageDto
import com.familyguardian.app.data.remote.SupervisionRemoteSource
import com.familyguardian.app.data.remote.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.security.SecureRandom

sealed interface PairingOutcome {
    data class ChildCodeIssued(val code: String, val linkId: String) : PairingOutcome
    data class Linked(val linkId: String, val childDisplayName: String) : PairingOutcome
    data object CodeNotFound : PairingOutcome
    data object CodeAlreadyUsed : PairingOutcome
    data object RemoteUnavailable : PairingOutcome
    data object Failed : PairingOutcome
}

class SupervisionRepository internal constructor(
    context: Context,
    private val remote: SupervisionRemoteSource = FirestoreSupervisionRemoteSource(),
    database: WhatsAppDatabase? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val db = database ?: WhatsAppDatabase.getInstance(context)
    private val identityDao = db.deviceIdentityDao()
    private val linkDao = db.supervisionLinkDao()
    private val eventDao = db.supervisedEventDao()
    private val mirroredChatDao = db.mirroredChatDao()
    private val mirroredMessageDao = db.mirroredMessageDao()

    private val random = SecureRandom()
    private val linkWatchers = mutableMapOf<String, Job>()

    private val _transportReady = MutableStateFlow<Boolean?>(null)
    val transportReady: StateFlow<Boolean?> = _transportReady

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private val bootstrapLock = Mutex()
    private var deviceIdCached: String = ""

    val deviceId: String
        get() = deviceIdCached

    init {
        scope.launch { initialise() }
    }

    // Eagerly shared: supervisedBy/supervisedChildren filter on deviceId, and callers
    // read .value during the first composition. With WhileSubscribed the first read
    // would race deviceId being populated and could latch on an empty result.
    val identity: StateFlow<DeviceIdentityEntity?> = identityDao.observe()
        .stateIn(scope, SharingStarted.Eagerly, null)

    val role: StateFlow<DeviceRole> = identity
        .map { it?.role?.let(DeviceRole::valueOf) ?: DeviceRole.UNSET }
        .stateIn(scope, SharingStarted.Eagerly, DeviceRole.UNSET)

    private val localLinks: StateFlow<List<SupervisionLinkEntity>> = linkDao.observeAll()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val activeLinks: StateFlow<List<SupervisionLinkEntity>> = localLinks
        .map { links -> links.filter { it.status == LinkStatus.ACTIVE.name } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val supervisedBy: StateFlow<List<SupervisionLinkEntity>> = localLinks
        .map { links ->
            links.filter { it.status == LinkStatus.ACTIVE.name && it.childDeviceId == deviceId }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val supervisedChildren: StateFlow<List<SupervisionLinkEntity>> = localLinks
        .map { links ->
            links.filter { it.status == LinkStatus.ACTIVE.name && it.parentDeviceId == deviceId }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val recentEvents: StateFlow<List<SupervisedEventEntity>> =
        eventDao.observeRecent().stateIn(scope, SharingStarted.Eagerly, emptyList())

    suspend fun initialise() = bootstrapLock.withLock {
        if (_ready.value) return@withLock

        val existing = identityDao.get()
        if (existing != null && existing.deviceId.isNotBlank()) {
            deviceIdCached = existing.deviceId
        } else {
            deviceIdCached = newDeviceId()
            identityDao.upsert(
                DeviceIdentityEntity(
                    role = existing?.role ?: DeviceRole.UNSET.name,
                    displayName = existing?.displayName.orEmpty(),
                    deviceId = deviceIdCached,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        _transportReady.value = runCatching { remote.isAvailable() }.getOrDefault(false)
        attachLinkWatchers()

        // Only report ready once the identity row has actually reached the shared flow,
        // otherwise a caller that awaits ready() and then reads .value sees UNSET.
        withTimeoutOrNull(IDENTITY_PROPAGATION_TIMEOUT_MS) { identity.first { it != null } }
        _ready.value = true
    }

    suspend fun localLink(linkId: String): SupervisionLinkEntity? = linkDao.getById(linkId)

    suspend fun refreshLinkStatuses() {
        linkDao.observeAll().first().forEach { link ->
            val fresh = remote.getLink(link.linkId) ?: return@forEach
            val entity = fresh.toEntity()
            if (entity.status != link.status) linkDao.upsert(entity)
        }
    }

    suspend fun setRole(newRole: DeviceRole, displayName: String) {        identityDao.upsert(
            DeviceIdentityEntity(
                id = "local",
                role = newRole.name,
                displayName = displayName.trim().ifBlank { defaultDisplayName(newRole) },
                deviceId = deviceId,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateDisplayName(name: String) {
        val current = identityDao.get() ?: return
        identityDao.upsert(current.copy(displayName = name.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun issueChildPairingCode(): PairingOutcome {
        if (_transportReady.value != true) return PairingOutcome.RemoteUnavailable
        val code = newPairingCode()
        val now = System.currentTimeMillis()
        val link = LinkDto(
            linkId = code,
            status = LinkStatus.PENDING.name,
            childDeviceId = deviceId,
            childDisplayName = identity.value?.displayName.orEmpty(),
            parentDeviceId = "",
            parentDisplayName = "",
            createdAt = now,
            lastEventAt = now
        )
        if (!remote.createPendingLink(link)) return PairingOutcome.Failed
        linkDao.upsert(link.toEntity())
        return PairingOutcome.ChildCodeIssued(code, code)
    }

    suspend fun claimChildCode(code: String): PairingOutcome {
        if (_transportReady.value != true) return PairingOutcome.RemoteUnavailable
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return PairingOutcome.CodeNotFound

        return when (val result = remote.claimLink(trimmed, deviceId, identity.value?.displayName.orEmpty())) {
            is ClaimResult.Success -> {
                linkDao.upsert(result.link.toEntity())
                PairingOutcome.Linked(result.link.linkId, result.link.childDisplayName)
            }

            ClaimResult.NotFound -> PairingOutcome.CodeNotFound
            ClaimResult.AlreadyClaimed -> PairingOutcome.CodeAlreadyUsed
            ClaimResult.Failed -> PairingOutcome.Failed
        }
    }

    suspend fun endToEndRevoke() {
        val current = identityDao.get() ?: return
        identityDao.upsert(current.copy(role = DeviceRole.UNSET.name, updatedAt = System.currentTimeMillis()))
    }

    suspend fun revokeLink(linkId: String) {
        val now = System.currentTimeMillis()
        val link = linkDao.getById(linkId) ?: return
        remote.setLinkStatus(linkId, LinkStatus.REVOKED.name, now)
        linkDao.upsert(link.copy(status = LinkStatus.REVOKED.name, revokedAt = now, lastEventAt = now))
        if (link.parentDeviceId == deviceId) {
            mirroredChatDao.deleteForLink(linkId)
            mirroredMessageDao.deleteForLink(linkId)
            eventDao.deleteForLink(linkId)
        }
    }

    fun mirroredChats(linkId: String): Flow<List<com.familyguardian.app.data.models.MirroredChatEntity>> =
        mirroredChatDao.observeForLink(linkId)

    fun mirroredMessages(linkId: String, chatId: String): Flow<List<com.familyguardian.app.data.models.MirroredMessageEntity>> =
        mirroredMessageDao.observeForChat(linkId, chatId)

    fun mirroredEvents(linkId: String): Flow<List<com.familyguardian.app.data.models.SupervisedEventEntity>> =
        eventDao.observeForLink(linkId)

    fun startParentIngest(linkId: String) {
        scope.launch { ingestChatStream(linkId) }
        scope.launch { ingestEventStream(linkId) }
    }

    /**
     * Mirrors a single observed notification to every active supervisor link.
     *
     * This is the only message entry point now that the fake clone is gone: there is
     * no local send path, only what the notification listener observed.
     */
    suspend fun recordCaptured(message: CapturedMessageEntity) {
        if (identity.value?.role != DeviceRole.CHILD.name) return
        val active = activeLinks.value.filter { it.childDeviceId == deviceId }
        if (active.isEmpty()) return

        val isOutgoing = message.direction == CapturedDirection.OUTGOING.name
        val type = if (isOutgoing) SupervisionEventType.MESSAGE_SENT else SupervisionEventType.MESSAGE_RECEIVED
        val chatId = message.contactId
        val body = message.body
        val senderName = message.senderName

        val detail = buildString {
            append("sender=").append(senderName)
            if (message.kind != "TEXT") append("  kind=").append(message.kind)
            append("  body=").append(body.take(MAX_DETAIL_CHARS))
        }
        val summary = if (message.isGroup) {
            "$senderName: ${body.take(SUMMARY_CHARS)}"
        } else {
            body.take(SUMMARY_CHARS)
        }

        val chatDto = MirroredChatDto(
            chatId = chatId,
            title = message.contactName,
            isGroup = message.isGroup,
            lastMessage = body.take(SUMMARY_CHARS),
            lastMessageTime = message.timeText,
            timestamp = message.occurredAt
        )
        val messageDto = MirroredMessageDto(
            sourceMessageId = WhatsAppNotificationParser.stableLongId(message.id),
            chatId = chatId,
            senderName = senderName,
            text = body,
            timestamp = message.occurredAt,
            timeFormatted = message.timeText,
            isOutgoing = isOutgoing,
            messageType = message.kind
        )

        active.forEach { link ->
            scope.launch {
                runCatching {
                    if (link.status != LinkStatus.ACTIVE.name) return@launch
                    remote.pushChat(link.linkId, chatDto)
                    remote.pushMessage(link.linkId, messageDto)
                    remote.pushEvent(
                        link.linkId,
                        EventDto(
                            eventId = newEventId(),
                            type = type.name,
                            chatId = chatId,
                            chatTitle = message.contactName,
                            summary = summary,
                            detail = detail,
                            occurredAt = message.occurredAt
                        )
                    )
                }
            }
        }
    }

    suspend fun recordChatOpened(chatTitle: String) {
        pushToSupervisors(SupervisionEventType.CHAT_OPENED, chatTitle, "Opened the log for $chatTitle")
    }

    suspend fun recordSimpleEvent(type: SupervisionEventType, summary: String, detail: String? = null) {
        pushToSupervisors(type, null, summary, detail)
    }

    private fun pushToSupervisors(
        type: SupervisionEventType,
        chatId: String?,
        summary: String,
        detail: String? = null
    ) {
        if (identity.value?.role != DeviceRole.CHILD.name) return
        activeLinks.value.filter { it.childDeviceId == deviceId }.forEach { link ->
            scope.launch {
                runCatching {
                    if (link.status != LinkStatus.ACTIVE.name) return@launch
                    remote.pushEvent(
                        link.linkId,
                        EventDto(
                            eventId = newEventId(),
                            type = type.name,
                            chatId = chatId,
                            chatTitle = null,
                            summary = summary,
                            detail = detail?.take(MAX_DETAIL_CHARS),
                            occurredAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    private suspend fun ingestEventStream(linkId: String) {
        runCatching {
            remote.observeEvents(linkId, EVENT_PAGE).collect { dtos ->
                eventDao.upsertAll(dtos.map { it.toEntity(linkId) })
            }
        }
    }

    private suspend fun ingestChatStream(linkId: String) {
        runCatching {
            remote.observeChats(linkId).collect { dtos ->
                mirroredChatDao.upsertAll(dtos.map { it.toEntity(linkId) })
            }
        }
        runCatching {
            remote.observeAllMessages(linkId).collect { dtos ->
                mirroredMessageDao.upsertAll(dtos.map { it.toEntity(linkId) })
            }
        }
    }

    private fun attachLinkWatchers() {
        scope.launch {
            localLinks
                .map { links -> links.filterNot { it.status == LinkStatus.REVOKED.name } }
                .collect { links ->
                    val wanted = links.associateBy { it.linkId }
                    linkWatchers.keys.filterNot { it in wanted }.forEach { stale ->
                        linkWatchers.remove(stale)?.cancel()
                    }
                    wanted.values.forEach { link ->
                        if (linkWatchers.containsKey(link.linkId)) return@forEach
                        linkWatchers[link.linkId] = scope.launch {
                            runCatching {
                                remote.observeLink(link.linkId).filterNotNull().collect { remoteLink ->
                                    val current = linkDao.getById(link.linkId) ?: return@collect
                                    val entity = remoteLink.toEntity()
                                    if (entity.status != current.status || entity.lastEventAt != current.lastEventAt) {
                                        linkDao.upsert(entity)
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun newDeviceId(): String = (1..16)
        .map { DEVICE_ALPHABET[random.nextInt(DEVICE_ALPHABET.length)] }
        .joinToString("")

    private fun newPairingCode(): String {
        val body = (1..PAIRING_CODE_LENGTH)
            .map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }
            .joinToString("")
        return "${body.take(5)}-${body.drop(5)}"
    }

    private fun newEventId(): String = "${System.currentTimeMillis()}-${newDeviceId().take(8)}"

    private fun defaultDisplayName(role: DeviceRole) = when (role) {
        DeviceRole.PARENT -> "Parent device"
        DeviceRole.CHILD -> "Child device"
        DeviceRole.UNSET -> "This device"
    }

    companion object {
        const val MAX_DETAIL_CHARS = 4000
        const val EVENT_PAGE = 500
        const val SUMMARY_CHARS = 160

        private const val PAIRING_CODE_LENGTH = 16
        private const val IDENTITY_PROPAGATION_TIMEOUT_MS = 2000L
        private val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val DEVICE_ALPHABET = "0123456789abcdef"

        @Volatile
        private var INSTANCE: SupervisionRepository? = null

        fun getInstance(context: Context): SupervisionRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SupervisionRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        internal fun newTestInstance(
            context: Context,
            remote: SupervisionRemoteSource,
            database: WhatsAppDatabase
        ): SupervisionRepository = SupervisionRepository(context.applicationContext, remote, database)
    }
}
