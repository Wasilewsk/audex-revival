package com.infinity.audex_revivel

import com.infinity.audex_revivel.core.AudexEvent
import com.infinity.audex_revivel.core.SoundScheme
import com.infinity.audex_revivel.core.builtInSoundRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundSchemeTest {
    @Test
    fun thereAreSixSchemes() {
        assertEquals(6, SoundScheme.entries.size)
    }

    @Test
    fun builtInSchemesCoverEveryEvent() {
        val builtIn = listOf(
            SoundScheme.INNOVEX,
            SoundScheme.VOICEOVER,
            SoundScheme.TALKBACK,
            SoundScheme.CLASSIC_TALKBACK,
        )
        for (scheme in builtIn) {
            for (event in AudexEvent.entries) {
                assertNotEquals(
                    "$scheme is missing a sound for $event",
                    0,
                    builtInSoundRes(scheme, event),
                )
            }
        }
    }

    @Test
    fun fileBackedSchemesDoNotUseRawResources() {
        for (scheme in listOf(SoundScheme.SHARED, SoundScheme.CUSTOM)) {
            for (event in AudexEvent.entries) {
                assertEquals("$scheme should resolve $event from files", 0, builtInSoundRes(scheme, event))
            }
        }
    }

    @Test
    fun screenOnAndOffUseDifferentClips() {
        for (scheme in listOf(
            SoundScheme.INNOVEX,
            SoundScheme.VOICEOVER,
            SoundScheme.TALKBACK,
            SoundScheme.CLASSIC_TALKBACK,
        )) {
            assertNotEquals(
                builtInSoundRes(scheme, AudexEvent.SCREEN_ON),
                builtInSoundRes(scheme, AudexEvent.SCREEN_OFF),
            )
        }
    }

    @Test
    fun storedNamesIncludeTheOriginalMisspelling() {
        assertEquals(SoundScheme.INNOVEX, SoundScheme.fromStored(null))
        assertEquals(SoundScheme.INNOVEX, SoundScheme.fromStored("Innovex"))
        assertEquals(SoundScheme.CLASSIC_TALKBACK, SoundScheme.fromStored("Clasic Talkback"))
        assertEquals(SoundScheme.CUSTOM, SoundScheme.fromStored("CUSTOM"))
        assertEquals(SoundScheme.INNOVEX, SoundScheme.fromStored("something else"))
    }

    @Test
    fun allLabelsAreDistinct() {
        val labels = SoundScheme.entries.map { it.displayName }
        assertEquals(labels.size, labels.toSet().size)
        assertTrue(labels.all { it.isNotBlank() })
    }
}
