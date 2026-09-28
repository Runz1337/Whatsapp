package com.familyguardian.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.familyguardian.app.capture.ParsedCapture
import com.familyguardian.app.capture.RawNotification
import com.familyguardian.app.capture.WhatsAppNotificationParser
import com.familyguardian.app.data.local.WhatsAppDatabase
import com.familyguardian.app.data.models.CapturedContactEntity
import com.familyguardian.app.data.models.CapturedMessageEntity
import com.familyguardian.app.data.models.CapturedDirection
import com.familyguardian.app.data.models.SupervisionEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CaptureRepository private constructor(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = WhatsAppDatabase.getInstance(context)
    private val contactDao = db.capturedContactDao()
    private val messageDao = db.capturedMessageDao()
    private val supervision = SupervisionRepository.getInstance(context)

    private val _listenerConnected = MutableStateFlow(false)
    val listenerConnected: StateFlow<Boolean> = _listenerConnected

    val contacts: StateFlow<List<CapturedContactEntity>> = contactDao.observeAll()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMessages: StateFlow<List<CapturedMessageEntity>> = messageDao.observeRecent()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun messagesFor(contactId: String): Flow<List<CapturedMessageEntity>> =
        messageDao.observeForContact(contactId)

    fun onListenerStateChanged(isConnected: Boolean) {
        _listenerConnected.value = isConnected
    }

    suspend fun record(parsed: ParsedCapture, raw: RawNotification) {
        val contactId = WhatsAppNotificationParser.contactId(parsed.contactName)
        val previous = messageDao.latestForNotification(raw.notificationKey)
        val delta = WhatsAppNotificationParser.extractDelta(previous?.body, parsed.body)
        if (delta.isBlank()) return

        val occurredAt = if (raw.postTime > 0L) raw.postTime else System.currentTimeMillis()
        val messageId = WhatsAppNotificationParser.messageId(raw.notificationKey, occurredAt, delta)
        if (messageDao.get(messageId) != null) return

        val existing = contactDao.get(contactId)
        val timeText = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(occurredAt))
        val isNewContact = existing == null

        val message = CapturedMessageEntity(
            id = messageId,
            contactId = contactId,
            contactName = parsed.contactName,
            body = delta,
            occurredAt = occurredAt,
            senderName = parsed.senderName,
            timeText = timeText,
            direction = parsed.direction.name,
            kind = parsed.kind.name,
            isGroup = parsed.isGroup,
            notificationKey = raw.notificationKey
        )

        db.withTransaction {
            messageDao.upsert(message)
            contactDao.upsert(
                CapturedContactEntity(
                    id = contactId,
                    displayName = parsed.contactName,
                    avatarColorHex = existing?.avatarColorHex ?: pickColor(contactId),
                    isGroup = parsed.isGroup,
                    isBusinessAccount = parsed.isBusinessAccount,
                    lastPreview = delta,
                    lastTimeText = timeText,
                    lastOccurredAt = occurredAt,
                    messageCount = (existing?.messageCount ?: 0) + 1,
                    firstSeenAt = existing?.firstSeenAt ?: occurredAt
                )
            )
        }

        enforceRetention()

        if (isNewContact) {
            supervision.recordSimpleEvent(
                SupervisionEventType.CONTACT_ADDED,
                summary = "New activity observed from ${parsed.contactName}"
            )
        }
        supervision.recordCaptured(message)
    }

    private suspend fun enforceRetention() {
        val total = messageDao.count()
        if (total <= MAX_RETAINED_MESSAGES) return
        messageDao.deleteByIds(messageDao.oldestIds(total - MAX_RETAINED_MESSAGES))
    }

    suspend fun clearLog() {
        messageDao.clear()
        contactDao.clear()
    }

    suspend fun counts(): Pair<Int, Int> = messageDao.count() to contactDao.count()

    private fun pickColor(seed: String): String {
        val palette = listOf(
            "#008069", "#128C7E", "#25D366", "#075E54", "#1EBEA5",
            "#0288D1", "#E91E63", "#3F51B5", "#FF9800", "#9C27B0"
        )
        val index = (seed.hashCode().toLong() and 0xFFFFFFFFL).mod(palette.size.toLong())
        return palette[index.toInt()]
    }

    companion object {
        const val MAX_RETAINED_MESSAGES = 2000

        @Volatile
        private var INSTANCE: CaptureRepository? = null

        fun getInstance(context: Context): CaptureRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = CaptureRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
