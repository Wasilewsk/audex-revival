package com.infinity.audex_revivel.core

/** One sound pair per interaction, even when Android also emits detection events. */
class GestureFeedback {
    private var touching = false
    private var begun = false
    private var ended = false

    fun touchStarted(): AudexEvent? {
        touching = true
        begun = true
        ended = false
        return AudexEvent.GESTURE_BEGIN
    }

    fun detectionStarted(): AudexEvent? {
        if (begun) return null
        begun = true
        ended = false
        return AudexEvent.GESTURE_BEGIN
    }

    fun detectionEnded(): AudexEvent? {
        if (!begun || ended) return null
        ended = true
        if (!touching) begun = false
        return AudexEvent.GESTURE_END
    }

    fun touchEnded(): AudexEvent? {
        val result = if (begun && !ended) AudexEvent.GESTURE_END else null
        reset()
        return result
    }

    fun reset() {
        touching = false
        begun = false
        ended = false
    }
}
