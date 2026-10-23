package com.infinity.audex_revivel.core.theme

import android.content.Context
import com.infinity.audex_revivel.core.AudexEvent
import com.infinity.audex_revivel.core.AudexPreferences
import java.io.File
import java.util.Locale

/** Turns the user's Custom scheme into a package they can share and submit. */
class ThemeExporter(context: Context) {
    private val appContext = context.applicationContext

    fun export(
        preferences: AudexPreferences,
        name: String,
        author: String,
        description: String,
        license: String,
    ): File {
        val sources = LinkedHashMap<AudexEvent, File>()
        val sounds = LinkedHashMap<AudexEvent, String>()
        for (event in AudexEvent.entries) {
            val path = preferences.customSoundPath(event) ?: continue
            val file = File(path)
            if (!file.isFile || file.length() <= 0) continue
            val extension = file.extension.lowercase(Locale.US)
                .takeIf { it in supportedExtensions } ?: "ogg"
            sources[event] = file
            sounds[event] = "sounds/${event.themeKey}.$extension"
        }
        if (!sources.containsKey(AudexEvent.CLICK)) {
            throw ThemeFormatException("Choose a Click sound first, then export the theme.")
        }

        val trimmedName = ThemeManifest.sanitizeText(name).take(60)
        val id = ThemeManifest.slugify(trimmedName)
        val manifest = ThemeManifest(
            id = id,
            name = trimmedName,
            author = ThemeManifest.sanitizeText(author).take(80),
            description = ThemeManifest.sanitizeText(description).take(400),
            version = "1.0.0",
            license = ThemeManifest.sanitizeText(license).take(40),
            sounds = sounds,
        )

        val directory = File(
            appContext.getExternalFilesDir(null) ?: appContext.filesDir,
            "exports",
        )
        val target = File(directory, "${id}-1.0.0.audextheme")
        ThemePackager.write(target, manifest, sources)
        return target
    }

    private companion object {
        val supportedExtensions = setOf("ogg", "wav", "mp3", "mid", "m4a")
    }
}
