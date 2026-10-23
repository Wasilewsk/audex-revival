package com.infinity.audex_revivel.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class Haptics(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun click() {
        vibrate(longArrayOf(0, 30), repeat = -1)
    }

    fun startup() {
        vibrate(longArrayOf(25, 50, 100, 100, 100), repeat = -1)
    }

    @Suppress("DEPRECATION")
    private fun vibrate(pattern: LongArray, repeat: Int) {
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                device.vibrate(VibrationEffect.createWaveform(pattern, repeat))
            } else {
                device.vibrate(pattern, repeat)
            }
        }
    }
}
