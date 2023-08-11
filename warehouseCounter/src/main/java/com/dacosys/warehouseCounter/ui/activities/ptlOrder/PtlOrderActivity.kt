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
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
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
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.PtlOrderActivityBottomPanelCollapsedBinding
import com.dacosys.warehouseCounter.ktor.v1.dto.ptlOrder.Label
import com.dacosys.warehouseCounter.ktor.v1.dto.ptlOrder.PickItem
import com.dacosys.warehouseCounter.ktor.v1.dto.ptlOrder.PtlContent
import com.dacosys.warehouseCounter.ktor.v1.dto.ptlOrder.PtlOrder
import com.dacosys.warehouseCounter.ktor.v1.functions.*
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
        adapter?.refreshListeners()
        orderHeaderFragment?.onDestroy()
        printLabelFragment?.onDestroy()
        stopSyncTimer()
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
                    onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) },
                    onFinish = {
                        if (it) getContents(ptlOrder.id, waId)
                        else gentlyReturn()
                    }).execute()
            }
        }
    }

    private fun showSnackBar(it: SnackBarEventData) {
        makeText(binding.root, it.text, it.snackBarType)
    }

    private fun getContents(orderId: Long, waId: Long) {
        if (getContentBlocked) return
        getContentBlocked = true

        thread {
            GetPtlOrderContent(orderId = orderId,
                warehouseAreaId = waId,
                onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) },
                onFinish = { onGetContent(it) }).execute()
        }
    }

    private fun checkForCompletedOrder(contents: ArrayList<PtlContent>): Boolean {
        if (contents.sumOf { it.qtyCollected } >= contents.sumOf { it.qtyRequested }) {
            showSnackBar(SnackBarEventData(context.getString(R.string.completed_order), SUCCESS))
            return true
        }
        return false
    }

    private fun onGetContent(contents: ArrayList<PtlContent>) {
        // Recién ahora que tenemos los contenidos, definimos la orden actual.
        currentPtlOrder = orderHeaderFragment?.ptlOrder

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
                    onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) },
                    onFinish = {
                        if (it) fillAdapter(contents)
                        else gentlyReturn()
                    }).execute()
            }
        }
    }

    private fun onGetLabel(it: ArrayList<Label>) {
        if (it.any()) {
            printLabelFragment?.printPtlLabels(it)
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

        if (orderHeaderFragment != null) {
            b.putParcelable(ARG_WAREHOUSE_AREA, orderHeaderFragment!!.warehouseArea)
            b.putParcelable(ARG_PTL_ORDER, orderHeaderFragment!!.ptlOrder)
        }
    }

    private fun loadBundleValues(b: Bundle) {
        val t1 = b.getString(ARG_TITLE)
        if (!t1.isNullOrEmpty()) tempTitle = t1

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
        val t1 = b.getString(ARG_TITLE)
        if (!t1.isNullOrEmpty()) tempTitle = t1

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
    private var changePanelTopStateAtFinish: Boolean = false
    private var changePanelBottomStateAtFinish: Boolean = false

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
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
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
        // Si estamos mostrando el teclado, colapsamos los paneles.
        if (isKeyboardVisible) {
            when {
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT && !qtyPrinterIsFocused -> {
                    collapseBottomPanel()
                    collapseTopPanel()
                }

                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT && qtyPrinterIsFocused -> {
                    collapseBottomPanel()
                }

                resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT && !qtyPrinterIsFocused -> {
                    collapseTopPanel()
                }
            }
        }

        // Si estamos esperando que termine la animación para ejecutar un cambio de vista
        if (changePanelTopStateAtFinish) {
            changePanelTopStateAtFinish = false
            binding.expandTopPanelButton.performClick()
        }
        if (changePanelBottomStateAtFinish) {
            changePanelBottomStateAtFinish = false
            binding.expandBottomPanelButton?.performClick()
        }
    }

    private fun collapseBottomPanel() {
        if (panelBottomIsExpanded && resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            runOnUiThread {
                binding.expandBottomPanelButton?.performClick()
            }
        }
    }

    private fun collapseTopPanel() {
        if (panelTopIsExpanded) {
            runOnUiThread {
                binding.expandTopPanelButton.performClick()
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

    private fun setupHeader() {
        if (orderHeaderFragment == null)
            orderHeaderFragment = supportFragmentManager.findFragmentById(R.id.headerFragment) as PtlOrderHeaderFragment

        orderHeaderFragment?.setOrder(order = currentPtlOrder, location = warehouseArea, sendEvent = false)
        orderHeaderFragment?.setChangeOrderListener(this)

        if (printLabelFragment == null)
            printLabelFragment = supportFragmentManager.findFragmentById(R.id.printFragment) as PrintLabelFragment

        printLabelFragment?.setListener(this)
    }

    private fun setPanels() {
        val orientation = resources.configuration.orientation

        val currentLayout = ConstraintSet()
        if (panelBottomIsExpanded) {
            if (panelTopIsExpanded) currentLayout.load(this, R.layout.ptl_order_activity)
            else currentLayout.load(this, R.layout.ptl_order_activity_top_panel_collapsed)
        } else {
            if (panelTopIsExpanded) currentLayout.load(this, R.layout.ptl_order_activity_bottom_panel_collapsed)
            else currentLayout.load(this, R.layout.ptl_order_activity_both_panels_collapsed)
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

        if (panelBottomIsExpanded) binding.expandBottomPanelButton?.text = context.getString(R.string.collapse_panel)
        else binding.expandBottomPanelButton?.text = context.getString(R.string.item_operations)

        if (panelTopIsExpanded) binding.expandTopPanelButton.text = context.getString(R.string.collapse_panel)
        else binding.expandTopPanelButton.text =
            if (orientation == Configuration.ORIENTATION_PORTRAIT) context.getString(R.string.select_order)
            else context.getString(R.string.print_labels)
    }

    private fun setBottomPanelAnimation() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) return

        binding.expandBottomPanelButton?.setOnClickListener {
            val bottomVisible = panelBottomIsExpanded
            val imeVisible = isKeyboardVisible

            if (!bottomVisible && imeVisible) {
                // Esperar que se cierre el teclado luego de perder el foco el TextView para expandir el panel
                changePanelBottomStateAtFinish = true
                return@setOnClickListener
            }

            val nextLayout = ConstraintSet()
            if (panelBottomIsExpanded) {
                if (panelTopIsExpanded) nextLayout.load(this, R.layout.ptl_order_activity_bottom_panel_collapsed)
                else nextLayout.load(this, R.layout.ptl_order_activity_both_panels_collapsed)
            } else if (panelTopIsExpanded) {
                nextLayout.load(this, R.layout.ptl_order_activity)
            } else {
                nextLayout.load(this, R.layout.ptl_order_activity_top_panel_collapsed)
            }

            panelBottomIsExpanded = !panelBottomIsExpanded
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

            TransitionManager.beginDelayedTransition(binding.ptlOrderContent, transition)
            nextLayout.applyTo(binding.ptlOrderContent)

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

            if (!topVisible && imeVisible) {
                // Esperar que se cierre el teclado luego de perder el foco el TextView para expandir el panel
                changePanelTopStateAtFinish = true
                return@setOnClickListener
            }

            val nextLayout = ConstraintSet()
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (panelBottomIsExpanded) {
                    if (panelTopIsExpanded) nextLayout.load(this, R.layout.ptl_order_activity_top_panel_collapsed)
                    else nextLayout.load(this, R.layout.ptl_order_activity)
                } else if (panelTopIsExpanded) {
                    nextLayout.load(this, R.layout.ptl_order_activity_both_panels_collapsed)
                } else {
                    nextLayout.load(this, R.layout.ptl_order_activity_bottom_panel_collapsed)
                }
            } else if (panelTopIsExpanded) {
                nextLayout.load(this, R.layout.ptl_order_activity_top_panel_collapsed)
            } else {
                nextLayout.load(this, R.layout.ptl_order_activity)
            }

            panelTopIsExpanded = !panelTopIsExpanded

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

            TransitionManager.beginDelayedTransition(binding.ptlOrderContent, transition)
            nextLayout.applyTo(binding.ptlOrderContent)

            if (panelTopIsExpanded) binding.expandTopPanelButton.text =
                getString(R.string.collapse_panel)
            else binding.expandTopPanelButton.text =
                if (orientation == Configuration.ORIENTATION_PORTRAIT) context.getString(R.string.select_order) else context.getString(
                    R.string.print_labels
                )

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

                // Estas variables locales evitar posteriores cambios de estado.
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
        if (settingViewModel.showScannedCode) showSnackBar(SnackBarEventData(scanCode, INFO))

        JotterListener.lockScanner(this, true)

        thread {
            GetPtlOrderByCode(code = scanCode,
                onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) },
                onFinish = { onGetPtlOrder(it) }).execute()
        }
    }

    private fun onGetPtlOrder(it: ArrayList<PtlOrder>) {
        if (it.any()) {
            orderHeaderFragment?.setOrder(order = it.first(), location = warehouseArea)
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
        val waId = warehouseArea?.id
        val orderId = currentPtlOrder?.id

        if (waId == null || orderId == null) {
            gentlyReturn()
            return
        }

        thread {
            DetachOrderToLocation(orderId = orderId,
                warehouseAreaId = waId,
                onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) },
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
                    onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) },
                    onFinish = { gentlyReturn() }).execute()
            }
        } else {
            thread {
                BlinkAllOrder(orderId = orderId,
                    warehouseAreaId = waId,
                    onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) },
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
            }, onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) }).execute()
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
                onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) }).execute()
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
                onEvent = { if (it.snackBarType != SUCCESS) showSnackBar(it) }).execute()
        }
    }

    override fun onFilterChanged(printer: String, qty: Int?) {
    }

    override fun onPrintRequested(printer: String, qty: Int) {
        printSelected()
    }

    private var qtyPrinterIsFocused = false
    override fun onQtyTextViewFocusChanged(hasFocus: Boolean) {
        qtyPrinterIsFocused = hasFocus
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_WAREHOUSE_AREA = "warehouseArea"
        const val ARG_PTL_ORDER = "ptlOrder"
    }
}