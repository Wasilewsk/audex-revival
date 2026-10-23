package com.infinity.audex_revivel.core

data class ControlLabel(val viewId: String, val label: String)

/**
 * Turns an unlabeled control's view id and class into a spoken label.
 *
 * The original app concatenated the class suffix directly onto the id, which
 * produced strings such as "save Button". This version normalises the id to
 * words and appends a lowercase control kind.
 */
object SmartLabeler {
    private val kinds = listOf(
        "RadioButton" to "radio button",
        "CheckBox" to "checkbox",
        "Checkbox" to "checkbox",
        "Switch" to "switch",
        "EditText" to "edit box",
        "Spinner" to "dropdown",
        "ImageButton" to "button",
        "ImageView" to "button",
        "Button" to "button",
    )

    fun derive(viewId: String?, className: String?): ControlLabel? {
        if (viewId.isNullOrBlank() || className.isNullOrBlank()) return null
        val marker = viewId.indexOf(":id/")
        if (marker < 0) return null
        val rawName = viewId.substring(marker + 4)
        if (rawName.isBlank()) return null

        val kind = kinds.firstOrNull { className.contains(it.first, ignoreCase = true) }?.second
            ?: return null

        val words = rawName
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        if (words.isBlank()) return null

        val base = words.replaceFirstChar { it.uppercase() }
        val label = if (base.endsWith(" $kind", ignoreCase = true)) base else "$base $kind"
        return ControlLabel(viewId, label)
    }
}
