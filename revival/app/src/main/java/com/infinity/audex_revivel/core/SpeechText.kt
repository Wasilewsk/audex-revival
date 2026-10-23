package com.infinity.audex_revivel.core

import java.text.DateFormat
import java.util.Date
import java.util.Locale

object SpeechText {
    fun battery(level: Int): String = "Battery $level percent"

    fun time(now: Date = Date(), locale: Locale = Locale.getDefault()): String {
        val formatted = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(now)
        return "The time is $formatted"
    }
}
