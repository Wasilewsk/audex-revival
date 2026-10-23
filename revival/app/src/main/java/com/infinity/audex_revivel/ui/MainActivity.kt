package com.infinity.audex_revivel.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.infinity.audex_revivel.BuildConfig
import com.infinity.audex_revivel.R
import com.infinity.audex_revivel.core.AudexPreferences
import com.infinity.audex_revivel.databinding.ActivityMainBinding
import com.infinity.audex_revivel.service.AudexAccessibilityService

class MainActivity : AudexActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: AudexPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(binding.root)
        setUpToolbar(binding.toolbar, showBack = false)

        preferences = AudexPreferences(this)
        binding.versionText.text = getString(R.string.version_format, BuildConfig.VERSION_NAME)

        binding.enableButton.setOnClickListener { openAccessibilitySettings() }
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.eventsButton.setOnClickListener {
            startActivity(Intent(this, EventSettingsActivity::class.java))
        }
        binding.volumeButton.setOnClickListener {
            startActivity(Intent(this, VolumeSettingsActivity::class.java))
        }
        binding.aboutButton.setOnClickListener { showHelp() }
        binding.suspendSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked != preferences.suspended) {
                preferences.suspended = checked
                announce(
                    getString(if (checked) R.string.suspend_audex else R.string.service_running),
                )
            }
        }

        if (savedInstanceState == null) {
            announce(getString(R.string.welcome_message))
            playStartupSound()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        binding.suspendSwitch.isChecked = preferences.suspended
    }

    private fun updateStatus() {
        val enabled = isServiceEnabled()
        binding.statusText.text = getString(
            if (enabled) R.string.service_running else R.string.service_stopped,
        )
        binding.enableButton.setText(
            if (enabled) R.string.open_accessibility_settings else R.string.enable_service,
        )
    }

    private fun isServiceEnabled(): Boolean {
        val masterEnabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0,
        ) == 1
        if (!masterEnabled) return false

        val services = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val fullClassName = AudexAccessibilityService::class.java.name
        val shortClassName = ".${AudexAccessibilityService::class.java.simpleName}"
        return services.split(':').any { component ->
            val componentPackage = component.substringBefore('/', "")
            val componentClass = component.substringAfter('/', "")
            componentPackage == packageName &&
                (componentClass == fullClassName || componentClass == shortClassName)
        }
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            announce(getString(R.string.accessibility_settings_unavailable))
        }
    }

    private fun showHelp() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.help_title)
            .setMessage(R.string.help_body)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun playStartupSound() {
        if (!preferences.startupFeedback || preferences.disableAudibleFeedback) return
        runCatching {
            val player = MediaPlayer.create(this, R.raw.serviceinitialised) ?: return
            val volume = preferences.volume
            player.setVolume(volume, volume)
            player.setOnCompletionListener { it.release() }
            player.start()
        }
    }
}
