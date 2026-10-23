package com.infinity.audex_revivel.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.infinity.audex_revivel.R
import com.infinity.audex_revivel.core.AudexEvent
import com.infinity.audex_revivel.core.SoundScheme
import com.infinity.audex_revivel.core.builtInSoundRes
import com.infinity.audex_revivel.core.theme.ThemeDraft
import com.infinity.audex_revivel.core.theme.ThemeManifest
import com.infinity.audex_revivel.core.theme.ThemeStore
import java.io.File
import java.util.UUID

class ThemeCreatorActivity : AudexActivity() {
    private lateinit var draft: ThemeDraft
    private lateinit var rows: LinearLayout
    private var pendingEvent: AudexEvent? = null
    private var player: MediaPlayer? = null
    private var exportFile: File? = null
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val event = pendingEvent
        pendingEvent = null
        if (uri != null && event != null) runCatching {
            val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }.orEmpty()
            val extension = name.substringAfterLast('.', "").lowercase().ifEmpty {
                when (contentResolver.getType(uri)) {
                    "audio/mpeg" -> "mp3"
                    "audio/wav", "audio/x-wav" -> "wav"
                    "audio/mp4", "audio/x-m4a" -> "m4a"
                    "audio/midi", "audio/x-midi" -> "mid"
                    "audio/ogg" -> "ogg"
                    else -> ""
                }
            }
            val decoder = MediaPlayer.create(this, uri) ?: error("Unsupported sound")
            decoder.release()
            draft.replaceSound(event, extension, contentResolver.openInputStream(uri) ?: error("Cannot read sound"))
            getSharedPreferences("studio-labels", MODE_PRIVATE).edit()
                .putString("${draft.manifest.id}:${event.name}", name.ifBlank { "${event.themeKey}.$extension" }).apply()
            renderRows()
            announce(getString(R.string.custom_sound_selected, getString(event.labelRes)))
        }.onFailure { announce(getString(R.string.creator_file_error)) }
    }
    private val exportPicker = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) runCatching {
            val file = exportFile ?: error("Save the theme again before exporting.")
            contentResolver.openOutputStream(uri)?.use { output -> file.inputStream().use { it.copyTo(output) } }
                ?: error("Could not write the theme file.")
            announce(getString(R.string.creator_exported))
            MaterialAlertDialogBuilder(this).setMessage(R.string.creator_upload_hint)
                .setPositiveButton(R.string.creator_submission) { _, _ -> openSubmission() }
                .setNegativeButton(R.string.close, null).show()
        }.onFailure { showError(it) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra("draft") ?: run { finish(); return }
        if (!ThemeManifest.isValidId(id)) { finish(); return }
        draft = runCatching { ThemeDraft(File(filesDir, "theme-drafts/$id")) }.getOrElse { finish(); return }
        pendingEvent = savedInstanceState?.getString("event")?.let { AudexEvent.valueOf(it) }
        exportFile = savedInstanceState?.getString("export")?.let(::File)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val toolbar = MaterialToolbar(this).apply { title = getString(R.string.theme_creator_title) }
        root.addView(toolbar)
        val scroll = ScrollView(this)
        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)) }
        scroll.addView(rows)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        applyWindowInsets(root)
        setUpToolbar(toolbar, true)
        renderRows()
    }
    private fun renderRows() {
        rows.removeAllViews()
        text(draft.manifest.name, true)
        text(getString(R.string.creator_hint))
        button(getString(R.string.creator_save)) { saveTheme() }
        button(getString(R.string.creator_upload)) { uploadDetails() }
        for (event in AudexEvent.entries) {
            val label = getString(event.labelRes)
            text(label, true)
            val assigned = getSharedPreferences("studio-labels", MODE_PRIVATE)
                .getString("${draft.manifest.id}:${event.name}", getString(R.string.creator_default)).orEmpty()
            text(getString(R.string.creator_assigned, assigned))
            button(getString(R.string.creator_preview), getString(R.string.creator_preview_event, label)) { preview(event) }
            button(getString(R.string.creator_change), getString(R.string.creator_change_event, label)) {
                stopPreview(); pendingEvent = event; picker.launch(arrayOf("audio/*"))
            }
        }
    }
    private fun text(value: String, heading: Boolean = false) {
        rows.addView(TextView(this).apply {
            text = value
            setTextAppearance(if (heading) com.google.android.material.R.style.TextAppearance_Material3_TitleMedium
                else com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setPadding(0, dp(12), 0, dp(4))
            ViewCompat.setAccessibilityHeading(this, heading)
        })
    }
    private fun button(label: String, description: String = label, action: () -> Unit) {
        rows.addView(MaterialButton(this).apply {
            text = label; contentDescription = description; minHeight = dp(48)
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(-1, -2))
    }
    private fun preview(event: AudexEvent) {
        runCatching {
            stopPreview()
            player = MediaPlayer.create(this, Uri.fromFile(draft.sound(event))) ?: error("Cannot play sound")
            player?.setOnCompletionListener { stopPreview() }
            player?.setOnErrorListener { _, _, _ -> stopPreview(); announce(getString(R.string.sound_not_loaded)); true }
            player?.start()
        }.onFailure { stopPreview(); announce(getString(R.string.sound_not_loaded)) }
    }
    private fun saveTheme(): File? = runCatching {
        val file = File(getExternalFilesDir(null) ?: filesDir, "exports/${draft.manifest.id}-1.0.0.audextheme")
        draft.export(file)
        ThemeStore(this).install(file, draft.manifest.id)
        com.infinity.audex_revivel.core.AudexPreferences(this).edit()
            .putLong(com.infinity.audex_revivel.core.AudexPreferences.KEY_THEME_CONTENT, System.nanoTime()).apply()
        exportFile = file
        announce(getString(R.string.creator_saved))
        file
    }.onFailure { showError(it) }.getOrNull()
    private fun uploadDetails() {
        val fields = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), 0, dp(24), 0) }
        fun field(label: Int, value: String): TextInputLayout {
            val layout = TextInputLayout(this).apply { hint = getString(label) }
            layout.addView(TextInputEditText(this).apply { setText(value); isSingleLine = true })
            fields.addView(layout)
            return layout
        }
        val author = field(R.string.share_theme_author, draft.manifest.author.takeUnless { it == "Local creator" }.orEmpty())
        val license = field(R.string.share_theme_license, draft.manifest.license.takeUnless { it == "Private use" }.orEmpty())
        val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.creator_upload)
            .setMessage(R.string.creator_upload_hint).setView(fields)
            .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.creator_export, null).create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val a = ThemeManifest.sanitizeText(author.editText?.text.toString())
                val l = ThemeManifest.sanitizeText(license.editText?.text.toString())
                if (a.isBlank() || l.isBlank() || a.length > 80 || l.length > 40) {
                    announce(getString(R.string.creator_author_required)); return@setOnClickListener
                }
                try { draft.update(draft.manifest.copy(author = a, license = l)) }
                catch (error: Exception) { showError(error); return@setOnClickListener }
                val file = saveTheme() ?: return@setOnClickListener
                dialog.dismiss(); exportPicker.launch(file.name)
            }
        }
        dialog.show()
    }
    private fun openSubmission() {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Wasilewsk/audex-themes/upload/main/themes"))) }
        catch (_: ActivityNotFoundException) { announce(getString(R.string.theme_store_open_failed)) }
    }
    private fun showError(error: Throwable) { announce(getString(R.string.creator_failed, error.message.orEmpty())) }
    private fun stopPreview() { player?.release(); player = null }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onStop() { stopPreview(); super.onStop() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("event", pendingEvent?.name); outState.putString("export", exportFile?.absolutePath)
        super.onSaveInstanceState(outState)
    }
    companion object {
        fun promptNew(activity: Activity) {
            val layout = TextInputLayout(activity).apply { hint = activity.getString(R.string.creator_name); setPadding(24, 8, 24, 0) }
            val edit = TextInputEditText(activity).apply { isSingleLine = true }
            layout.addView(edit)
            val dialog = MaterialAlertDialogBuilder(activity).setTitle(R.string.create_new_theme).setView(layout)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.creator_okay, null).create()
            dialog.setOnShowListener {
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val name = ThemeManifest.sanitizeText(edit.text.toString())
                    if (name.isEmpty() || name.length > 60) { layout.error = activity.getString(R.string.creator_name_required); return@setOnClickListener }
                    val id = "studio-${UUID.randomUUID().toString().replace("-", "").take(24)}"
                    val dir = File(activity.filesDir, "theme-drafts/$id")
                    runCatching {
                        check(File(dir, "sounds").mkdirs())
                        val sounds = AudexEvent.entries.associateWith { event ->
                            val path = "sounds/${event.themeKey}.ogg"
                            activity.resources.openRawResource(builtInSoundRes(SoundScheme.INNOVEX, event)).use { source ->
                                File(dir, path).outputStream().use { source.copyTo(it) }
                            }; path
                        }
                        val manifest = ThemeManifest(id, name, "Local creator", "Created in Audex theme creator", "1.0.0", "Private use", sounds)
                        File(dir, ThemeManifest.FILE_NAME).writeText(manifest.toJson())
                        activity.startActivity(Intent(activity, ThemeCreatorActivity::class.java).putExtra("draft", id)); dialog.dismiss()
                    }.onFailure { dir.deleteRecursively(); layout.error = activity.getString(R.string.creator_failed, it.message.orEmpty()) }
                }
            }; dialog.show()
        }
        fun chooseDraft(activity: Activity) {
            val drafts = File(activity.filesDir, "theme-drafts").listFiles().orEmpty()
                .mapNotNull { runCatching { ThemeDraft(it) }.getOrNull() }.sortedBy { it.manifest.name }
            if (drafts.isEmpty()) { MaterialAlertDialogBuilder(activity).setMessage(R.string.creator_empty).setPositiveButton(R.string.close, null).show(); return }
            MaterialAlertDialogBuilder(activity).setTitle(R.string.edit_created_theme)
                .setItems(drafts.map { it.manifest.name }.toTypedArray()) { _, index ->
                    activity.startActivity(Intent(activity, ThemeCreatorActivity::class.java).putExtra("draft", drafts[index].manifest.id))
                }.setNegativeButton(R.string.cancel, null).show()
        }
    }
}
