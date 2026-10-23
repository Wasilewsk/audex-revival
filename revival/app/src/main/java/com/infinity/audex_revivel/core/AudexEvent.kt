package com.infinity.audex_revivel.core

import androidx.annotation.StringRes
import com.infinity.audex_revivel.R
import java.util.Locale

/**
 * One tunable feedback event. Preference keys mirror the original Audex
 * SharedPreferences so the revival behaves like the 2017 app.
 */
enum class AudexEvent(
    val prefKey: String,
    val customPrefKey: String,
    val sharedFileName: String,
    @StringRes val labelRes: Int,
    val defaultEnabled: Boolean,
) {
    CLICK("clickEvent", "clickSound", "click", R.string.event_click, true),
    VIEW_ENTER("viewEnterEvent", "viewEnterSound", "touch", R.string.event_touch, true),
    VIEW_EXIT("viewExitEvent", "viewExitSound", "touchCleared", R.string.event_touch_cleared, true),
    FOCUS_INTERACTIVE(
        "focusInteractiveEvent",
        "focusInteractiveSound",
        "focusInteractive",
        R.string.event_focus_interactive,
        true,
    ),
    FOCUS_NON_INTERACTIVE(
        "focusNonInteractiveEvent",
        "focusNonInteractiveSound",
        "focusNoninteractive",
        R.string.event_focus_non_interactive,
        true,
    ),
    LONG_CLICK("longPressEvent", "longPressSound", "longPress", R.string.event_long_press, true),
    SCROLL("scrollEvent", "scrollSound", "scroll", R.string.event_scroll, true),
    GESTURE_BEGIN(
        "gestureBeginEvent",
        "gestureBeginSound",
        "gestureBegin",
        R.string.event_gesture_begin,
        true,
    ),
    GESTURE_END("gestureEndEvent", "gestureEndSound", "gestureEnd", R.string.event_gesture_end, true),
    SELECT("selectEvent", "selectSound", "select", R.string.event_select, false),
    WINDOW_STATE("windowStateEvent", "windowStateSound", "windowState", R.string.event_window_state, true),
    CHARGER_PLUGGED(
        "chargerPluggedEvent",
        "chargerPluggedSound",
        "chargerPlugged",
        R.string.event_charger_plugged,
        true,
    ),
    CHARGER_UNPLUGGED(
        "chargerUnpluggedEvent",
        "chargerUnpluggedSound",
        "chargerUnplugged",
        R.string.event_charger_unplugged,
        true,
    ),
    SCREEN_ON("screenOnEvent", "screenOnSound", "screenOn", R.string.event_screen_on, true),
    SCREEN_OFF("screenOffEvent", "screenOffSound", "screenOff", R.string.event_screen_off, true);

    /** Stable id used by theme manifests and theme sound file names. */
    val themeKey: String get() = name.lowercase(Locale.US)

    companion object {
        fun fromThemeKey(value: String): AudexEvent? =
            entries.firstOrNull { it.themeKey == value.lowercase(Locale.US) }
    }
}
