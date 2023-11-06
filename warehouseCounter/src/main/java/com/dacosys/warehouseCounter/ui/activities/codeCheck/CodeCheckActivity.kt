package com.dacosys.warehouseCounter.ui.activities.codeCheck

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
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelTemplate
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelType
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeParam
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.PrintOps
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Rack
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.functions.barcode.GetItemBarcode
import com.dacosys.warehouseCounter.data.ktor.v2.functions.barcode.GetOrderBarcode
import com.dacosys.warehouseCounter.data.ktor.v2.functions.barcode.GetRackBarcode
import com.dacosys.warehouseCounter.data.ktor.v2.functions.barcode.GetWarehouseAreaBarcode
import com.dacosys.warehouseCounter.data.ktor.v2.functions.itemCode.GetItemCode
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
import com.dacosys.warehouseCounter.ui.fragments.order.OrderDetailFragment
import com.dacosys.warehouseCounter.ui.fragments.print.PrintLabelFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.utils.TextViewUtils.Companion.isActionDone
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.Item as ItemKtor

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

        if (!settingsVm.useBtRfid) {
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

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
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
            showSnackBar(res, ERROR)
            Log.d(tag, res)
            return
        }

        LifecycleListener.lockScanner(this, true)

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
                LifecycleListener.lockScanner(this, false)
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
            LifecycleListener.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
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
                        if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(
                            it.text,
                            it.snackBarType
                        )
                    },
                    onFinish = {
                        if (it.any()) {
                            printLabelFragment?.printBarcodes(labelArray = it, onFinish = {})
                        } else {
                            showSnackBar(getString(R.string.there_are_no_labels_to_print), ERROR)
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
                        if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(
                            it.text,
                            it.snackBarType
                        )
                    },
                    onFinish = {
                        if (it.any()) {
                            printLabelFragment?.printBarcodes(labelArray = it, onFinish = {})
                        } else {
                            showSnackBar(getString(R.string.there_are_no_labels_to_print), ERROR)
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
                        if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(
                            it.text,
                            it.snackBarType
                        )
                    },
                    onFinish = {
                        if (it.any()) {
                            printLabelFragment?.printBarcodes(labelArray = it, onFinish = {})
                        } else {
                            showSnackBar(getString(R.string.there_are_no_labels_to_print), ERROR)
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
                        if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(
                            it.text,
                            it.snackBarType
                        )
                    },
                    onFinish = {
                        if (it.any()) {
                            printLabelFragment?.printBarcodes(labelArray = it, onFinish = {})
                        } else {
                            showSnackBar(getString(R.string.there_are_no_labels_to_print), ERROR)
                        }
                    }
                ).execute()
            }
        }
    }

    override fun onQtyTextViewFocusChanged(hasFocus: Boolean) {}

    //endregion READERS Reception
}
