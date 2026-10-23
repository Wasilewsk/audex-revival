package com.infinity.audex_revivel.theme

import com.infinity.audex_revivel.core.AudexEvent
import com.infinity.audex_revivel.core.theme.ThemeFormatException
import com.infinity.audex_revivel.core.theme.ThemeManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeManifestTest {
    private fun manifestJson(
        id: String = "test-theme",
        sounds: String = """
            "click": "sounds/click.wav",
            "view_enter": "sounds/view_enter.ogg",
            "screen_off": "sounds/screen_off.mp3"
        """.trimIndent(),
    ): String = """
        {
          "schema": 1,
          "id": "$id",
          "name": "Test Theme",
          "author": "Tester",
          "description": "A theme used by unit tests.",
          "version": "1.2.3",
          "license": "CC0-1.0",
          "sounds": { $sounds }
        }
    """.trimIndent()

    @Test
    fun parsesFieldsAndMapsEventKeys() {
        val manifest = ThemeManifest.parse(manifestJson())

        assertEquals("test-theme", manifest.id)
        assertEquals("Test Theme", manifest.name)
        assertEquals("Tester", manifest.author)
        assertEquals("1.2.3", manifest.version)
        assertEquals(3, manifest.sounds.size)
        assertEquals("sounds/click.wav", manifest.sounds[AudexEvent.CLICK])
        assertEquals("sounds/view_enter.ogg", manifest.sounds[AudexEvent.VIEW_ENTER])
        assertEquals("sounds/screen_off.mp3", manifest.sounds[AudexEvent.SCREEN_OFF])
    }

    @Test
    fun ignoresUnknownFutureEvents() {
        val manifest = ThemeManifest.parse(
            manifestJson(
                sounds = """
                    "click": "sounds/click.wav",
                    "hologram": "sounds/hologram.wav"
                """.trimIndent(),
            ),
        )

        assertEquals(setOf(AudexEvent.CLICK), manifest.sounds.keys)
    }

    @Test(expected = ThemeFormatException::class)
    fun rejectsManifestWithoutClick() {
        ThemeManifest.parse(
            manifestJson(sounds = "\"scroll\": \"sounds/scroll.wav\""),
        )
    }

    @Test(expected = ThemeFormatException::class)
    fun rejectsPathTraversal() {
        ThemeManifest.parse(
            manifestJson(sounds = "\"click\": \"sounds/../../click.wav\""),
        )
    }

    @Test(expected = ThemeFormatException::class)
    fun rejectsUnsupportedExtension() {
        ThemeManifest.parse(
            manifestJson(sounds = "\"click\": \"sounds/click.exe\""),
        )
    }

    @Test(expected = ThemeFormatException::class)
    fun rejectsIdMismatch() {
        ThemeManifest.parse(manifestJson(id = "test-theme"), expectedId = "other-theme")
    }

    @Test(expected = ThemeFormatException::class)
    fun rejectsUnknownSchema() {
        ThemeManifest.parse(manifestJson().replace("\"schema\": 1", "\"schema\": 2"))
    }

    @Test
    fun roundTripsThroughWriter() {
        val original = ThemeManifest.parse(manifestJson())
        val copy = ThemeManifest.parse(original.toJson())

        assertEquals(original, copy)
    }

    @Test
    fun slugifyProducesValidThemeIds() {
        assertEquals("warm-chime", ThemeManifest.slugify("Warm Chime"))
        assertEquals("my-theme", ThemeManifest.slugify("   "))
        assertEquals("theme-a", ThemeManifest.slugify("A"))
        assertTrue(ThemeManifest.isValidId(ThemeManifest.slugify("Clicks & Bells!")))
    }

    @Test
    fun sanitizesMultilineFields() {
        assertEquals(
            "Bright bells for clicks",
            ThemeManifest.sanitizeText("  Bright\n bells\t\tfor   clicks  "),
        )
    }

    @Test
    fun hashesAreRecognised() {
        assertTrue(ThemeManifest.isSha256("a".repeat(64)))
        assertTrue(!ThemeManifest.isSha256("a".repeat(63)))
        assertTrue(!ThemeManifest.isSha256("g".repeat(64)))
    }
}
