package com.infinity.audex_revivel.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.infinity.audex_revivel.R
import com.infinity.audex_revivel.core.AudexEvent
import com.infinity.audex_revivel.core.AudexPreferences
import com.infinity.audex_revivel.core.SoundEngine
import com.infinity.audex_revivel.core.SoundScheme
import com.infinity.audex_revivel.databinding.ActivityEventsBinding
import com.infinity.audex_revivel.databinding.ItemEventBinding
import java.io.File
import java.io.IOException
import java.util.Locale

class EventSettingsActivity : AudexActivity() {
    private lateinit var binding: ActivityEventsBinding
    private lateinit var preferences: AudexPreferences
    private lateinit var previewEngine: SoundEngine
    private val rows = LinkedHashMap<AudexEvent, ItemEventBinding>()
    private var pendingEvent: AudexEvent? = null
    private var updatingUi = false

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val event = pendingEvent ?: return@registerForActivityResult
        pendingEvent = null
        if (uri == null) return@registerForActivityResult
        try {
            val path = copySound(uri, event)
            preferences.setCustomSoundPath(event, path)
            previewEngine.load(preferences)
            announce(getString(R.string.custom_sound_selected, getString(event.labelRes)))
        } catch (_: IOException) {
            announce(getString(R.string.custom_sound_failed))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(binding.root)
        setUpToolbar(binding.toolbar, showBack = true)

        preferences = AudexPreferences(this)
        previewEngine = SoundEngine(this)
        previewEngine.load(preferences)

        setupSchemeSpinner()
        buildEventRows()
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        previewEngine.load(preferences)
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
                previewEngine.load(preferences)
                refreshUi()
                announce(getString(R.string.scheme_changed, scheme.displayName))
                if (scheme == SoundScheme.CUSTOM) announce(getString(R.string.scheme_custom_hint))
                if (scheme == SoundScheme.SHARED) announce(getString(R.string.scheme_shared_hint))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun buildEventRows() {
        for (event in AudexEvent.entries) {
            val row = ItemEventBinding.inflate(layoutInflater, binding.eventsContainer, false)
            row.eventSwitch.setText(event.labelRes)
            row.eventSwitch.contentDescription = getString(event.labelRes)
            row.eventSwitch.setOnCheckedChangeListener { _, checked ->
                if (!updatingUi) preferences.setEventEnabled(event, checked)
            }

            row.playButton.contentDescription =
                getString(R.string.play_sound, getString(event.labelRes))
            row.playButton.setOnClickListener { preview(event) }

            row.chooseButton.contentDescription =
                getString(R.string.choose_sound, getString(event.labelRes))
            row.chooseButton.setOnClickListener { pickSound(event) }

            binding.eventsContainer.addView(row.root)
            rows[event] = row
        }
    }

    private fun refreshUi() {
        updatingUi = true
        try {
            binding.schemeSpinner.setSelection(preferences.scheme.ordinal, false)
            rows.forEach { (event, row) ->
                row.eventSwitch.isChecked = preferences.isEventEnabled(event)
                row.chooseButton.isVisible = preferences.scheme == SoundScheme.CUSTOM
            }
            when (preferences.scheme) {
                SoundScheme.CUSTOM -> {
                    binding.schemeInfo.isVisible = true
                    binding.schemeInfo.setText(R.string.scheme_custom_hint)
                }

                SoundScheme.SHARED -> {
                    binding.schemeInfo.isVisible = true
                    binding.schemeInfo.setText(R.string.scheme_shared_hint)
                }

                else -> binding.schemeInfo.isVisible = false
            }
        } finally {
            updatingUi = false
        }
    }

    private fun preview(event: AudexEvent) {
        if (previewEngine.preview(event)) {
            announce(getString(R.string.sound_played, getString(event.labelRes)))
        } else {
            announce(getString(R.string.sound_not_loaded))
        }
    }

    private fun pickSound(event: AudexEvent) {
        if (preferences.scheme != SoundScheme.CUSTOM) {
            announce(getString(R.string.scheme_custom_hint))
            return
        }
        pendingEvent = event
        filePicker.launch(arrayOf("audio/*"))
    }

    private fun copySound(uri: Uri, event: AudexEvent): String {
        val directory = preferences.customSoundsDir()
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Could not create the custom sounds directory")
        }
        val target = File(
            directory,
            event.name.lowercase(Locale.US) + "." + extensionFor(uri),
        )
        val input = contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open the selected audio file")
        input.use { source ->
            target.outputStream().use { output ->
                source.copyTo(output)
            }
        }
        return target.absolutePath
    }

    private fun extensionFor(uri: Uri): String {
        val displayName = runCatching {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()

        val fromName = displayName
            ?.substringAfterLast('.', "")
            ?.lowercase(Locale.US)
        if (fromName in supportedExtensions) return fromName!!

        return when (contentResolver.getType(uri)) {
            "audio/ogg" -> "ogg"
            "audio/mpeg" -> "mp3"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/midi", "audio/x-midi" -> "mid"
            "audio/mp4", "audio/x-m4a" -> "m4a"
            else -> "ogg"
        }
    }

    override fun onDestroy() {
        previewEngine.release()
        super.onDestroy()
    }

    private companion object {
        val supportedExtensions = setOf("ogg", "wav", "mp3", "mid", "m4a")
    }
}
