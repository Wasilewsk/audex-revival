package com.infinity.audex_revivel.update

import com.infinity.audex_revivel.core.update.UpdateRelease
import org.junit.Assert.*
import org.junit.Test

class UpdateReleaseTest {
    private fun release(tag: String) = UpdateRelease(tag, "Notes", "", 100)
    @Test fun comparesVersionsAndSameVersionBuildsWithoutDowngrading() {
        assertTrue(release("v7.1.0").isNewer("7.0", 1024))
        assertTrue(release("v7.0-build1025").isNewer("7.0", 1024))
        assertFalse(release("v7.0-build1024").isNewer("7.0", 1024))
        assertFalse(release("v6.1.0").isNewer("7.0", 1024))
        assertFalse(release("v7.0").isNewer("7.0", 1024))
        assertFalse(release("development").isNewer("7.0", 1024))
    }
    private fun json(url: String = "https://github.com/Wasilewsk/audex-revival/releases/download/v7.1/Audex.apk", flags: String = "") =
        """{"tag_name":"v7.1", "body":"New sounds", $flags "assets":[{"name":"Audex.apk","size":1234,"browser_download_url":"$url"}]}"""
    @Test fun readsReleaseNotesAndOfficialApk() {
        assertEquals("New sounds", UpdateRelease.parse(json())?.notes)
        assertEquals(1234L, UpdateRelease.parse(json())?.size)
    }
    @Test fun excludesPrereleasesAndForeignDownloads() {
        assertNull(UpdateRelease.parse(json(flags = "\"prerelease\":true,")))
        assertThrows(IllegalArgumentException::class.java) { UpdateRelease.parse(json("https://example.com/malware.apk")) }
        assertThrows(IllegalArgumentException::class.java) { UpdateRelease.parse(json("http://github.com/Wasilewsk/audex-revival/releases/download/v7.1/Audex.apk")) }
    }
}
