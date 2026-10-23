package com.infinity.audex_revivel.ui

import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar

abstract class AudexActivity : AppCompatActivity() {
    protected fun applyWindowInsets(root: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
    }

    protected fun setUpToolbar(toolbar: MaterialToolbar, showBack: Boolean) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(showBack)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        toolbar.setNavigationOnClickListener {
            if (showBack) onBackPressedDispatcher.onBackPressed()
        }
    }

    protected fun announce(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
