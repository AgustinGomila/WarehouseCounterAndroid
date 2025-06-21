package com.dacosys.warehouseCounter.ui.activities.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.data.room.entity.item.Item
import com.dacosys.warehouseCounter.databinding.ObservationsActivityBinding
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.devices.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.jotter.ScannerManager
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.utils.Screen

class ObservationActivity : AppCompatActivity(), Scanner.ScannerListener {
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

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString(ARG_OBS, binding.obsEditText.text.trim().toString())
    }

    private lateinit var binding: ObservationsActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = ObservationsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isBackPressed()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        title = getString(R.string.observations)

        if (savedInstanceState != null) {
            val t1 = savedInstanceState.getString(ARG_OBS)
            if (t1 != null) obs = t1
        }

        val extras = intent.extras
        if (extras != null) {
            val t1 = extras.getString(ARG_OBS)
            if (t1 != null) obs = t1
        }

        binding.okButton.setOnClickListener { confirmObs() }
        binding.cancelButton.setOnClickListener { cancelObs() }

        fillControls()

        if (settingsVm.useNfc) Nfc.setupNFCReader(this)

        Screen.setupUI(binding.root, this)
    }

    private fun confirmObs() {
        Screen.closeKeyboard(this)

        val data = Intent()
        data.putExtra(ARG_OBS, binding.obsEditText.text.trim().toString())
        setResult(RESULT_OK, data)
        finish()
    }

    private fun fillControls() {
        runOnUiThread {
            binding.obsEditText.setText(obs, TextView.BufferType.EDITABLE)
        }
    }

    private fun cancelObs() {
        Screen.closeKeyboard(this)

        setResult(RESULT_CANCELED, null)
        finish()
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

        ItemCoroutines.getByQuery(tempCode) {
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
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
            ScannerManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun scannerCompleted(scanCode: String) {
        ScannerManager.lockScanner(this, true)

        try {
            addAutoText(scanCode)
            val obsText = binding.obsEditText.text.toString() + autoText

            runOnUiThread {
                binding.obsEditText.setText(obsText, TextView.BufferType.EDITABLE)
                binding.obsEditText.setSelection(binding.obsEditText.text.length)
                binding.obsEditText.scrollTo(0, binding.obsEditText.bottom)
            }

            showMessage(getString(R.string.ok), SnackBarType.SUCCESS)
        } catch (ex: Exception) {
            ex.printStackTrace()
            showMessage(ex.message.toString(), ERROR)
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
        } finally {
            // Unless is blocked, unlock the partial
            ScannerManager.lockScanner(this, false)
        }
    }

    private fun showMessage(msg: String, type: SnackBarType) {
        if (isFinishing || isDestroyed) return
        if (type == ERROR) Log.e(javaClass.simpleName, msg)
        makeText(binding.root, msg, type)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Nfc.nfcHandleIntent(intent, this)
    }

    private fun isBackPressed() {
        Screen.closeKeyboard(this)

        setResult(RESULT_CANCELED)
        finish()
    }

    companion object {
        const val ARG_OBS = "obs"
    }
}
