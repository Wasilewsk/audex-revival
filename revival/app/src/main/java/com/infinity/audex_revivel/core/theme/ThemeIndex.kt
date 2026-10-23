package com.infinity.audex_revivel.core.theme

import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/** One theme advertised by the store's index.json. */
data class ThemeListing(
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val version: String,
    val license: String,
    val sounds: Int,
    val downloadUrl: String,
    val sha256: String,
    val size: Long,
    val minAppVersion: Int,
)

data class ThemeIndex(val themes: List<ThemeListing>)

object ThemeIndexParser {
    const val SCHEMA = 1
    const val MAX_INDEX_BYTES = 512 * 1024
    const val MAX_PACKAGE_BYTES = 8L * 1024 * 1024

    private val ID_PATTERN = Regex("^[a-z0-9][a-z0-9-]{1,31}$")
    private val VERSION_PATTERN = Regex("^\\d+\\.\\d+\\.\\d+$")

    fun parse(json: String, indexUrl: String): ThemeIndex {
        val root = try {
            JSONObject(json)
        } catch (exception: JSONException) {
            throw ThemeFormatException("The theme store returned an unreadable index.", exception)
        }
        if (root.optInt("schema", -1) != SCHEMA) {
            throw ThemeFormatException("The theme store uses an unsupported format; update the app.")
        }
        val array = root.optJSONArray("themes")
            ?: throw ThemeFormatException("The theme store index has no theme list.")

        val base = try {
            URI(indexUrl)
        } catch (exception: URISyntaxException) {
            throw ThemeFormatException("The theme store address is not valid.", exception)
        }

        val themes = mutableListOf<ThemeListing>()
        val ids = mutableSetOf<String>()
        for (index in 0 until array.length()) {
            val entry = array.optJSONObject(index) ?: continue
            val listing = parseListing(entry, base)
            if (!ids.add(listing.id)) {
                throw ThemeFormatException("The theme store lists ${listing.id} twice.")
            }
            themes += listing
        }
        return ThemeIndex(themes)
    }

    private fun parseListing(entry: JSONObject, base: URI): ThemeListing {
        val id = text(entry, "id", 32)
        if (!ID_PATTERN.matches(id)) {
            throw ThemeFormatException("The store contains an invalid theme id.")
        }
        val name = text(entry, "name", 60)
        val author = text(entry, "author", 80)
        val description = text(entry, "description", 400)
        val version = text(entry, "version", 20)
        if (!VERSION_PATTERN.matches(version)) {
            throw ThemeFormatException("$name has an invalid version.")
        }
        val license = text(entry, "license", 40)
        val file = text(entry, "file", 200)
        val sha256 = text(entry, "sha256", 64).lowercase(Locale.US)
        if (!ThemeManifest.isSha256(sha256)) {
            throw ThemeFormatException("$name has no valid checksum.")
        }
        val size = entry.optLong("size", -1)
        if (size <= 0 || size > MAX_PACKAGE_BYTES) {
            throw ThemeFormatException("$name has an unsupported package size.")
        }
        val sounds = entry.optInt("sounds", 0)
        if (sounds !in 1..15) {
            throw ThemeFormatException("$name has an invalid sound count.")
        }
        val minAppVersion = entry.optInt("minAppVersion", 0)
        if (minAppVersion < 0) {
            throw ThemeFormatException("$name has an invalid minimum app version.")
        }

        val resolved = try {
            base.resolve(file)
        } catch (exception: IllegalArgumentException) {
            throw ThemeFormatException("$name has an invalid download address.", exception)
        }
        val allowedScheme = resolved.scheme == "https" ||
            (base.scheme == "http" && resolved.scheme == "http")
        if (!allowedScheme) {
            throw ThemeFormatException("$name must be downloaded over HTTPS.")
        }

        return ThemeListing(
            id = id,
            name = name,
            author = author,
            description = description,
            version = version,
            license = license,
            sounds = sounds,
            downloadUrl = resolved.toString(),
            sha256 = sha256,
            size = size,
            minAppVersion = minAppVersion,
        )
    }

    private fun text(entry: JSONObject, key: String, maximum: Int): String {
        val value = entry.opt(key)
        if (value !is String) {
            throw ThemeFormatException("The store index is missing '$key'.")
        }
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.length > maximum) {
            throw ThemeFormatException("The store index has an invalid '$key'.")
        }
        return trimmed
    }
}
