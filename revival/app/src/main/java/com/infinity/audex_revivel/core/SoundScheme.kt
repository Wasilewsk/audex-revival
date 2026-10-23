package com.infinity.audex_revivel.core

import com.infinity.audex_revivel.R

enum class SoundScheme(
    val displayName: String,
    val usesExternalFiles: Boolean = false,
) {
    INNOVEX("Innovex"),
    VOICEOVER("Voiceover"),
    TALKBACK("Talkback"),
    CLASSIC_TALKBACK("Classic Talkback"),
    SHARED("Shared", usesExternalFiles = true),
    CUSTOM("Custom", usesExternalFiles = true),

    /** Sounds come from an installed theme package; see AudexPreferences.activeThemeId. */
    THEME("Theme", usesExternalFiles = true);

    companion object {
        /** Accepts both the enum name and the display name, including the original misspelling. */
        fun fromStored(value: String?): SoundScheme {
            if (value.isNullOrBlank()) return INNOVEX
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: entries.firstOrNull { it.displayName.equals(value, ignoreCase = true) }
                ?: if (value.equals("Clasic Talkback", ignoreCase = true)) {
                    CLASSIC_TALKBACK
                } else {
                    INNOVEX
                }
        }
    }
}

/**
 * Resource IDs from FunctionsHub.loadSounds in the 2017 APK.
 * Returns 0 for schemes that are resolved from files at runtime.
 */
internal fun builtInSoundRes(scheme: SoundScheme, event: AudexEvent): Int = when (scheme) {
    SoundScheme.INNOVEX -> when (event) {
        AudexEvent.CLICK -> R.raw.click
        AudexEvent.VIEW_ENTER -> R.raw.viewenter
        AudexEvent.VIEW_EXIT -> R.raw.viewexit
        AudexEvent.FOCUS_INTERACTIVE -> R.raw.focusinteractive
        AudexEvent.FOCUS_NON_INTERACTIVE -> R.raw.focusnoninteractive
        AudexEvent.LONG_CLICK -> R.raw.longclick
        AudexEvent.SCROLL -> R.raw.scroll
        AudexEvent.GESTURE_BEGIN -> R.raw.gesturebegin
        AudexEvent.GESTURE_END -> R.raw.gestureend
        AudexEvent.SELECT -> R.raw.select
        AudexEvent.WINDOW_STATE -> R.raw.windowstate
        AudexEvent.CHARGER_PLUGGED -> R.raw.chargerplugged
        AudexEvent.CHARGER_UNPLUGGED -> R.raw.chargerunplugged
        AudexEvent.SCREEN_ON -> R.raw.screenunlock
        AudexEvent.SCREEN_OFF -> R.raw.screenlock
    }

    SoundScheme.VOICEOVER -> when (event) {
        AudexEvent.CLICK -> R.raw.voclick
        AudexEvent.VIEW_ENTER -> R.raw.voviewenter
        AudexEvent.VIEW_EXIT -> R.raw.voviewexit
        AudexEvent.FOCUS_INTERACTIVE -> R.raw.vofocusinteractive
        AudexEvent.FOCUS_NON_INTERACTIVE -> R.raw.vofocusnoninteractive
        AudexEvent.LONG_CLICK -> R.raw.volongclick
        AudexEvent.SCROLL -> R.raw.voscroll
        AudexEvent.GESTURE_BEGIN -> R.raw.vogesturebegin
        AudexEvent.GESTURE_END -> R.raw.vogestureend
        AudexEvent.SELECT -> R.raw.voselect
        AudexEvent.WINDOW_STATE -> R.raw.vowindowstate
        AudexEvent.CHARGER_PLUGGED -> R.raw.vochargerplugged
        AudexEvent.CHARGER_UNPLUGGED -> R.raw.vochargerunplugged
        AudexEvent.SCREEN_ON -> R.raw.voscreenunlock
        AudexEvent.SCREEN_OFF -> R.raw.voscreenlock
    }

    SoundScheme.TALKBACK -> when (event) {
        AudexEvent.CLICK -> R.raw.tbclick
        AudexEvent.VIEW_ENTER -> R.raw.tbviewenter
        AudexEvent.VIEW_EXIT -> R.raw.tbviewexit
        AudexEvent.FOCUS_INTERACTIVE -> R.raw.tbfocusinteractive
        AudexEvent.FOCUS_NON_INTERACTIVE -> R.raw.tbfocusnoninteractive
        AudexEvent.LONG_CLICK -> R.raw.tblongclick
        AudexEvent.SCROLL -> R.raw.tbscroll
        AudexEvent.GESTURE_BEGIN -> R.raw.tbgesturebegin
        AudexEvent.GESTURE_END -> R.raw.tbgestureend
        AudexEvent.SELECT -> R.raw.tbselect
        AudexEvent.WINDOW_STATE -> R.raw.tbwindowstate
        AudexEvent.CHARGER_PLUGGED -> R.raw.tbchargerplugged
        AudexEvent.CHARGER_UNPLUGGED -> R.raw.tbchargerunplugged
        AudexEvent.SCREEN_ON -> R.raw.tbscreenunlock
        AudexEvent.SCREEN_OFF -> R.raw.tbscreenlock
    }

    SoundScheme.CLASSIC_TALKBACK -> when (event) {
        AudexEvent.CLICK -> R.raw.ctbclick
        AudexEvent.VIEW_ENTER -> R.raw.ctbviewenter
        AudexEvent.VIEW_EXIT -> R.raw.ctbviewexit
        AudexEvent.FOCUS_INTERACTIVE -> R.raw.ctbfocusinteractive
        AudexEvent.FOCUS_NON_INTERACTIVE -> R.raw.ctbfocusnoninteractive
        AudexEvent.LONG_CLICK -> R.raw.ctblongclick
        AudexEvent.SCROLL -> R.raw.ctbscroll
        AudexEvent.GESTURE_BEGIN -> R.raw.ctbgesturebegin
        AudexEvent.GESTURE_END -> R.raw.ctbgestureend
        AudexEvent.SELECT -> R.raw.ctbselect
        AudexEvent.WINDOW_STATE -> R.raw.ctbwindowstate
        AudexEvent.CHARGER_PLUGGED -> R.raw.ctbchargerplugged
        AudexEvent.CHARGER_UNPLUGGED -> R.raw.ctbchargerunplugged
        AudexEvent.SCREEN_ON -> R.raw.ctbscreenunlock
        AudexEvent.SCREEN_OFF -> R.raw.ctbscreenlock
    }

    SoundScheme.SHARED, SoundScheme.CUSTOM, SoundScheme.THEME -> 0
}

internal val soundFileExtensions = listOf(".ogg", ".wav", ".mp3", ".mid", ".m4a")
