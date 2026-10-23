package com.infinity.audex_revivel.ui

import android.os.Bundle
import android.view.ViewGroup
import com.google.android.material.radiobutton.MaterialRadioButton
import com.infinity.audex_revivel.R
import com.infinity.audex_revivel.core.AudexPreferences
import com.infinity.audex_revivel.databinding.ActivityVolumeBinding

class VolumeSettingsActivity : AudexActivity() {
    private lateinit var binding: ActivityVolumeBinding
    private lateinit var preferences: AudexPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVolumeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(binding.root)
        setUpToolbar(binding.toolbar, showBack = true)

        preferences = AudexPreferences(this)
        buildVolumeOptions()
    }

    private fun buildVolumeOptions() {
        val currentPercent = preferences.volumePercent
        for (percent in 10..100 step 10) {
            val button = MaterialRadioButton(this).apply {
                text = if (percent == 100) {
                    getString(R.string.volume_match)
                } else {
                    getString(R.string.volume_option, percent)
                }
                minHeight = resources.getDimensionPixelSize(R.dimen.volume_option_min_height)
                isChecked = percent == currentPercent
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }
            binding.volumeGroup.addView(button)
            button.setOnCheckedChangeListener { _, checked ->
                if (!checked) return@setOnCheckedChangeListener
                preferences.volume = percent / 100f
                announce(
                    getString(
                        if (percent == 100) R.string.volume_set_match else R.string.volume_set,
                        percent,
                    ),
                )
            }
        }
    }
}
