package com.dacosys.warehouseCounter.ui.activities.codeCheck

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.data.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.data.room.entity.item.Item
import com.dacosys.warehouseCounter.data.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.databinding.CodeCheckActivityBinding
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.scanners.LifecycleListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.ui.fragments.item.ItemDetailFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.utils.Screen
import java.util.*

class CodeCheckActivity : AppCompatActivity(), Scanner.ScannerListener, Rfid.RfidDeviceListener {

    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    private var rejectNewInstances = false

    private var currentFragment: androidx.fragment.app.Fragment? = null

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString(ARG_CODE, binding.codeEditText.text.toString())
    }

    private lateinit var binding: CodeCheckActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = CodeCheckActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState != null) {
            binding.codeEditText.setText(
                savedInstanceState.getString(ARG_CODE), TextView.BufferType.EDITABLE
            )
        }

        binding.codeEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                fillHexTextView()
                true
            } else {
                false
            }
        }

        if (savedInstanceState == null) {
            clearControls()
        } else {
            fillHexTextView()
        }

        binding.codeEditText.requestFocus()

        Screen.setupUI(binding.root, this)
    }

    private fun fillPanel(fragment: androidx.fragment.app.Fragment?) {
        if (isFinishing) return

        var newFragment: androidx.fragment.app.Fragment? = null
        if (fragment != null) {
            newFragment = fragment
        }

        var oldFragment: androidx.fragment.app.Fragment? = null
        if (currentFragment != null) {
            oldFragment = currentFragment
        }

        var fragmentTransaction = supportFragmentManager.beginTransaction()
        if (oldFragment != null) {
            try {
                if (!isFinishing) fragmentTransaction.remove(oldFragment).commitAllowingStateLoss()
            } catch (ignore: java.lang.Exception) {
            }
        }

        // Close keyboard in transition
        if (currentFocus != null) {
            val inputManager = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(
                (currentFocus ?: return).windowToken, InputMethodManager.HIDE_NOT_ALWAYS
            )
        }

        if (newFragment != null) {
            runOnUiThread {
                fragmentTransaction = supportFragmentManager.beginTransaction()
                fragmentTransaction.setCustomAnimations(
                    R.anim.animation_fade_in, R.anim.animation_fade_out
                )
                try {
                    if (!isFinishing) fragmentTransaction.replace(
                        binding.fragmentLayout.id, newFragment
                    ).commitAllowingStateLoss()
                } catch (ignore: java.lang.Exception) {
                }
            }
        }

        currentFragment = fragment
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_read_activity, menu)

        if (!settingsVm.useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home, android.R.id.home -> {
                @Suppress("DEPRECATION") onBackPressed()
                return true
            }

            R.id.action_rfid_connect -> {
                LifecycleListener.rfidStart(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_trigger_scan -> {
                LifecycleListener.trigger(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_read_barcode -> {
                LifecycleListener.toggleCameraFloatingWindowVisibility(this)
                return super.onOptionsItemSelected(item)
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    private fun clearControls() {
        fillPanel(null)
        binding.infoTextView.setText("", TextView.BufferType.EDITABLE)
    }

    private fun fillHexTextView() {
        clearControls()

        val scannedCode = binding.codeEditText.text.toString()

        if (scannedCode.trim().isEmpty()) return

        val buf = StringBuilder(200)
        for (ch in scannedCode.toCharArray()) {
            if (buf.isNotEmpty()) buf.append(' ')
            buf.append(String.format("%04x", ch.code).uppercase(Locale.getDefault()))
        }

        binding.hexTextView.text = buf.toString()
        Screen.closeKeyboard(this)

        // Nada que hacer, volver
        if (scannedCode.isEmpty()) {
            val res = getString(R.string.invalid_code)
            showSnackBar(res, ERROR)
            Log.d(tag, res)
            return
        }

        LifecycleListener.lockScanner(this, true)

        var itemObj: Item? = null
        var itemCode: ItemCode? = null

        ItemCoroutines.getByQuery(scannedCode) {
            if (it.size > 0) itemObj = it.first()

            if (itemObj == null) {
                // ¿No está? Buscar en la tabla item_code de la base de datos
                ItemCodeCoroutines.getByCode(scannedCode) { it2 ->
                    if (it2.size > 0) itemCode = it2.first()

                    try {
                        when {
                            itemObj != null -> {
                                fillPanel(ItemDetailFragment.newInstance(itemObj!!))
                                binding.infoTextView.setText(
                                    R.string.the_code_belongs_to_an_item,
                                    TextView.BufferType.EDITABLE
                                )
                            }

                            itemCode != null -> {
                                fillPanel(ItemDetailFragment.newInstance(itemCode!!))
                                binding.infoTextView.setText(
                                    R.string.the_code_belongs_to_an_item_code,
                                    TextView.BufferType.EDITABLE
                                )
                            }

                            else -> {
                                fillPanel(null)
                                binding.infoTextView.setText(
                                    R.string.the_code_does_not_belong_to_any_iem_in_the_database,
                                    TextView.BufferType.EDITABLE
                                )
                            }
                        }
                    } catch (ex: Exception) {
                        showSnackBar(ex.message.toString(), ERROR)
                        ErrorLog.writeLog(this, tag, ex.message.toString())
                    } finally {
                        LifecycleListener.lockScanner(this, false)
                    }
                }
            } else {
                LifecycleListener.lockScanner(this, false)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT)) LifecycleListener.onRequestPermissionsResult(
            this, requestCode, permissions, grantResults
        )
    }

    override fun scannerCompleted(scanCode: String) {
        runOnUiThread {
            binding.codeEditText.setText(scanCode)
            binding.codeEditText.dispatchKeyEvent(
                KeyEvent(
                    0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        rejectNewInstances = false
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_CODE = "code"

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }

    // region READERS Reception

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Nfc.nfcHandleIntent(intent, this)
    }

    override fun onGetBluetoothName(name: String) {}

    override fun onWriteCompleted(isOk: Boolean) {}

    override fun onReadCompleted(scanCode: String) {
        scannerCompleted(scanCode)
    }

    //endregion READERS Reception
}
