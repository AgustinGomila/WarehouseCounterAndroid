package com.dacosys.warehouseCounter.ui.activities.common

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.ScanMultiplierSelectBinding
import com.dacosys.warehouseCounter.ui.utils.Screen
import org.parceler.Parcels

class MultiplierSelectActivity : AppCompatActivity() {
    private var tempMultiplier: Int? = null

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString(ARG_TITLE, title.toString())
        savedInstanceState.putInt(ARG_MULTIPLIER, tempMultiplier ?: 1)
    }

    private lateinit var binding: ScanMultiplierSelectBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = ScanMultiplierSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Permite finalizar la actividad si se toca la pantalla
        // fuera de la ventana. Esta actividad se ve como un diálogo.
        setFinishOnTouchOutside(true)

        var tempTitle = getString(R.string.select_multiplier)

        if (savedInstanceState != null) {
            val t1 = savedInstanceState.getString(ARG_TITLE)
            if (!t1.isNullOrEmpty()) tempTitle = t1

            tempMultiplier = savedInstanceState.getInt(ARG_MULTIPLIER)
        } else {
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString(ARG_TITLE)
                if (!t1.isNullOrEmpty()) tempTitle = t1

                tempMultiplier = extras.getInt(ARG_MULTIPLIER)
            }
        }

        title = tempTitle

        binding.selectButton.setOnClickListener { multiplierSelect() }

        binding.multiplierSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (progress == 0) {
                    binding.multiplierSeekBar.progress = 1
                } else {
                    if (fromUser) {
                        tempMultiplier = progress
                        binding.multiplierTextView.text = String.format("%sX", tempMultiplier)
                    }
                }
            }
        })

        binding.multiplierSeekBar.max = 100
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.multiplierSeekBar.min = 1
        }
        binding.multiplierSeekBar.progress = tempMultiplier ?: 1

        binding.multiplierTextView.text = String.format("%sX", tempMultiplier)

        binding.multiplierSelect.setOnClickListener { onBackPressed() }

        Screen.setupUI(binding.root, this)
    }

    private fun multiplierSelect() {
        Screen.closeKeyboard(this)

        val data = Intent()
        if (tempMultiplier == null) {
            setResult(RESULT_CANCELED, null)
            finish()
        } else {
            settingViewModel.scanMultiplier = tempMultiplier!!

            data.putExtra(ARG_MULTIPLIER, Parcels.wrap(tempMultiplier))
            setResult(RESULT_OK, data)
            finish()
        }
    }

    override fun onBackPressed() {
        Screen.closeKeyboard(this)

        setResult(RESULT_CANCELED)
        finish()
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_MULTIPLIER = "multiplier"
    }
}
