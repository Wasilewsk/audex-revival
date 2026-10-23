package com.infinity.audex_revivel.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Reproduces getScrollPosition / getScrollPercent / playback rate from
 * AudexService in the original app. Kept pure so it is unit-testable.
 */
object ScrollMath {
    fun position(fromIndex: Int, itemCount: Int, scrollY: Int, maxScrollY: Int): Float {
        if (fromIndex >= 0 && itemCount > 0) {
            return fromIndex.toFloat() / itemCount.toFloat()
        }
        if (scrollY >= 0 && maxScrollY > 0) {
            return scrollY.toFloat() / maxScrollY.toFloat()
        }
        if (scrollY >= 0 && itemCount > 0 && scrollY <= itemCount) {
            return scrollY.toFloat() / itemCount.toFloat()
        }
        return 0.5f
    }

    fun percent(fromIndex: Int, itemCount: Int, scrollY: Int, maxScrollY: Int): Float {
        val raw = position(fromIndex, itemCount, scrollY, maxScrollY)
        return 100f * max(0f, min(1f, raw))
    }

    /** 2^(percent / 50 - 1), the pitch ramp used for scroll earcons. */
    fun rateForPercent(percent: Float): Float {
        val rate = 2.0.pow((percent.toDouble() / 50.0) - 1.0).toFloat()
        return rate.coerceIn(0.5f, 2f)
    }
}
