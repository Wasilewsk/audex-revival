package com.infinity.audex_revivel.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.infinity.audex_revivel.R
import com.infinity.audex_revivel.core.AudexEvent
import com.infinity.audex_revivel.core.AudexPreferences
import com.infinity.audex_revivel.core.AudexPriority
import com.infinity.audex_revivel.core.Haptics
import com.infinity.audex_revivel.core.ScrollMath
import com.infinity.audex_revivel.core.SmartLabeler
import com.infinity.audex_revivel.core.SoundEngine
import com.infinity.audex_revivel.core.Speech
import com.infinity.audex_revivel.core.SpeechText

class AudexAccessibilityService : AccessibilityService() {
    private lateinit var preferences: AudexPreferences
    private lateinit var engine: SoundEngine
    private lateinit var speech: Speech
    private lateinit var haptics: Haptics
    private lateinit var overlay: SmartLabelingOverlay
    private var receiver: SystemEventReceiver? = null

    @Volatile
    private var screenOn = true

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            handlePreferenceChange(key)
        }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            preferences = AudexPreferences(this)
            engine = SoundEngine(this)
            speech = Speech(this)
            haptics = Haptics(this)
            overlay = SmartLabelingOverlay(
                service = this,
                preferences = preferences,
                onFocusSound = ::playSmartLabelingFocusSound,
                onToggleSound = ::playSmartLabelingToggleSound,
            )

            preferences.raw.registerOnSharedPreferenceChangeListener(preferenceListener)
            registerSystemReceiver()
            screenOn = getSystemService(PowerManager::class.java)?.isInteractive ?: true
            applyServicePriority()
            reloadAll()

            if (preferences.startupFeedback) {
                haptics.startup()
                playStartupSound()
            }
            Toast.makeText(this, R.string.service_ready, Toast.LENGTH_LONG).show()
            Log.i(TAG, "Audex Revival accessibility service connected")
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to start the accessibility service", error)
        }
    }

    private fun registerSystemReceiver() {
        receiver = SystemEventReceiver(::handleSystemAction)
        ContextCompat.registerReceiver(
            this,
            receiver,
            SystemEventReceiver.filter(),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun reloadAll() {
        if (!::preferences.isInitialized) return
        engine.load(preferences)
        applySuspension()
        applyOverlay()
        applyServicePriority()
    }

    private fun applySuspension() {
        engine.enabled = !preferences.suspended
        if (preferences.suspended) {
            engine.stopAll()
        } else {
            engine.volume = preferences.volume
            engine.resume()
        }
        applyOverlay()
    }

    private fun applyOverlay() {
        if (!::overlay.isInitialized) return
        overlay.setEnabled(preferences.useSls && !preferences.suspended)
    }

    private fun handlePreferenceChange(key: String?) {
        if (!::preferences.isInitialized) return
        try {
            if (key == null) {
                reloadAll()
                return
            }
            when {
                key == AudexPreferences.KEY_SCHEME || isCustomSoundKey(key) -> reloadAll()
                key == AudexPreferences.KEY_VOLUME_LEFT || key == AudexPreferences.KEY_VOLUME_RIGHT -> {
                    engine.volume = if (preferences.suspended) 0f else preferences.volume
                }
                key == AudexPreferences.KEY_SUSPENDED -> applySuspension()
                key == AudexPreferences.KEY_USE_SLS -> applyOverlay()
                key == AudexPreferences.KEY_PRIORITY -> applyServicePriority()
            }
        } catch (error: Throwable) {
            Log.w(TAG, "Could not apply preference change for $key", error)
        }
    }

    private fun isCustomSoundKey(key: String): Boolean =
        AudexEvent.entries.any { it.customPrefKey == key }

    private fun applyServicePriority() {
        if (!::preferences.isInitialized) return
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.flags = AccessibilityServiceInfo.DEFAULT or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.feedbackType = when (preferences.priority) {
            AudexPriority.TWENTY ->
                AccessibilityServiceInfo.FEEDBACK_GENERIC or AccessibilityServiceInfo.FEEDBACK_SPOKEN

            AudexPriority.FORTY ->
                AccessibilityServiceInfo.FEEDBACK_AUDIBLE or AccessibilityServiceInfo.FEEDBACK_HAPTIC

            AudexPriority.SIXTY ->
                AccessibilityServiceInfo.FEEDBACK_AUDIBLE or
                    AccessibilityServiceInfo.FEEDBACK_HAPTIC or
                    AccessibilityServiceInfo.FEEDBACK_SPOKEN

            AudexPriority.EIGHTY ->
                AccessibilityServiceInfo.FEEDBACK_AUDIBLE or
                    AccessibilityServiceInfo.FEEDBACK_GENERIC or
                    AccessibilityServiceInfo.FEEDBACK_HAPTIC or
                    AccessibilityServiceInfo.FEEDBACK_SPOKEN

            AudexPriority.HUNDRED -> AccessibilityServiceInfo.FEEDBACK_VISUAL
        }
        info.notificationTimeout = 0
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val accessibilityEvent = event ?: return
        if (!::preferences.isInitialized) return
        try {
            when (accessibilityEvent.eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED,
                -> handleClick()

                AccessibilityEvent.TYPE_VIEW_FOCUSED,
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,
                -> handleFocus(accessibilityEvent)

                AccessibilityEvent.TYPE_VIEW_SCROLLED -> handleScroll(accessibilityEvent)

                AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START ->
                    playIfEnabled(AudexEvent.VIEW_ENTER)

                AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END ->
                    playIfEnabled(AudexEvent.VIEW_EXIT)

                AccessibilityEvent.TYPE_VIEW_SELECTED -> playIfEnabled(AudexEvent.SELECT)

                AccessibilityEvent.TYPE_GESTURE_DETECTION_START ->
                    playIfEnabled(AudexEvent.GESTURE_BEGIN)

                AccessibilityEvent.TYPE_GESTURE_DETECTION_END ->
                    playIfEnabled(AudexEvent.GESTURE_END)

                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> playIfEnabled(AudexEvent.LONG_CLICK)

                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    if (screenOn) playIfEnabled(AudexEvent.WINDOW_STATE)
                    if (::overlay.isInitialized) overlay.onWindowChanged()
                }
            }
        } catch (error: Throwable) {
            Log.w(TAG, "Accessibility event handling failed", error)
        }
    }

    private fun handleClick() {
        if (preferences.suspended) return
        if (!preferences.isEventEnabled(AudexEvent.CLICK)) return
        if (!preferences.disableAudibleFeedback) engine.play(AudexEvent.CLICK)
        if (preferences.clickHapticFeedback) haptics.click()
    }

    private fun handleFocus(event: AccessibilityEvent) {
        val node = event.source

        if (!preferences.disableAudibleFeedback &&
            preferences.isEventEnabled(AudexEvent.FOCUS_INTERACTIVE)
        ) {
            when {
                node == null -> engine.play(AudexEvent.FOCUS_INTERACTIVE)
                isInteractive(node) -> engine.play(AudexEvent.FOCUS_INTERACTIVE)
                preferences.isEventEnabled(AudexEvent.FOCUS_NON_INTERACTIVE) ->
                    engine.play(AudexEvent.FOCUS_NON_INTERACTIVE)
            }
        }

        if (!preferences.useSls || preferences.suspended || !::overlay.isInitialized) return

        if (node == null) {
            overlay.clearFocus()
            return
        }

        val hasLabel = !node.text.isNullOrEmpty() || !node.contentDescription.isNullOrEmpty()
        if (hasLabel) {
            overlay.clearFocus()
            return
        }

        val label = SmartLabeler.derive(
            node.viewIdResourceName,
            node.className?.toString(),
        )
        if (label == null) {
            overlay.clearFocus()
        } else {
            overlay.onFocus(label)
        }
    }

    private fun isInteractive(node: AccessibilityNodeInfo): Boolean =
        node.isCheckable || node.isClickable || node.isDismissable || node.isEditable

    private fun handleScroll(event: AccessibilityEvent) {
        if (!canPlay(AudexEvent.SCROLL)) return
        val percent = ScrollMath.percent(
            fromIndex = event.fromIndex,
            itemCount = event.itemCount,
            scrollY = event.scrollY,
            maxScrollY = event.maxScrollY,
        )
        engine.play(AudexEvent.SCROLL, ScrollMath.rateForPercent(percent))
    }

    private fun canPlay(event: AudexEvent): Boolean =
        !preferences.suspended &&
            !preferences.disableAudibleFeedback &&
            preferences.isEventEnabled(event)

    private fun playIfEnabled(event: AudexEvent) {
        if (!canPlay(event)) return
        engine.play(event)
    }

    private fun playSmartLabelingFocusSound() {
        if (preferences.suspended || preferences.disableAudibleFeedback) return
        engine.play(AudexEvent.SCROLL)
    }

    private fun playSmartLabelingToggleSound() {
        if (preferences.suspended || preferences.disableAudibleFeedback) return
        engine.play(AudexEvent.SELECT)
    }

    private fun handleSystemAction(action: String) {
        if (!::preferences.isInitialized) return
        when (action) {
            Intent.ACTION_POWER_CONNECTED -> playIfEnabled(AudexEvent.CHARGER_PLUGGED)
            Intent.ACTION_POWER_DISCONNECTED -> playIfEnabled(AudexEvent.CHARGER_UNPLUGGED)
            Intent.ACTION_SCREEN_ON -> {
                screenOn = true
                playIfEnabled(AudexEvent.SCREEN_ON)
            }

            Intent.ACTION_SCREEN_OFF -> {
                playIfEnabled(AudexEvent.SCREEN_OFF)
                screenOn = false
            }
        }
    }

    override fun onGesture(gestureId: Int): Boolean = when (gestureId) {
        AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT -> {
            speech.speak(SpeechText.time())
            true
        }

        AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT -> {
            speech.speak(SpeechText.battery(batteryLevel()))
            true
        }

        else -> super.onGesture(gestureId)
    }

    private fun batteryLevel(): Int {
        val manager = getSystemService(BatteryManager::class.java)
        val capacity = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        return capacity.coerceIn(0, 100)
    }

    private fun playStartupSound() {
        if (preferences.disableAudibleFeedback) return
        runCatching {
            val player = MediaPlayer.create(this, R.raw.serviceinitialised) ?: return
            val volume = preferences.volume
            player.setVolume(volume, volume)
            player.setOnCompletionListener { it.release() }
            player.start()
        }.onFailure { Log.w(TAG, "Could not play the startup sound", it) }
    }

    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // The overlay uses logical pixels; recreate it so padding and position
        // follow density changes.
        if (::overlay.isInitialized) {
            overlay.hide()
            applyOverlay()
        }
    }

    override fun onDestroy() {
        try {
            if (::preferences.isInitialized) {
                preferences.raw.unregisterOnSharedPreferenceChangeListener(preferenceListener)
            }
            receiver?.let { runCatching { unregisterReceiver(it) } }
            receiver = null
            if (::overlay.isInitialized) overlay.hide()
            if (::engine.isInitialized) engine.release()
            if (::speech.isInitialized) speech.shutdown()
        } catch (error: Throwable) {
            Log.w(TAG, "Cleanup failed", error)
        }
        super.onDestroy()
    }

    private companion object {
        const val TAG = "AudexService"
    }
}
