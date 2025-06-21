package com.dacosys.warehouseCounter.ui.activities.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestContent
import com.dacosys.warehouseCounter.databinding.EnterCodeActivityBinding
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.devices.rfid.Rfid
import com.dacosys.warehouseCounter.scanners.devices.vh75.Vh75Bt
import com.dacosys.warehouseCounter.scanners.jotter.ScannerManager
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.utils.Screen.Companion.closeKeyboard
import com.dacosys.warehouseCounter.ui.utils.TextViewUtils.Companion.isActionDone
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent


class EnterCodeActivity : AppCompatActivity(), Scanner.ScannerListener, Rfid.RfidDeviceListener {
    private var capSentences: Boolean = false
    private var orc: OrderRequestContent? = null

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString(ARG_TITLE, title.toString())
        savedInstanceState.putString(ARG_HINT, binding.codeEditText.hint.toString())
        savedInstanceState.putParcelable(ARG_ORC, orc)
        savedInstanceState.putString(ARG_DEFAULT_VALUE, binding.codeEditText.editableText.toString().trim())
        savedInstanceState.putBoolean(ARG_CAP_SENTENCES, capSentences)
    }

    private lateinit var binding: EnterCodeActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = EnterCodeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Permite finalizar la actividad si se toca la pantalla
        // fuera de la ventana. Esta actividad se ve como un diálogo.
        setFinishOnTouchOutside(true)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isBackPressed()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        var tempTitle = getString(R.string.enter_the_code)
        var tempHint = getString(R.string.item_code)
        var defaultValue = ""

        if (savedInstanceState != null) {
            val t1 = savedInstanceState.getString(ARG_TITLE)
            if (!t1.isNullOrEmpty()) tempTitle = t1

            val t2 = savedInstanceState.getString(ARG_HINT)
            if (!t2.isNullOrEmpty()) tempHint = t2

            orc = savedInstanceState.parcelable(ARG_ORC)

            capSentences = savedInstanceState.getBoolean(ARG_CAP_SENTENCES)

            defaultValue = savedInstanceState.getString(ARG_DEFAULT_VALUE) ?: ""
        } else {
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString(ARG_TITLE)
                if (!t1.isNullOrEmpty()) tempTitle = t1

                val t2 = extras.getString(ARG_HINT)
                if (!t2.isNullOrEmpty()) tempHint = t2

                val t3 = extras.parcelable(ARG_ORC) as OrderRequestContent?
                if (t3 != null) orc = t3

                capSentences = extras.getBoolean(ARG_CAP_SENTENCES)

                defaultValue = extras.getString(ARG_DEFAULT_VALUE) ?: ""
            }
        }

        title = tempTitle

        binding.okButton.setOnClickListener { codeSelect() }

        binding.codeEditText.hint = tempHint

        if (capSentences) {
            binding.codeEditText.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        binding.codeEditText.setOnEditorActionListener { _, actionId, event ->
            if (isActionDone(actionId, event)) {
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
                Screen.showKeyboard(this)
            }
        }

        binding.enterCode.setOnClickListener {
            isBackPressed()
        }

        binding.codeEditText.isCursorVisible = true
        binding.codeEditText.isFocusable = true
        binding.codeEditText.isFocusableInTouchMode = true
        binding.codeEditText.requestFocus()
    }

    private fun codeSelect() {
        closeKeyboard(this)

        val data = Intent()
        val result: Int = if (binding.codeEditText.editableText.toString().isEmpty()) {
            RESULT_CANCELED
        } else {
            RESULT_OK
        }

        data.putExtra(ARG_CODE, binding.codeEditText.editableText.toString().trim())
        data.putExtra(ARG_ORC, orc)
        setResult(result, data)
        finish()
    }

    private fun isBackPressed() {
        closeKeyboard(this)

        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
            ScannerManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun scannerCompleted(scanCode: String) {
        if (settingsVm.showScannedCode) showMessage(scanCode, SnackBarType.INFO)

        binding.codeEditText.setText(scanCode)
    }

    private fun showMessage(msg: String, type: SnackBarType) {
        if (isFinishing || isDestroyed) return
        if (type == ERROR) Log.e(javaClass.simpleName, msg)
        makeText(binding.root, msg, type)
    }

    override fun onReadCompleted(scanCode: String) {
        scannerCompleted(scanCode)
    }

    override fun onGetBluetoothName(name: String) {}

    override fun onWriteCompleted(isOk: Boolean) {}

    override fun onStateChanged(state: Int) {
        if (!::binding.isInitialized || isFinishing || isDestroyed) return
        if (settingsVm.rfidShowConnectedMessage) {
            when (Rfid.vh75State) {
                Vh75Bt.STATE_CONNECTED -> showMessage(getString(R.string.rfid_connected), SnackBarType.SUCCESS)
                Vh75Bt.STATE_CONNECTING -> showMessage(
                    getString(R.string.searching_rfid_reader),
                    SnackBarType.RUNNING
                )

                else -> showMessage(getString(R.string.there_is_no_rfid_device_connected), SnackBarType.INFO)
            }
        }
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_HINT = "hint"
        const val ARG_CAP_SENTENCES = "captFirst"
        const val ARG_ORC = "orc"
        const val ARG_CODE = "code"
        const val ARG_DEFAULT_VALUE = "defaultValue"
    }
}
