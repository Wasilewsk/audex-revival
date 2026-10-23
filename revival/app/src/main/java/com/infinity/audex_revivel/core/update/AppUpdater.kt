package com.infinity.audex_revivel.core.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.infinity.audex_revivel.BuildConfig
import com.infinity.audex_revivel.R
import com.infinity.audex_revivel.ui.UpdateActivity
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object AppUpdater {
    const val AUTO = "automatic_updates"
    private const val CACHED = "available_release"
    private const val INTERVAL = 5 * 60 * 1000L
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var context: Context? = null
    private var checking = false
    private fun prefs(context: Context) = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
    fun automatic(context: Context) = prefs(context).getBoolean(AUTO, true)
    fun setAutomatic(context: Context, enabled: Boolean) { prefs(context).edit().putBoolean(AUTO, enabled).apply() }
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == AUTO) { handler.removeCallbacks(tick); context?.let { if (automatic(it)) handler.post(tick) } }
    }
    private val tick = object : Runnable {
        override fun run() {
            val app = context ?: return
            if (!automatic(app)) return
            check(app) { release, _ -> if (release != null && context != null && automatic(app)) notify(app, release) }
            handler.postDelayed(this, INTERVAL)
        }
    }
    fun start(context: Context) {
        stop()
        this.context = context.applicationContext
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
        if (automatic(context)) handler.post(tick)
    }
    fun stop() {
        context?.let { prefs(it).unregisterOnSharedPreferenceChangeListener(listener) }
        context = null
        handler.removeCallbacks(tick)
    }
    fun cached(context: Context): UpdateRelease? = runCatching {
        prefs(context).getString(CACHED, null)?.let(UpdateRelease::parse)
            ?.takeIf { it.isNewer(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE) }
    }.getOrNull()
    fun check(context: Context, result: (UpdateRelease?, Throwable?) -> Unit) {
        if (checking) { result(null, IllegalStateException("An update check is already running.")); return }
        checking = true
        val app = context.applicationContext
        executor.execute {
            val outcome = runCatching {
                val text = request("https://api.github.com/repos/Wasilewsk/audex-revival/releases/latest") { connection ->
                    require(connection.responseCode == 200) { "The releases page could not be reached (${connection.responseCode})." }
                    connection.inputStream.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= 1024 * 1024) { "Release information is too large." }
                            output.write(buffer, 0, count)
                        }
                        val bytes = output.toByteArray()
                        require(bytes.size <= 1024 * 1024) { "Release information is too large." }
                        bytes.toString(Charsets.UTF_8)
                    }
                }
                val release = UpdateRelease.parse(text)?.takeIf { it.isNewer(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE) }
                prefs(app).edit().apply { if (release == null) remove(CACHED) else putString(CACHED, text) }.apply()
                release
            }
            handler.post { checking = false; result(outcome.getOrNull(), outcome.exceptionOrNull()) }
        }
    }
    private fun notify(context: Context, release: UpdateRelease) {
        val prefs = prefs(context)
        if (prefs.getString("notified", null) == release.tag) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("updates", context.getString(R.string.update_channel), NotificationManager.IMPORTANCE_DEFAULT))
        }
        val intent = Intent(context, UpdateActivity::class.java)
        val pending = PendingIntent.getActivity(context, 1024, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            manager.notify(1024, NotificationCompat.Builder(context, "updates").setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(context.getString(R.string.update_title)).setContentText(release.tag)
                .setContentIntent(pending).setAutoCancel(true).build())
            prefs.edit().putString("notified", release.tag).apply()
        } catch (_: SecurityException) { /* The cached update remains available in settings. */ }
    }
    fun download(context: Context, release: UpdateRelease, result: (File?, Throwable?) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            val target = File(app.cacheDir, "updates/audex-update.apk")
            val outcome = runCatching {
                target.parentFile?.mkdirs()
                request(release.url) { connection ->
                    require(connection.responseCode == 200) { "Update download failed." }
                    var total = 0L
                    connection.inputStream.use { input -> target.outputStream().use { output ->
                        val buffer = ByteArray(32768)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= release.size) { "Update size does not match the release." }
                            output.write(buffer, 0, count)
                        }
                    } }
                    require(total == release.size) { "The update download was incomplete." }
                }
                target
            }
            if (outcome.isFailure) target.delete()
            handler.post { result(outcome.getOrNull(), outcome.exceptionOrNull()) }
        }
    }
    private fun <T> request(url: String, block: (HttpURLConnection) -> T): T {
        var next = URL(url)
        repeat(6) {
            require(next.protocol == "https") { "Updates require HTTPS." }
            val connection = (next.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000; readTimeout = 20000; instanceFollowRedirects = false
                setRequestProperty("User-Agent", "AudexRevival/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            try {
                if (connection.responseCode in listOf(301, 302, 303, 307, 308)) {
                    next = URL(next, connection.getHeaderField("Location") ?: error("Missing download redirect."))
                } else return block(connection)
            } finally { connection.disconnect() }
        }
        error("Too many download redirects.")
    }
}
