package com.infinity.audex_revivel.core

import android.content.Context
import com.infinity.audex_revivel.R
import com.infinity.audex_revivel.core.theme.ThemeStore

/** One entry in the sound-scheme picker: a built-in scheme or an installed theme. */
data class SchemeChoice(
    val label: String,
    val scheme: SoundScheme,
    val themeId: String? = null,
    val themeName: String? = null,
)

fun schemeChoices(context: Context, preferences: AudexPreferences): List<SchemeChoice> {
    val builtIns = SoundScheme.entries
        .filter { it != SoundScheme.THEME }
        .map { SchemeChoice(it.displayName, it) }
    val themes = ThemeStore(context).installedThemes().map { installed ->
        SchemeChoice(
            context.getString(R.string.theme_scheme_label, installed.manifest.name),
            SoundScheme.THEME,
            installed.manifest.id,
            installed.manifest.name,
        )
    }
    return builtIns + themes
}

fun selectedSchemeIndex(preferences: AudexPreferences, choices: List<SchemeChoice>): Int {
    val activeTheme = preferences.activeThemeId
    if (activeTheme != null) {
        val index = choices.indexOfFirst { it.themeId == activeTheme }
        if (index >= 0) return index
    }
    val scheme = preferences.scheme
    val index = choices.indexOfFirst { it.themeId == null && it.scheme == scheme }
    return if (index >= 0) index else 0
}

fun applySchemeChoice(preferences: AudexPreferences, choice: SchemeChoice) {
    val themeId = choice.themeId
    if (themeId != null) {
        preferences.selectTheme(themeId)
    } else if (choice.scheme != SoundScheme.THEME) {
        preferences.scheme = choice.scheme
    }
}
