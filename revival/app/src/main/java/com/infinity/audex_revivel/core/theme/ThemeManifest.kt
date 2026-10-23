package com.infinity.audex_revivel.core.theme

import com.infinity.audex_revivel.core.AudexEvent
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

class ThemeFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * `theme.json` inside a `.audextheme` package. Sound paths are package-relative
 * and always live under `sounds/`.
 */
data class ThemeManifest(
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val version: String,
    val license: String,
    val sounds: Map<AudexEvent, String>,
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("schema", SCHEMA)
        root.put("id", id)
        root.put("name", name)
        root.put("author", author)
        root.put("description", description)
        root.put("version", version)
        root.put("license", license)
        val soundObject = JSONObject()
        for (event in AudexEvent.entries) {
            sounds[event]?.let { soundObject.put(event.themeKey, it) }
        }
        root.put("sounds", soundObject)
        return root.toString(2)
    }

    companion object {
        const val SCHEMA = 1
        const val FILE_NAME = "theme.json"

        private val ID_PATTERN = Regex("^[a-z0-9][a-z0-9-]{1,31}$")
        private val VERSION_PATTERN = Regex("^\\d+\\.\\d+\\.\\d+$")
        private val SHA256_PATTERN = Regex("^[0-9a-f]{64}$")
        private val AUDIO_EXTENSIONS = setOf("wav", "ogg", "mp3", "mid", "m4a")

        fun parse(json: String, expectedId: String? = null): ThemeManifest {
            val root = try {
                JSONObject(json)
            } catch (exception: JSONException) {
                throw ThemeFormatException("theme.json is not valid JSON", exception)
            }

            if (root.optInt("schema", -1) != SCHEMA) {
                throw ThemeFormatException("Unsupported theme format; update Audex Revival.")
            }
            val id = requiredText(root, "id", 32)
            if (!ID_PATTERN.matches(id)) {
                throw ThemeFormatException("The theme id is not a valid lowercase slug.")
            }
            if (expectedId != null && id != expectedId) {
                throw ThemeFormatException("The package id does not match the store listing.")
            }
            val name = requiredText(root, "name", 60)
            val author = requiredText(root, "author", 80)
            val description = requiredText(root, "description", 400)
            val version = requiredText(root, "version", 20)
            if (!VERSION_PATTERN.matches(version)) {
                throw ThemeFormatException("The theme version must look like 1.0.0.")
            }
            val license = requiredText(root, "license", 40)

            val soundObject = root.optJSONObject("sounds")
                ?: throw ThemeFormatException("The theme has no sounds.")
            val sounds = LinkedHashMap<AudexEvent, String>()
            val keys = soundObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val event = AudexEvent.fromThemeKey(key) ?: continue
                val path = soundObject.optString(key, "").trim()
                if (!isSafeSoundPath(path)) {
                    throw ThemeFormatException("The sound path for $key is not allowed.")
                }
                sounds[event] = path
            }
            if (!sounds.containsKey(AudexEvent.CLICK)) {
                throw ThemeFormatException("A theme must include a click sound.")
            }

            return ThemeManifest(id, name, author, description, version, license, sounds)
        }

        fun isSafeSoundPath(path: String): Boolean {
            if (path.isEmpty() || path.length > 180 || path.startsWith("/") || '\\' in path) {
                return false
            }
            val parts = path.split('/')
            if (parts.size != 2 || parts[0] != "sounds") return false
            val fileName = parts[1]
            if (fileName.isEmpty() || fileName == "." || fileName == "..") return false
            val extension = fileName.substringAfterLast('.', "").lowercase(Locale.US)
            return extension in AUDIO_EXTENSIONS
        }

        fun isSha256(value: String): Boolean = SHA256_PATTERN.matches(value)

        fun isValidId(value: String): Boolean = ID_PATTERN.matches(value)

        /** Collapses line breaks and repeated spaces so text fits a manifest field. */
        fun sanitizeText(value: String): String =
            value.replace(Regex("\\s+"), " ").trim()

        fun slugify(name: String): String {
            val base = name.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .take(32)
                .trim('-')
            return when {
                base.length >= 2 -> base
                base.isEmpty() -> "my-theme"
                else -> "theme-$base"
            }
        }

        private fun requiredText(root: JSONObject, key: String, maximum: Int): String {
            val value = root.opt(key)
            if (value !is String) {
                throw ThemeFormatException("theme.json is missing '$key'.")
            }
            val trimmed = value.trim()
            if (trimmed.isEmpty() || trimmed.length > maximum) {
                throw ThemeFormatException("'$key' must be 1-$maximum characters long.")
            }
            if (trimmed.any { it.code < 32 }) {
                throw ThemeFormatException("'$key' contains an unsupported character.")
            }
            return trimmed
        }
    }
}
