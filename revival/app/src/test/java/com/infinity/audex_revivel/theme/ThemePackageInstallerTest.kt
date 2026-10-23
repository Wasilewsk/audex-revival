package com.infinity.audex_revivel.theme

import com.infinity.audex_revivel.core.AudexEvent
import com.infinity.audex_revivel.core.theme.ThemeFormatException
import com.infinity.audex_revivel.core.theme.ThemeManifest
import com.infinity.audex_revivel.core.theme.ThemePackageInstaller
import com.infinity.audex_revivel.core.theme.ThemePackager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ThemePackageInstallerTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private fun manifestJson(id: String = "test-theme", clickPath: String = "sounds/click.wav"): String =
        """
        {
          "schema": 1,
          "id": "$id",
          "name": "Test Theme",
          "author": "Tester",
          "description": "A theme used by unit tests.",
          "version": "1.0.0",
          "license": "CC0-1.0",
          "sounds": { "click": "$clickPath" }
        }
        """.trimIndent()

    private fun packageWith(entries: Map<String, ByteArray>): File {
        val file = temporary.newFile("theme.audextheme")
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }

    @Test
    fun extractsValidPackage() {
        val archive = packageWith(
            mapOf(
                "theme.json" to manifestJson().toByteArray(),
                "sounds/click.wav" to ByteArray(64) { it.toByte() },
            ),
        )
        val destination = File(temporary.root, "installed")

        val manifest = ThemePackageInstaller.extract(archive, destination, "test-theme")

        assertEquals("test-theme", manifest.id)
        assertEquals("Test Theme", manifest.name)
        assertTrue(File(destination, "sounds/click.wav").isFile)
        assertTrue(File(destination, "theme.json").isFile)
        assertEquals(manifest, ThemeManifest.parse(File(destination, "theme.json").readText(), "test-theme"))
    }

    @Test
    fun toleratesDirectoryEntries() {
        val archive = packageWith(
            mapOf(
                "sounds/" to ByteArray(0),
                "theme.json" to manifestJson().toByteArray(),
                "sounds/click.wav" to ByteArray(8),
            ),
        )

        ThemePackageInstaller.extract(archive, File(temporary.root, "installed"), "test-theme")
    }

    @Test(expected = ThemeFormatException::class)
    fun rejectsPathTraversal() {
        val archive = packageWith(
            mapOf(
                "theme.json" to manifestJson().toByteArray(),
                "sounds/click.wav" to ByteArray(8),
                "../escape.wav" to ByteArray(8),
            ),
        )

        ThemePackageInstaller.extract(archive, File(temporary.root, "installed"))
    }

    @Test(expected = ThemeFormatException::class)
    fun rejectsUnexpectedEntries() {
        val archive = packageWith(
            mapOf(
                "theme.json" to manifestJson().toByteArray(),
                "sounds/click.wav" to ByteArray(8),
                "run.sh" to "echo hello".toByteArray(),
            ),
        )

        ThemePackageInstaller.extract(archive, File(temporary.root, "installed"))
    }

    @Test(expected = ThemeFormatException::class)
    fun rejectsMissingReferencedSound() {
        val archive = packageWith(mapOf("theme.json" to manifestJson().toByteArray()))

        ThemePackageInstaller.extract(archive, File(temporary.root, "installed"))
    }

    @Test(expected = ThemeFormatException::class)
    fun rejectsIdMismatch() {
        val archive = packageWith(
            mapOf(
                "theme.json" to manifestJson().toByteArray(),
                "sounds/click.wav" to ByteArray(8),
            ),
        )

        ThemePackageInstaller.extract(archive, File(temporary.root, "installed"), "other-theme")
    }

    @Test
    fun packagerRoundTripsThroughInstaller() {
        val manifest = ThemeManifest.parse(manifestJson())
        val source = temporary.newFile("click.wav").apply { writeBytes(ByteArray(128) { 7 }) }
        val target = File(temporary.root, "export.audextheme")

        ThemePackager.write(target, manifest, mapOf(AudexEvent.CLICK to source))
        val destination = File(temporary.root, "unpacked")
        val extracted = ThemePackageInstaller.extract(target, destination, "test-theme")

        assertEquals(manifest, extracted)
        assertEquals(128, File(destination, "sounds/click.wav").length().toInt())
        assertTrue(File(destination, "theme.json").isFile)
    }
}
