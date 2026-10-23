package com.infinity.audex_revivel.core

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.util.Collections
import java.util.EnumMap

/**
 * Loads one sample per event for the active scheme and plays it at the
 * configured volume. Sounds that cannot be resolved fall back to Innovex so a
 * missing custom file never leaves an event silent.
 */
class SoundEngine(context: Context) {
    private val appContext = context.applicationContext
    private var pool: SoundPool? = null
    private val samples = EnumMap<AudexEvent, Int>(AudexEvent::class.java)
    private val loadedSamples: MutableSet<Int> = Collections.synchronizedSet(mutableSetOf())
    private val pendingPreviews: MutableSet<Int> = Collections.synchronizedSet(mutableSetOf())

    @Volatile
    var volume: Float = 1f

    @Volatile
    var enabled: Boolean = true

    private var preferences: AudexPreferences? = null

    @Synchronized
    fun load(preferences: AudexPreferences) {
        this.preferences = preferences
        this.volume = if (preferences.suspended) 0f else preferences.volume
        recreatePool()

        for (event in AudexEvent.entries) {
            val sampleId = loadSample(event, preferences.scheme)
            if (sampleId != 0) samples[event] = sampleId
        }
    }

    private fun recreatePool() {
        pool?.release()
        samples.clear()
        loadedSamples.clear()
        pendingPreviews.clear()

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val newPool = SoundPool.Builder()
            .setAudioAttributes(attributes)
            .setMaxStreams(10)
            .build()

        newPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSamples.add(sampleId)
                if (pendingPreviews.remove(sampleId)) {
                    playSample(sampleId, 1f)
                }
            }
        }
        pool = newPool
    }

    private fun loadSample(event: AudexEvent, scheme: SoundScheme): Int {
        val preferences = preferences ?: return 0
        return when (scheme) {
            SoundScheme.SHARED -> loadShared(event, preferences) ?: loadBuiltIn(SoundScheme.INNOVEX, event)
            SoundScheme.CUSTOM -> loadCustom(event, preferences) ?: loadBuiltIn(SoundScheme.INNOVEX, event)
            else -> loadBuiltIn(scheme, event)
        }
    }

    private fun loadBuiltIn(scheme: SoundScheme, event: AudexEvent): Int {
        val resId = builtInSoundRes(scheme, event)
        if (resId == 0) return 0
        return pool?.load(appContext, resId, 1) ?: 0
    }

    private fun loadCustom(event: AudexEvent, preferences: AudexPreferences): Int? {
        val path = preferences.customSoundPath(event) ?: return null
        if (!File(path).isFile) return null
        val id = pool?.load(path, 1) ?: 0
        return id.takeIf { it != 0 }
    }

    private fun loadShared(event: AudexEvent, preferences: AudexPreferences): Int? {
        val directory = preferences.sharedSoundsDir()
        if (!directory.isDirectory) return null
        for (extension in soundFileExtensions) {
            val file = File(directory, event.sharedFileName + extension)
            if (file.isFile) {
                val id = pool?.load(file.absolutePath, 1) ?: 0
                if (id != 0) return id
            }
        }
        return null
    }

    fun play(event: AudexEvent, rate: Float = 1f) {
        if (!enabled || volume <= 0f) return
        val sampleId = samples[event] ?: return
        if (sampleId !in loadedSamples) return
        playSample(sampleId, rate)
    }

    /**
     * Plays an event for a settings preview. If the sample is still loading the
     * playback is queued so the first tap is not silently dropped.
     */
    fun preview(event: AudexEvent): Boolean {
        val sampleId = samples[event] ?: return false
        if (sampleId in loadedSamples) {
            playSample(sampleId, 1f)
            return true
        }
        pendingPreviews.add(sampleId)
        return true
    }

    private fun playSample(sampleId: Int, rate: Float) {
        if (!enabled) return
        pool?.play(sampleId, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun stopAll() {
        pool?.autoPause()
        pendingPreviews.clear()
    }

    fun resume() {
        pool?.autoResume()
    }

    @Synchronized
    fun release() {
        pool?.release()
        pool = null
        samples.clear()
        loadedSamples.clear()
        pendingPreviews.clear()
    }
}
