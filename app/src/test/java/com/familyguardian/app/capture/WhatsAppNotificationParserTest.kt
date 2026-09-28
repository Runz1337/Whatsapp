package com.familyguardian.app.capture

import com.familyguardian.app.data.models.CapturedDirection
import com.familyguardian.app.data.models.CapturedKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppNotificationParserTest {

    private fun raw(
        title: String?,
        text: String? = null,
        bigText: String? = null,
        subText: String? = null,
        pkg: String = "com.whatsapp",
        key: String = "0|com.whatsapp|1|null|10123",
        postTime: Long = 1_700_000_000_000L
    ) = RawNotification(
        packageName = pkg,
        notificationKey = key,
        title = title,
        text = text,
        bigText = bigText,
        subText = subText,
        postTime = postTime
    )

    @Test
    fun `ignores non whatsapp packages`() {
        val parsed = WhatsAppNotificationParser.parse(
            raw(title = "Gmail", text = "New mail", pkg = "com.google.android.gm")
        )
        assertNull(parsed)
    }

    @Test
    fun `parses a one to one incoming text message`() {
        val parsed = WhatsAppNotificationParser.parse(raw(title = "Sarah", text = "Are we still on for tonight?"))

        assertEquals("Sarah", parsed!!.contactName)
        assertEquals("Sarah", parsed.senderName)
        assertEquals("Are we still on for tonight?", parsed.body)
        assertEquals(CapturedKind.TEXT, parsed.kind)
        assertEquals(CapturedDirection.INCOMING, parsed.direction)
        assertFalse(parsed.isGroup)
    }

    @Test
    fun `prefers bigText over text`() {
        val parsed = WhatsAppNotificationParser.parse(
            raw(title = "Sarah", text = "Hey", bigText = "Hey there, full preview")
        )
        assertEquals("Hey there, full preview", parsed!!.body)
    }

    @Test
    fun `parses a group message and lifts the sender out of the body`() {
        val parsed = WhatsAppNotificationParser.parse(
            raw(title = "Study Group", bigText = "Alex: can you share the notes?")
        )

        assertEquals("Study Group", parsed!!.contactName)
        assertEquals("Alex", parsed.senderName)
        assertEquals("can you share the notes?", parsed.body)
        assertTrue(parsed.isGroup)
    }

    @Test
    fun `treats a You prefix as an outgoing group message`() {
        val parsed = WhatsAppNotificationParser.parse(
            raw(title = "Study Group", bigText = "You: shipping it now")
        )

        assertEquals("You", parsed!!.senderName)
        assertEquals("shipping it now", parsed.body)
        assertEquals(CapturedDirection.OUTGOING, parsed.direction)
        assertTrue(parsed.isGroup)
    }

    @Test
    fun `does not treat a time prefix as a sender`() {
        val parsed = WhatsAppNotificationParser.parse(
            raw(title = "Sarah", text = "10:30 works for me")
        )

        assertFalse(parsed!!.isGroup)
        assertEquals("Sarah", parsed.senderName)
        assertEquals("10:30 works for me", parsed.body)
    }

    @Test
    fun `does not treat a url as a sender prefix`() {
        val parsed = WhatsAppNotificationParser.parse(
            raw(title = "Sarah", text = "https://example.com/a")
        )

        assertFalse(parsed!!.isGroup)
        assertEquals("https://example.com/a", parsed.body)
    }

    @Test
    fun `does not treat an emoji prefix as a sender`() {
        val parsed = WhatsAppNotificationParser.parse(
            raw(title = "Sarah", text = "\uD83D\uDE00: morning")
        )

        assertFalse(parsed!!.isGroup)
        assertEquals("\uD83D\uDE00: morning", parsed.body)
    }

    /**
     * Known, accepted limitation: a one-to-one body that happens to start with an
     * alphabetic word plus a colon is indistinguishable from a group sender prefix.
     * The contact name is still correct; only the sender label and group badge are off.
     */
    @Test
    fun `known limitation - an alphabetic colon prefix is read as a sender`() {
        val parsed = WhatsAppNotificationParser.parse(
            raw(title = "Sarah", text = "Meeting: 3pm")
        )

        assertTrue(parsed!!.isGroup)
        assertEquals("Sarah", parsed.contactName)
    }

    @Test
    fun `uses subText as a group hint when the body has no prefix`() {
        val parsed = WhatsAppNotificationParser.parse(
            raw(title = "Study Group", text = "see you soon", subText = "Alex")
        )
        assertTrue(parsed!!.isGroup)
    }

    @Test
    fun `strips the left to right mark whatsapp injects`() {
        val parsed = WhatsAppNotificationParser.parse(
            raw(title = "\u200ESarah", text = "\u200Ehi")
        )
        assertEquals("Sarah", parsed!!.contactName)
        assertEquals("hi", parsed.body)
    }

    @Test
    fun `classifies media and call notifications`() {
        val cases = mapOf(
            "📷 Photo sent" to CapturedKind.IMAGE,
            "Alex sent a video" to CapturedKind.VIDEO,
            "sent a voice message" to CapturedKind.VOICE,
            "sent a document" to CapturedKind.DOCUMENT,
            "sent a contact card" to CapturedKind.CONTACT,
            "is live now" to CapturedKind.LIVE_LOCATION,
            "incoming voice call" to CapturedKind.CALL,
            "sent a sticker" to CapturedKind.STICKER
        )
        cases.forEach { (body, expected) ->
            assertEquals("body=$body", expected, WhatsAppNotificationParser.classify(body))
        }
    }

    @Test
    fun `flags the business package`() {
        val parsed = WhatsAppNotificationParser.parse(
            raw(title = "Acme", text = "Your invoice", pkg = "com.whatsapp.w4b")
        )
        assertTrue(parsed!!.isBusinessAccount)
    }

    @Test
    fun `extractDelta returns only appended text when a preview grows`() {
        val delta = WhatsAppNotificationParser.extractDelta("first", "first second")
        assertEquals("second", delta)
    }

    @Test
    fun `extractDelta is empty when nothing changed`() {
        assertEquals("", WhatsAppNotificationParser.extractDelta("same", "same"))
    }

    @Test
    fun `extractDelta returns the whole body when the preview was replaced`() {
        assertEquals("totally new", WhatsAppNotificationParser.extractDelta("old text", "totally new"))
    }

    @Test
    fun `message ids are stable and content sensitive`() {
        val a = WhatsAppNotificationParser.messageId("k", 100L, "hello")
        val b = WhatsAppNotificationParser.messageId("k", 100L, "hello")
        val c = WhatsAppNotificationParser.messageId("k", 100L, "hello!")
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `contact ids are case insensitive but hash without collisions on the obvious axis`() {
        assertEquals(
            WhatsAppNotificationParser.contactId("Sarah"),
            WhatsAppNotificationParser.contactId("sarah")
        )
        assertNotEquals(
            WhatsAppNotificationParser.contactId("Sarah"),
            WhatsAppNotificationParser.contactId("Alex")
        )
    }

    @Test
    fun `stableLongId is positive and deterministic`() {
        val id = WhatsAppNotificationParser.messageId("k", 100L, "hello")
        val long = WhatsAppNotificationParser.stableLongId(id)
        assertEquals(long, WhatsAppNotificationParser.stableLongId(id))
        assertTrue("id must stay positive for Firestore document keys", long > 0)
    }

    @Test
    fun `skips notifications with no usable text`() {
        assertNull(WhatsAppNotificationParser.parse(raw(title = "Sarah", text = "   ")))
        assertNull(WhatsAppNotificationParser.parse(raw(title = null, text = "orphan")))
    }
}
