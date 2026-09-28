package com.familyguardian.app.capture

import com.familyguardian.app.data.models.CapturedDirection
import com.familyguardian.app.data.models.CapturedKind
import java.security.MessageDigest
import java.util.Locale

data class ParsedCapture(
    val contactName: String,
    val senderName: String,
    val body: String,
    val kind: CapturedKind,
    val direction: CapturedDirection,
    val isGroup: Boolean,
    val isBusinessAccount: Boolean
)

data class RawNotification(
    val packageName: String,
    val notificationKey: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val postTime: Long
)

/**
 * Turns a posted WhatsApp notification into a capture record.
 *
 * Deliberately free of Android types so the whole thing is unit testable.
 */
object WhatsAppNotificationParser {

    val WATCHED_PACKAGES = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b"
    )

    private const val MAX_SENDER_LENGTH = 48
    private val LRM = "\u200E"

    private val groupPrefix = Regex("^([^:]{1,$MAX_SENDER_LENGTH}):\\s(.+)$", RegexOption.DOT_MATCHES_ALL)
    private val outgoingPrefix = Regex("^You:\\s(.+)$", RegexOption.DOT_MATCHES_ALL)

    /**
     * Guards the group heuristic. A one-to-one body such as "10:30 works" or
     * "https://example.com" must not be mistaken for "sender: text".
     */
    private val senderNameShape = Regex("^[\\p{L}][\\p{L} .'_-]{0,47}$")

    private val kindPatterns: List<Pair<Regex, CapturedKind>> = listOf(
        Regex("(?i)\\b(sent|shared)\\b.*\\b(photo|image|picture)s?\\b|📷") to CapturedKind.IMAGE,
        Regex("(?i)\\b(sent|shared)\\b.*\\bvideos?\\b|🎥") to CapturedKind.VIDEO,
        Regex("(?i)\\b(sent|shared)\\b.*\\bvoice (message|note)\\b|🎤|\\baudio\\b") to CapturedKind.VOICE,
        Regex("(?i)\\b(sent|shared)\\b.*\\b(documents?|files?|pdf)s?\\b|📄") to CapturedKind.DOCUMENT,
        Regex("(?i)\\bsticker sent\\b|\\bsent\\b.*\\bsticker\\b") to CapturedKind.STICKER,
        Regex("(?i)\\b(sent|shared)\\b.*\\bcontact (card|name)\\b") to CapturedKind.CONTACT,
        Regex("(?i)\\bis live now\\b|\\blive location\\b|📍") to CapturedKind.LIVE_LOCATION,
        Regex("(?i)\\b(voice|video) call\\b") to CapturedKind.CALL,
        Regex("(?i)\\b(sent|shared)\\b.*\\bgif\\b") to CapturedKind.STICKER
    )

    fun isWatched(packageName: String): Boolean = packageName in WATCHED_PACKAGES

    fun parse(raw: RawNotification): ParsedCapture? {
        if (!isWatched(raw.packageName)) return null

        val title = raw.title.clean()
        val body = (raw.bigText ?: raw.text).clean()
        if (title.isBlank() || body.isBlank()) return null

        val outgoingMatch = outgoingPrefix.find(body)
        val groupMatch = groupPrefix.find(body)
        val hasSenderHint = !raw.subText.isNullOrBlank() && raw.subText.clean() != title

        // "Sender: text" only marks a group when the prefix really looks like a name.
        val isGroup = hasSenderHint || (groupMatch != null && looksLikeSenderName(groupMatch.groupValues[1]))
        val isOutgoing = outgoingMatch != null

        val senderName = when {
            isOutgoing -> "You"
            isGroup -> groupMatch?.groupValues?.get(1)?.clean().orEmpty().ifBlank { title }
            else -> title
        }

        val strippedBody = when {
            isOutgoing -> outgoingMatch.groupValues[1].clean()
            isGroup -> groupMatch?.groupValues?.get(2)?.clean().orEmpty().ifBlank { body }
            else -> body
        }

        return ParsedCapture(
            contactName = title,
            senderName = senderName,
            body = strippedBody,
            kind = classify(strippedBody),
            direction = if (isOutgoing) CapturedDirection.OUTGOING else CapturedDirection.INCOMING,
            isGroup = isGroup,
            isBusinessAccount = raw.packageName == "com.whatsapp.w4b"
        )
    }

    fun classify(body: String): CapturedKind {
        kindPatterns.firstOrNull { (pattern, _) -> pattern.containsMatchIn(body) }?.let { return it.second }
        return CapturedKind.TEXT
    }

    private fun looksLikeSenderName(candidate: String): Boolean =
        senderNameShape.matches(candidate.trim())

    /**
     * WhatsApp often reuses one notification slot and grows the preview rather than
     * posting a new notification. Returns only the newly appended part so a growing
     * preview does not re-log the earlier messages.
     */
    fun extractDelta(previousBody: String?, incomingBody: String): String {
        if (previousBody.isNullOrBlank()) return incomingBody
        if (incomingBody == previousBody) return ""
        val longer = if (incomingBody.length > previousBody.length) incomingBody else previousBody
        val shorter = if (incomingBody.length > previousBody.length) previousBody else incomingBody
        if (!longer.startsWith(shorter)) return incomingBody
        return longer.substring(shorter.length).trim()
    }

    fun contactId(displayName: String): String = "c_" + shortHash(displayName.trim().lowercase(Locale.ROOT))

    fun messageId(notificationKey: String, postTime: Long, body: String): String =
        "m_" + shortHash("$notificationKey|$postTime|$body")

    /**
     * The mirrored-message DTO carries a Long id, so derive a stable positive Long
     * from the same string id rather than losing determinism.
     */
    fun stableLongId(id: String): Long =
        java.lang.Long.parseUnsignedLong(shortHash(id).take(15), 16)

    private fun shortHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.take(9).joinToString("") { "%02x".format(it) }
    }

    private fun String?.clean(): String {
        if (this == null) return ""
        return replace(LRM, "").replace('\n', ' ').trim()
    }
}
