package com.infinity.audex_revivel.theme

import com.infinity.audex_revivel.core.theme.ThemeFormatException
import com.infinity.audex_revivel.core.theme.ThemeIndexParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeIndexTest {
    private val indexUrl =
        "https://raw.githubusercontent.com/Wasilewsk/audex-themes/main/index.json"

    private fun entry(
        id: String = "warm-chime",
        file: String = "themes/warm-chime-1.0.0.audextheme",
        sha: String = "a".repeat(64),
    ): String = """
        {
          "id": "$id",
          "name": "Warm Chime",
          "author": "Tester",
          "description": "Soft bells.",
          "version": "1.0.0",
          "license": "CC0-1.0",
          "sounds": 15,
          "file": "$file",
          "sha256": "$sha",
          "size": 12345,
          "minAppVersion": 610
        }
    """.trimIndent()

    private fun index(vararg entries: String): String =
        """{"schema":1,"themes":[${entries.joinToString(",")}]}"""

    @Test
    fun resolvesRelativePackageUrlsAgainstTheIndex() {
        val parsed = ThemeIndexParser.parse(index(entry()), indexUrl)

        assertEquals(1, parsed.themes.size)
        val theme = parsed.themes.single()
        assertEquals("warm-chime", theme.id)
        assertEquals(
            "https://raw.githubusercontent.com/Wasilewsk/audex-themes/main/themes/warm-chime-1.0.0.audextheme",
            theme.downloadUrl,
        )
        assertEquals(610, theme.minAppVersion)
    }

    @Test
    fun keepsAbsoluteHttpsUrls() {
        val parsed = ThemeIndexParser.parse(
            index(entry(file = "https://example.com/themes/theme.audextheme")),
            indexUrl,
        )

        assertEquals(
            "https://example.com/themes/theme.audextheme",
            parsed.themes.single().downloadUrl,
        )
    }

    @Test(expected = ThemeFormatException::class)
    fun rejectsInsecureDownloadUrls() {
        ThemeIndexParser.parse(
            index(entry(file = "http://example.com/themes/theme.audextheme")),
            indexUrl,
        )
    }

    @Test
    fun allowsHttpForLocalDevelopmentIndex() {
        val parsed = ThemeIndexParser.parse(
            index(entry(file = "http://10.0.2.2:8000/themes/theme.audextheme")),
            "http://10.0.2.2:8000/index.json",
        )

        assertEquals(
            "http://10.0.2.2:8000/themes/theme.audextheme",
            parsed.themes.single().downloadUrl,
        )
    }

    @Test(expected = ThemeFormatException::class)
    fun rejectsHttpWhenIndexIsHttps() {
        ThemeIndexParser.parse(
            index(entry(file = "http://example.com/themes/theme.audextheme")),
            "https://example.com/index.json",
        )
    }

    @Test(expected = ThemeFormatException::class)
    fun rejectsDuplicateThemeIds() {
        ThemeIndexParser.parse(index(entry(), entry(file = "themes/warm-chime-2.0.0.audextheme")), indexUrl)
    }

    @Test(expected = ThemeFormatException::class)
    fun rejectsInvalidChecksum() {
        ThemeIndexParser.parse(index(entry(sha = "not-a-hash")), indexUrl)
    }

    @Test(expected = ThemeFormatException::class)
    fun rejectsUnsupportedSchema() {
        ThemeIndexParser.parse("""{"schema":99,"themes":[]}""", indexUrl)
    }
}
