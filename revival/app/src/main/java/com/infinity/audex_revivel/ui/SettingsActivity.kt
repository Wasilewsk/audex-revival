package com.infinity.audex_revivel.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.infinity.audex_revivel.BuildConfig
import com.infinity.audex_revivel.R
import com.infinity.audex_revivel.core.AudexPreferences
import com.infinity.audex_revivel.core.AudexPriority
import com.infinity.audex_revivel.core.SoundScheme
import com.infinity.audex_revivel.databinding.ActivitySettingsBinding

class SettingsActivity : AudexActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferences: AudexPreferences
    private var updatingUi = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(binding.root)
        setUpToolbar(binding.toolbar, showBack = true)

        preferences = AudexPreferences(this)
        binding.versionText.text = getString(R.string.version_format, BuildConfig.VERSION_NAME)
        binding.resetButton.setTextColor(getColor(R.color.audex_error))

        setupSchemeSpinner()
        setupPrioritySpinner()
        setupSwitches()
        refreshUi()

        binding.sharedFolderButton.setOnClickListener { showSharedFolderDialog() }
        binding.volumeSettingsButton.setOnClickListener {
            startActivity(Intent(this, VolumeSettingsActivity::class.java))
        }
        binding.eventSettingsButton.setOnClickListener {
            startActivity(Intent(this, EventSettingsActivity::class.java))
        }
        binding.resetButton.setOnClickListener { confirmReset() }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun setupSchemeSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            SoundScheme.entries.map { it.displayName },
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.schemeSpinner.adapter = adapter
        binding.schemeSpinner.setSelection(preferences.scheme.ordinal, false)
        binding.schemeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                if (updatingUi) return
                val scheme = SoundScheme.entries[position]
                if (scheme == preferences.scheme) return
                preferences.scheme = scheme
                announce(getString(R.string.scheme_changed, scheme.displayName))
                if (scheme == SoundScheme.CUSTOM) announce(getString(R.string.scheme_custom_hint))
                if (scheme == SoundScheme.SHARED) announce(getString(R.string.scheme_shared_hint))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupPrioritySpinner() {
        val values = AudexPriority.entries.map { it.percent }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            values.map { getString(R.string.priority_percent, it) },
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.prioritySpinner.adapter = adapter
        binding.prioritySpinner.setSelection(preferences.priority.ordinal, false)
        binding.prioritySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                if (updatingUi) return
                val priority = AudexPriority.entries[position]
                if (priority == preferences.priority) return
                preferences.priority = priority
                announce(getString(R.string.priority_changed, priority.percent))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupSwitches() {
        binding.slsSwitch.setOnCheckedChangeListener { _, checked ->
            if (!updatingUi) preferences.useSls = checked
        }
        binding.disableAudibleSwitch.setOnCheckedChangeListener { _, checked ->
            if (!updatingUi) preferences.disableAudibleFeedback = checked
        }
        binding.hapticSwitch.setOnCheckedChangeListener { _, checked ->
            if (!updatingUi) preferences.clickHapticFeedback = checked
        }
        binding.startupSwitch.setOnCheckedChangeListener { _, checked ->
            if (!updatingUi) preferences.startupFeedback = checked
        }
    }

    private fun refreshUi() {
        updatingUi = true
        try {
            binding.schemeSpinner.setSelection(preferences.scheme.ordinal, false)
            binding.prioritySpinner.setSelection(preferences.priority.ordinal, false)
            binding.slsSwitch.isChecked = preferences.useSls
            binding.disableAudibleSwitch.isChecked = preferences.disableAudibleFeedback
            binding.hapticSwitch.isChecked = preferences.clickHapticFeedback
            binding.startupSwitch.isChecked = preferences.startupFeedback
            binding.prioritySpinner.contentDescription = getString(
                R.string.priority_content_description,
                preferences.priority.percent,
            )
        } finally {
            updatingUi = false
        }
    }

    private fun showSharedFolderDialog() {
        val directory = preferences.ensureSharedSoundsDir()
        if (!directory.isDirectory) {
            announce(getString(R.string.shared_folder_create_failed))
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.shared_folder_title)
            .setMessage(getString(R.string.shared_folder_message, directory.absolutePath))
            .setPositiveButton(R.string.copy_path) { _, _ ->
                val clipboard = getSystemService(ClipboardManager::class.java)
                clipboard?.setPrimaryClip(
                    ClipData.newPlainText(getString(R.string.shared_folder_title), directory.absolutePath),
                )
                announce(getString(R.string.path_copied))
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun confirmReset() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reset_confirm_title)
            .setMessage(R.string.reset_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.reset_confirm_action) { _, _ ->
                preferences.reset()
                refreshUi()
                announce(getString(R.string.reset_done))
            }
            .show()
    }
}
