package com.infinity.audex_revivel

import com.infinity.audex_revivel.core.AudexEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudexEventTest {
    @Test
    fun allFifteenEventsArePresent() {
        assertEquals(15, AudexEvent.entries.size)
    }

    @Test
    fun preferenceKeysAreUnique() {
        assertEquals(
            AudexEvent.entries.size,
            AudexEvent.entries.map { it.prefKey }.toSet().size,
        )
        assertEquals(
            AudexEvent.entries.size,
            AudexEvent.entries.map { it.customPrefKey }.toSet().size,
        )
        assertEquals(
            AudexEvent.entries.size,
            AudexEvent.entries.map { it.sharedFileName }.toSet().size,
        )
    }

    @Test
    fun defaultsMatchTheOriginalApp() {
        assertFalse(AudexEvent.SELECT.defaultEnabled)
        assertTrue(AudexEvent.entries.filter { it != AudexEvent.SELECT }.all { it.defaultEnabled })
    }

    @Test
    fun keysMatchThe2017PreferenceNames() {
        assertEquals("clickEvent", AudexEvent.CLICK.prefKey)
        assertEquals("clickSound", AudexEvent.CLICK.customPrefKey)
        assertEquals("focusNonInteractiveEvent", AudexEvent.FOCUS_NON_INTERACTIVE.prefKey)
        assertEquals("screenOffSound", AudexEvent.SCREEN_OFF.customPrefKey)
    }
}
