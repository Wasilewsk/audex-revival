package com.infinity.audex_revivel

import com.infinity.audex_revivel.core.ScrollMath
import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollMathTest {
    @Test
    fun fromIndexTakesPriority() {
        assertEquals(0.25f, ScrollMath.position(fromIndex = 1, itemCount = 4, scrollY = 40, maxScrollY = 100), 0.001f)
    }

    @Test
    fun scrollYAndMaxAreUsedWhenNoIndex() {
        assertEquals(0.25f, ScrollMath.position(fromIndex = -1, itemCount = 0, scrollY = 50, maxScrollY = 200), 0.001f)
    }

    @Test
    fun scrollYAndItemCountAreTheThirdOption() {
        assertEquals(0.5f, ScrollMath.position(fromIndex = -1, itemCount = 4, scrollY = 2, maxScrollY = 0), 0.001f)
    }

    @Test
    fun unknownPositionFallsBackToMiddle() {
        assertEquals(0.5f, ScrollMath.position(fromIndex = -1, itemCount = 0, scrollY = -1, maxScrollY = 0), 0.001f)
    }

    @Test
    fun percentClampsToRange() {
        assertEquals(25f, ScrollMath.percent(1, 4, 0, 0), 0.001f)
        assertEquals(50f, ScrollMath.percent(-1, 0, 10, 0), 0.001f)
    }

    @Test
    fun rateRampsFromHalfToDouble() {
        assertEquals(0.5f, ScrollMath.rateForPercent(0f), 0.001f)
        assertEquals(1f, ScrollMath.rateForPercent(50f), 0.001f)
        assertEquals(2f, ScrollMath.rateForPercent(100f), 0.001f)
    }
}
