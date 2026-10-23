package com.infinity.audex_revivel.core.update

import org.json.JSONObject
import java.net.URI

data class UpdateRelease(val tag: String, val notes: String, val url: String, val size: Long) {
    fun isNewer(currentVersion: String, currentBuild: Int): Boolean {
        fun parts(value: String): List<Int> = Regex("^v?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?")
            .find(value)?.let { match -> (1..3).map { match.groupValues[it].toIntOrNull() ?: 0 } }.orEmpty()
        val release = parts(tag)
        val current = parts(currentVersion)
        if (release.isEmpty() || current.isEmpty()) return false
        for (i in 0..2) if (release[i] != current[i]) return release[i] > current[i]
        val build = Regex("(?:build[- ._]?|\\+)(\\+?\\d+)", RegexOption.IGNORE_CASE).find(tag)
            ?.groupValues?.get(1)?.toIntOrNull()
        return build != null && build > currentBuild
    }
    companion object {
        fun parse(json: String): UpdateRelease? {
            val root = JSONObject(json)
            if (root.optBoolean("draft") || root.optBoolean("prerelease")) return null
            val assets = root.optJSONArray("assets") ?: return null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (!asset.optString("name").endsWith(".apk", ignoreCase = true)) continue
                val url = asset.getString("browser_download_url")
                val uri = URI(url)
                require(uri.scheme == "https" && uri.host == "github.com" && uri.userInfo == null &&
                    uri.path.startsWith("/Wasilewsk/audex-revival/releases/download/")) { "Unexpected update download address." }
                val size = asset.getLong("size")
                require(size in 1..100L * 1024 * 1024) { "Unexpected update size." }
                return UpdateRelease(root.getString("tag_name"), root.optString("body").take(50000), url, size)
            }
            return null
        }
    }
}
