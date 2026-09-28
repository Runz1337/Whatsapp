package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.WhatsAppDatabase
import com.example.data.models.AccessibilityLogEntity
import com.example.data.repository.WhatsAppRepository
import com.example.util.AccessibilitySpeechManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WhatsApp", appName)
    }

    @Test
    fun `accessibility logs maintain history for incoming messages`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = WhatsAppRepository(context)

        repository.clearAccessibilityLogs()
        repository.addAccessibilityLog(
            title = "Message from Sarah",
            content = "New message from Sarah: Sounds like a plan!",
            wasSpoken = true
        )

        val logs = repository.allAccessibilityLogs.first()
        assertTrue("Expected at least 1 accessibility log", logs.isNotEmpty())
        assertEquals("Message from Sarah", logs.first().title)
        assertEquals("New message from Sarah: Sounds like a plan!", logs.first().content)
        assertTrue("Log should be marked as spoken", logs.first().wasSpoken)
    }

    @Test
    fun `speech manager handles text and rate adjustments`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val speechManager = AccessibilitySpeechManager(context)
        assertNotNull(speechManager)
        speechManager.setSpeechRate(1.25f)
        speechManager.speak("Testing sequential accessibility announcement", queueInSequence = true)
        speechManager.stop()
        speechManager.shutdown()
    }
}
