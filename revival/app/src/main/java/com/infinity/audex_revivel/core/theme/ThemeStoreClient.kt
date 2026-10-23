package com.infinity.audex_revivel.core.theme

import com.infinity.audex_revivel.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches the index and packages over HTTPS only. The index is small, packages
 * are capped and every download is checked against the published SHA-256.
 */
class ThemeStoreClient(
    private val indexUrl: String = BuildConfig.THEME_STORE_URL,
) {
    fun fetchIndex(): ThemeIndex {
        val url = URL(indexUrl)
        requireScheme(url)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("The theme store answered with HTTP $code.")
            }
            val body = connection.inputStream.use {
                readLimited(it, ThemeIndexParser.MAX_INDEX_BYTES)
            }
            return ThemeIndexParser.parse(body.toString(Charsets.UTF_8), indexUrl)
        } finally {
            connection.disconnect()
        }
    }

    fun download(listing: ThemeListing, target: File): File {
        val url = URL(listing.downloadUrl)
        requireScheme(url)
        target.parentFile?.mkdirs()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("The theme download answered with HTTP $code.")
            }
            var total = 0L
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        total += read
                        if (total > ThemePackageInstaller.MAX_PACKAGE_BYTES) {
                            throw IOException("The theme package is too large.")
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
            if (total != listing.size) {
                throw IOException("The theme download was incomplete.")
            }
            if (ThemeHashes.sha256(target) != listing.sha256) {
                throw IOException("The theme download failed its checksum.")
            }
            return target
        } catch (exception: Exception) {
            target.delete()
            throw exception
        } finally {
            connection.disconnect()
        }
    }

    private fun requireScheme(url: URL) {
        val allowed = url.protocol == "https" ||
            (indexUrl.startsWith("http://") && url.protocol == "http")
        if (!allowed) throw IOException("Theme downloads must use HTTPS.")
    }

    private fun readLimited(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            total += read
            if (total > limit) throw IOException("The theme store index is too large.")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private companion object {
        const val TIMEOUT_MS = 15_000
        const val USER_AGENT = "AudexRevival/${BuildConfig.VERSION_NAME}"
    }
}
