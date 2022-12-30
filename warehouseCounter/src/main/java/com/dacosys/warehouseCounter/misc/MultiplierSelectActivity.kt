package com.dacosys.warehouseCounter.misc

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.ScanMultiplierSelectBinding
import org.parceler.Parcels

class MultiplierSelectActivity : AppCompatActivity() {
    private var tempMultiplier: Int? = null

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString("title", title.toString())
        savedInstanceState.putInt("tempMultiplier", tempMultiplier ?: 1)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI(view: View) {
        // Set up touch checkedChangedListener for non-text box views to hide keyboard.
        if (view !is EditText) {
            view.setOnTouchListener { _, motionEvent ->
                Statics.closeKeyboard(this)
                if (view is Button && view !is Switch && view !is CheckBox) {
                    touchButton(motionEvent, view)
                    true
                } else {
                    false
                }
            }
        }

        //If a layout container, iterate over children and seed recursion.
        if (view is ViewGroup) {
            (0 until view.childCount).map { view.getChildAt(it) }.forEach { setupUI(it) }
        }
    }

    private lateinit var binding: ScanMultiplierSelectBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Statics.setScreenRotation(this)
        binding = ScanMultiplierSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Permite finalizar la actividad si se toca la pantalla
        // fuera de la ventana. Esta actividad se ve como un diálogo.
        setFinishOnTouchOutside(true)

        var tempTitle = getString(R.string.select_multiplier)

        if (savedInstanceState != null) {
            val t1 = savedInstanceState.getString("title")
            if (t1 != null && t1.isNotEmpty()) tempTitle = t1

            tempMultiplier = savedInstanceState.getInt("tempMultiplier")
        } else {
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString("title")
                if (t1 != null && t1.isNotEmpty()) tempTitle = t1

                tempMultiplier = extras.getInt("multiplier")
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

        // ESTO SIRVE PARA OCULTAR EL TECLADO EN PANTALLA CUANDO PIERDEN EL FOCO LOS CONTROLES QUE LO NECESITAN
        setupUI(binding.multiplierSelect)
    }

    private fun touchButton(motionEvent: MotionEvent, button: Button) {
        when (motionEvent.action) {
            MotionEvent.ACTION_UP -> {
                button.isPressed = false
                button.performClick()
            }
            MotionEvent.ACTION_DOWN -> {
                button.isPressed = true
            }
        }
    }

    private fun multiplierSelect() {
        Statics.closeKeyboard(this)

        val data = Intent()
        if (tempMultiplier == null) {
            setResult(RESULT_CANCELED, null)
            finish()
        } else {
            settingViewModel().scanMultiplier = tempMultiplier!!

            data.putExtra("multiplier", Parcels.wrap(tempMultiplier))
            setResult(RESULT_OK, data)
            finish()
        }
    }

    override fun onBackPressed() {
        Statics.closeKeyboard(this)

        setResult(RESULT_CANCELED)
        finish()
    }
}