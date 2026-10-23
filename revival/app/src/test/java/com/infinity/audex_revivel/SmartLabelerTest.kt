package com.infinity.audex_revivel

import com.infinity.audex_revivel.core.SmartLabeler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmartLabelerTest {
    @Test
    fun buttonLabelUsesWordsAndKind() {
        val label = SmartLabeler.derive("com.example:id/save_button", "android.widget.Button")
        assertEquals("Save button", label?.label)
        assertEquals("com.example:id/save_button", label?.viewId)
    }

    @Test
    fun editTextBecomesEditBox() {
        assertEquals(
            "Email address edit box",
            SmartLabeler.derive("com.example:id/email_address", "android.widget.EditText")?.label,
        )
    }

    @Test
    fun radioButtonIsNotCollapsedIntoButton() {
        assertEquals(
            "Option 1 radio button",
            SmartLabeler.derive("com.example:id/option_1", "android.widget.RadioButton")?.label,
        )
    }

    @Test
    fun spinnerBecomesDropdown() {
        assertEquals(
            "Size dropdown",
            SmartLabeler.derive("com.example:id/size", "android.widget.Spinner")?.label,
        )
    }

    @Test
    fun viewWithoutAnIdIsNotLabeled() {
        assertNull(SmartLabeler.derive(null, "android.widget.Button"))
        assertNull(SmartLabeler.derive("", "android.widget.Button"))
        assertNull(SmartLabeler.derive("com.example:id/", "android.widget.Button"))
    }

    @Test
    fun unsupportedClassIsNotLabeled() {
        assertNull(SmartLabeler.derive("com.example:id/container", "android.widget.LinearLayout"))
        assertNull(SmartLabeler.derive("com.example:id/view", "android.view.View"))
    }
}
