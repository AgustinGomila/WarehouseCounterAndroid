package com.dacosys.warehouseCounter.ui.activities.ptlOrder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.adapter.ptlOrder.PtlOrderAdapter
import com.dacosys.warehouseCounter.databinding.PtlSelectOrderActivityBinding
import com.dacosys.warehouseCounter.dto.ptlOrder.PtlOrder
import com.dacosys.warehouseCounter.ktor.functions.GetPtlOrder
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.retrofit.functions.GetPtlOrderByCode
import com.dacosys.warehouseCounter.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.dacosys.warehouseCounter.ui.utils.Screen
import java.util.*
import kotlin.concurrent.thread

class PtlOrderSelectActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    Scanner.ScannerListener, Rfid.RfidDeviceListener, PtlOrderAdapter.CheckedChangedListener,
    PtlOrderAdapter.DataSetChangedListener {
    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        arrayAdapter?.refreshListeners(null, null)
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshItem.isRefreshing = false
            }
        }, 100)
    }

    private var tempTitle = ""

    private var rejectNewInstances = false

    private var multiSelect = false
    private var arrayAdapter: PtlOrderAdapter? = null
    private var lastSelected: PtlOrder? = null
    private var firstVisiblePos: Int? = null

    private var searchText: String = ""

    // region Variables para LoadOnDemand
    private var completeList: ArrayList<PtlOrder> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()
    // endregion

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        saveBundleValues(savedInstanceState)
    }

    private fun saveBundleValues(b: Bundle) {
        b.putString("title", title.toString())
        b.putBoolean("multiSelect", multiSelect)

        if (arrayAdapter != null) {
            b.putParcelable("lastSelected", (arrayAdapter ?: return).currentItem())
            b.putInt("firstVisiblePos", (arrayAdapter ?: return).firstVisiblePos())
            b.putParcelableArrayList("completeList", arrayAdapter?.getAll())
            b.putLongArray(
                "checkedIdArray", arrayAdapter?.getAllIdChecked()?.map { it }?.toLongArray()
            )
        }

        b.putString("searchText", searchText)
    }


    private fun loadBundleValues(b: Bundle) {
        tempTitle = b.getString("title") ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.select_order)

        multiSelect = b.getBoolean("multiSelect", multiSelect)

        searchText = b.getString("searchText") ?: ""

        // Adapter
        checkedIdArray =
            (b.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(ArrayList())
        completeList = b.getParcelableArrayList<PtlOrder>("completeList") as ArrayList<PtlOrder>
        lastSelected = b.getParcelable("lastSelected")
        firstVisiblePos = if (b.containsKey("firstVisiblePos")) b.getInt("firstVisiblePos") else -1
    }

    private fun loadExtrasBundleValues(b: Bundle) {
        tempTitle = b.getString("title") ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.select_item)

        multiSelect = b.getBoolean("multiSelect", false)
    }

    private lateinit var binding: PtlSelectOrderActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = PtlSelectOrderActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState != null) {
            loadBundleValues(savedInstanceState)
        } else {
            // Inicializar la actividad
            val extras = intent.extras
            if (extras != null) loadExtrasBundleValues(extras)
        }

        title = tempTitle

        binding.swipeRefreshItem.setOnRefreshListener(this)
        binding.swipeRefreshItem.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        binding.okButton.setOnClickListener { orderSelect() }

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
            }

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int,
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int,
            ) {
                searchText = s.toString()
                arrayAdapter?.refreshFilter(searchText, true)
            }
        })
        binding.searchEditText.setText(searchText, TextView.BufferType.EDITABLE)
        binding.searchEditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        Screen.closeKeyboard(this)
                    }
                }
            }
            false
        }
        binding.searchEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)

        binding.searchTextImageView.setOnClickListener { binding.searchEditText.requestFocus() }
        binding.searchTextClearImageView.setOnClickListener {
            binding.searchEditText.setText("")
        }

        Screen.setupUI(binding.root, this)
    }

    private fun orderSelect() {
        if (!multiSelect && arrayAdapter?.currentItem() != null) {
            Screen.closeKeyboard(this)

            val data = Intent()
            data.putParcelableArrayListExtra("ptlOrder", arrayListOf(arrayAdapter!!.currentItem()))
            setResult(RESULT_OK, data)
            finish()
        } else if (multiSelect && ((arrayAdapter?.countChecked()) ?: 0) > 0) {
            Screen.closeKeyboard(this)

            val data = Intent()
            data.putParcelableArrayListExtra("ptlOrder", arrayAdapter!!.getAllChecked())
            setResult(RESULT_OK, data)
            finish()
        }
    }

    private fun showSnackBar(it: SnackBarEventData) {
        makeText(binding.root, it.text, it.snackBarType)
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshItem.isRefreshing = show
            }
        }, 20)
    }

    private fun getOrders() {
        thread {
            GetPtlOrder(onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it) },
                onFinish = { if (it.any()) onGetPtlOrder(it) }).execute()
        }
    }

    private fun onGetPtlOrder(it: ArrayList<PtlOrder>) {
        if (it.any()) {
            fillAdapter(it)
            return
        }
        showProgressBar(false)
    }

    private fun fillAdapter(t: ArrayList<PtlOrder>) {
        showProgressBar(true)

        if (!t.any()) {
            checkedIdArray.clear()
            getOrders()
            return
        }
        completeList = t

        runOnUiThread {
            try {
                if (arrayAdapter != null) {
                    lastSelected = arrayAdapter?.currentItem()
                    firstVisiblePos = arrayAdapter?.firstVisiblePos()
                }

                arrayAdapter = PtlOrderAdapter(
                    activity = this,
                    resource = R.layout.ptl_order_row,
                    itemList = completeList,
                    suggestedList = ArrayList(),
                    listView = binding.itemListView,
                    multiSelect = multiSelect,
                    checkedIdArray = checkedIdArray
                )

                arrayAdapter?.refreshListeners(
                    checkedChangedListener = this, dataSetChangedListener = this
                )

                arrayAdapter?.refreshFilter(searchText, true)

                while (binding.itemListView.adapter == null) {
                    // Horrible wait for full load
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    run {
                        arrayAdapter?.setSelectItemAndScrollPos(lastSelected, firstVisiblePos)
                    }
                }, 20)
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                showProgressBar(false)
            }
        }
    }

    private fun fillSummaryRow() {
        Log.d(this::class.java.simpleName, "fillSummaryRow")
        runOnUiThread {
            if (multiSelect) {
                binding.totalLabelTextView.text = context.getString(R.string.total)
                binding.qtyReqLabelTextView.text = context.getString(R.string.cant)
                binding.selectedLabelTextView.text = context.getString(R.string.checked)

                if (arrayAdapter != null) {
                    binding.totalTextView.text = arrayAdapter?.count.toString()
                    binding.qtyReqTextView.text = "0"
                    binding.selectedTextView.text = arrayAdapter?.countChecked().toString()
                }
            } else {
                binding.totalLabelTextView.text = context.getString(R.string.total)
                binding.qtyReqLabelTextView.text = context.getString(R.string.cont_)
                binding.selectedLabelTextView.text = context.getString(R.string.orders)

                if (arrayAdapter != null) {
                    val cont = 0
                    val t = arrayAdapter?.count ?: 0
                    binding.totalTextView.text = t.toString()
                    binding.qtyReqTextView.text = cont.toString()
                    binding.selectedTextView.text = (t - cont).toString()
                }
            }

            if (arrayAdapter == null) {
                binding.totalTextView.text = 0.toString()
                binding.qtyReqTextView.text = 0.toString()
                binding.selectedTextView.text = 0.toString()
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        rejectNewInstances = false

        Screen.closeKeyboard(this)
        JotterListener.resumeReaderDevices(this)

        thread {
            fillAdapter(completeList)
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
        if (settingViewModel.showScannedCode) makeText(binding.root, scanCode, INFO)

        // Nada que hacer, volver
        if (scanCode.trim().isEmpty()) {
            val res = context.getString(R.string.invalid_code)
            makeText(binding.root, res, ERROR)
            ErrorLog.writeLog(this, this::class.java.simpleName, res)
            return
        }

        JotterListener.lockScanner(this, true)

        thread {
            GetPtlOrderByCode(code = scanCode,
                onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it) },
                onFinish = { if (it.any()) onGetPtlOrder(it) }).execute()
        }
    }

    override fun onBackPressed() {
        Screen.closeKeyboard(this)

        setResult(RESULT_CANCELED)
        finish()
    }

    private val menuItemRandomIt = 999001
    private val menuItemManualCode = 999002
    private val menuItemRandomOnListL = 999003

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_read_activity, menu)

        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        if (!settingViewModel.useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_visibility)
        val toolbar = findViewById<Toolbar>(R.id.action_bar)
        toolbar.overflowIcon = drawable

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.home, android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.action_rfid_connect -> {
                JotterListener.rfidStart(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_trigger_scan -> {
                JotterListener.trigger(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_read_barcode -> {
                JotterListener.toggleCameraFloatingWindowVisibility(this)
                return super.onOptionsItemSelected(item)
            }

            menuItemRandomOnListL -> {
                val codes: ArrayList<String> = ArrayList()
                (arrayAdapter?.getAll() ?: ArrayList()).mapTo(codes) { it.description }
                if (codes.any()) scannerCompleted(codes[Random().nextInt(codes.count())])
                return super.onOptionsItemSelected(item)
            }

            menuItemRandomIt -> {
                ItemCoroutines().getCodes(true) {
                    if (it.any()) scannerCompleted(it[Random().nextInt(it.count())])
                }
                return super.onOptionsItemSelected(item)
            }

            menuItemManualCode -> {
                enterCode()
                return super.onOptionsItemSelected(item)
            }
        }

        item.isChecked = !item.isChecked
        return true
    }

    private fun enterCode() {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.enter_code)

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            builder.setPositiveButton(R.string.ok) { _, _ ->
                scannerCompleted(input.text.toString())
            }
            builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }

            builder.show()
        }
    }

    override fun onCheckedChanged(isChecked: Boolean, pos: Int) {
        runOnUiThread {
            binding.selectedTextView.text = arrayAdapter?.countChecked().toString()
        }
    }

    override fun onDataSetChanged() {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                fillSummaryRow()
            }
        }, 100)
    }

    // region READERS Reception

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

    override fun onGetBluetoothName(name: String) {}

    override fun onWriteCompleted(isOk: Boolean) {}

    override fun onReadCompleted(scanCode: String) {
        scannerCompleted(scanCode)
    }

    //endregion READERS Reception
}