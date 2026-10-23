package com.infinity.audex_revivel.core.theme

import com.infinity.audex_revivel.core.AudexEvent
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Writes a `.audextheme` package from a manifest and source audio files. */
object ThemePackager {
    private const val MAX_FILE_BYTES = 2L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 20L * 1024 * 1024

    fun write(target: File, manifest: ThemeManifest, soundFiles: Map<AudexEvent, File>) {
        if (!manifest.sounds.containsKey(AudexEvent.CLICK)) {
            throw ThemeFormatException("A theme must include a click sound.")
        }
        var total = 0L
        val entries = LinkedHashMap<String, File>()
        for ((event, path) in manifest.sounds) {
            val source = soundFiles[event]
                ?: throw ThemeFormatException("Missing audio for ${event.themeKey}.")
            if (!ThemeManifest.isSafeSoundPath(path)) {
                throw ThemeFormatException("Invalid sound path for ${event.themeKey}.")
            }
            if (!source.isFile || source.length() <= 0) {
                throw ThemeFormatException("The audio for ${event.themeKey} is unreadable.")
            }
            if (source.length() > MAX_FILE_BYTES) {
                throw ThemeFormatException("The audio for ${event.themeKey} is too large.")
            }
            total += source.length()
            if (total > MAX_TOTAL_BYTES) {
                throw ThemeFormatException("The theme is too large to export.")
            }
            entries[path] = source
        }

        val parent = target.parentFile
        if (parent != null && !parent.isDirectory && !parent.mkdirs()) {
            throw IOException("Could not create the export folder.")
        }

        try {
            ZipOutputStream(target.outputStream().buffered()).use { zip ->
                val manifestEntry = ZipEntry(ThemeManifest.FILE_NAME)
                zip.putNextEntry(manifestEntry)
                zip.write(manifest.toJson().toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                for ((path, source) in entries) {
                    zip.putNextEntry(ZipEntry(path))
                    source.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } catch (exception: Exception) {
            target.delete()
            throw exception
        }
    }
}
