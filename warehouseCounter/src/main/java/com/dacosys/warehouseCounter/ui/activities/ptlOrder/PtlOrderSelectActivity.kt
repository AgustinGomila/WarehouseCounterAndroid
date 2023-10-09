package com.dacosys.warehouseCounter.ui.activities.ptlOrder

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlOrder
import com.dacosys.warehouseCounter.data.ktor.v1.functions.GetPtlOrder
import com.dacosys.warehouseCounter.data.ktor.v1.functions.GetPtlOrderByCode
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.databinding.PtlSelectOrderActivityBinding
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.scanners.LifecycleListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.ui.adapter.FilterOptions
import com.dacosys.warehouseCounter.ui.adapter.ptlOrder.PtlOrderAdapter
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelableArrayList
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*
import kotlin.concurrent.thread

class PtlOrderSelectActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    Scanner.ScannerListener, Rfid.RfidDeviceListener, PtlOrderAdapter.CheckedChangedListener,
    PtlOrderAdapter.DataSetChangedListener {

    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        adapter?.refreshListeners(null, null)
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshItem.isRefreshing = false
        }, 100)
    }

    private var tempTitle = ""

    private var rejectNewInstances = false

    private var multiSelect = false
    private var adapter: PtlOrderAdapter? = null
    private var lastSelected: PtlOrder? = null
    private var currentScrollPosition: Int = 0
    private var firstVisiblePos: Int? = null

    private var searchedText: String = ""

    // region Variables para LoadOnDemand
    private var completeList: ArrayList<PtlOrder> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()
    // endregion

    private var showCheckBoxes
        get() =
            if (!multiSelect) false
            else settingsVm.selectPtlOrderShowCheckBoxes
        set(value) {
            settingsVm.selectPtlOrderShowCheckBoxes = value
        }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        saveBundleValues(savedInstanceState)
    }

    private fun saveBundleValues(b: Bundle) {
        b.putString(ARG_TITLE, tempTitle)
        b.putBoolean(ARG_MULTI_SELECT, multiSelect)

        if (adapter != null) {
            b.putParcelable("lastSelected", (adapter ?: return).currentPtlOrder())
            b.putInt("firstVisiblePos", (adapter ?: return).firstVisiblePos())
            b.putParcelableArrayList("completeList", adapter?.fullList)
            b.putLongArray("checkedIdArray", adapter?.getAllChecked()?.map { it.id }?.toLongArray())
            b.putInt("currentScrollPosition", currentScrollPosition)
        }

        b.putString("searchText", searchedText)
    }


    private fun loadBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.select_order)

        multiSelect = b.getBoolean("multiSelect", multiSelect)

        searchedText = b.getString("searchText") ?: ""

        // Adapter
        checkedIdArray = (b.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(ArrayList())
        completeList = b.parcelableArrayList("completeList") ?: ArrayList()
        lastSelected = b.parcelable("lastSelected")
        firstVisiblePos = if (b.containsKey("firstVisiblePos")) b.getInt("firstVisiblePos") else -1
        currentScrollPosition = b.getInt("currentScrollPosition")
    }

    private fun loadExtrasBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
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

        setSupportActionBar(binding.topAppbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                currentScrollPosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            }
        })

        if (savedInstanceState != null) {
            loadBundleValues(savedInstanceState)
        } else {
            // Inicializar la actividad
            val extras = intent.extras
            if (extras != null) loadExtrasBundleValues(extras)
        }

        binding.topAppbar.title = tempTitle

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
                searchedText = s.toString()
                adapter?.refreshFilter(FilterOptions(searchedText))
            }
        })
        binding.searchEditText.setText(searchedText, TextView.BufferType.EDITABLE)
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

    // region Inset animation
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupWindowInsetsAnimation()
    }

    private var isKeyboardVisible: Boolean = false

    /**
     * Change panels state at Ime animation finish
     *
     * Estados que recuerdan está pendiente de ejecutar un cambio en estado (colapsado/expandido) de los paneles al
     * terminar la animación de mostrado/ocultamiento del teclado en pantalla. Esto es para sincronizar los cambios,
     * ejecutándolos de manera secuencial. A ojos del usuario la vista completa acompaña el desplazamiento de la
     * animación. Si se ejecutara al mismo tiempo el cambio en los paneles y la animación del teclado la vista no
     * acompaña correctamente al teclado, ya que cambia durante la animación.
     */

    private fun setupWindowInsetsAnimation() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val rootView = binding.root

        // Adjust root layout to bottom navigation bar
        val windowInsets = window.decorView.rootWindowInsets
        @Suppress("DEPRECATION") rootView.setPadding(
            windowInsets.systemWindowInsetLeft,
            windowInsets.systemWindowInsetTop,
            windowInsets.systemWindowInsetRight,
            windowInsets.systemWindowInsetBottom
        )

        implWindowInsetsAnimation()
    }

    private fun implWindowInsetsAnimation() {
        val rootView = binding.root

        ViewCompat.setWindowInsetsAnimationCallback(
            rootView,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    val isIme = animation.typeMask and WindowInsetsCompat.Type.ime() != 0
                    if (!isIme) return
                    super.onEnd(animation)
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    paddingBottomView(rootView, insets)

                    return insets
                }
            })
    }

    private fun paddingBottomView(rootView: ConstraintLayout, insets: WindowInsetsCompat) {
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val paddingBottom = imeInsets.bottom.coerceAtLeast(systemBarInsets.bottom)

        isKeyboardVisible = imeInsets.bottom > 0

        rootView.setPadding(
            rootView.paddingLeft,
            rootView.paddingTop,
            rootView.paddingRight,
            paddingBottom
        )

        Log.d(javaClass.simpleName, "IME Size: ${imeInsets.bottom}")
    }
    // endregion

    private fun orderSelect() {
        if (!multiSelect && adapter?.currentPtlOrder() != null) {
            Screen.closeKeyboard(this)

            val data = Intent()
            data.putParcelableArrayListExtra(ARG_PTL_ORDER, arrayListOf(adapter!!.currentPtlOrder()))
            setResult(RESULT_OK, data)
            finish()
        } else if (multiSelect && ((adapter?.countChecked()) ?: 0) > 0) {
            Screen.closeKeyboard(this)

            val data = Intent()
            data.putParcelableArrayListExtra(ARG_PTL_ORDER, adapter!!.getAllChecked())
            setResult(RESULT_OK, data)
            finish()
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshItem.isRefreshing = show
        }, 20)
    }

    private fun getOrders() {
        thread {
            GetPtlOrder(onEvent = {
                if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(
                    it.text,
                    it.snackBarType
                )
            },
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
                if (adapter != null) {
                    // Si el adapter es NULL es porque aún no fue creado.
                    // Por lo tanto, puede ser que los valores de [lastSelected]
                    // sean valores guardados de la instancia anterior y queremos preservarlos.
                    lastSelected = adapter?.currentPtlOrder()
                }

                adapter = PtlOrderAdapter.Builder()
                    .recyclerView(binding.recyclerView)
                    .fullList(completeList)
                    .checkedIdArray(checkedIdArray)
                    .multiSelect(multiSelect)
                    .showCheckBoxes(`val` = showCheckBoxes, listener = { showCheckBoxes = it })
                    .filterOptions(FilterOptions(searchedText))
                    .checkedChangedListener(this)
                    .dataSetChangedListener(this)
                    .build()

                binding.recyclerView.layoutManager = LinearLayoutManager(this)
                binding.recyclerView.adapter = adapter

                while (binding.recyclerView.adapter == null) {
                    // Horrible wait for a full load
                }

                // Variables locales para evitar cambios posteriores de estado.
                val ls = lastSelected ?: t.firstOrNull()
                val cs = currentScrollPosition
                Handler(Looper.getMainLooper()).postDelayed({
                    adapter?.selectItem(ls, false)
                    adapter?.scrollToPos(cs, true)
                }, 200)
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, tag, ex)
            } finally {
                showProgressBar(false)
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        rejectNewInstances = false

        Screen.closeKeyboard(this)

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
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
            LifecycleListener.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun scannerCompleted(scanCode: String) {
        if (settingsVm.showScannedCode) showSnackBar(scanCode, INFO)

        // Nada que hacer, volver
        if (scanCode.trim().isEmpty()) {
            val res = context.getString(R.string.invalid_code)
            showSnackBar(res, ERROR)
            ErrorLog.writeLog(this, tag, res)
            return
        }

        LifecycleListener.lockScanner(this, true)

        thread {
            GetPtlOrderByCode(code = scanCode,
                onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it.text, it.snackBarType) },
                onFinish = {
                    if (it.any()) onGetPtlOrder(it)
                    LifecycleListener.lockScanner(this, false)
                }
            ).execute()
        }
    }

    @SuppressLint("MissingSuperCall")
    @Suppress("OVERRIDE_DEPRECATION")
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
        menuInflater.inflate(R.menu.menu_read_activity, menu)

        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        if (!settingsVm.useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_visibility)
        binding.topAppbar.overflowIcon = drawable

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

            menuItemRandomOnListL -> {
                val codes: ArrayList<String> = ArrayList()
                (adapter?.fullList ?: ArrayList()).mapTo(codes) { it.description }
                if (codes.any()) scannerCompleted(codes[Random().nextInt(codes.count())])
                return super.onOptionsItemSelected(item)
            }

            menuItemRandomIt -> {
                ItemCoroutines.getEanCodes(true) {
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
                                alertDialog!!.getButton(DialogInterface.BUTTON_POSITIVE)
                                    .performClick()
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

    override fun onCheckedChanged(isChecked: Boolean, pos: Int) {
        fillSummaryRow()
    }

    override fun onDataSetChanged() {
        Handler(Looper.getMainLooper()).postDelayed({
            fillSummaryRow()
        }, 100)
    }

    private fun fillSummaryRow() {
        Log.d(tag, "fillSummaryRow")
        runOnUiThread {
            if (multiSelect) {
                binding.totalLabelTextView.text = context.getString(R.string.total)
                binding.qtyReqLabelTextView.text = context.getString(R.string.cant)
                binding.selectedLabelTextView.text = context.getString(R.string.checked)

                if (adapter != null) {
                    binding.totalTextView.text = adapter?.itemCount.toString()
                    binding.qtyReqTextView.text = "0"
                    binding.selectedTextView.text = adapter?.countChecked().toString()
                }
            } else {
                binding.totalLabelTextView.text = context.getString(R.string.total)
                binding.qtyReqLabelTextView.text = context.getString(R.string.cont_)
                binding.selectedLabelTextView.text = context.getString(R.string.ptl_orders)

                if (adapter != null) {
                    val cont = 0
                    val t = adapter?.itemCount ?: 0
                    binding.totalTextView.text = t.toString()
                    binding.qtyReqTextView.text = cont.toString()
                    binding.selectedTextView.text = (t - cont).toString()
                }
            }

            if (adapter == null) {
                binding.totalTextView.text = 0.toString()
                binding.qtyReqTextView.text = 0.toString()
                binding.selectedTextView.text = 0.toString()
            }
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

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_MULTI_SELECT = "multiSelect"
        const val ARG_PTL_ORDER = "ptlOrder"
    }
}
