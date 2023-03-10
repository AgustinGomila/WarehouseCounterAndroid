package com.dacosys.warehouseCounter.ui.activities.ptlOrder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.constraintlayout.widget.ConstraintSet
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.adapter.ptlItem.PtlContentAdapter
import com.dacosys.warehouseCounter.databinding.PtlOrderActivityBottomPanelCollapsedBinding
import com.dacosys.warehouseCounter.dto.ptlOrder.Label
import com.dacosys.warehouseCounter.dto.ptlOrder.PickItem
import com.dacosys.warehouseCounter.dto.ptlOrder.PtlContent
import com.dacosys.warehouseCounter.dto.ptlOrder.PtlOrder
import com.dacosys.warehouseCounter.dto.warehouse.WarehouseArea
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.retrofit.functions.*
import com.dacosys.warehouseCounter.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.ui.fragments.print.PrintLabelFragment
import com.dacosys.warehouseCounter.ui.fragments.ptlOrder.PtlOrderHeaderFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.SUCCESS
import com.dacosys.warehouseCounter.ui.utils.Screen
import org.parceler.Parcels
import java.util.*
import kotlin.concurrent.thread

class PtlOrderActivity : AppCompatActivity(), PtlContentAdapter.EditQtyListener,
    PtlContentAdapter.DataSetChangedListener, Scanner.ScannerListener, Rfid.RfidDeviceListener,
    PtlOrderHeaderFragment.OrderChangedListener, SwipeRefreshLayout.OnRefreshListener,
    PrintLabelFragment.FragmentListener {
    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        adapter?.refreshListeners(null, null, null)
        orderHeaderFragment?.onDestroy()
        printLabelFragment?.onDestroy()
        stopSyncTimer()
    }

    override fun onEditQtyRequired(
        position: Int,
        item: PtlContent,
        initialQty: Double,
        minValue: Double,
        maxValue: Double,
        multiplier: Int,
    ) {
    }

    override fun onOrderChanged(ptlOrder: PtlOrder?) {
        if (completeList.any()) {
            val r = completeList
            completeList.clear()

            fillAdapter(r)
            return
        }

        val newOrder = ptlOrder ?: return
        val waId = warehouseArea?.id ?: return
        currentPtlOrder = newOrder

        showProgressBar(true)

        val oldOrder = orderHeaderFragment?.ptlOrder
        if (oldOrder == null || oldOrder == newOrder) {
            // Es la misma orden o
            // No había ninguna orden seleccionada, buscamos los contenidos directamente...
            getContents(newOrder.id, waId)
        } else {
            // Desacoplamos la orden anterior y proseguimos...
            thread {
                DetachOrderToLocation(orderId = oldOrder.id,
                    warehouseAreaId = waId,
                    onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) },
                    onFinish = {
                        if (it) getContents(newOrder.id, waId)
                        else gentlyReturn()
                    }).execute()
            }
        }
    }

    private fun showSnackBar(it: SnackBarEventData) {
        makeText(binding.root, it.text, it.snackBarType)
    }

    private fun getContents(oldOrderId: Long, waId: Long) {
        if (getContentBlocked) {
            showProgressBar(false)
            return
        }
        getContentBlocked = true

        thread {
            GetPtlOrderContent(orderId = oldOrderId,
                warehouseAreaId = waId,
                onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) },
                onFinish = { onGetContent(it) }).execute()
        }
    }

    private fun checkForCompletedOrder(contents: ArrayList<PtlContent>): Boolean {
        if (contents.sumOf { it.qtyCollected } >= contents.sumOf { it.qtyRequested }) {
            makeText(this, context.getString(R.string.completed_order), SUCCESS)
            return true
        }
        return false
    }

    private fun onGetContent(contents: ArrayList<PtlContent>) {
        // Comprobar si la orden fue completada
        if (contents.any() && checkForCompletedOrder(contents)) {
            finish()
        } else {
            val oldOrder = orderHeaderFragment?.ptlOrder
            if (oldOrder == currentPtlOrder) {
                // Actualizamos la lista...
                fillAdapter(contents)
            } else {
                val waId = warehouseArea?.id ?: return
                val oldOrderId = oldOrder?.id ?: return

                // Tomamos la orden y actualizamos la lista...
                thread {
                    AttachOrderToLocation(orderId = oldOrderId,
                        warehouseAreaId = waId,
                        onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) },
                        onFinish = {
                            if (it) fillAdapter(contents)
                            else gentlyReturn()
                        }).execute()
                }
            }
            getContentBlocked = false
        }
    }

    private fun onGetLabel(it: ArrayList<Label>) {
        printLabelFragment?.printPtlLabels(it)
    }

    private fun onPickItem(it: ArrayList<PickItem>) {
        runOnUiThread {
            val item = it.firstOrNull() ?: return@runOnUiThread

            adapter?.updateQtyCollected(
                itemId = item.itemId, qtyCollected = item.qtyCollected.toDouble()
            )

            val all = adapter?.getAll() ?: ArrayList()

            // Comprobamos si está completada la orden...
            if (all.any() && checkForCompletedOrder(all)) {
                finish()
            }
        }
    }

    override fun onDataSetChanged() {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                fillSummaryRow()
            }
        }, 100)
    }

    override fun onStart() {
        super.onStart()
        rejectNewInstances = false

        refreshAdapterListeners()

        if (initialScannedCode.isNotEmpty()) {
            scannerCompleted(initialScannedCode)
            initialScannedCode = ""
        }

        setSyncTimer()
    }

    override fun onStop() {
        super.onStop()
        stopSyncTimer()
    }

    private fun stopSyncTimer() {
        timer?.cancel()
        timer = null

        timerTask?.cancel()
        timerTask = null
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshWac.isRefreshing = false
            }
        }, 100)
    }

    private var currentInventory: ArrayList<String>? = null

    // Código de escaneo inicial, se envía directamente a la actividad desde HomeActivity
    private var initialScannedCode: String = ""

    private var adapter: PtlContentAdapter? = null
    private var lastSelected: PtlContent? = null
    private var firstVisiblePos: Int? = null

    // Lista temporal que se utiliza entre threads al agregar wacs.
    private var tempContArray: ArrayList<PtlContent> = ArrayList()
    private var completeList: ArrayList<PtlContent> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()

    private var currentPtlOrder: PtlOrder? = null
    private var warehouseArea: WarehouseArea? = null

    private var allowClicks = true
    private var rejectNewInstances = false

    private var orderHeaderFragment: PtlOrderHeaderFragment? = null
    private var printLabelFragment: PrintLabelFragment? = null

    private var panelBottomIsExpanded = false
    private var panelTopIsExpanded = true

    private var tempTitle = ""

    private var getContentBlocked = false
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null

    override fun onSaveInstanceState(b: Bundle) {
        super.onSaveInstanceState(b)

        b.putString("title", title.toString())

        b.putParcelableArrayList("tempWacArray", tempContArray)
        if (adapter != null) {
            b.putParcelable("lastSelected", (adapter ?: return).currentContent())
            b.putInt("firstVisiblePos", (adapter ?: return).firstVisiblePos())
            b.putParcelableArrayList("completeList", adapter?.getAll())
            b.putLongArray("checkedIdArray", adapter?.getAllIdChecked()?.map { it }?.toLongArray())
        }

        b.putBoolean("panelTopIsExpanded", panelTopIsExpanded)
        b.putBoolean("panelBottomIsExpanded", panelBottomIsExpanded)

        if (orderHeaderFragment != null) {
            b.putParcelable("warehouseArea", orderHeaderFragment!!.warehouseArea)
            b.putParcelable("ptlOrder", orderHeaderFragment!!.ptlOrder)
        }
    }

    private fun loadBundleValues(b: Bundle) {
        val t1 = b.getString("title")
        if (t1 != null && t1.isNotEmpty()) tempTitle = t1

        warehouseArea = b.getParcelable("warehouseArea")
        currentPtlOrder = b.getParcelable("ptlOrder")

        currentInventory = b.getStringArrayList("currentInventory")

        tempContArray = b.getParcelableArrayList("tempWacArray") ?: ArrayList()
        completeList = b.getParcelableArrayList("completeList") ?: ArrayList()
        checkedIdArray =
            (b.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(ArrayList())
        lastSelected = b.getParcelable("lastSelected")
        firstVisiblePos = if (b.containsKey("firstVisiblePos")) b.getInt(
            "firstVisiblePos"
        ) else -1

        panelBottomIsExpanded = b.getBoolean("panelBottomIsExpanded")
        panelTopIsExpanded = b.getBoolean("panelTopIsExpanded")
    }

    private fun loadExtrasBundleValues(b: Bundle) {
        val t1 = b.getString("title")
        if (t1 != null && t1.isNotEmpty()) tempTitle = t1

        warehouseArea = Parcels.unwrap<WarehouseArea>(b.getParcelable("warehouseArea"))
        currentPtlOrder = Parcels.unwrap<PtlOrder>(b.getParcelable("ptlOrder"))

        initialScannedCode = b.getString("initial_scanned_code") ?: ""
    }

    private lateinit var binding: PtlOrderActivityBottomPanelCollapsedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = PtlOrderActivityBottomPanelCollapsedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tempTitle = getString(R.string.ptl_order)

        if (savedInstanceState != null) {
            loadBundleValues(savedInstanceState)
        } else {
            // Inicializar la actividad
            val extras = intent.extras
            if (extras != null) loadExtrasBundleValues(extras)
        }

        title = tempTitle

        binding.swipeRefreshWac.setOnRefreshListener(this)
        binding.swipeRefreshWac.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Para expandir y colapsar el panel inferior
        setBottomPanelAnimation()
        setTopPanelAnimation()

        binding.okButton.setOnClickListener { finishOrder() }

        binding.flashButton.setOnClickListener { flashItem() }

        binding.manualButton.setOnClickListener { manualPick() }

        binding.addBoxButton.setOnClickListener { addBox() }

        setupHeader()

        setPanels()

        // ESTO SIRVE PARA OCULTAR EL TECLADO EN PANTALLA CUANDO PIERDEN EL FOCO LOS CONTROLES QUE LO NECESITAN
        Screen.setupUI(binding.root, this)
    }

    private fun setSyncTimer() {
        val oId = currentPtlOrder?.id ?: return
        val waId = warehouseArea?.id ?: return
        val updateTime = settingViewModel.wcSyncRefreshOrder

        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                getContents(oId, waId)
            }
        }
        timer?.schedule(timerTask, 0, updateTime * 1000L)
    }

    private fun setupHeader() {
        if (orderHeaderFragment == null) orderHeaderFragment =
            supportFragmentManager.findFragmentById(R.id.headerFragment) as PtlOrderHeaderFragment

        orderHeaderFragment?.setChangeOrderListener(this)
        orderHeaderFragment?.setOrder(currentPtlOrder, warehouseArea)

        if (printLabelFragment == null) printLabelFragment =
            supportFragmentManager.findFragmentById(R.id.printFragment) as PrintLabelFragment

        printLabelFragment?.setListener(this)
    }

    private fun setPanels() {
        val orientation = resources.configuration.orientation

        val currentLayout = ConstraintSet()
        if (panelBottomIsExpanded) {
            if (panelTopIsExpanded) {
                currentLayout.load(this, R.layout.ptl_order_activity)
            } else {
                currentLayout.load(this, R.layout.ptl_order_activity_top_panel_collapsed)
            }
        } else {
            if (panelTopIsExpanded) {
                currentLayout.load(this, R.layout.ptl_order_activity_bottom_panel_collapsed)
            } else {
                currentLayout.load(this, R.layout.ptl_order_activity_both_panels_collapsed)
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

        TransitionManager.beginDelayedTransition(binding.ptlOrderContent, transition)

        currentLayout.applyTo(binding.ptlOrderContent)

        when {
            panelBottomIsExpanded -> {
                binding.expandBottomPanelButton?.text = context.getString(R.string.collapse_panel)
            }
            else -> {
                binding.expandBottomPanelButton?.text = context.getString(R.string.item_operations)
            }
        }

        when {
            panelTopIsExpanded -> {
                binding.expandTopPanelButton.text = context.getString(R.string.collapse_panel)
            }
            else -> {
                binding.expandTopPanelButton.text =
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) context.getString(R.string.select_order) else context.getString(
                        R.string.print_labels
                    )
            }
        }
    }

    private fun setBottomPanelAnimation() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            return
        }

        binding.expandBottomPanelButton?.setOnClickListener {
            val nextLayout = ConstraintSet()
            if (panelBottomIsExpanded) {
                if (panelTopIsExpanded) {
                    nextLayout.load(this, R.layout.ptl_order_activity_bottom_panel_collapsed)
                } else {
                    nextLayout.load(this, R.layout.ptl_order_activity_both_panels_collapsed)
                }
            } else {
                if (panelTopIsExpanded) {
                    nextLayout.load(this, R.layout.ptl_order_activity)
                } else {
                    nextLayout.load(this, R.layout.ptl_order_activity_top_panel_collapsed)
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

            TransitionManager.beginDelayedTransition(binding.ptlOrderContent, transition)

            when {
                panelBottomIsExpanded -> {
                    binding.expandBottomPanelButton?.text =
                        context.getString(R.string.collapse_panel)
                }
                else -> {
                    binding.expandBottomPanelButton?.text =
                        context.getString(R.string.item_operations)
                }
            }

            nextLayout.applyTo(binding.ptlOrderContent)
        }
    }

    private fun setTopPanelAnimation() {
        val orientation = resources.configuration.orientation

        binding.expandTopPanelButton.setOnClickListener {
            val nextLayout = ConstraintSet()
            if (panelBottomIsExpanded) {
                if (panelTopIsExpanded) {
                    nextLayout.load(this, R.layout.ptl_order_activity_top_panel_collapsed)
                } else {
                    nextLayout.load(this, R.layout.ptl_order_activity)
                }
            } else {
                if (panelTopIsExpanded) {
                    nextLayout.load(this, R.layout.ptl_order_activity_both_panels_collapsed)
                } else {
                    nextLayout.load(this, R.layout.ptl_order_activity_bottom_panel_collapsed)
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

            TransitionManager.beginDelayedTransition(binding.ptlOrderContent, transition)

            when {
                panelTopIsExpanded -> {
                    binding.expandTopPanelButton.text = context.getString(R.string.collapse_panel)
                }
                else -> {
                    binding.expandTopPanelButton.text =
                        if (orientation == Configuration.ORIENTATION_PORTRAIT) context.getString(R.string.select_order) else context.getString(
                            R.string.print_labels
                        )
                }
            }

            nextLayout.applyTo(binding.ptlOrderContent)
        }
    }

    private fun refreshTextViews() {
        runOnUiThread {
            if (panelTopIsExpanded) {
                printLabelFragment?.refreshViews()
            }
        }
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshWac.isRefreshing = show
            }
        }, 20)
    }

    private fun fillAdapter(ptlContArray: ArrayList<PtlContent>) {
        showProgressBar(true)

        runOnUiThread {
            try {
                if (adapter != null) {
                    lastSelected = (adapter ?: return@runOnUiThread).currentContent()
                    firstVisiblePos = (adapter ?: return@runOnUiThread).firstVisiblePos()
                }

                adapter = PtlContentAdapter(
                    activity = this,
                    resource = R.layout.ptl_item_row,
                    ptlContents = ptlContArray,
                    checkedIdArray = checkedIdArray,
                    listView = binding.itemListView,
                    multiSelect = false,
                    showQtyPanel = true,
                    allowEditQty = false,
                    setQtyOnCheckedChanged = false
                )

                refreshAdapterListeners()

                while (binding.itemListView.adapter == null) {
                    // Horrible wait for full load
                }

                adapter?.setSelectItemAndScrollPos(lastSelected, firstVisiblePos)
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                gentlyReturn()
            }
        }
    }

    private fun refreshAdapterListeners() {
        adapter?.refreshListeners(
            checkedChangedListener = null, dataSetChangedListener = this, editQtyListener = this
        )
    }

    private val multiselect: Boolean
        get() {
            return adapter?.multiSelect ?: false
        }

    private fun fillSummaryRow() {
        runOnUiThread {
            if (multiselect) {
                binding.totalLabelTextView.text = getString(R.string.total)
                binding.qtyReqLabelTextView.text = getString(R.string.cant)
                binding.selectedLabelTextView.text = getString(R.string.checked)

                if (adapter != null) {
                    binding.totalTextView.text = adapter!!.count.toString()
                    binding.qtyReqTextView.text =
                        Statics.roundToString(adapter!!.qtyRequestedTotal(), 3)
                    binding.selectedTextView.text = adapter!!.countChecked().toString()
                }
            } else {
                binding.totalLabelTextView.text = getString(R.string.total)
                binding.qtyReqLabelTextView.text = getString(R.string.cant)
                binding.selectedLabelTextView.text = getString(R.string.cont_)

                if (adapter != null) {
                    val t = adapter!!.count
                    val r = adapter!!.qtyRequestedTotal()
                    val c = adapter!!.qtyCollectedTotal()
                    binding.totalTextView.text = t.toString()
                    binding.qtyReqTextView.text = r.toString()
                    binding.selectedTextView.text = c.toString()
                }
            }

            if (adapter == null) {
                binding.totalTextView.text = 0.toString()
                binding.qtyReqTextView.text = 0.toString()
                binding.selectedTextView.text = 0.toString()
            }
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

    // region RFID Things
    override fun onReadCompleted(scanCode: String) {
        if (currentInventory == null) {
            currentInventory = ArrayList()
        }

        if (!currentInventory!!.contains(scanCode)) {
            currentInventory!!.add(scanCode)
        }

        scannerCompleted(scanCode)
    }

    override fun onWriteCompleted(isOk: Boolean) {}

    override fun onGetBluetoothName(name: String) {}

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Nfc.nfcHandleIntent(intent, this)
    }
    // endregion RFID Things

    // region Events from SCANNER, RFID, NFC
    override fun scannerCompleted(scanCode: String) {
        if (settingViewModel.showScannedCode) makeText(binding.root, scanCode, INFO)

        JotterListener.lockScanner(this, true)

        thread {
            GetPtlOrderByCode(code = scanCode,
                onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) },
                onFinish = { if (it.any()) onGetPtlOrder(it) }).execute()
        }
    }

    private fun onGetPtlOrder(it: ArrayList<PtlOrder>) {
        if (it.any()) {
            orderHeaderFragment?.setOrder(it.first(), warehouseArea)
            return
        }
        gentlyReturn()
    }
    // endregion

    private fun gentlyReturn() {
        Screen.closeKeyboard(this)
        allowClicks = true

        JotterListener.lockScanner(this, false)
        rejectNewInstances = false

        showProgressBar(false)
    }

    private val menuItemRandomIt = 999001
    private val menuItemManualCode = 999002
    private val menuItemRandomOnListL = 999003
    private val menuRegexItem = 999004

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_read_activity, menu)

        if (!settingViewModel.useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        if (BuildConfig.DEBUG || Statics.testMode) {
            menu.add(Menu.NONE, menuItemManualCode, Menu.NONE, "Manual code")
            menu.add(Menu.NONE, menuItemRandomIt, Menu.NONE, "Random item")
            menu.add(Menu.NONE, menuItemRandomOnListL, Menu.NONE, "Random item on list")
            menu.add(Menu.NONE, menuRegexItem, Menu.NONE, "Regex")
        }

        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

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
                (adapter?.getAll()
                    ?: ArrayList<PtlContent>().filter { it.item.first().ean.isNotEmpty() }).mapTo(
                    codes
                ) {
                    it.item.first().ean
                }
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
            menuRegexItem -> {
                scannerCompleted("0SM20220721092826007792261002857001038858")
                return super.onOptionsItemSelected(item)
            }
        }
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

    private fun finishOrder() {
        val waId = warehouseArea?.id ?: return
        val orderId = currentPtlOrder?.id ?: return

        if (allowClicks) {
            allowClicks = false

            thread {
                DetachOrderToLocation(orderId = orderId,
                    warehouseAreaId = waId,
                    onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) },
                    onFinish = {
                        if (it) finish()
                        else allowClicks = true
                    }).execute()
            }
        }
    }

    private fun flashItem() {
        val waId = warehouseArea?.id ?: return
        val orderId = currentPtlOrder?.id ?: return

        if (allowClicks) {
            allowClicks = false

            val cc = adapter?.currentContent()

            if (cc != null) {
                thread {
                    BlinkOneItem(itemId = cc.itemId,
                        warehouseAreaId = waId,
                        onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) },
                        onFinish = { allowClicks = true }).execute()
                }
            } else {
                thread {
                    BlinkAllOrder(orderId = orderId,
                        warehouseAreaId = waId,
                        onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) },
                        onFinish = { allowClicks = true }).execute()
                }
            }
        }
    }

    private fun addBox() {
        val waId = warehouseArea?.id ?: return
        val orderId = currentPtlOrder?.id ?: return

        if (allowClicks) {
            allowClicks = false

            thread {
                AddBoxToOrder(orderId = orderId, onFinish = {
                    if (it) getContents(orderId, waId)
                    allowClicks = true
                }, onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) }).execute()
            }
        }
    }

    private fun manualPick() {
        val waId = warehouseArea?.id ?: return
        val orderId = currentPtlOrder?.id ?: return
        val cc = adapter?.currentContent() ?: return

        if (allowClicks) {
            allowClicks = false

            thread {
                PickManual(orderId = orderId,
                    warehouseAreaId = waId,
                    itemId = cc.itemId,
                    qty = 1,
                    onFinish = {
                        if (it.any()) onPickItem(it)
                        allowClicks = true
                    },
                    onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) }).execute()
            }
        }
    }

    private fun printSelected() {
        val orderId = currentPtlOrder?.id ?: return

        if (allowClicks) {
            allowClicks = false

            thread {
                PrintBox(orderId = orderId, onFinish = {
                    if (it.any()) onGetLabel(it)
                    allowClicks = true
                }, onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) }).execute()
            }
        }
    }

    override fun onFilterChanged(printer: String, qty: Int?) {
    }

    override fun onPrintRequested(printer: String, qty: Int) {
        printSelected()
    }

    override fun onQtyTextViewFocusChanged(hasFocus: Boolean) {
    }
}