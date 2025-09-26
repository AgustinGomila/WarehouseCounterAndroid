package com.example.warehouseCounter.ui.activities.codeCheck

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.warehouseCounter.BuildConfig
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelTemplate
import com.example.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelType
import com.example.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeParam
import com.example.warehouseCounter.data.ktor.v2.dto.barcode.PrintOps
import com.example.warehouseCounter.data.ktor.v2.dto.location.Rack
import com.example.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.example.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.example.warehouseCounter.data.ktor.v2.functions.barcode.GetItemBarcode
import com.example.warehouseCounter.data.ktor.v2.functions.barcode.GetOrderBarcode
import com.example.warehouseCounter.data.ktor.v2.functions.barcode.GetRackBarcode
import com.example.warehouseCounter.data.ktor.v2.functions.barcode.GetWarehouseAreaBarcode
import com.example.warehouseCounter.data.ktor.v2.functions.item.GetItem
import com.example.warehouseCounter.data.ktor.v2.functions.itemCode.GetItemCode
import com.example.warehouseCounter.data.ktor.v2.functions.location.GetRack
import com.example.warehouseCounter.data.ktor.v2.functions.location.GetWarehouseArea
import com.example.warehouseCounter.data.ktor.v2.functions.order.GetOrder
import com.example.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.example.warehouseCounter.data.room.entity.item.Item
import com.example.warehouseCounter.databinding.CodeCheckActivityBinding
import com.example.warehouseCounter.misc.Statics
import com.example.warehouseCounter.scanners.Scanner
import com.example.warehouseCounter.scanners.devices.nfc.Nfc
import com.example.warehouseCounter.scanners.devices.rfid.Rfid
import com.example.warehouseCounter.scanners.devices.vh75.Vh75Bt
import com.example.warehouseCounter.scanners.deviceLifecycle.ScannerManager
import com.example.warehouseCounter.scanners.scanCode.GetResultFromCode
import com.example.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.PREFIX_ITEM
import com.example.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.PREFIX_ITEM_URL
import com.example.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.PREFIX_ORDER
import com.example.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.PREFIX_RACK
import com.example.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.PREFIX_WA
import com.example.warehouseCounter.ui.fragments.item.ItemDetailFragment
import com.example.warehouseCounter.ui.fragments.location.RackDetailFragment
import com.example.warehouseCounter.ui.fragments.location.WarehouseAreaDetailFragment
import com.example.warehouseCounter.ui.fragments.order.OrderDetailFragment
import com.example.warehouseCounter.ui.fragments.print.PrintLabelFragment
import com.example.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.example.warehouseCounter.ui.snackBar.SnackBarType
import com.example.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.example.warehouseCounter.ui.utils.Screen
import com.example.warehouseCounter.ui.utils.TextViewUtils.Companion.isActionDone
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*
import com.example.warehouseCounter.data.ktor.v2.dto.item.Item as ItemKtor

class CodeCheckActivity : AppCompatActivity(), Scanner.ScannerListener, Rfid.RfidDeviceListener,
    PrintLabelFragment.FragmentListener {

    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    private var rejectNewInstances = false

    private var currentFragment: Fragment? = null
    private var currentItem: Any? = null
    private var printLabelFragment: PrintLabelFragment? = null

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

        printLabelFragment = supportFragmentManager.findFragmentById(R.id.printFragment) as PrintLabelFragment

        var tempCode = ""
        if (savedInstanceState != null) {
            tempCode = savedInstanceState.getString(ARG_CODE) ?: ""
        } else {
            val extras = intent.extras
            if (extras != null) {
                tempCode = extras.getString(ARG_CODE) ?: ""
            }
        }

        binding.codeEditText.setText(tempCode, TextView.BufferType.EDITABLE)
        binding.codeEditText.setOnEditorActionListener { _, actionId, event ->
            if (isActionDone(actionId, event)) {
                fillHexTextView()
                true
            } else {
                false
            }
        }
        binding.codeEditText.requestFocus()

        Screen.setupUI(binding.root, this)
    }

    override fun onStart() {
        super.onStart()

        fillHexTextView()
    }

    private fun setupPrintLabelFragment(templateId: Long, typesId: ArrayList<Long>) {
        if (isFinishing) return

        runOnUiThread {
            binding.printFragment.visibility = View.VISIBLE

            printLabelFragment?.saveSharedPreferences()
            val fragment =
                PrintLabelFragment.Builder()
                    .setTemplateTypeIdList(typesId)
                    .setTemplateId(templateId)
                    .setQty(settingsVm.printerQty)
                    .build()
            printLabelFragment = fragment
            supportFragmentManager.beginTransaction().replace(R.id.printFragment, fragment).commit()
        }
    }

    private fun hidePrintLabelFragment() {
        if (isFinishing) return

        runOnUiThread {
            binding.printFragment.visibility = View.GONE
        }
    }

    private fun setPanelFragment(fragment: Fragment?) {
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

        if (!settingsVm.useRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        if (BuildConfig.DEBUG || Statics.TEST_MODE) {
            menu.add(Menu.NONE, menuItemManualCode, Menu.NONE, "Manual code")
            menu.add(Menu.NONE, menuItemRandomEan, Menu.NONE, "Random ean")
            menu.add(Menu.NONE, menuItemRandomIt, Menu.NONE, "Random item id")
            menu.add(Menu.NONE, menuItemRandomItUrl, Menu.NONE, "Random item id url")
            menu.add(Menu.NONE, menuItemRandomOrder, Menu.NONE, "Random order")
            menu.add(Menu.NONE, menuItemRandomRack, Menu.NONE, "Random rack")
            menu.add(Menu.NONE, menuItemRandomWa, Menu.NONE, "Random area")
            menu.add(Menu.NONE, menuRegexItem, Menu.NONE, "Regex")
            menu.add(Menu.NONE, menuItemRandomItemCode, Menu.NONE, "Random item code")
            menu.add(Menu.NONE, menuItemRandomExternalId, Menu.NONE, "Random external id")
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
    private val menuRegexItem = 999008
    private val menuItemRandomItemCode = 999009
    private val menuItemRandomExternalId = 999010

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home, android.R.id.home -> {
                finish()
                return true
            }

            R.id.action_rfid_connect -> {
                ScannerManager.rfidStart(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_trigger_scan -> {
                ScannerManager.trigger(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_read_barcode -> {
                ScannerManager.toggleCameraFloatingWindowVisibility(this)
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
                GetItem(onFinish = {
                    if (!it.any()) return@GetItem
                    scannerCompleted("$PREFIX_ITEM_URL${it[Random().nextInt(it.count())].uuid}#")
                }).execute()
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

            menuItemRandomExternalId -> {
                GetOrder(onFinish = {
                    if (it.any()) scannerCompleted(it[Random().nextInt(it.count())].externalId)
                }).execute()
                return super.onOptionsItemSelected(item)
            }

            menuItemRandomItemCode -> {
                GetItemCode(onFinish = {
                    if (it.any()) scannerCompleted(it[Random().nextInt(it.count())].code)
                }).execute()
                return super.onOptionsItemSelected(item)
            }

            menuRegexItem -> {
                scannerCompleted("0SM20220721092826007792261002857001038858")
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
            input.setOnKeyListener { _, _, event ->
                if (isActionDone(event)) {
                    alertDialog?.getButton(DialogInterface.BUTTON_POSITIVE)?.performClick()
                    true
                } else {
                    false
                }
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

    private fun clearControls() {
        currentItem = null
        setPanelFragment(null)

        hidePrintLabelFragment()

        runOnUiThread {
            binding.infoTextView.setText("", TextView.BufferType.EDITABLE)
        }
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
            showMessage(res, ERROR)
            Log.d(tag, res)
            return
        }

        ScannerManager.lockScanner(this, true)

        GetResultFromCode.Builder()
            .withCode(scannedCode)
            .searchItemCode()
            .searchItemEan()
            .searchItemId()
            .searchItemRegex()
            .searchItemUrl()
            .searchOrder()
            .searchOrderExternalId()
            .searchRackId()
            .searchWarehouseAreaId()
            .onFinish {
                ScannerManager.lockScanner(this, false)
                proceedByResult(it)
            }
            .build()
    }

    private fun proceedByResult(it: GetResultFromCode.CodeResult) {
        when (val r = it.item) {
            is ItemKtor -> fillItemPanel(r.toRoom())
            is WarehouseArea -> fillWarehouseAreaPanel(r)
            is Rack -> fillRackPanel(r)
            is OrderResponse -> fillOrderPanel(r)
            else -> {
                currentItem = null
                setPanelFragment(null)

                hidePrintLabelFragment()

                runOnUiThread {
                    binding.infoTextView.setText(
                        R.string.the_code_does_not_belong_to_any_iem_in_the_database,
                        TextView.BufferType.EDITABLE
                    )
                }
            }
        }
    }

    private fun fillOrderPanel(r: OrderResponse) {
        runOnUiThread {
            currentItem = r
            setPanelFragment(OrderDetailFragment.newInstance(r))

            setupPrintLabelFragment(
                templateId = settingsVm.defaultOrderTemplateId,
                typesId = arrayListOf(BarcodeLabelType.order.id)
            )

            binding.infoTextView.setText(
                R.string.the_code_belongs_to_an_order, TextView.BufferType.EDITABLE
            )
        }
    }

    private fun fillRackPanel(r: Rack) {
        runOnUiThread {
            currentItem = r
            setPanelFragment(RackDetailFragment.newInstance(r))

            setupPrintLabelFragment(
                templateId = settingsVm.defaultRackTemplateId,
                typesId = arrayListOf(BarcodeLabelType.rack.id)
            )

            binding.infoTextView.setText(
                R.string.the_code_belongs_to_an_rack, TextView.BufferType.EDITABLE
            )
        }
    }

    private fun fillWarehouseAreaPanel(r: WarehouseArea) {
        runOnUiThread {
            currentItem = r
            setPanelFragment(WarehouseAreaDetailFragment.newInstance(r))

            setupPrintLabelFragment(
                templateId = settingsVm.defaultWaTemplateId,
                typesId = arrayListOf(BarcodeLabelType.warehouseArea.id)
            )

            binding.infoTextView.setText(
                R.string.the_code_belongs_to_an_area, TextView.BufferType.EDITABLE
            )
        }
    }

    private fun fillItemPanel(itemRoom: Item) {
        runOnUiThread {
            currentItem = itemRoom
            setPanelFragment(ItemDetailFragment.newInstance(itemRoom))

            setupPrintLabelFragment(
                templateId = settingsVm.defaultItemTemplateId,
                typesId = arrayListOf(BarcodeLabelType.item.id)
            )

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
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
            ScannerManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
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

    override fun onReadCompleted(scanCode: String) {
        scannerCompleted(scanCode)
    }

    override fun onFilterChanged(printer: String, template: BarcodeLabelTemplate?, qty: Int?) {}

    override fun onPrintRequested(printer: String, qty: Int) {
        val tempObj = currentItem ?: return
        val template = printLabelFragment?.template ?: return

        when (tempObj) {
            is Item -> {
                GetItemBarcode(
                    param = BarcodeParam(
                        idList = arrayListOf(tempObj.itemId),
                        templateId = template.templateId,
                        printOps = PrintOps.getPrintOps()
                    ),
                    onEvent = {
                        if (!SnackBarType.SUCCESS.equals(it.snackBarType)) showMessage(
                            it.text,
                            it.snackBarType
                        )
                    },
                    onFinish = {
                        if (it.any()) {
                            printLabelFragment?.printBarcodes(labelArray = it, onFinish = {})
                        } else {
                            showMessage(getString(R.string.there_are_no_labels_to_print), ERROR)
                        }
                    }
                ).execute()
            }

            is OrderResponse -> {
                GetOrderBarcode(
                    param = BarcodeParam(
                        idList = arrayListOf(tempObj.id),
                        templateId = template.templateId,
                        printOps = PrintOps.getPrintOps()
                    ),
                    onEvent = {
                        if (!SnackBarType.SUCCESS.equals(it.snackBarType)) showMessage(
                            it.text,
                            it.snackBarType
                        )
                    },
                    onFinish = {
                        if (it.any()) {
                            printLabelFragment?.printBarcodes(labelArray = it, onFinish = {})
                        } else {
                            showMessage(getString(R.string.there_are_no_labels_to_print), ERROR)
                        }
                    }
                ).execute()
            }

            is WarehouseArea -> {
                GetWarehouseAreaBarcode(
                    param = BarcodeParam(
                        idList = arrayListOf(tempObj.id),
                        templateId = template.templateId,
                        printOps = PrintOps.getPrintOps()
                    ),
                    onEvent = {
                        if (!SnackBarType.SUCCESS.equals(it.snackBarType)) showMessage(
                            it.text,
                            it.snackBarType
                        )
                    },
                    onFinish = {
                        if (it.any()) {
                            printLabelFragment?.printBarcodes(labelArray = it, onFinish = {})
                        } else {
                            showMessage(getString(R.string.there_are_no_labels_to_print), ERROR)
                        }
                    }
                ).execute()
            }

            is Rack -> {
                GetRackBarcode(
                    param = BarcodeParam(
                        idList = arrayListOf(tempObj.id),
                        templateId = template.templateId,
                        printOps = PrintOps.getPrintOps()
                    ),
                    onEvent = {
                        if (!SnackBarType.SUCCESS.equals(it.snackBarType)) showMessage(
                            it.text,
                            it.snackBarType
                        )
                    },
                    onFinish = {
                        if (it.any()) {
                            printLabelFragment?.printBarcodes(labelArray = it, onFinish = {})
                        } else {
                            showMessage(getString(R.string.there_are_no_labels_to_print), ERROR)
                        }
                    }
                ).execute()
            }
        }
    }

    override fun onQtyTextViewFocusChanged(hasFocus: Boolean) {}

    //endregion READERS Reception

    private fun showMessage(msg: String, type: Int) {
        if (isFinishing || isDestroyed) return
        if (ERROR.equals(type)) Log.e(javaClass.simpleName, msg)
        makeText(binding.root, msg, type)
    }

    private fun showMessage(msg: String, type: SnackBarType) {
        showMessage(msg, type.snackBarTypeId)
    }
}
