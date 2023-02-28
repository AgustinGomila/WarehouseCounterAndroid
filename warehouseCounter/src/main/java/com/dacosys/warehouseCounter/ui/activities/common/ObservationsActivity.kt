package com.dacosys.warehouseCounter.ui.activities.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.databinding.ObservationsActivityBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.room.entity.item.Item
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType

class ObservationsActivity : AppCompatActivity(), Scanner.ScannerListener {
    private var obs = ""
    private var autoText = ""
    private val newLine = "\n"
    private val divisionChar = ";"

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI(view: View) {
        // Set up touch listener for non-text box views to hide keyboard.
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

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString("obs", binding.obsEditText.text.trim().toString())
    }

    private lateinit var binding: ObservationsActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Statics.setScreenRotation(this)
        binding = ObservationsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = getString(R.string.observations)

        if (savedInstanceState != null) {
            val t1 = savedInstanceState.getString("obs")
            if (t1 != null) {
                obs = t1
            }
        }

        val extras = intent.extras
        if (extras != null) {
            val t1 = extras.getString("obs")
            if (t1 != null) {
                obs = t1
            }
        }

        binding.okButton.setOnClickListener { confirmObs() }
        binding.cancelButton.setOnClickListener { cancelObs() }

        fillControls()

        // Conectar el lectura NFC
        if (WarehouseCounterApp.settingViewModel.useNfc) {
            Nfc.setupNFCReader(this)
        }

        // ESTO SIRVE PARA OCULTAR EL TECLADO EN PANTALLA CUANDO PIERDEN EL FOCO LOS CONTROLES QUE LO NECESITAN
        setupUI(binding.root)
    }

    private fun confirmObs() {
        Statics.closeKeyboard(this)

        val data = Intent()
        data.putExtra("obs", binding.obsEditText.text.trim().toString())
        setResult(RESULT_OK, data)
        finish()
    }

    private fun fillControls() {
        runOnUiThread {
            binding.obsEditText.setText(obs, TextView.BufferType.EDITABLE)
        }
    }

    private fun cancelObs() {
        Statics.closeKeyboard(this)

        setResult(RESULT_CANCELED, null)
        finish()
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

    private fun addAutoText(code: String) {
        var tempCode = code
        val tempPos: Int = tempCode.indexOf(divisionChar)

        if (tempPos >= 0) {
            tempCode = try {
                val tempLabelNumber: String = tempCode.substring(tempPos + 1)
                tempCode.substring(0, tempCode.length - (tempLabelNumber.length + 1))
            } catch (ex: Exception) {
                code
            }
        }

        autoText = ""

        ItemCoroutines().getByQuery(tempCode) {
            var item: Item? = null
            if (it.size > 0) item = it.first()

            var description = ""
            if (item != null) description = ", " + item.description

            if (binding.codePasteSwitch.isChecked) {
                autoText += tempCode + description + newLine
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT)) JotterListener.onRequestPermissionsResult(
            this, requestCode, permissions, grantResults
        )
    }

    override fun scannerCompleted(scanCode: String) {
        JotterListener.lockScanner(this, true)

        try {
            addAutoText(scanCode)
            val obsText = binding.obsEditText.text.toString() + autoText

            runOnUiThread {
                binding.obsEditText.setText(obsText, TextView.BufferType.EDITABLE)
                binding.obsEditText.setSelection(binding.obsEditText.text.length)
                binding.obsEditText.scrollTo(0, binding.obsEditText.bottom)
            }

            makeText(binding.root, getString(R.string.ok), SnackBarType.SUCCESS)
        } catch (ex: Exception) {
            ex.printStackTrace()
            makeText(binding.root, ex.message.toString(), SnackBarType.ERROR)
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
        } finally {
            // Unless is blocked, unlock the partial
            JotterListener.lockScanner(this, false)
        }
    }

    override fun onNewIntent(intent: Intent) {
        /*
          This method gets called, when a new Intent gets associated with the current activity instance.
          Instead of creating a new activity, onNewIntent will be called. For more information have a look
          at the documentation.

          In our case this method gets called, when the user attaches a className to the device.
         */
        super.onNewIntent(intent)
        Nfc.nfcHandleIntent(intent, this)
    }

    override fun onBackPressed() {
        Statics.closeKeyboard(this)

        setResult(RESULT_CANCELED)
        finish()
    }
}