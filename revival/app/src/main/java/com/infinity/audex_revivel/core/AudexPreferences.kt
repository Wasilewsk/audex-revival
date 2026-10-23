package com.infinity.audex_revivel.core

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import kotlin.math.roundToInt

/**
 * Typed wrapper around the original "AudexPreferences" file. Most keys keep the
 * 2017 names so event toggles and volumes remain recognisable.
 */
class AudexPreferences(context: Context) {
    private val appContext = context.applicationContext
    val raw: SharedPreferences = appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun edit(): SharedPreferences.Editor = raw.edit()

    var scheme: SoundScheme
        get() = if (activeThemeId != null) {
            SoundScheme.THEME
        } else {
            SoundScheme.fromStored(raw.getString(KEY_SCHEME, null))
        }
        set(value) {
            if (value == SoundScheme.THEME) return
            edit().putString(KEY_SCHEME, value.name).remove(KEY_THEME_ID).apply()
        }

    val activeThemeId: String?
        get() = raw.getString(KEY_THEME_ID, null)?.takeIf { it.isNotBlank() }

    fun selectTheme(themeId: String) {
        edit()
            .putString(KEY_SCHEME, SoundScheme.THEME.name)
            .putString(KEY_THEME_ID, themeId)
            .apply()
    }

    var priority: AudexPriority
        get() = AudexPriority.fromPercent(raw.getInt(KEY_PRIORITY, AudexPriority.HUNDRED.percent))
        set(value) {
            edit().putInt(KEY_PRIORITY, value.percent).apply()
        }

    var volume: Float
        get() = raw.getFloat(KEY_VOLUME_LEFT, 1f).coerceIn(0.1f, 1f)
        set(value) {
            val clamped = value.coerceIn(0.1f, 1f)
            edit().putFloat(KEY_VOLUME_LEFT, clamped).putFloat(KEY_VOLUME_RIGHT, clamped).apply()
        }

    val volumePercent: Int
        get() = (volume * 100).roundToInt()

    var useSls: Boolean
        get() = raw.getBoolean(KEY_USE_SLS, true)
        set(value) {
            edit().putBoolean(KEY_USE_SLS, value).apply()
        }

    var disableAudibleFeedback: Boolean
        get() = raw.getBoolean(KEY_DISABLE_AUDIBLE, false)
        set(value) {
            edit().putBoolean(KEY_DISABLE_AUDIBLE, value).apply()
        }

    var clickHapticFeedback: Boolean
        get() = raw.getBoolean(KEY_HAPTIC, true)
        set(value) {
            edit().putBoolean(KEY_HAPTIC, value).apply()
        }

    var startupFeedback: Boolean
        get() = raw.getBoolean(KEY_STARTUP, true)
        set(value) {
            edit().putBoolean(KEY_STARTUP, value).apply()
        }

    var suspended: Boolean
        get() = raw.getBoolean(KEY_SUSPENDED, false)
        set(value) {
            edit().putBoolean(KEY_SUSPENDED, value).apply()
        }

    fun isEventEnabled(event: AudexEvent): Boolean =
        raw.getBoolean(event.prefKey, event.defaultEnabled)

    fun setEventEnabled(event: AudexEvent, enabled: Boolean) {
        edit().putBoolean(event.prefKey, enabled).apply()
    }

    fun customSoundPath(event: AudexEvent): String? =
        raw.getString(event.customPrefKey, null)?.takeIf { it.isNotBlank() }

    fun setCustomSoundPath(event: AudexEvent, path: String?) {
        val editor = edit()
        if (path.isNullOrBlank()) editor.remove(event.customPrefKey) else editor.putString(event.customPrefKey, path)
        editor.apply()
    }

    fun excludedControls(): MutableSet<String> =
        HashSet(raw.getStringSet(KEY_EXCLUDED, emptySet()) ?: emptySet())

    fun setExcludedControls(controls: Set<String>) {
        edit().putStringSet(KEY_EXCLUDED, controls.toSet()).apply()
    }

    fun sharedSoundsDir(): File =
        File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "Audex")

    fun ensureSharedSoundsDir(): File = sharedSoundsDir().apply { mkdirs() }

    fun customSoundsDir(): File = File(appContext.filesDir, "custom_sounds").apply { mkdirs() }

    fun reset() {
        raw.edit().clear().apply()
        File(appContext.filesDir, "custom_sounds").deleteRecursively()
    }

    companion object {
        const val FILE = "AudexPreferences"

        const val KEY_SCHEME = "currentSoundScheme"
        const val KEY_PRIORITY = "audexPriority"
        const val KEY_VOLUME_LEFT = "leftVolume"
        const val KEY_VOLUME_RIGHT = "rightVolume"
        const val KEY_USE_SLS = "UseSLS"
        const val KEY_DISABLE_AUDIBLE = "DisableAudibleFeedback"
        const val KEY_HAPTIC = "ClickHapticFeedback"
        const val KEY_STARTUP = "StartupFeedback"
        const val KEY_SUSPENDED = "suspendAudex"
        const val KEY_EXCLUDED = "ControlsToExcludeFromSLS"
        const val KEY_THEME_CONTENT = "themeContentRevision"
        const val KEY_THEME_ID = "activeThemeId"

        private val customKeys = AudexEvent.entries.map { it.customPrefKey }.toSet()

        /** True when a preference change requires reloading sounds or the overlay. */
        fun isStructuralKey(key: String?): Boolean =
            key == null ||
                key == KEY_SCHEME ||
                key == KEY_VOLUME_LEFT ||
                key == KEY_VOLUME_RIGHT ||
                key == KEY_USE_SLS ||
                key == KEY_SUSPENDED ||
                key == KEY_PRIORITY ||
                key == KEY_EXCLUDED ||
                key == KEY_THEME_ID ||
                key in customKeys
    }
}
