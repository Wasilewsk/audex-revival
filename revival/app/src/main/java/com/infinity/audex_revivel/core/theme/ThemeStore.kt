package com.infinity.audex_revivel.core.theme

import android.content.Context
import com.infinity.audex_revivel.core.AudexEvent
import java.io.File
import java.io.IOException
import java.util.Locale

data class InstalledTheme(
    val manifest: ThemeManifest,
    val directory: File,
)

/** Owns the app-private copies of downloaded themes. */
class ThemeStore(context: Context) {
    private val root = File(context.filesDir, "themes")

    fun installedThemes(): List<InstalledTheme> =
        (root.listFiles() ?: emptyArray())
            .asSequence()
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .mapNotNull { directory ->
                runCatching { InstalledTheme(readManifest(directory), directory) }.getOrNull()
            }
            .sortedBy { it.manifest.name.lowercase(Locale.US) }
            .toList()

    fun find(id: String): InstalledTheme? {
        val directory = File(root, id)
        if (!directory.isDirectory) return null
        return runCatching { InstalledTheme(readManifest(directory), directory) }.getOrNull()
    }

    fun install(packageFile: File, expectedId: String): InstalledTheme {
        if (!ThemeManifest.isValidId(expectedId)) {
            throw ThemeFormatException("That theme id is not valid.")
        }
        if (!root.isDirectory && !root.mkdirs()) {
            throw IOException("Could not create the themes folder.")
        }
        val temporary = File(root, ".install-$expectedId-${System.nanoTime()}")
        try {
            val manifest = ThemePackageInstaller.extract(packageFile, temporary, expectedId)
            val target = File(root, manifest.id)
            if (target.exists() && !target.deleteRecursively()) {
                throw IOException("Could not replace the installed theme.")
            }
            if (!temporary.renameTo(target)) {
                throw IOException("Could not finish installing the theme.")
            }
            return InstalledTheme(manifest, target)
        } finally {
            if (temporary.exists()) temporary.deleteRecursively()
        }
    }

    fun remove(id: String): Boolean {
        val directory = File(root, id)
        if (!directory.isDirectory) return false
        return directory.deleteRecursively()
    }

    fun soundFile(themeId: String, event: AudexEvent): File? {
        val directory = File(root, themeId)
        if (!directory.isDirectory) return null
        val manifest = runCatching { readManifest(directory) }.getOrNull() ?: return null
        val relative = manifest.sounds[event] ?: return null
        val base = directory.canonicalFile
        val file = File(directory, relative).canonicalFile
        if (!file.path.startsWith(base.path + File.separator)) return null
        return file.takeIf { it.isFile && it.length() > 0 }
    }

    private fun readManifest(directory: File): ThemeManifest {
        val file = File(directory, ThemeManifest.FILE_NAME)
        if (!file.isFile) throw ThemeFormatException("The installed theme has no manifest.")
        return ThemeManifest.parse(file.readText(), directory.name)
    }
}
