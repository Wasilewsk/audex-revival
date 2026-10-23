package com.infinity.audex_revivel.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.infinity.audex_revivel.BuildConfig
import com.infinity.audex_revivel.R
import com.infinity.audex_revivel.core.AudexEvent
import com.infinity.audex_revivel.core.AudexPreferences
import com.infinity.audex_revivel.core.SoundScheme
import com.infinity.audex_revivel.core.theme.InstalledTheme
import com.infinity.audex_revivel.core.theme.ThemeListing
import com.infinity.audex_revivel.core.theme.ThemeStore
import com.infinity.audex_revivel.core.theme.ThemeStoreClient
import com.infinity.audex_revivel.databinding.ActivityThemeStoreBinding
import com.infinity.audex_revivel.databinding.ItemThemeBinding
import java.io.File
import java.util.concurrent.Executors

class ThemeStoreActivity : AudexActivity() {
    private lateinit var binding: ActivityThemeStoreBinding
    private lateinit var preferences: AudexPreferences
    private lateinit var store: ThemeStore
    private lateinit var client: ThemeStoreClient

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var listings: List<ThemeListing> = emptyList()
    private var loading = false
    private var busyThemeId: String? = null
    private var previewPlayer: MediaPlayer? = null
    private var destroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(binding.root)
        setUpToolbar(binding.toolbar, showBack = true)

        preferences = AudexPreferences(this)
        store = ThemeStore(this)
        client = ThemeStoreClient()

        binding.refreshButton.setOnClickListener { loadStore() }
        binding.uploadButton.setOnClickListener { openUploadPage() }
        loadStore()
    }

    override fun onResume() {
        super.onResume()
        if (!loading && listings.isNotEmpty()) render()
    }

    override fun onDestroy() {
        destroyed = true
        previewPlayer?.release()
        previewPlayer = null
        executor.shutdownNow()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun loadStore() {
        if (loading) return
        loading = true
        binding.progress.isVisible = true
        binding.refreshButton.isEnabled = false
        binding.statusText.setText(R.string.theme_store_loading)
        executor.execute {
            val result = runCatching { client.fetchIndex() }
            handler.post {
                if (destroyed) return@post
                loading = false
                binding.progress.isVisible = false
                binding.refreshButton.isEnabled = true
                result.onSuccess { index ->
                    listings = index.themes
                    render()
                }.onFailure {
                    listings = emptyList()
                    binding.themeContainer.removeAllViews()
                    binding.statusText.setText(R.string.theme_store_error)
                    announce(getString(R.string.theme_store_error))
                }
            }
        }
    }

    private fun render() {
        binding.themeContainer.removeAllViews()
        val installed = store.installedThemes().associateBy { it.manifest.id }
        binding.statusText.text = if (listings.isEmpty()) {
            getString(R.string.theme_store_empty)
        } else {
            getString(R.string.theme_store_count, listings.size)
        }

        for (listing in listings) {
            val row = ItemThemeBinding.inflate(layoutInflater, binding.themeContainer, false)
            val installedTheme = installed[listing.id]
            val active = preferences.activeThemeId == listing.id
            val tooNew = listing.minAppVersion > BuildConfig.VERSION_CODE
            val busy = busyThemeId == listing.id

            row.themeName.text = listing.name
            row.themeMeta.text = getString(
                R.string.theme_store_meta,
                listing.author,
                listing.sounds,
                listing.version,
            )
            row.themeDescription.text = listing.description

            configurePrimaryButton(row, listing, installedTheme, active, tooNew, busy)
            row.previewButton.isVisible = installedTheme != null
            row.previewButton.isEnabled = !busy
            row.previewButton.contentDescription =
                getString(R.string.theme_preview_description, listing.name)
            row.previewButton.setOnClickListener { previewTheme(listing) }
            row.removeButton.isVisible = installedTheme != null
            row.removeButton.isEnabled = !busy
            row.removeButton.contentDescription =
                getString(R.string.theme_remove_description, listing.name)
            row.removeButton.setOnClickListener { confirmRemove(listing) }

            binding.themeContainer.addView(row.root)
        }
    }

    private fun configurePrimaryButton(
        row: ItemThemeBinding,
        listing: ThemeListing,
        installed: InstalledTheme?,
        active: Boolean,
        tooNew: Boolean,
        busy: Boolean,
    ) {
        val hasUpdate = installed != null && installed.manifest.version != listing.version
        when {
            busy -> {
                row.primaryButton.setText(R.string.theme_downloading)
                row.primaryButton.isEnabled = false
            }

            tooNew -> {
                row.primaryButton.setText(R.string.theme_install)
                row.primaryButton.isEnabled = false
                row.primaryButton.contentDescription =
                    getString(R.string.theme_needs_update_description, listing.name)
                return
            }

            installed == null -> {
                row.primaryButton.setText(R.string.theme_install)
                row.primaryButton.isEnabled = true
                row.primaryButton.contentDescription =
                    getString(R.string.theme_install_description, listing.name)
            }

            hasUpdate -> {
                row.primaryButton.setText(R.string.theme_update)
                row.primaryButton.isEnabled = true
                row.primaryButton.contentDescription =
                    getString(R.string.theme_update_description, listing.name)
            }

            active -> {
                row.primaryButton.setText(R.string.theme_applied)
                row.primaryButton.isEnabled = false
                row.primaryButton.contentDescription =
                    getString(R.string.theme_applied_description, listing.name)
            }

            else -> {
                row.primaryButton.setText(R.string.theme_apply)
                row.primaryButton.isEnabled = true
                row.primaryButton.contentDescription =
                    getString(R.string.theme_apply_description, listing.name)
            }
        }
        row.primaryButton.setOnClickListener {
            when {
                tooNew -> announce(getString(R.string.theme_needs_update))
                installed == null || hasUpdate -> install(listing, reapply = active)
                active -> announce(getString(R.string.theme_already_applied, listing.name))
                else -> applyTheme(listing)
            }
        }
    }

    private fun install(listing: ThemeListing, reapply: Boolean) {
        if (busyThemeId != null) return
        busyThemeId = listing.id
        render()
        binding.statusText.setText(R.string.theme_downloading)

        val cacheFile = File(cacheDir, "theme-${listing.id}.audextheme")
        executor.execute {
            val result = runCatching {
                client.download(listing, cacheFile)
                store.install(cacheFile, listing.id)
            }
            cacheFile.delete()
            handler.post {
                busyThemeId = null
                if (destroyed) return@post
                result.onSuccess {
                    if (reapply) preferences.selectTheme(listing.id)
                    Log.i(TAG, "Installed theme ${listing.id} ${listing.version}")
                    announce(getString(R.string.theme_installed, listing.name))
                    render()
                }.onFailure { error ->
                    Log.w(TAG, "Could not install ${listing.id}", error)
                    val message = getString(
                        R.string.theme_install_failed,
                        listing.name,
                        error.message ?: getString(R.string.theme_error_unknown),
                    )
                    announce(message)
                    render()
                    binding.statusText.text = message
                }
            }
        }
    }

    private fun applyTheme(listing: ThemeListing) {
        preferences.selectTheme(listing.id)
        announce(getString(R.string.theme_applied_message, listing.name))
        render()
    }

    private fun confirmRemove(listing: ThemeListing) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.theme_remove_confirm_title, listing.name))
            .setMessage(R.string.theme_remove_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.theme_remove) { _, _ ->
                val wasActive = preferences.activeThemeId == listing.id
                if (store.remove(listing.id)) {
                    if (wasActive) preferences.scheme = SoundScheme.INNOVEX
                    announce(getString(R.string.theme_removed, listing.name))
                    render()
                } else {
                    announce(getString(R.string.theme_remove_failed, listing.name))
                }
            }
            .show()
    }

    private fun previewTheme(listing: ThemeListing) {
        val file = store.soundFile(listing.id, AudexEvent.CLICK)
        if (file == null) {
            announce(getString(R.string.theme_preview_failed))
            return
        }
        runCatching {
            previewPlayer?.release()
            val player = MediaPlayer.create(this, Uri.fromFile(file))
                ?: throw IllegalStateException("No player")
            val volume = preferences.volume
            player.setVolume(volume, volume)
            player.setOnCompletionListener {
                it.release()
                if (previewPlayer === it) previewPlayer = null
            }
            player.start()
            previewPlayer = player
            announce(getString(R.string.theme_previewing, listing.name))
        }.onFailure {
            announce(getString(R.string.theme_preview_failed))
        }
    }

    private fun openUploadPage() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UPLOAD_URL)))
        } catch (_: ActivityNotFoundException) {
            announce(getString(R.string.theme_store_open_failed))
        }
    }

    private companion object {
        const val TAG = "ThemeStore"
        const val UPLOAD_URL = "https://github.com/Wasilewsk/audex-themes/upload/main/themes"
    }
}
