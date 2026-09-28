package com.familyguardian.app.data.remote

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import android.util.Log

sealed interface ClaimResult {
    data class Success(val link: LinkDto) : ClaimResult
    data object NotFound : ClaimResult
    data object AlreadyClaimed : ClaimResult
    data object Failed : ClaimResult
}

interface SupervisionRemoteSource {
    suspend fun isAvailable(): Boolean
    suspend fun createPendingLink(link: LinkDto): Boolean
    suspend fun getLink(linkId: String): LinkDto?
    suspend fun claimLink(linkId: String, parentDeviceId: String, parentDisplayName: String): ClaimResult
    suspend fun setLinkStatus(linkId: String, status: String, revokedAt: Long?): Boolean

    suspend fun pushEvent(linkId: String, event: EventDto)
    suspend fun pushChat(linkId: String, chat: MirroredChatDto)
    suspend fun pushMessage(linkId: String, message: MirroredMessageDto)
    suspend fun deleteMirroredChat(linkId: String, chatId: String)

    fun observeLinks(parentDeviceId: String): Flow<List<LinkDto>>
    fun observeLink(linkId: String): Flow<LinkDto?>
    fun observeEvents(linkId: String, limit: Int): Flow<List<EventDto>>
    fun observeChats(linkId: String): Flow<List<MirroredChatDto>>
    fun observeAllMessages(linkId: String): Flow<List<MirroredMessageDto>>
}

/**
 * Firestore-backed transport.
 *
 * The Firestore instance is resolved lazily and defensively: without a
 * `google-services.json` the app must still install and run its local capture log,
 * so every operation degrades to "unavailable" instead of throwing at construction.
 */
class FirestoreSupervisionRemoteSource(
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) : SupervisionRemoteSource {

    private val firestore: FirebaseFirestore? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runCatching { firestoreProvider() }.getOrElse { error ->
            Log.w(TAG, "Firestore unavailable; supervision mirroring is disabled", error)
            null
        }
    }

    private fun links(): CollectionReference? = firestore?.collection(COLLECTION_LINKS)
    private fun events(linkId: String): CollectionReference? = links()?.document(linkId)?.collection(COLLECTION_EVENTS)
    private fun chats(linkId: String): CollectionReference? = links()?.document(linkId)?.collection(COLLECTION_CHATS)
    private fun messages(linkId: String): CollectionReference? = links()?.document(linkId)?.collection(COLLECTION_MESSAGES)

    override suspend fun isAvailable(): Boolean = firestore?.app?.options?.projectId != null

    override suspend fun createPendingLink(link: LinkDto): Boolean = runCatching {
        val target = links()?.document(link.linkId) ?: return false
        target.set(link).await()
        true
    }.getOrDefault(false)

    override suspend fun getLink(linkId: String): LinkDto? = runCatching {
        links()?.document(linkId)?.get()?.await()?.toObject(LinkDto::class.java)
    }.getOrNull()

    override suspend fun claimLink(
        linkId: String,
        parentDeviceId: String,
        parentDisplayName: String
    ): ClaimResult {
        val current = getLink(linkId) ?: return ClaimResult.NotFound
        if (current.status != "PENDING") return ClaimResult.AlreadyClaimed
        if (current.childDeviceId.isBlank()) return ClaimResult.NotFound

        val now = System.currentTimeMillis()
        val activated = current.copy(
            status = "ACTIVE",
            parentDeviceId = parentDeviceId,
            parentDisplayName = parentDisplayName,
            activatedAt = now,
            lastEventAt = now
        )
        val committed = runCatching {
            links()?.document(linkId)?.update(activated.toMap())?.await()
        }.isSuccess

        return if (committed) ClaimResult.Success(activated) else ClaimResult.Failed
    }

    override suspend fun setLinkStatus(linkId: String, status: String, revokedAt: Long?): Boolean =
        runCatching {
            val patch = mutableMapOf<String, Any>("status" to status, "lastEventAt" to System.currentTimeMillis())
            if (revokedAt != null) patch["revokedAt"] = revokedAt
            links()?.document(linkId)?.update(patch)?.await()
            true
        }.getOrDefault(false)

    override suspend fun pushEvent(linkId: String, event: EventDto) {
        runCatching { events(linkId)?.document(event.eventId)?.set(event)?.await() }
    }

    override suspend fun pushChat(linkId: String, chat: MirroredChatDto) {
        runCatching { chats(linkId)?.document(chat.chatId)?.set(chat)?.await() }
    }

    override suspend fun pushMessage(linkId: String, message: MirroredMessageDto) {
        runCatching {
            messages(linkId)?.document(message.sourceMessageId.toString())?.set(message)?.await()
        }
    }

    override suspend fun deleteMirroredChat(linkId: String, chatId: String) {
        runCatching {
            val store = firestore ?: return
            val batch = store.batch()
            val chatRef = chats(linkId)?.document(chatId) ?: return
            batch.delete(chatRef)
            val stale = messages(linkId)
                ?.whereEqualTo("chatId", chatId)
                ?.get()
                ?.await()
                ?: return
            stale.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    override fun observeLinks(parentDeviceId: String): Flow<List<LinkDto>> {
        val ref = links()?.whereEqualTo("parentDeviceId", parentDeviceId) ?: return flowOf(emptyList())
        return listen(ref) { it.toObject(LinkDto::class.java) }
    }

    override fun observeLink(linkId: String): Flow<LinkDto?> {
        val ref = links()?.document(linkId) ?: return flowOf(null)
        return callbackFlow {
            val registration = ref.addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(LinkDto::class.java))
            }
            awaitClose { registration.remove() }
        }
    }

    override fun observeEvents(linkId: String, limit: Int): Flow<List<EventDto>> {
        val ref = events(linkId)?.orderBy("occurredAt", Query.Direction.DESCENDING)?.limit(limit.toLong())
            ?: return flowOf(emptyList())
        return listen(ref) { it.toObject(EventDto::class.java) }
    }

    override fun observeChats(linkId: String): Flow<List<MirroredChatDto>> {
        val ref = chats(linkId)?.orderBy("timestamp", Query.Direction.DESCENDING)
            ?: return flowOf(emptyList())
        return listen(ref) { it.toObject(MirroredChatDto::class.java) }
    }

    override fun observeAllMessages(linkId: String): Flow<List<MirroredMessageDto>> {
        val ref = messages(linkId)?.orderBy("timestamp", Query.Direction.ASCENDING)
            ?: return flowOf(emptyList())
        return listen(ref) { it.toObject(MirroredMessageDto::class.java) }
    }

    private fun <T> listen(
        query: Query,
        mapper: (com.google.firebase.firestore.DocumentSnapshot) -> T
    ): Flow<List<T>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val docs = snapshot?.documents.orEmpty().map(mapper)
            trySend(docs)
        }
        awaitClose { registration.remove() }
    }

    private fun LinkDto.toMap(): Map<String, Any> = mapOf(
        "status" to status,
        "childDeviceId" to childDeviceId,
        "childDisplayName" to childDisplayName,
        "parentDeviceId" to parentDeviceId,
        "parentDisplayName" to parentDisplayName,
        "createdAt" to createdAt,
        "lastEventAt" to lastEventAt
    ) + mapOfNotNull(
        "activatedAt" to activatedAt,
        "revokedAt" to revokedAt
    )

    companion object {
        const val COLLECTION_LINKS = "supervisionLinks"
        const val COLLECTION_EVENTS = "events"
        const val COLLECTION_CHATS = "chats"
        const val COLLECTION_MESSAGES = "messages"
        private const val TAG = "FirestoreSupervision"
    }
}
