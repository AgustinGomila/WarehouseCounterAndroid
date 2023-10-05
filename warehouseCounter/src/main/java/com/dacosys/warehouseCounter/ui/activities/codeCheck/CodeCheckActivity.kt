package com.dacosys.warehouseCounter.ui.activities.codeCheck

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Rack
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.GetRack
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.GetWarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.GetOrder
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.data.room.entity.item.Item
import com.dacosys.warehouseCounter.databinding.CodeCheckActivityBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.scanners.LifecycleListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.PREFIX_ITEM
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.PREFIX_ITEM_URL
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.PREFIX_ORDER
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.PREFIX_RACK
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.PREFIX_WA
import com.dacosys.warehouseCounter.ui.fragments.item.ItemDetailFragment
import com.dacosys.warehouseCounter.ui.fragments.location.RackDetailFragment
import com.dacosys.warehouseCounter.ui.fragments.location.WarehouseAreaDetailFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.Item as ItemKtor

class CodeCheckActivity : AppCompatActivity(), Scanner.ScannerListener, Rfid.RfidDeviceListener {

    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    private var rejectNewInstances = false

    private var currentFragment: Fragment? = null

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

    private fun fillPanel(fragment: Fragment?) {
        if (isFinishing) return

        var newFragment: Fragment? = null
        if (fragment != null) {
            newFragment = fragment
        }

        var oldFragment: Fragment? = null
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

        if (BuildConfig.DEBUG || Statics.TEST_MODE) {
            menu.add(Menu.NONE, menuItemManualCode, Menu.NONE, "Manual code")
            menu.add(Menu.NONE, menuItemRandomEan, Menu.NONE, "Random EAN")
            menu.add(Menu.NONE, menuItemRandomIt, Menu.NONE, "Random item ID")
            menu.add(Menu.NONE, menuItemRandomItUrl, Menu.NONE, "Random item ID URL")
            menu.add(Menu.NONE, menuItemRandomOrder, Menu.NONE, "Random order")
            menu.add(Menu.NONE, menuItemRandomRack, Menu.NONE, "Random rack")
            menu.add(Menu.NONE, menuItemRandomWa, Menu.NONE, "Random area")
        }

        return true
    }

    private val menuItemManualCode = 999001
    private val menuItemRandomEan = 999002
    private val menuItemRandomIt = 999003
    private val menuItemRandomItUrl = 999004
    private val menuItemRandomOrder = 999005
    private val menuItemRandomRack = 999006
    private val menuItemRandomWa = 999007

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

            menuItemRandomEan -> {
                ItemCoroutines.getEanCodes(true) {
                    if (it.any()) scannerCompleted(it[Random().nextInt(it.count())])
                }
                return super.onOptionsItemSelected(item)
            }

            menuItemRandomIt -> {
                ItemCoroutines.getIds(true) {
                    if (it.any()) scannerCompleted("$PREFIX_ITEM${it[Random().nextInt(it.count())]}#")
                }
                return super.onOptionsItemSelected(item)
            }

            menuItemRandomItUrl -> {
                ItemCoroutines.getIds(true) {
                    if (it.any()) scannerCompleted("$PREFIX_ITEM_URL${it[Random().nextInt(it.count())]}")
                }
                return super.onOptionsItemSelected(item)
            }

            menuItemRandomRack -> {
                GetRack(onFinish = {
                    if (it.any()) scannerCompleted("$PREFIX_RACK${it[Random().nextInt(it.count())].id}#")
                }).execute()
                return super.onOptionsItemSelected(item)
            }

            menuItemRandomWa -> {
                GetWarehouseArea(onFinish = {
                    if (it.any()) scannerCompleted("$PREFIX_WA${it[Random().nextInt(it.count())].id}#")
                }).execute()
                return super.onOptionsItemSelected(item)
            }

            menuItemRandomOrder -> {
                GetOrder(onFinish = {
                    if (it.any()) scannerCompleted("$PREFIX_ORDER${it[Random().nextInt(it.count())].id}#")
                }).execute()
                return super.onOptionsItemSelected(item)
            }

            menuItemManualCode -> {
                enterCode()
                return super.onOptionsItemSelected(item)
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun enterCode() {
        runOnUiThread {
            var alertDialog: AlertDialog? = null
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.enter_code))

            val inputLayout = TextInputLayout(this)
            inputLayout.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT

            val input = TextInputEditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
            input.isFocusable = true
            input.isFocusableInTouchMode = true
            input.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (alertDialog != null) {
                                alertDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
                            }
                        }
                    }
                }
                false
            }

            inputLayout.addView(input)
            builder.setView(inputLayout)
            builder.setPositiveButton(R.string.accept) { _, _ ->
                scannerCompleted(input.text.toString())
            }
            builder.setNegativeButton(R.string.cancel, null)
            alertDialog = builder.create()

            alertDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            alertDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            alertDialog.show()
            input.requestFocus()
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

        GetResultFromCode(code = scannedCode,
            searchItemId = true,
            searchItemCode = true,
            searchItemEan = true,
            searchItemUrl = true,
            searchWarehouseAreaId = true,
            searchRackId = true,
            searchOrder = true,
            onFinish = {
                val r = it.typedObject
                when {
                    r is ArrayList<*> && r.any() -> {
                        if (r.first() is ItemKtor) {
                            val itemKtor = r.first() as ItemKtor
                            fillItemPanel(itemKtor.toRoom())
                        } else if (r.first() is WarehouseArea) {
                            fillWarehouseAreaPanel(r.first() as WarehouseArea)
                        } else if (r.first() is Rack) {
                            fillRackPanel(r.first() as Rack)
                        } else if (r.first() is OrderResponse) {
                            fillOrderPanel(r.first() as OrderResponse)
                        }
                    }

                    r is ItemKtor -> {
                        fillItemPanel(r.toRoom())
                    }

                    r is WarehouseArea -> {
                        fillWarehouseAreaPanel(r)
                    }

                    r is Rack -> {
                        fillRackPanel(r)
                    }

                    r is OrderResponse -> {
                        fillOrderPanel(r)
                    }

                    else -> {
                        fillPanel(null)
                        binding.infoTextView.setText(
                            R.string.the_code_does_not_belong_to_any_iem_in_the_database, TextView.BufferType.EDITABLE
                        )
                    }
                }
                LifecycleListener.lockScanner(this, false)
            })
    }

    private fun fillOrderPanel(r: OrderResponse) {
        runOnUiThread {
            // fillPanel(OrderDetailFragment.newInstance(r))
            binding.infoTextView.setText(
                R.string.the_code_belongs_to_an_order, TextView.BufferType.EDITABLE
            )
        }
    }

    private fun fillRackPanel(r: Rack) {
        runOnUiThread {
            fillPanel(RackDetailFragment.newInstance(r))
            binding.infoTextView.setText(
                R.string.the_code_belongs_to_an_rack, TextView.BufferType.EDITABLE
            )
        }
    }

    private fun fillWarehouseAreaPanel(r: WarehouseArea) {
        runOnUiThread {
            fillPanel(WarehouseAreaDetailFragment.newInstance(r))
            binding.infoTextView.setText(
                R.string.the_code_belongs_to_an_area, TextView.BufferType.EDITABLE
            )
        }
    }

    private fun fillItemPanel(itemRoom: Item) {
        runOnUiThread {
            fillPanel(ItemDetailFragment.newInstance(itemRoom))
            binding.infoTextView.setText(
                R.string.the_code_belongs_to_an_item, TextView.BufferType.EDITABLE
            )
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
            fillHexTextView()
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
