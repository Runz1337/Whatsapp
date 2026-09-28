package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AccessibilitySpeechManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var currentSpeechRate: Float = 1.0f
    private val pendingSpeechQueue = mutableListOf<Pair<String, Boolean>>()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            tts?.setSpeechRate(currentSpeechRate)
            isInitialized = true
            synchronized(pendingSpeechQueue) {
                for ((text, queue) in pendingSpeechQueue) {
                    val queueMode = if (queue) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
                    tts?.speak(text, queueMode, null, "utterance_${System.currentTimeMillis()}")
                }
                pendingSpeechQueue.clear()
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        currentSpeechRate = rate
        if (isInitialized) {
            tts?.setSpeechRate(rate)
        }
    }

    fun speak(text: String, queueInSequence: Boolean = true) {
        if (text.isBlank()) return
        if (!isInitialized) {
            synchronized(pendingSpeechQueue) {
                pendingSpeechQueue.add(text to queueInSequence)
            }
            return
        }
        val queueMode = if (queueInSequence) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
        tts?.speak(text, queueMode, null, "utterance_${System.currentTimeMillis()}")
    }

    fun stop() {
        synchronized(pendingSpeechQueue) {
            pendingSpeechQueue.clear()
        }
        if (isInitialized) {
            tts?.stop()
        }
    }

    fun shutdown() {
        try {
            stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Exception) {
        }
    }
}
