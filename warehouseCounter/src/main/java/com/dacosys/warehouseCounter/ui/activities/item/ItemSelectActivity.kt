package com.dacosys.warehouseCounter.ui.activities.item

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.adapter.item.ItemAdapter
import com.dacosys.warehouseCounter.databinding.ItemPrintLabelActivityTopPanelCollapsedBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.room.entity.item.Item
import com.dacosys.warehouseCounter.room.entity.itemCategory.ItemCategory
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.settings.SettingsRepository
import com.dacosys.warehouseCounter.ui.fragments.item.ItemSelectFilterFragment
import com.dacosys.warehouseCounter.ui.fragments.print.PrintLabelFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import org.parceler.Parcels
import java.util.*
import kotlin.concurrent.thread

class ItemSelectActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    Scanner.ScannerListener, Rfid.RfidDeviceListener,
    ItemSelectFilterFragment.OnFilterChangedListener, ItemAdapter.CheckedChangedListener,
    PrintLabelFragment.FragmentListener, ItemAdapter.DataSetChangedListener {
    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        arrayAdapter?.refreshListeners()
        itemSelectFilterFragment!!.onDestroy()
        printLabelFragment!!.onDestroy()
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
    private var arrayAdapter: ItemAdapter? = null
    private var lastSelected: Item? = null
    private var firstVisiblePos: Int? = null

    private var panelBottomIsExpanded = true
    private var panelTopIsExpanded = false

    private var hideFilterPanel = false

    private var searchText: String = ""

    // region Variables para LoadOnDemand
    private var completeList: ArrayList<Item> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()
    // endregion

    private var itemSelectFilterFragment: ItemSelectFilterFragment? = null
    private var printLabelFragment: PrintLabelFragment? = null

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        saveBundleValues(savedInstanceState)
    }

    private fun saveBundleValues(b: Bundle) {
        b.putString("title", title.toString())
        b.putBoolean("multiSelect", multiSelect)
        b.putBoolean("hideFilterPanel", hideFilterPanel)
        b.putBoolean("panelTopIsExpanded", panelTopIsExpanded)
        b.putBoolean("panelBottomIsExpanded", panelBottomIsExpanded)

        if (arrayAdapter != null) {
            b.putParcelable("lastSelected", (arrayAdapter ?: return).currentItem())
            b.putInt("firstVisiblePos", (arrayAdapter ?: return).firstVisiblePos())
            b.putParcelableArrayList("completeList", arrayAdapter?.getAll())
            b.putLongArray("checkedIdArray", arrayAdapter?.getAllChecked()?.toLongArray())
        }

        b.putString("searchText", searchText)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI(view: View) {
        // Set up touch checkedChangedListener for non-text box views to hide keyboard.
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

    private fun loadBundleValues(b: Bundle) {
        tempTitle = b.getString("title") ?: ""
        if (tempTitle.isEmpty()) tempTitle = context().getString(R.string.select_item)

        multiSelect = b.getBoolean("multiSelect", multiSelect)
        hideFilterPanel = b.getBoolean("hideFilterPanel", hideFilterPanel)
        panelBottomIsExpanded = b.getBoolean("panelBottomIsExpanded")
        panelTopIsExpanded = b.getBoolean("panelTopIsExpanded")

        searchText = b.getString("searchText") ?: ""

        // Adapter
        checkedIdArray =
            (b.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(ArrayList())
        completeList = b.getParcelableArrayList<Item>("completeList") as ArrayList<Item>
        lastSelected = b.getParcelable("lastSelected")
        firstVisiblePos = if (b.containsKey("firstVisiblePos")) b.getInt("firstVisiblePos") else -1
    }

    private fun loadExtrasBundleValues(b: Bundle) {
        tempTitle = b.getString("title") ?: ""
        if (tempTitle.isEmpty()) tempTitle = context().getString(R.string.select_item)

        itemSelectFilterFragment?.itemCode = b.getString("itemCode") ?: ""
        itemSelectFilterFragment?.itemCategory =
            Parcels.unwrap<ItemCategory>(b.getParcelable("itemCategory"))

        hideFilterPanel = b.getBoolean("hideFilterPanel")
        multiSelect = b.getBoolean("multiSelect", false)
    }

    private lateinit var binding: ItemPrintLabelActivityTopPanelCollapsedBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Statics.setScreenRotation(this)
        binding = ItemPrintLabelActivityTopPanelCollapsedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        itemSelectFilterFragment =
            supportFragmentManager.findFragmentById(R.id.filterFragment) as ItemSelectFilterFragment
        printLabelFragment =
            supportFragmentManager.findFragmentById(R.id.printFragment) as PrintLabelFragment

        if (savedInstanceState != null) {
            loadBundleValues(savedInstanceState)
        } else {
            // Inicializar la actividad
            val extras = intent.extras
            if (extras != null) loadExtrasBundleValues(extras)
        }

        title = tempTitle

        itemSelectFilterFragment!!.setListener(this)
        printLabelFragment!!.setListener(this)

        binding.swipeRefreshItem.setOnRefreshListener(this)
        binding.swipeRefreshItem.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Para expandir y colapsar los paneles
        setBottomPanelAnimation()
        setTopPanelAnimation()

        binding.okButton.setOnClickListener { itemSelect() }

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
                        Statics.closeKeyboard(this)
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

        // OCULTAR PANEL DE CONTROLES DE FILTRADO
        if (hideFilterPanel) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (panelBottomIsExpanded) {
                    binding.expandBottomPanelButton?.performClick()
                }

                runOnUiThread {
                    binding.expandBottomPanelButton!!.visibility = GONE
                }
            }
        }

        setPanels()

        setupUI(binding.root)
    }

    private fun itemSelect() {
        Statics.closeKeyboard(this)

        val data = Intent()

        if (arrayAdapter != null) {
            val item = arrayAdapter?.currentItem()
            val itemIdArray = arrayAdapter?.getAllCheckedAsInt()

            if (!multiSelect && item != null) {
                data.putIntegerArrayListExtra("ids", arrayListOf(item.itemId.toInt()))
                setResult(RESULT_OK, data)
            } else if (multiSelect && (itemIdArray?.count() ?: 0) > 0) {
                data.putIntegerArrayListExtra("ids", itemIdArray)
                setResult(RESULT_OK, data)
            } else {
                setResult(RESULT_CANCELED)
            }
        } else {
            setResult(RESULT_CANCELED)
        }

        finish()
    }

    private fun setPanels() {
        val currentLayout = ConstraintSet()
        if (panelBottomIsExpanded) {
            if (panelTopIsExpanded) {
                currentLayout.load(this, R.layout.item_print_label_activity)
            } else {
                currentLayout.load(this, R.layout.item_print_label_activity_top_panel_collapsed)
            }
        } else {
            if (panelTopIsExpanded) {
                currentLayout.load(this, R.layout.item_print_label_activity_bottom_panel_collapsed)
            } else {
                currentLayout.load(this, R.layout.item_print_label_activity_both_panels_collapsed)
            }
        }

        val transition = ChangeBounds()
        transition.interpolator = FastOutSlowInInterpolator()
        transition.addListener(object : Transition.TransitionListener {
            override fun onTransitionResume(transition: Transition?) {}
            override fun onTransitionPause(transition: Transition?) {}
            override fun onTransitionStart(transition: Transition?) {}
            override fun onTransitionEnd(transition: Transition?) {
                refreshTextViews()
            }

            override fun onTransitionCancel(transition: Transition?) {}
        })

        TransitionManager.beginDelayedTransition(binding.itemPrintLabel, transition)

        currentLayout.applyTo(binding.itemPrintLabel)

        when {
            panelBottomIsExpanded -> {
                binding.expandBottomPanelButton?.text = context().getString(R.string.collapse_panel)
            }
            else -> {
                binding.expandBottomPanelButton?.text = context().getString(R.string.search_options)
            }
        }

        when {
            panelTopIsExpanded -> {
                binding.expandTopPanelButton.text = context().getString(R.string.collapse_panel)
            }
            else -> {
                binding.expandTopPanelButton.text = context().getString(R.string.print_labels)
            }
        }
    }

    private fun setBottomPanelAnimation() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            return
        }

        binding.expandBottomPanelButton!!.setOnClickListener {
            val nextLayout = ConstraintSet()
            if (panelBottomIsExpanded) {
                if (panelTopIsExpanded) {
                    nextLayout.load(this, R.layout.item_print_label_activity_bottom_panel_collapsed)
                } else {
                    nextLayout.load(this, R.layout.item_print_label_activity_both_panels_collapsed)
                }
            } else {
                if (panelTopIsExpanded) {
                    nextLayout.load(this, R.layout.item_print_label_activity)
                } else {
                    nextLayout.load(this, R.layout.item_print_label_activity_top_panel_collapsed)
                }
            }

            panelBottomIsExpanded = !panelBottomIsExpanded
            val transition = ChangeBounds()
            transition.interpolator = FastOutSlowInInterpolator()
            transition.addListener(object : Transition.TransitionListener {
                override fun onTransitionResume(transition: Transition?) {}
                override fun onTransitionPause(transition: Transition?) {}
                override fun onTransitionStart(transition: Transition?) {}
                override fun onTransitionEnd(transition: Transition?) {
                    refreshTextViews()
                }

                override fun onTransitionCancel(transition: Transition?) {}
            })

            TransitionManager.beginDelayedTransition(binding.itemPrintLabel, transition)

            when {
                panelBottomIsExpanded -> {
                    binding.expandBottomPanelButton?.text =
                        context().getString(R.string.collapse_panel)
                }
                else -> {
                    binding.expandBottomPanelButton?.text =
                        context().getString(R.string.search_options)
                }
            }

            nextLayout.applyTo(binding.itemPrintLabel)
        }
    }

    private fun setTopPanelAnimation() {
        binding.expandTopPanelButton.setOnClickListener {
            val nextLayout = ConstraintSet()
            if (panelBottomIsExpanded) {
                if (panelTopIsExpanded) {
                    nextLayout.load(this, R.layout.item_print_label_activity_top_panel_collapsed)
                } else {
                    nextLayout.load(this, R.layout.item_print_label_activity)
                }
            } else {
                if (panelTopIsExpanded) {
                    nextLayout.load(this, R.layout.item_print_label_activity_both_panels_collapsed)
                } else {
                    nextLayout.load(this, R.layout.item_print_label_activity_bottom_panel_collapsed)
                }
            }

            panelTopIsExpanded = !panelTopIsExpanded

            val transition = ChangeBounds()
            transition.interpolator = FastOutSlowInInterpolator()
            transition.addListener(object : Transition.TransitionListener {
                override fun onTransitionResume(transition: Transition?) {}
                override fun onTransitionPause(transition: Transition?) {}
                override fun onTransitionStart(transition: Transition?) {}
                override fun onTransitionEnd(transition: Transition?) {
                    refreshTextViews()
                }

                override fun onTransitionCancel(transition: Transition?) {}
            })

            TransitionManager.beginDelayedTransition(binding.itemPrintLabel, transition)

            when {
                panelTopIsExpanded -> {
                    binding.expandTopPanelButton.text = context().getString(R.string.collapse_panel)
                }
                else -> {
                    binding.expandTopPanelButton.text = context().getString(R.string.print_labels)
                }
            }

            nextLayout.applyTo(binding.itemPrintLabel)
        }
    }

    private fun refreshTextViews() {
        runOnUiThread {
            if (panelTopIsExpanded) {
                printLabelFragment!!.refreshViews()
            }

            if (panelBottomIsExpanded) {
                itemSelectFilterFragment!!.refreshTextViews()
            }
        }
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshItem.isRefreshing = show
            }
        }, 20)
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

    private fun getItems() {
        val fr = itemSelectFilterFragment ?: return

        val itemCode = fr.itemCode
        val itemCategory = fr.itemCategory

        if (itemCode.isEmpty() && itemCategory == null) {
            showProgressBar(false)
            return
        }

        try {
            Log.d(this::class.java.simpleName, "Selecting items...")
            ItemCoroutines().getByQuery(
                ean = itemCode.trim(),
                description = itemCode.trim(),
                itemCategoryId = itemCategory?.itemCategoryId
            ) {
                if (it.any()) fillAdapter(it)
                showProgressBar(false)
            }
        } catch (ex: java.lang.Exception) {
            ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())
            showProgressBar(false)
        }
    }

    private fun fillAdapter(t: ArrayList<Item>) {
        showProgressBar(true)

        if (!t.any()) {
            checkedIdArray.clear()
            getItems()
            return
        }
        completeList = t

        runOnUiThread {
            try {
                if (arrayAdapter != null) {
                    lastSelected = arrayAdapter?.currentItem()
                    firstVisiblePos = arrayAdapter?.firstVisiblePos()
                }

                arrayAdapter = ItemAdapter(
                    activity = this,
                    resource = R.layout.item_row,
                    itemList = completeList,
                    suggestedList = ArrayList(),
                    listView = binding.itemListView,
                    multiSelect = multiSelect,
                    checkedIdArray = checkedIdArray
                )

                arrayAdapter?.refreshListeners(
                    checkedChangedListener = this,
                    dataSetChangedListener = this,
                    selectedItemChangedListener = null
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
                binding.totalLabelTextView.text = context().getString(R.string.total)
                binding.qtyReqLabelTextView.text = context().getString(R.string.cant)
                binding.selectedLabelTextView.text = context().getString(R.string.checked)

                if (arrayAdapter != null) {
                    binding.totalTextView.text = arrayAdapter?.count.toString()
                    binding.qtyReqTextView.text = "0"
                    binding.selectedTextView.text = arrayAdapter?.countChecked().toString()
                }
            } else {
                binding.totalLabelTextView.text = context().getString(R.string.total)
                binding.qtyReqLabelTextView.text = context().getString(R.string.cont_)
                binding.selectedLabelTextView.text = context().getString(R.string.items)

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

        Statics.closeKeyboard(this)
        JotterListener.resumeReaderDevices(this)

        thread {
            refreshTextViews()
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
        if (settingViewModel().showScannedCode) makeText(binding.root, scanCode, INFO)

        // Nada que hacer, volver
        if (scanCode.trim().isEmpty()) {
            val res = context().getString(R.string.invalid_code)
            makeText(binding.root, res, ERROR)
            ErrorLog.writeLog(this, this::class.java.simpleName, res)
            return
        }

        JotterListener.lockScanner(this, true)

        CheckItemCode(callback = { onCheckCodeEnded(it) },
            scannedCode = scanCode,
            adapter = arrayAdapter!!,
            onEventData = { showSnackBar(it) }).execute()
    }

    private fun showSnackBar(it: SnackBarEventData) {
        makeText(binding.root, it.text, it.snackBarType)
    }

    override fun onBackPressed() {
        Statics.closeKeyboard(this)

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

        if (!settingViewModel().useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        val drawable = ContextCompat.getDrawable(context(), R.drawable.ic_visibility)
        val toolbar = findViewById<Toolbar>(R.id.action_bar)
        toolbar.overflowIcon = drawable

        // Opciones de visibilidad del menú
        val allControls = SettingsRepository.getAllSelectItemVisibleControls()
        allControls.forEach { p ->
            menu.add(0, p.key.hashCode(), menu.size(), p.description).setChecked(
                itemSelectFilterFragment!!.getVisibleFilters().contains(p)
            ).isCheckable = true
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
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
                (arrayAdapter?.getAll() ?: ArrayList()).mapTo(codes) { it.ean }
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
        when (id) {
            settingRepository().selectItemSearchByItemEan.key.hashCode() -> {
                itemSelectFilterFragment!!.setEanDescriptionVisibility(if (item.isChecked) View.VISIBLE else GONE)
            }
            settingRepository().selectItemSearchByItemCategory.key.hashCode() -> {
                itemSelectFilterFragment!!.setCategoryVisibility(if (item.isChecked) View.VISIBLE else GONE)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun enterCode() {
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

    override fun onFilterChanged(code: String, itemCategory: ItemCategory?, onlyActive: Boolean) {
        Statics.closeKeyboard(this)
        thread {
            checkedIdArray.clear()
            getItems()
        }
    }

    override fun onCheckedChanged(isChecked: Boolean, pos: Int) {
        runOnUiThread {
            binding.selectedTextView.text = arrayAdapter?.countChecked().toString()
        }
    }

    private fun onCheckCodeEnded(it: CheckItemCode.CheckCodeEnded) {
        JotterListener.lockScanner(this, false)

        val item: Item = it.item ?: return

        if (arrayAdapter?.itemExists(item) == true) {
            arrayAdapter?.selectItem(item)
        } else {
            itemSelectFilterFragment!!.itemCode = item.ean
            thread {
                completeList = arrayListOf(item)
                checkedIdArray.clear()
                fillAdapter(completeList)
            }
        }
    }

    override fun onFilterChanged(printer: String, qty: Int?) {}

    override fun onPrintRequested(printer: String, qty: Int) {
        printLabelFragment!!.printItemById(arrayAdapter?.getAllChecked() ?: ArrayList())
    }

    override fun onQtyTextViewFocusChanged(hasFocus: Boolean) {}

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