package com.infinity.audex_revivel.core

enum class AudexPriority(val percent: Int) {
    TWENTY(20),
    FORTY(40),
    SIXTY(60),
    EIGHTY(80),
    HUNDRED(100);

    companion object {
        fun fromPercent(value: Int): AudexPriority =
            entries.firstOrNull { it.percent == value } ?: HUNDRED
    }
}
