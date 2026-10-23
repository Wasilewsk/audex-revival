package com.infinity.audex_revivel.theme

import com.infinity.audex_revivel.core.AudexEvent
import com.infinity.audex_revivel.core.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class ThemeDraftTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun draft(): ThemeDraft {
        val directory = temporary.newFolder("studio-test")
        File(directory, "sounds").mkdirs()
        File(directory, "sounds/click.ogg").writeBytes(byteArrayOf(1, 2, 3))
        File(directory, ThemeManifest.FILE_NAME).writeText(ThemeManifest("studio-test", "My theme", "Me", "Test theme", "1.0.0", "Private use",
            mapOf(AudexEvent.CLICK to "sounds/click.ogg")).toJson())
        return ThemeDraft(directory)
    }
    @Test fun replacedSoundSurvivesReopeningAndPackageRoundTrip() {
        val draft = draft()
        draft.replaceSound(AudexEvent.CLICK, "wav", ByteArrayInputStream(byteArrayOf(4, 5, 6)))
        val reopened = ThemeDraft(draft.directory)
        assertArrayEquals(byteArrayOf(4, 5, 6), reopened.sound(AudexEvent.CLICK).readBytes())
        val zip = File(temporary.root, "theme.audextheme")
        reopened.export(zip)
        val output = File(temporary.root, "unpacked")
        val manifest = ThemePackageInstaller.extract(zip, output, "studio-test")
        assertEquals("My theme", manifest.name)
        assertArrayEquals(byteArrayOf(4, 5, 6), File(output, manifest.sounds.getValue(AudexEvent.CLICK)).readBytes())
    }
    @Test fun oversizedImportLeavesPreviousSoundAndManifestUntouched() {
        val draft = draft()
        val old = draft.manifest
        assertThrows(IllegalArgumentException::class.java) {
            draft.replaceSound(AudexEvent.CLICK, "mp3", ByteArrayInputStream(ByteArray(2 * 1024 * 1024 + 1)))
        }
        assertEquals(old, ThemeDraft(draft.directory).manifest)
        assertArrayEquals(byteArrayOf(1, 2, 3), draft.sound(AudexEvent.CLICK).readBytes())
        assertFalse(File(draft.directory, ".incoming").exists())
    }
}
