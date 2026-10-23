package com.infinity.audex_revivel.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.infinity.audex_revivel.BuildConfig
import com.infinity.audex_revivel.R
import com.infinity.audex_revivel.core.update.AppUpdater
import com.infinity.audex_revivel.core.update.UpdateRelease
import java.io.File

class UpdateActivity : AudexActivity() {
    private lateinit var status: TextView
    private var waitingFile: File? = null
    private var dialogShown = false
    private val permission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val file = waitingFile
        if (file != null && (Build.VERSION.SDK_INT < 26 || packageManager.canRequestPackageInstalls())) install(file)
        else status.setText(R.string.update_install_permission)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        waitingFile = savedInstanceState?.getString("file")?.let(::File)
        status = TextView(this).apply { setPadding(32, 64, 32, 32); setText(R.string.update_checking) }
        setContentView(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(status) })
        val cached = AppUpdater.cached(this)
        if (cached != null && intent.getBooleanExtra("cached", true)) showRelease(cached)
        else AppUpdater.check(this) { release, error ->
            if (isDestroyed || isFinishing) return@check
            when {
                error != null -> status.text = getString(R.string.update_failed, error.message.orEmpty())
                release == null -> status.setText(R.string.update_current)
                else -> showRelease(release)
            }
        }
    }
    private fun showRelease(release: UpdateRelease) {
        if (dialogShown) return
        dialogShown = true
        status.text = getString(R.string.update_available, release.tag)
        val notes = TextView(this).apply {
            text = "${release.tag}\n\n${release.notes.ifBlank { getString(R.string.update_no_notes) }}"
            setPadding(32, 16, 32, 16)
        }
        MaterialAlertDialogBuilder(this).setTitle(R.string.update_title)
            .setView(ScrollView(this).apply { addView(notes) })
            .setNegativeButton(R.string.update_later) { _, _ -> finish() }
            .setPositiveButton(R.string.update_now) { _, _ ->
                status.setText(R.string.update_downloading)
                AppUpdater.download(this, release) { file, error ->
                    if (isDestroyed || isFinishing) return@download
                    if (file == null) status.text = getString(R.string.update_failed, error?.message.orEmpty())
                    else runCatching { validate(file); requestInstall(file) }
                        .onFailure { file.delete(); status.text = getString(R.string.update_failed, it.message.orEmpty()) }
                }
            }.setOnCancelListener { finish() }.show()
    }
    @Suppress("DEPRECATION")
    private fun validate(file: File) {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = packageManager.getPackageArchiveInfo(file.absolutePath, flags) ?: error("The download is not a valid Android app.")
        require(archive.packageName == packageName) { "The update belongs to another app." }
        val version = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()
        require(version > BuildConfig.VERSION_CODE) { "The release does not contain a newer build." }
        val installed = packageManager.getPackageInfo(packageName, flags)
        val candidate = if (Build.VERSION.SDK_INT >= 28) archive.signingInfo?.apkContentsSigners else archive.signatures
        val trusted = if (Build.VERSION.SDK_INT >= 28) installed.signingInfo?.signingCertificateHistory else installed.signatures
        require(!candidate.isNullOrEmpty() && !trusted.isNullOrEmpty() && candidate.all { trusted.contains(it) }) {
            "The update signing key does not match this installation."
        }
    }
    private fun requestInstall(file: File) {
        waitingFile = file
        if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
            status.setText(R.string.update_install_permission)
            permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
        } else install(file)
    }
    private fun install(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.themes", file)
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            status.setText(R.string.update_installer_open)
            waitingFile = null
        } catch (_: ActivityNotFoundException) { status.setText(R.string.update_no_installer) }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("file", waitingFile?.absolutePath)
        super.onSaveInstanceState(outState)
    }
}
