package com.infinity.audex_revivel.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.infinity.audex_revivel.R
import com.infinity.audex_revivel.core.AudexEvent
import com.infinity.audex_revivel.core.AudexPreferences
import com.infinity.audex_revivel.core.SchemeChoice
import com.infinity.audex_revivel.core.SoundEngine
import com.infinity.audex_revivel.core.SoundScheme
import com.infinity.audex_revivel.core.applySchemeChoice
import com.infinity.audex_revivel.core.schemeChoices
import com.infinity.audex_revivel.core.selectedSchemeIndex
import com.infinity.audex_revivel.core.theme.ThemeExporter
import com.infinity.audex_revivel.databinding.ActivityEventsBinding
import com.infinity.audex_revivel.databinding.ItemEventBinding
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class EventSettingsActivity : AudexActivity() {
    private lateinit var binding: ActivityEventsBinding
    private lateinit var preferences: AudexPreferences
    private lateinit var previewEngine: SoundEngine
    private val rows = LinkedHashMap<AudexEvent, ItemEventBinding>()
    private val exportExecutor = Executors.newSingleThreadExecutor()
    private var pendingEvent: AudexEvent? = null
    private var updatingUi = false
    private var choices: List<SchemeChoice> = emptyList()

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
        refreshSchemeSpinner()
        buildEventRows()
        binding.shareThemeButton.setOnClickListener { showExportDialog() }
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        previewEngine.load(preferences)
        refreshSchemeSpinner()
        refreshUi()
    }

    private fun setupSchemeSpinner() {
        binding.schemeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                if (updatingUi) return
                val choice = choices.getOrNull(position) ?: return
                val alreadySelected = if (choice.themeId != null) {
                    preferences.activeThemeId == choice.themeId
                } else {
                    preferences.activeThemeId == null && preferences.scheme == choice.scheme
                }
                if (alreadySelected) return

                applySchemeChoice(preferences, choice)
                previewEngine.load(preferences)
                refreshUi()
                announce(getString(R.string.scheme_changed, choice.label))
                when (choice.scheme) {
                    SoundScheme.CUSTOM -> announce(getString(R.string.scheme_custom_hint))
                    SoundScheme.SHARED -> announce(getString(R.string.scheme_shared_hint))
                    else -> Unit
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun refreshSchemeSpinner() {
        updatingUi = true
        try {
            choices = schemeChoices(this, preferences)
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                choices.map { it.label },
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.schemeSpinner.adapter = adapter
            binding.schemeSpinner.setSelection(selectedSchemeIndex(preferences, choices), false)
        } finally {
            updatingUi = false
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
            rows.forEach { (event, row) ->
                row.eventSwitch.isChecked = preferences.isEventEnabled(event)
                row.chooseButton.isVisible = preferences.scheme == SoundScheme.CUSTOM
            }
            when (preferences.scheme) {
                SoundScheme.CUSTOM -> {
                    binding.schemeInfo.isVisible = true
                    binding.schemeInfo.setText(R.string.scheme_custom_hint)
                    binding.shareThemeButton.isVisible = true
                }

                SoundScheme.SHARED -> {
                    binding.schemeInfo.isVisible = true
                    binding.schemeInfo.setText(R.string.scheme_shared_hint)
                    binding.shareThemeButton.isVisible = false
                }

                SoundScheme.THEME -> {
                    val themeId = preferences.activeThemeId
                    val name = choices.firstOrNull { it.themeId == themeId }?.themeName
                        ?: themeId.orEmpty()
                    binding.schemeInfo.isVisible = true
                    binding.schemeInfo.text = getString(R.string.scheme_theme_hint, name)
                    binding.shareThemeButton.isVisible = false
                }

                else -> {
                    binding.schemeInfo.isVisible = false
                    binding.shareThemeButton.isVisible = false
                }
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

    private fun showExportDialog() {
        if (preferences.scheme != SoundScheme.CUSTOM) {
            announce(getString(R.string.scheme_custom_hint))
            return
        }
        val click = preferences.customSoundPath(AudexEvent.CLICK)?.let(::File)
        if (click == null || !click.isFile) {
            announce(getString(R.string.share_theme_no_click))
            return
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
        }
        val nameField = addTextField(container, R.string.share_theme_name)
        val authorField = addTextField(container, R.string.share_theme_author)
        val descriptionField = addTextField(container, R.string.share_theme_description, multiline = true)
        val licenseField = addTextField(
            container,
            R.string.share_theme_license,
            initial = "CC0-1.0",
            helper = getString(R.string.share_theme_license_hint),
        )
        val scroll = ScrollView(this).apply { addView(container) }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.share_theme_title)
            .setView(scroll)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.share_theme_share, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = nameField.editText?.text?.toString()?.trim().orEmpty()
                val author = authorField.editText?.text?.toString()?.trim().orEmpty()
                val description = descriptionField.editText?.text?.toString()?.trim().orEmpty()
                val license = licenseField.editText?.text?.toString()?.trim().orEmpty()
                if (name.isEmpty() || author.isEmpty() || license.isEmpty()) {
                    announce(getString(R.string.share_theme_required))
                    return@setOnClickListener
                }
                dialog.dismiss()
                exportTheme(
                    name = name,
                    author = author,
                    description = description.ifEmpty {
                        getString(R.string.share_theme_default_description)
                    },
                    license = license,
                )
            }
        }
        dialog.show()
    }

    private fun addTextField(
        container: LinearLayout,
        @StringRes hint: Int,
        initial: String = "",
        multiline: Boolean = false,
        helper: String? = null,
    ): TextInputLayout {
        val layout = TextInputLayout(this).apply {
            this.hint = getString(hint)
            helperText = helper
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val edit = TextInputEditText(this).apply {
            setText(initial)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                if (multiline) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0
            isSingleLine = !multiline
            minLines = if (multiline) 2 else 1
            maxLines = if (multiline) 4 else 1
        }
        layout.addView(edit)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(8) }
        container.addView(layout, params)
        return layout
    }

    private fun exportTheme(name: String, author: String, description: String, license: String) {
        val included = AudexEvent.entries.count { event ->
            preferences.customSoundPath(event)?.let(::File)?.isFile == true
        }
        exportExecutor.execute {
            val result = runCatching {
                ThemeExporter(this).export(preferences, name, author, description, license)
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.onSuccess { file -> showExportReady(file, included) }
                    .onFailure { error ->
                        announce(
                            getString(
                                R.string.share_theme_failed,
                                error.message ?: getString(R.string.theme_error_unknown),
                            ),
                        )
                    }
            }
        }
    }

    private fun showExportReady(file: File, includedSounds: Int) {
        val uri = FileProvider.getUriForFile(this, "$packageName.themes", file)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.share_theme_ready_title)
            .setMessage(
                getString(R.string.share_theme_ready_message, includedSounds, file.name),
            )
            .setPositiveButton(R.string.share_theme_share) { _, _ -> shareTheme(uri) }
            .setNeutralButton(R.string.share_theme_upload) { _, _ -> openUploadPage() }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun shareTheme(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(getString(R.string.share_theme_chooser), uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(
                Intent.createChooser(intent, getString(R.string.share_theme_chooser)),
            )
        } catch (_: ActivityNotFoundException) {
            announce(getString(R.string.theme_store_open_failed))
        }
    }

    private fun openUploadPage() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UPLOAD_URL)))
        } catch (_: ActivityNotFoundException) {
            announce(getString(R.string.theme_store_open_failed))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    override fun onDestroy() {
        exportExecutor.shutdownNow()
        previewEngine.release()
        super.onDestroy()
    }

    private companion object {
        val supportedExtensions = setOf("ogg", "wav", "mp3", "mid", "m4a")
        const val UPLOAD_URL = "https://github.com/Wasilewsk/audex-themes/upload/main/themes"
    }
}
