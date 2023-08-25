package com.dacosys.warehouseCounter.ui.activities.ptlOrder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.PtlOrderActivityBottomPanelCollapsedBinding
import com.dacosys.warehouseCounter.ktor.v1.dto.ptlOrder.Label
import com.dacosys.warehouseCounter.ktor.v1.dto.ptlOrder.PickItem
import com.dacosys.warehouseCounter.ktor.v1.dto.ptlOrder.PtlContent
import com.dacosys.warehouseCounter.ktor.v1.dto.ptlOrder.PtlOrder
import com.dacosys.warehouseCounter.ktor.v1.functions.*
import com.dacosys.warehouseCounter.ktor.v2.dto.barcode.BarcodeLabelTemplate
import com.dacosys.warehouseCounter.ktor.v2.dto.barcode.BarcodeLabelType
import com.dacosys.warehouseCounter.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.ui.adapter.ptlOrder.PtlContentAdapter
import com.dacosys.warehouseCounter.ui.fragments.print.PrintLabelFragment
import com.dacosys.warehouseCounter.ui.fragments.ptlOrder.PtlOrderHeaderFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
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
        stopSyncTimer()

        adapter?.refreshListeners()
    }

    override fun onEditQtyRequired(
        position: Int,
        content: PtlContent,
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

        val waId = warehouseArea?.id

        if (ptlOrder == null || waId == null) {
            gentlyReturn()
            return
        }

        showProgressBar(true)

        val oldOrder = currentPtlOrder
        if (oldOrder == null || oldOrder == ptlOrder) {
            // Es la misma orden o
            // No había ninguna orden seleccionada, buscamos los contenidos directamente...
            getContents(ptlOrder.id, waId)
        } else {
            // Desacoplamos la orden anterior y proseguimos...
            thread {
                DetachOrderToLocation(orderId = oldOrder.id,
                    warehouseAreaId = waId,
                    onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it.text, it.snackBarType) },
                    onFinish = {
                        if (it) getContents(ptlOrder.id, waId)
                        else gentlyReturn()
                    }).execute()
            }
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    private fun getContents(orderId: Long, waId: Long) {
        if (getContentBlocked) return
        getContentBlocked = true

        thread {
            GetPtlOrderContent(orderId = orderId,
                warehouseAreaId = waId,
                onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it.text, it.snackBarType) },
                onFinish = { onGetContent(it) }).execute()
        }
    }

    private fun checkForCompletedOrder(contents: ArrayList<PtlContent>): Boolean {
        if (contents.sumOf { it.qtyCollected } >= contents.sumOf { it.qtyRequested }) {
            showSnackBar(context.getString(R.string.completed_order), SUCCESS)
            return true
        }
        return false
    }

    private fun onGetContent(contents: ArrayList<PtlContent>) {
        // Recién ahora que tenemos los contenidos, definimos la orden actual.
        currentPtlOrder = orderHeaderFragment.ptlOrder

        // Comprobar si la orden fue completada
        if (contents.any() && checkForCompletedOrder(contents)) {
            finishOrder()
        } else {
            val oldOrderId = currentPtlOrder?.id
            val waId = warehouseArea?.id

            if (waId == null || oldOrderId == null) {
                gentlyReturn()
                return
            }

            // Tomamos la orden y actualizamos la lista...
            thread {
                AttachOrderToLocation(orderId = oldOrderId,
                    warehouseAreaId = waId,
                    onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it.text, it.snackBarType) },
                    onFinish = {
                        if (it) fillAdapter(contents)
                        else gentlyReturn()
                    }).execute()
            }
        }
    }

    private fun onGetLabel(it: ArrayList<Label>) {
        if (it.any()) {
            printLabelFragment.printPtlLabels(it)
        }
        gentlyReturn()
    }

    private fun onPickItem(it: ArrayList<PickItem>) {
        val item = it.firstOrNull()
        if (item == null) {
            gentlyReturn()
            return
        }

        runOnUiThread {
            adapter?.updateQtyCollected(
                itemId = item.itemId, qtyCollected = item.qtyCollected.toDouble()
            )

            val all = adapter?.fullList ?: ArrayList()

            // Comprobamos si está completada la orden...
            if (all.any() && checkForCompletedOrder(all)) {
                finishOrder()
            } else {
                gentlyReturn()
            }
        }
    }

    override fun onDataSetChanged() {
        Handler(Looper.getMainLooper()).postDelayed({
            fillSummaryRow()
        }, 100)
    }

    override fun onStart() {
        super.onStart()
        rejectNewInstances = false

        setPanels()

        if (initialScannedCode.isNotEmpty()) {
            scannerCompleted(initialScannedCode)
            initialScannedCode = ""
        }

        if (fillRequired) {
            fillAdapter(completeList)
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
            binding.swipeRefreshWac.isRefreshing = false
        }, 100)
    }

    private var currentInventory: ArrayList<String>? = null

    // Código de escaneo inicial, se envía directamente a la actividad desde HomeActivity
    private var initialScannedCode: String = ""

    private var adapter: PtlContentAdapter? = null
    private var lastSelected: PtlContent? = null
    private var currentScrollPosition: Int = 0
    private var firstVisiblePos: Int? = null

    // Lista temporal que se utiliza entre threads al agregar wacs.
    private var tempContArray: ArrayList<PtlContent> = ArrayList()
    private var completeList: ArrayList<PtlContent> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()

    private var currentPtlOrder: PtlOrder? = null
    private var warehouseArea: WarehouseArea? = null

    private var allowClicks = true
    private var rejectNewInstances = false

    private lateinit var orderHeaderFragment: PtlOrderHeaderFragment
    private lateinit var printLabelFragment: PrintLabelFragment

    private var panelBottomIsExpanded = false
    private var panelTopIsExpanded = true

    private var printQtyIsFocused = false

    private var tempTitle = ""

    private var getContentBlocked = false
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        saveBundleValues(savedInstanceState)
    }

    private fun saveBundleValues(b: Bundle) {
        b.putString(ARG_TITLE, title.toString())

        b.putParcelableArrayList("tempWacArray", tempContArray)
        if (adapter != null) {
            b.putParcelable("lastSelected", adapter?.currentItem())
            b.putInt("firstVisiblePos", adapter?.firstVisiblePos() ?: RecyclerView.NO_POSITION)
            b.putParcelableArrayList("completeList", adapter?.fullList)
            b.putLongArray("checkedIdArray", adapter?.checkedIdArray?.map { it }?.toLongArray())
            b.putInt("currentScrollPosition", currentScrollPosition)
        }

        b.putBoolean("panelTopIsExpanded", panelTopIsExpanded)
        b.putBoolean("panelBottomIsExpanded", panelBottomIsExpanded)

        b.putParcelable(ARG_WAREHOUSE_AREA, orderHeaderFragment.warehouseArea)
        b.putParcelable(ARG_PTL_ORDER, orderHeaderFragment.ptlOrder)
    }

    private fun loadBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.ptl_order)

        warehouseArea = b.getParcelable(ARG_WAREHOUSE_AREA)
        currentPtlOrder = b.getParcelable(ARG_PTL_ORDER)

        currentInventory = b.getStringArrayList("currentInventory")

        tempContArray = b.getParcelableArrayList("tempWacArray") ?: ArrayList()
        completeList = b.getParcelableArrayList("completeList") ?: ArrayList()
        checkedIdArray = (b.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(ArrayList())
        lastSelected = b.getParcelable("lastSelected")
        firstVisiblePos = if (b.containsKey("firstVisiblePos")) b.getInt("firstVisiblePos") else -1
        currentScrollPosition = b.getInt("currentScrollPosition")

        panelBottomIsExpanded = b.getBoolean("panelBottomIsExpanded")
        panelTopIsExpanded = b.getBoolean("panelTopIsExpanded")
    }

    private fun loadExtrasBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.ptl_order)

        warehouseArea = Parcels.unwrap<WarehouseArea>(b.getParcelable(ARG_WAREHOUSE_AREA))
        currentPtlOrder = Parcels.unwrap<PtlOrder>(b.getParcelable(ARG_PTL_ORDER))

        initialScannedCode = b.getString("initial_scanned_code") ?: ""
    }

    private lateinit var binding: PtlOrderActivityBottomPanelCollapsedBinding
    private var fillRequired = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = PtlOrderActivityBottomPanelCollapsedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                currentScrollPosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            }
        })

        fillRequired = true

        orderHeaderFragment = supportFragmentManager.findFragmentById(R.id.headerFragment) as PtlOrderHeaderFragment
        printLabelFragment = supportFragmentManager.findFragmentById(R.id.printFragment) as PrintLabelFragment

        if (savedInstanceState != null) {
            loadBundleValues(savedInstanceState)
        } else {
            // Inicializar la actividad
            val extras = intent.extras
            if (extras != null) loadExtrasBundleValues(extras)
        }

        binding.topAppbar.title = tempTitle

        setupHeaderFragments()
        setupPrinterLabelFragments()

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

        // ESTO SIRVE PARA OCULTAR EL TECLADO EN PANTALLA CUANDO PIERDEN EL FOCO LOS CONTROLES QUE LO NECESITAN
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
    private var changePanelsStateAtFinish: Boolean = false

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

                    postExecuteImeAnimation()
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

    private fun postExecuteImeAnimation() {
        // Si estamos esperando que termine la animación para ejecutar un cambio de vista
        if (changePanelsStateAtFinish) {
            changePanelsStateAtFinish = false
            setPanels()
        } else if (isKeyboardVisible) {
            val orientation = resources.configuration.orientation
            when {
                orientation == Configuration.ORIENTATION_PORTRAIT && !printQtyIsFocused -> {
                    panelBottomIsExpanded = false
                    panelTopIsExpanded = false
                    setPanels()
                }

                orientation == Configuration.ORIENTATION_PORTRAIT && printQtyIsFocused -> {
                    panelBottomIsExpanded = false
                    setPanels()
                }

                orientation != Configuration.ORIENTATION_PORTRAIT && !printQtyIsFocused -> {
                    panelTopIsExpanded = false
                    setPanels()
                }
            }
        }
    }
    // endregion

    private fun setSyncTimer() {
        val oId = currentPtlOrder?.id
        val waId = warehouseArea?.id

        if (oId == null || waId == null) {
            gentlyReturn()
            return
        }

        val updateTime = settingViewModel.wcSyncRefreshOrder

        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                getContents(oId, waId)
            }
        }
        timer?.schedule(timerTask, 0, updateTime * 1000L)
    }

    private fun setupHeaderFragments() {
        // TODO Hacer Builders
        orderHeaderFragment.setOrder(
            order = currentPtlOrder,
            location = warehouseArea,
            sendEvent = false
        )
    }

    private fun setupPrinterLabelFragments() {
        printLabelFragment =
            PrintLabelFragment.Builder()
                .setTemplateTypeIdList(arrayListOf(BarcodeLabelType.order.id))
                .setTemplateId(settingViewModel.defaultOrderTemplateId)
                .build()
        supportFragmentManager.beginTransaction().replace(R.id.printFragment, printLabelFragment).commit()
    }

    private val requiredLayout: Int
        get() {
            val orientation = resources.configuration.orientation
            val r =
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    if (panelBottomIsExpanded) {
                        if (panelTopIsExpanded) {
                            layoutBothPanelsExpanded
                        } else {
                            layoutTopPanelCollapsed
                        }
                    } else if (panelTopIsExpanded) {
                        layoutBottomPanelCollapsed
                    } else {
                        layoutBothPanelsCollapsed
                    }
                } else if (panelTopIsExpanded) {
                    layoutBothPanelsExpanded
                } else {
                    layoutTopPanelCollapsed
                }

            if (BuildConfig.DEBUG) {
                when (r) {
                    layoutBothPanelsExpanded -> println("SELECTED LAYOUT: Both Panels Expanded")
                    layoutBothPanelsCollapsed -> println("SELECTED LAYOUT: Both Panels Collapsed")
                    layoutTopPanelCollapsed -> println("SELECTED LAYOUT: Top Panel Collapsed")
                    layoutBottomPanelCollapsed -> println("SELECTED LAYOUT: Bottom Panel Collapsed")
                }
            }

            return r
        }

    private val layoutBothPanelsExpanded: Int
        get() {
            return R.layout.ptl_order_activity
        }

    private val layoutBottomPanelCollapsed: Int
        get() {
            return R.layout.ptl_order_activity_bottom_panel_collapsed
        }

    private val layoutTopPanelCollapsed: Int
        get() {
            return R.layout.ptl_order_activity_top_panel_collapsed
        }

    private val layoutBothPanelsCollapsed: Int
        get() {
            return R.layout.ptl_order_activity_both_panels_collapsed
        }

    private fun setPanels() {
        val orientation = resources.configuration.orientation

        runOnUiThread {
            val currentLayout = ConstraintSet()
            currentLayout.load(this, requiredLayout)
            currentLayout.applyTo(binding.root)

            if (panelBottomIsExpanded) binding.expandBottomPanelButton?.text =
                context.getString(R.string.collapse_panel)
            else binding.expandBottomPanelButton?.text = context.getString(R.string.item_operations)

            if (panelTopIsExpanded) binding.expandTopPanelButton.text = context.getString(R.string.collapse_panel)
            else binding.expandTopPanelButton.text =
                if (orientation == Configuration.ORIENTATION_PORTRAIT) context.getString(R.string.select_order)
                else context.getString(R.string.print_labels)

            refreshTextViews()
        }
    }

    private fun setBottomPanelAnimation() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) return

        binding.expandBottomPanelButton?.setOnClickListener {
            val bottomVisible = panelBottomIsExpanded
            val imeVisible = isKeyboardVisible
            panelBottomIsExpanded = !panelBottomIsExpanded
            if (!bottomVisible && imeVisible) {
                // Esperar que se cierre el teclado luego de perder el foco el TextView para expandir el panel
                changePanelsStateAtFinish = true
                return@setOnClickListener
            }

            val nextLayout = ConstraintSet()
            nextLayout.load(this, requiredLayout)

            val transition = ChangeBounds()
            transition.interpolator = FastOutSlowInInterpolator()
            transition.addListener(object : Transition.TransitionListener {
                override fun onTransitionResume(transition: Transition) {}
                override fun onTransitionPause(transition: Transition) {}
                override fun onTransitionStart(transition: Transition) {}
                override fun onTransitionEnd(transition: Transition) {
                    refreshTextViews()
                }

                override fun onTransitionCancel(transition: Transition) {}
            })

            TransitionManager.beginDelayedTransition(binding.root, transition)
            nextLayout.applyTo(binding.root)

            if (panelBottomIsExpanded) binding.expandBottomPanelButton?.text =
                getString(R.string.collapse_panel)
            else binding.expandBottomPanelButton?.text = getString(R.string.search_options)
        }
    }

    private fun setTopPanelAnimation() {
        val orientation = resources.configuration.orientation

        binding.expandTopPanelButton.setOnClickListener {
            val topVisible = panelTopIsExpanded
            val imeVisible = isKeyboardVisible
            panelTopIsExpanded = !panelTopIsExpanded
            if (!topVisible && imeVisible) {
                // Esperar que se cierre el teclado luego de perder el foco el TextView para expandir el panel
                changePanelsStateAtFinish = true
                return@setOnClickListener
            }

            val nextLayout = ConstraintSet()
            nextLayout.load(this, requiredLayout)

            val transition = ChangeBounds()
            transition.interpolator = FastOutSlowInInterpolator()
            transition.addListener(object : Transition.TransitionListener {
                override fun onTransitionResume(transition: Transition) {}
                override fun onTransitionPause(transition: Transition) {}
                override fun onTransitionStart(transition: Transition) {}
                override fun onTransitionEnd(transition: Transition) {
                    refreshTextViews()
                }

                override fun onTransitionCancel(transition: Transition) {}
            })

            TransitionManager.beginDelayedTransition(binding.root, transition)
            nextLayout.applyTo(binding.root)

            if (panelTopIsExpanded) binding.expandTopPanelButton.text =
                getString(R.string.collapse_panel)
            else binding.expandTopPanelButton.text =
                if (orientation == Configuration.ORIENTATION_PORTRAIT) context.getString(R.string.select_order)
                else context.getString(R.string.print_labels)
        }
    }

    private fun refreshTextViews() {
        runOnUiThread {
            if (panelTopIsExpanded) printLabelFragment.refreshViews()
        }
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshWac.isRefreshing = show
        }, 20)
    }

    private fun fillAdapter(ptlContArray: ArrayList<PtlContent>) {
        showProgressBar(true)

        runOnUiThread {
            try {
                if (adapter != null) {
                    // Si el adapter es NULL es porque aún no fue creado.
                    // Por lo tanto, puede ser que los valores de [lastSelected]
                    // sean valores guardados de la instancia anterior y queremos preservarlos.
                    lastSelected = adapter?.currentItem()
                }

                adapter = PtlContentAdapter.Builder()
                    .recyclerView(binding.recyclerView)
                    .fullList(ptlContArray)
                    .checkedIdArray(checkedIdArray)
                    .showQtyPanel(true)
                    .dataSetChangedListener(this)
                    .allowEditQty(true, this)
                    .build()

                binding.recyclerView.layoutManager = LinearLayoutManager(this)
                binding.recyclerView.adapter = adapter

                while (binding.recyclerView.adapter == null) {
                    // Horrible wait for a full load
                }

                // Variables locales para evitar cambios posteriores de estado.
                val ls = lastSelected
                val cs = currentScrollPosition
                Handler(Looper.getMainLooper()).postDelayed({
                    adapter?.selectItem(ls, false)
                    adapter?.scrollToPos(cs, true)
                }, 200)
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                gentlyReturn()
            }
        }
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
                    binding.totalTextView.text = adapter?.itemCount.toString()
                    binding.qtyReqTextView.text =
                        Statics.roundToString(adapter?.qtyRequestedTotal() ?: 0.0, 3)
                    binding.selectedTextView.text = adapter?.countChecked().toString()
                }
            } else {
                binding.totalLabelTextView.text = getString(R.string.total)
                binding.qtyReqLabelTextView.text = getString(R.string.cant)
                binding.selectedLabelTextView.text = getString(R.string.cont_)

                if (adapter != null) {
                    binding.totalTextView.text = adapter?.itemCount.toString()
                    binding.qtyReqTextView.text =
                        Statics.roundToString(adapter?.qtyRequestedTotal() ?: 0.0, 3)
                    binding.selectedTextView.text =
                        Statics.roundToString(adapter?.qtyCollectedTotal() ?: 0.0, 3)
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
        if (settingViewModel.showScannedCode) showSnackBar(scanCode, INFO)

        JotterListener.lockScanner(this, true)

        thread {
            GetPtlOrderByCode(code = scanCode,
                onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it.text, it.snackBarType) },
                onFinish = { onGetPtlOrder(it) }).execute()
        }
    }

    private fun onGetPtlOrder(it: ArrayList<PtlOrder>) {
        if (it.any()) {
            orderHeaderFragment.setOrder(order = it.first(), location = warehouseArea)
            return
        }
        gentlyReturn()
    }
    // endregion

    private fun gentlyReturn() {
        Screen.closeKeyboard(this)
        allowClicks = true
        getContentBlocked = false

        JotterListener.lockScanner(this, false)
        rejectNewInstances = false

        showProgressBar(false)
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_read_activity, menu)

        if (!settingViewModel.useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
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
        }
        return true
    }

    private fun finishOrder() {
        val waId = warehouseArea?.id
        val orderId = currentPtlOrder?.id

        if (waId == null || orderId == null) {
            gentlyReturn()
            return
        }

        thread {
            DetachOrderToLocation(orderId = orderId,
                warehouseAreaId = waId,
                onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it.text, it.snackBarType) },
                onFinish = {
                    if (it) finish()
                    else gentlyReturn()
                }).execute()
        }
    }

    private fun flashItem() {
        val waId = warehouseArea?.id
        val orderId = currentPtlOrder?.id

        if (waId == null || orderId == null) {
            gentlyReturn()
            return
        }

        if (!allowClicks) return
        allowClicks = false

        val cc = adapter?.currentItem()

        if (cc != null) {
            thread {
                BlinkOneItem(itemId = cc.itemId,
                    warehouseAreaId = waId,
                    onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it.text, it.snackBarType) },
                    onFinish = { gentlyReturn() }).execute()
            }
        } else {
            thread {
                BlinkAllOrder(orderId = orderId,
                    warehouseAreaId = waId,
                    onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it.text, it.snackBarType) },
                    onFinish = { gentlyReturn() }).execute()
            }
        }
    }

    private fun addBox() {
        val waId = warehouseArea?.id
        val orderId = currentPtlOrder?.id

        if (waId == null || orderId == null) {
            gentlyReturn()
            return
        }

        if (!allowClicks) return
        allowClicks = false

        thread {
            AddBoxToOrder(orderId = orderId, onFinish = {
                if (it) getContents(orderId, waId)
                else gentlyReturn()
            }, onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it.text, it.snackBarType) }).execute()
        }
    }

    private fun manualPick() {
        val waId = warehouseArea?.id
        val orderId = currentPtlOrder?.id
        val cc = adapter?.currentItem()

        if (waId == null || orderId == null || cc == null) {
            gentlyReturn()
            return
        }

        if (!allowClicks) return
        allowClicks = false

        val qtyLeft = (cc.qtyRequested - cc.qtyCollected).toInt()

        thread {
            PickManual(orderId = orderId,
                warehouseAreaId = waId,
                itemId = cc.itemId,
                qty = qtyLeft,
                onFinish = { onPickItem(it) },
                onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it.text, it.snackBarType) }).execute()
        }
    }

    private fun printSelected() {
        val orderId = currentPtlOrder?.id
        if (orderId == null) {
            gentlyReturn()
            return
        }

        if (!allowClicks) return
        allowClicks = false

        thread {
            PrintBox(orderId = orderId,
                onFinish = { onGetLabel(it) },
                onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it.text, it.snackBarType) }).execute()
        }
    }

    override fun onFilterChanged(printer: String, template: BarcodeLabelTemplate?, qty: Int?) {
        val templateId = template?.templateId ?: return
        settingViewModel.defaultOrderTemplateId = templateId
    }

    override fun onPrintRequested(printer: String, qty: Int) {
        printSelected()
    }

    override fun onQtyTextViewFocusChanged(hasFocus: Boolean) {
        printQtyIsFocused = hasFocus
        if (hasFocus) {
            /*
            TODO Transición suave de teclado.
            Acá el teclado Ime aparece y se tienen que colapsar los dos panels.
            Si el teclado Ime ya estaba en la pantalla (por ejemplo el foco estaba el control de cantidad de etiquetas),
            el teclado cambiará de tipo y puede tener una altura diferente.
            Esto no dispara los eventos de animación del teclado.
            Colapsar los paneles y reajustar el Layout al final es la solución temporal.
            */
            panelBottomIsExpanded = false
            panelTopIsExpanded = true
            setPanels()
        }
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_WAREHOUSE_AREA = "warehouseArea"
        const val ARG_PTL_ORDER = "ptlOrder"
    }
}
