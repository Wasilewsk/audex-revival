package com.infinity.audex_revivel.core

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class Speech(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                tts?.let { engine ->
                    runCatching { engine.language = Locale.getDefault() }
                }
            }
        }
    }

    fun speak(text: String) {
        if (!ready) return
        val engine = tts ?: return
        runCatching {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "audex-revival")
        }
    }

    fun shutdown() {
        ready = false
        val engine = tts ?: return
        runCatching { engine.stop() }
        runCatching { engine.shutdown() }
        tts = null
    }
}
