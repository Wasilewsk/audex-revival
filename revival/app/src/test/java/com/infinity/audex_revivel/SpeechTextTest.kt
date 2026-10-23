package com.infinity.audex_revivel

import com.infinity.audex_revivel.core.SpeechText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.Locale

class SpeechTextTest {
    @Test
    fun batteryTextMatchesTheOriginalPhrasing() {
        assertEquals("Battery 87 percent", SpeechText.battery(87))
    }

    @Test
    fun timeTextIncludesASpokenPrefixAndFormattedTime() {
        val text = SpeechText.time(Date(0), Locale.US)
        assertTrue(text.startsWith("The time is "))
        assertTrue(text.length > "The time is ".length)
    }
}
