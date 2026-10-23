package com.infinity.audex_revivel.core.theme

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Extracts a theme package without trusting it. Rejects absolute paths,
 * traversal, unexpected entries, oversized files and missing referenced audio.
 * The caller passes an empty temporary directory and moves it into place only
 * after this returns successfully.
 */
object ThemePackageInstaller {
    const val MAX_PACKAGE_BYTES = ThemeIndexParser.MAX_PACKAGE_BYTES
    private const val MAX_UNCOMPRESSED_BYTES = 20L * 1024 * 1024
    private const val MAX_ENTRIES = 40
    private const val MAX_FILE_BYTES = 2 * 1024 * 1024
    private const val MAX_MANIFEST_BYTES = 64 * 1024

    fun extract(packageFile: File, destination: File, expectedId: String? = null): ThemeManifest {
        if (!packageFile.isFile) {
            throw ThemeFormatException("The downloaded theme package is missing.")
        }
        if (packageFile.length() > MAX_PACKAGE_BYTES) {
            throw ThemeFormatException("The theme package is too large.")
        }
        if (destination.exists() && !destination.deleteRecursively()) {
            throw IOException("Could not clear the temporary theme folder.")
        }
        if (!destination.mkdirs()) {
            throw IOException("Could not create the temporary theme folder.")
        }

        ZipFile(packageFile).use { archive ->
            val entries = archive.entries().toList()
            if (entries.size > MAX_ENTRIES) {
                throw ThemeFormatException("The theme package contains too many files.")
            }
            val seen = mutableSetOf<String>()
            var totalBytes = 0L
            var manifestJson: String? = null

            for (entry in entries) {
                val name = entry.name.removeSuffix("/")
                if (entry.isDirectory) {
                    if (name != "sounds") {
                        throw ThemeFormatException("The theme package contains an unexpected folder.")
                    }
                    continue
                }
                validateEntryName(name)
                if (!seen.add(name)) {
                    throw ThemeFormatException("The theme package contains duplicate files.")
                }

                if (entry.size > MAX_FILE_BYTES) {
                    throw ThemeFormatException("A file in the theme package is too large.")
                }
                val limit = if (name == ThemeManifest.FILE_NAME) MAX_MANIFEST_BYTES else MAX_FILE_BYTES
                val bytes = archive.getInputStream(entry).use { readLimited(it, limit) }
                totalBytes += bytes.size
                if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
                    throw ThemeFormatException("The theme package unpacks to too much data.")
                }

                if (name == ThemeManifest.FILE_NAME) {
                    manifestJson = bytes.toString(Charsets.UTF_8)
                }
                val target = safeTarget(destination, name)
                target.parentFile?.mkdirs()
                target.writeBytes(bytes)
            }

            val manifest = ThemeManifest.parse(
                manifestJson ?: throw ThemeFormatException("The package has no theme.json."),
                expectedId,
            )
            for ((event, path) in manifest.sounds) {
                val file = safeTarget(destination, path)
                if (!file.isFile || file.length() == 0L) {
                    throw ThemeFormatException("The package is missing the ${event.themeKey} sound.")
                }
            }
            return manifest
        }
    }

    private fun validateEntryName(name: String) {
        if (name.isEmpty() || name.startsWith("/") || '\\' in name) {
            throw ThemeFormatException("The theme package contains an unsafe path.")
        }
        val parts = name.split('/')
        if (parts.any { it.isEmpty() || it == "." || it == ".." }) {
            throw ThemeFormatException("The theme package contains an unsafe path.")
        }
        if (name != ThemeManifest.FILE_NAME) {
            if (parts.size != 2 || parts[0] != "sounds") {
                throw ThemeFormatException("The theme package contains an unexpected file.")
            }
            if (!ThemeManifest.isSafeSoundPath(name)) {
                throw ThemeFormatException("The theme package contains an unsupported file.")
            }
        }
    }

    private fun safeTarget(destination: File, name: String): File {
        val base = destination.canonicalFile
        val target = File(destination, name).canonicalFile
        val prefix = base.path + File.separator
        if (!target.path.startsWith(prefix)) {
            throw ThemeFormatException("The theme package escapes its folder.")
        }
        return target
    }

    private fun readLimited(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            total += read
            if (total > limit) {
                throw ThemeFormatException("A file in the theme package is too large.")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
