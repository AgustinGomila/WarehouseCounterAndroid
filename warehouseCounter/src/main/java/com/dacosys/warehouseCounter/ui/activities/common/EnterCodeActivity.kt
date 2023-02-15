package com.dacosys.warehouseCounter.ui.activities.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.EnterCodeActivityBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.showKeyboard
import com.dacosys.warehouseCounter.model.orderRequest.OrderRequestContent
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.parceler.Parcels


class EnterCodeActivity : AppCompatActivity(), Scanner.ScannerListener, Rfid.RfidDeviceListener {
    private var orc: OrderRequestContent? = null

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString("title", title.toString())
        savedInstanceState.putString("hint", binding.codeEditText.hint.toString())
        savedInstanceState.putParcelable("orc", orc)
        savedInstanceState.putString(
            "defaultValue", binding.codeEditText.editableText.toString().trim()
        )
    }

    private lateinit var binding: EnterCodeActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Statics.setScreenRotation(this)
        binding = EnterCodeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Permite finalizar la actividad si se toca la pantalla
        // fuera de la ventana. Esta actividad se ve como un diálogo.
        setFinishOnTouchOutside(true)

        var tempTitle = getString(R.string.enter_the_code)
        var tempHint = getString(R.string.item_code)
        var defaultValue = ""

        if (savedInstanceState != null) {
            val t1 = savedInstanceState.getString("title")
            if (t1 != null && t1.isNotEmpty()) tempTitle = t1

            val t2 = savedInstanceState.getString("hint")
            if (t2 != null && t2.isNotEmpty()) {
                tempHint = t2
            }

            orc = savedInstanceState.getParcelable("orc")

            defaultValue = savedInstanceState.getString("defaultValue") ?: ""
        } else {
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString("title")
                if (t1 != null && t1.isNotEmpty()) tempTitle = t1

                val t2 = extras.getString("hint")
                if (t2 != null && t2.isNotEmpty()) {
                    tempHint = t2
                }

                val t3 = extras.getParcelable("orc") as OrderRequestContent?
                if (t3 != null) {
                    orc = t3
                }

                defaultValue = extras.getString("defaultValue") ?: ""
            }
        }

        title = tempTitle

        binding.okButton.setOnClickListener { codeSelect() }

        binding.codeEditText.hint = tempHint
        binding.codeEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                codeSelect()
                true
            } else {
                false
            }
        }
        binding.codeEditText.clearFocus()
        binding.codeEditText.setText(defaultValue)

        binding.codeEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !KeyboardVisibilityEvent.isKeyboardVisible(this)) {
                showKeyboard(this)
            }
        }

        binding.enterCode.setOnClickListener { onBackPressed() }

        binding.codeEditText.isCursorVisible = true
        binding.codeEditText.isFocusable = true
        binding.codeEditText.isFocusableInTouchMode = true
        binding.codeEditText.requestFocus()
    }

    private fun codeSelect() {
        val data = Intent()
        val result: Int = if (binding.codeEditText.editableText.toString().isEmpty()) {
            RESULT_CANCELED
        } else {
            RESULT_OK
        }

        data.putExtra("code", binding.codeEditText.editableText.toString().trim())
        data.putExtra("orc", Parcels.wrap(orc))
        setResult(result, data)
        finish()
    }

    override fun onBackPressed() {
        Statics.closeKeyboard(this)

        setResult(RESULT_CANCELED)
        finish()
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
        if (settingViewModel().showScannedCode) makeText(binding.root, scanCode, SnackBarType.INFO)

        binding.codeEditText.setText(scanCode)
    }

    override fun onReadCompleted(scanCode: String) {
        scannerCompleted(scanCode)
    }

    override fun onWriteCompleted(isOk: Boolean) {}

    override fun onGetBluetoothName(name: String) {}
}