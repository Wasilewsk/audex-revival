package com.infinity.audex_revivel.core.theme

import com.infinity.audex_revivel.core.AudexEvent
import java.io.File
import java.io.InputStream

/** Studio drafts never change the active sound scheme. */
class ThemeDraft(val directory: File) {
    var manifest = ThemeManifest.parse(File(directory, ThemeManifest.FILE_NAME).readText(), directory.name)
        private set
    fun sound(event: AudexEvent) = File(directory, manifest.sounds.getValue(event))
    fun replaceSound(event: AudexEvent, extension: String, input: InputStream) {
        val temporary = File(directory, ".incoming")
        try {
            input.use { source ->
                require(extension in setOf("ogg", "wav", "mp3", "mid", "m4a"))
                temporary.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 2L * 1024 * 1024) { "Sound files must be no larger than 2 MB." }
                        output.write(buffer, 0, count)
                    }
                    require(total > 0) { "The sound file is empty." }
                }
            }
            val path = "sounds/${event.themeKey}-${System.nanoTime()}.$extension"
            val target = File(directory, path)
            check(temporary.renameTo(target))
            val old = sound(event)
            try {
                update(manifest.copy(sounds = manifest.sounds + (event to path)))
                old.delete()
            } catch (error: Exception) { target.delete(); throw error }
        } finally { temporary.delete() }
    }
    fun update(value: ThemeManifest) {
        ThemeManifest.parse(value.toJson(), directory.name)
        val temporary = File(directory, ".manifest")
        temporary.writeText(value.toJson())
        check(temporary.renameTo(File(directory, ThemeManifest.FILE_NAME)))
        manifest = value
    }
    fun export(target: File) = ThemePackager.write(target, manifest, manifest.sounds.keys.associateWith { sound(it) })
}
