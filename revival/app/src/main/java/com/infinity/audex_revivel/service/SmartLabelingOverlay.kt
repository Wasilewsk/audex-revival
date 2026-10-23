package com.infinity.audex_revivel.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import com.infinity.audex_revivel.R
import com.infinity.audex_revivel.core.AudexPreferences
import com.infinity.audex_revivel.core.ControlLabel

/**
 * Floating SLS button. It is only added while smart labeling is enabled and is
 * always removed when the service stops, the setting is turned off, or Audex is
 * suspended.
 */
class SmartLabelingOverlay(
    private val service: AccessibilityService,
    private val preferences: AudexPreferences,
    private val onFocusSound: () -> Unit,
    private val onToggleSound: () -> Unit,
) {
    private val windowManager: WindowManager? =
        service.getSystemService(WindowManager::class.java)

    private var overlay: Button? = null
    private var currentLabel: ControlLabel? = null
    private var toast: Toast? = null
    private var addFailed = false

    fun setEnabled(enabled: Boolean) {
        if (enabled) show() else hide()
    }

    private fun show() {
        if (overlay != null || addFailed) return
        val manager = windowManager ?: return

        val button = Button(service).apply {
            text = service.getString(R.string.sls_button)
            contentDescription = service.getString(R.string.sls_tutorial)
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(14), dp(8), dp(14), dp(8))
            textSize = 14f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(0xCC00696A.toInt())
            }
            setOnClickListener { toggleCurrentControl() }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(4)
            y = dp(48)
        }

        try {
            manager.addView(button, params)
            overlay = button
        } catch (error: Throwable) {
            Log.w(TAG, "Could not add the SLS overlay", error)
            addFailed = true
        }
    }

    fun onFocus(label: ControlLabel) {
        currentLabel = label
        val excluded = preferences.excludedControls().contains(label.viewId)
        overlay?.contentDescription = service.getString(
            if (excluded) R.string.sls_include else R.string.sls_exclude,
            label.label,
        )
        if (excluded) return

        if (!preferences.disableAudibleFeedback) onFocusSound()
        showToast(label.label)
    }

    fun clearFocus() {
        currentLabel = null
        toast?.cancel()
        toast = null
    }

    fun onWindowChanged() {
        clearFocus()
        overlay?.contentDescription = service.getString(R.string.sls_tutorial)
    }

    fun hide() {
        clearFocus()
        val view = overlay ?: return
        runCatching { windowManager?.removeView(view) }
        overlay = null
    }

    private fun toggleCurrentControl() {
        val label = currentLabel
        if (label == null) {
            showToast(service.getString(R.string.sls_pick_control))
            return
        }

        onToggleSound()
        val excluded = preferences.excludedControls()
        val nowExcluded = if (excluded.contains(label.viewId)) {
            excluded.remove(label.viewId)
            false
        } else {
            excluded.add(label.viewId)
            true
        }
        preferences.setExcludedControls(excluded)
        overlay?.contentDescription = service.getString(
            if (nowExcluded) R.string.sls_include else R.string.sls_exclude,
            label.label,
        )
        val message = service.getString(
            if (nowExcluded) R.string.sls_not_applied else R.string.sls_applied,
            label.label,
        )
        showToast(message)
    }

    private fun showToast(message: String) {
        toast?.cancel()
        toast = Toast.makeText(service, message, Toast.LENGTH_SHORT).also { it.show() }
    }

    private fun dp(value: Int): Int =
        (value * service.resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG = "AudexSlsOverlay"
    }
}
