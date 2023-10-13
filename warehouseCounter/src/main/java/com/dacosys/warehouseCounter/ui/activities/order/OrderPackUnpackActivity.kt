package com.dacosys.warehouseCounter.ui.activities.order

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.format.DateFormat
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
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
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderStatus
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.GetRack
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.GetWarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.GetOrder
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.UpdateOrder
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.data.settings.SettingsRepository
import com.dacosys.warehouseCounter.databinding.OrderLocationActivityBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
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
import com.dacosys.warehouseCounter.ui.adapter.FilterOptions
import com.dacosys.warehouseCounter.ui.adapter.order.OrderAdapter
import com.dacosys.warehouseCounter.ui.fragments.common.SearchTextFragment
import com.dacosys.warehouseCounter.ui.fragments.common.SelectFilterFragment
import com.dacosys.warehouseCounter.ui.fragments.common.SummaryFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelableArrayList
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.utils.Screen.Companion.closeKeyboard
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*

class OrderPackUnpackActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    Scanner.ScannerListener, Rfid.RfidDeviceListener,
    SelectFilterFragment.OnFilterOrderChangedListener, OrderAdapter.CheckedChangedListener,
    OrderAdapter.DataSetChangedListener,
    SearchTextFragment.OnSearchTextFocusChangedListener, SearchTextFragment.OnSearchTextChangedListener {

    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        adapter?.refreshListeners()
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshItem.isRefreshing = false
        }, 100)
    }

    private var tempTitle = ""

    private var rejectNewInstances = false

    private var isFinishingByUser = false

    private var searchTextIsFocused = false

    private var showSelectButton = true

    private var multiSelect = false
    private var adapter: OrderAdapter? = null
    private var lastSelected: OrderResponse? = null
    private var firstVisiblePos: Int? = null
    private var currentScrollPosition: Int = 0

    // Se usa para saber si estamos en onStart luego de onCreate
    private var fillRequired = false

    private var panelIsExpanded = true

    private var hideFilterPanel = false

    private var completeList: ArrayList<OrderResponse> = ArrayList()
    private var checkedHashArray: ArrayList<Int> = ArrayList()

    private var showCheckBoxes
        get() =
            if (!multiSelect) false
            else settingsVm.itemSelectShowCheckBoxes
        set(value) {
            settingsVm.itemSelectShowCheckBoxes = value
        }

    private val countChecked: Int
        get() {
            return adapter?.countChecked() ?: 0
        }

    private val allChecked: ArrayList<OrderResponse>
        get() {
            return adapter?.getAllChecked() ?: arrayListOf()
        }

    private val currentItem: OrderResponse?
        get() {
            return adapter?.currentItem()
        }

    /** Acá seleccionamos siguiendo estos criterios:
     * 1. Si NO es [multiSelect] tomamos el ítem seleccionado de forma simple.
     * 2. Si es [multiSelect] nos fijamos que o bien estén marcados algunos ítems o bien tengamos un ítem seleccionado de forma simple.
     *     1. Si es así, vamos a devolver los ítems marcados si existen como prioridad.
     *     2. Si no, nos fijamos que NO sean visibles los CheckBoxes, esto quiere decir que el usuario está seleccionado el ítem de forma simple y devolvemos este ítem.
     **/
    private fun selectedOrders(): ArrayList<OrderResponse> {
        val item = currentItem
        val countChecked = countChecked
        var itemArray: ArrayList<OrderResponse> = ArrayList()

        if (!multiSelect && item != null) {
            itemArray = arrayListOf(item)
        } else if (multiSelect) {
            if (countChecked > 0 || item != null) {
                if (countChecked > 0) {
                    itemArray = allChecked
                } else if (!showCheckBoxes) {
                    itemArray = arrayListOf(item!!)
                }
                itemArray.map { it } as ArrayList<OrderResponse>
            }
        }
        return itemArray
    }

    private lateinit var filterFragment: SelectFilterFragment
    private lateinit var summaryFragment: SummaryFragment
    private lateinit var searchTextFragment: SearchTextFragment

    private var filterOrderId: String = ""
    private var filterOrderExternalId: String = ""
    private var filterOrderDescription: String = ""

    private var searchedText: String = ""

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        saveBundleValues(savedInstanceState)
    }

    private fun saveBundleValues(b: Bundle) {
        b.putString(ARG_TITLE, tempTitle)
        b.putBoolean(ARG_SHOW_SELECT_BUTTON, showSelectButton)
        b.putBoolean(ARG_MULTI_SELECT, multiSelect)
        b.putBoolean(ARG_HIDE_FILTER_PANEL, hideFilterPanel)

        b.putBoolean("panelIsExpanded", panelIsExpanded)

        if (adapter != null) {
            b.putParcelable("lastSelected", (adapter ?: return).currentItem())
            b.putInt("firstVisiblePos", (adapter ?: return).firstVisiblePos())
            b.putParcelableArrayList("completeList", adapter?.fullList)
            b.putIntArray("checkedHashArray", allChecked.map { it.hashCode }.toIntArray())
            b.putInt("currentScrollPosition", currentScrollPosition)
        }

        b.putString("filterOrderId", filterOrderId)
        b.putString("filterOrderExternalId", filterOrderExternalId)
        b.putString("filterOrderDescription", filterOrderDescription)

        b.putString("searchedText", searchedText)
    }

    private fun loadBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.order_location)

        showSelectButton = b.getBoolean(ARG_SHOW_SELECT_BUTTON, showSelectButton)
        multiSelect = b.getBoolean(ARG_MULTI_SELECT, multiSelect)
        hideFilterPanel = b.getBoolean(ARG_HIDE_FILTER_PANEL, hideFilterPanel)

        panelIsExpanded = b.getBoolean("panelIsExpanded")

        // Adapter
        checkedHashArray = (b.getIntArray("checkedHashArray") ?: intArrayOf()).toCollection(ArrayList())
        completeList = b.parcelableArrayList("completeList") ?: ArrayList()
        lastSelected = b.parcelable("lastSelected")
        firstVisiblePos = if (b.containsKey("firstVisiblePos")) b.getInt("firstVisiblePos") else -1
        currentScrollPosition = b.getInt("currentScrollPosition")

        filterOrderId = b.getString("filterOrderId") ?: ""
        filterOrderExternalId = b.getString("filterOrderExternalId") ?: ""
        filterOrderDescription = b.getString("filterOrderDescription") ?: ""

        searchedText = b.getString("searchedText") ?: ""
    }

    private fun loadExtrasBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.order_location)

        hideFilterPanel = b.getBoolean(ARG_HIDE_FILTER_PANEL)
        multiSelect = b.getBoolean(ARG_MULTI_SELECT, false)
        showSelectButton = b.getBoolean(ARG_SHOW_SELECT_BUTTON, true)
    }

    private lateinit var binding: OrderLocationActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = OrderLocationActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                currentScrollPosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            }
        })

        // Para el llenado en el onStart siguiente de onCreate
        fillRequired = true

        // We change the title of the select button
        binding.okButton.text = getString(R.string.unpack)

        filterFragment = supportFragmentManager.findFragmentById(R.id.filterFragment) as SelectFilterFragment
        summaryFragment = supportFragmentManager.findFragmentById(R.id.summaryFragment) as SummaryFragment
        searchTextFragment = supportFragmentManager.findFragmentById(R.id.searchTextFragment) as SearchTextFragment

        if (savedInstanceState != null) {
            loadBundleValues(savedInstanceState)
        } else {
            // Inicializar la actividad
            val extras = intent.extras
            if (extras != null) loadExtrasBundleValues(extras)
        }

        binding.topAppbar.title = tempTitle

        setupFilterFragment()
        setupSearchTextFragment()

        binding.swipeRefreshItem.setOnRefreshListener(this)
        binding.swipeRefreshItem.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Para expandir y colapsar los paneles
        setPanelAnimation()

        binding.okButton.setOnClickListener {
            askForUnpack(selectedOrders())
        }

        // OCULTAR PANEL DE CONTROLES DE FILTRADO
        if (hideFilterPanel) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                panelIsExpanded = false
                setPanels()

                runOnUiThread {
                    binding.expandBottomPanelButton?.visibility = GONE
                }
            }
        }

        Screen.setupUI(binding.root, this)
    }

    private fun setupSearchTextFragment() {
        searchTextFragment =
            SearchTextFragment.Builder()
                .focusChangedCallback(this)
                .searchTextChangedCallback(this)
                .setSearchText(searchedText)
                .build()
        supportFragmentManager.beginTransaction().replace(R.id.searchTextFragment, searchTextFragment).commit()
    }

    private fun setupFilterFragment() {
        val sv = settingsVm
        val sr = settingsRepository
        filterFragment =
            SelectFilterFragment.Builder()
                .searchByOrderId(sv.orderSearchByOrderId, sr.orderSearchByOrderId)
                .searchByOrderExtId(sv.orderSearchByOrderExtId, sr.orderSearchByOrderExtId)
                .searchByOrderDescription(sv.orderSearchByOrderDescription, sr.orderSearchByOrderDescription)
                .orderId(filterOrderId)
                .orderExternalId(filterOrderExternalId)
                .orderDescription(filterOrderDescription)
                .build()
        supportFragmentManager.beginTransaction().replace(R.id.filterFragment, filterFragment).commit()
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
        adjustRootLayout()
        implWindowInsetsAnimation()
    }

    private fun adjustRootLayout() {
        val rootView = binding.root

        // Adjust root layout to bottom navigation bar
        val windowInsets = window.decorView.rootWindowInsets
        @Suppress("DEPRECATION") rootView.setPadding(
            windowInsets.systemWindowInsetLeft,
            windowInsets.systemWindowInsetTop,
            windowInsets.systemWindowInsetRight,
            windowInsets.systemWindowInsetBottom
        )
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
                orientation == Configuration.ORIENTATION_PORTRAIT -> {
                    panelIsExpanded = false
                    setPanels()
                }

                orientation != Configuration.ORIENTATION_PORTRAIT -> {
                    setPanels()
                }
            }
        }
    }
    // endregion

    override fun onSearchTextFocusChange(hasFocus: Boolean) {
        searchTextIsFocused = hasFocus
        if (hasFocus) {
            /**
            Acá el teclado Ime aparece y se tienen que colapsar los dos panels.
            Si el teclado Ime ya estaba en la pantalla (por ejemplo el foco estaba el control de cantidad de etiquetas),
            el teclado cambiará de tipo y puede tener una altura diferente.
            Esto no dispara los eventos de animación del teclado.
            Colapsar los paneles y reajustar el Layout al final es la solución temporal.
             */
            panelIsExpanded = false
            setPanels()
        }
    }

    override fun onSearchTextChanged(searchText: String) {
        searchedText = searchText
        runOnUiThread {
            adapter?.refreshFilter(FilterOptions(searchedText))
        }
    }

    private val requiredLayout: Int
        get() {
            val r =
                if (panelIsExpanded) layoutPanelExpanded
                else layoutPanelCollapsed

            if (BuildConfig.DEBUG) {
                when (r) {
                    layoutPanelExpanded -> println("SELECTED LAYOUT: Panel Expanded")
                    layoutPanelCollapsed -> println("SELECTED LAYOUT: Panel Collapsed")
                }
            }

            return r
        }

    private val layoutPanelExpanded: Int
        get() {
            return if (showSelectButton) R.layout.order_location_activity
            else R.layout.order_location_activity_wo_select_button
        }

    private val layoutPanelCollapsed: Int
        get() {
            return if (showSelectButton) R.layout.order_location_activity_bottom_panel_collapsed
            else R.layout.order_location_activity_bottom_panel_collapsed_wo_select_button
        }

    private fun setPanels() {
        runOnUiThread {
            val currentLayout = ConstraintSet()
            currentLayout.load(this, requiredLayout)
            currentLayout.applyTo(binding.root)

            if (panelIsExpanded) binding.expandBottomPanelButton?.text =
                context.getString(R.string.collapse_panel)
            else binding.expandBottomPanelButton?.text = context.getString(R.string.search_options)

            refreshTextViews()
        }
    }

    private fun setPanelAnimation() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) return

        binding.expandBottomPanelButton?.setOnClickListener {
            val bottomVisible = panelIsExpanded
            val imeVisible = isKeyboardVisible
            panelIsExpanded = !panelIsExpanded
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
                override fun onTransitionResume(transition: Transition?) {}
                override fun onTransitionPause(transition: Transition?) {}
                override fun onTransitionStart(transition: Transition?) {}
                override fun onTransitionEnd(transition: Transition?) {
                    refreshTextViews()
                }

                override fun onTransitionCancel(transition: Transition?) {}
            })

            TransitionManager.beginDelayedTransition(binding.root, transition)
            nextLayout.applyTo(binding.root)

            if (panelIsExpanded) binding.expandBottomPanelButton?.text =
                context.getString(R.string.collapse_panel)
            else binding.expandBottomPanelButton?.text = context.getString(R.string.search_options)
        }
    }

    private fun refreshTextViews() {
        runOnUiThread {
            if (panelIsExpanded) filterFragment.refreshViews()
        }
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshItem.isRefreshing = show
        }, 20)
    }

    private fun unpack(itemArray: ArrayList<OrderResponse>) {
        for (item in itemArray) {
            item.startDate = DateFormat.format(Statics.DATE_FORMAT, System.currentTimeMillis()).toString()
        }

        if (itemArray.count() == 1) {
            val item = itemArray.first()
            // Agotado
            item.statusId = OrderStatus.outOfStock.id

            UpdateOrder(
                payload = arrayListOf(OrderRequest(item)),
                onEvent = { showSnackBar(it.text, it.snackBarType) },
                onFinish = {
                    if (it.isEmpty()) return@UpdateOrder
                    askForRepack(item)
                }
            ).execute()
        } else {
            for ((index, or) in itemArray.withIndex()) {
                // Agotado
                or.statusId = OrderStatus.outOfStock.id
                val payload = ArrayList(itemArray.mapNotNull { if (it.id == or.id) OrderRequest(it) else null })

                UpdateOrder(
                    payload = payload,
                    onEvent = { showSnackBar(it.text, it.snackBarType) },
                    onFinish = {
                        if (it.isEmpty()) return@UpdateOrder
                        if (index == itemArray.lastIndex) {
                            isFinishingByUser = true
                            finish()
                        }
                    }
                ).execute()
            }
        }
    }

    private fun askForRepack(order: OrderResponse) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(context.getString(R.string.repack_this_order))
            builder.setMessage(getString(R.string.you_want_to_repack_this_order))
            builder.setPositiveButton(getString(R.string.yes)) { dialogInterface, _ ->
                val data = Intent()
                data.putExtra(ARG_REPACK_ORDER_ID, order.id)
                setResult(RESULT_OK, data)
                dialogInterface.dismiss()
            }
            builder.setNegativeButton(getString(R.string.no)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            builder.setOnDismissListener {
                finish()
            }
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun askForUnpack(orders: ArrayList<OrderResponse>) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(context.getString(R.string.confirm_unpacking))
            builder.setMessage(getString(R.string.are_you_sure_to_unpack_the_selected_orders))
            builder.setPositiveButton(getString(R.string.yes)) { dialogInterface, _ ->
                unpack(orders)
                dialogInterface.dismiss()
            }
            builder.setNegativeButton(getString(R.string.no)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun getOrders() {
        checkedHashArray.clear()

        val filter = filterFragment.getFilters()
        if (!filter.any()) {
            fillAdapter(arrayListOf())
            return
        }

        try {
            Log.d(tag, "Selecting orders...")

            GetOrder(
                filter = filter,
                action = GetOrder.defaultAction,
                onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it.text, it.snackBarType) },
                onFinish = {
                    fillAdapter(ArrayList(it))
                }
            ).execute()
        } catch (ex: java.lang.Exception) {
            ErrorLog.writeLog(this, tag, ex.message.toString())
            showProgressBar(false)
        }
    }

    private fun fillAdapter(t: ArrayList<OrderResponse>) {
        showProgressBar(true)

        completeList = t

        runOnUiThread {
            try {
                if (adapter != null) {
                    // Si el adapter es NULL es porque aún no fue creado.
                    // Por lo tanto, puede ser que los valores de [lastSelected]
                    // sean valores guardados de la instancia anterior y queremos preservarlos.
                    lastSelected = currentItem
                }

                adapter = OrderAdapter.Builder()
                    .recyclerView(binding.recyclerView)
                    .fullList(completeList)
                    .checkedHashArray(checkedHashArray)
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

    override fun onResume() {
        super.onResume()
        rejectNewInstances = false

        closeKeyboard(this)
    }

    public override fun onStart() {
        super.onStart()

        setPanels()

        if (fillRequired) {
            fillRequired = false
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
        // Nada que hacer, volver
        if (scanCode.trim().isEmpty()) {
            val res = context.getString(R.string.invalid_code)
            showSnackBar(res, ERROR)
            ErrorLog.writeLog(this, tag, res)
            return
        }

        if (settingsVm.showScannedCode) showSnackBar(scanCode, INFO)

        GetResultFromCode.Builder()
            .withCode(scanCode)
            .searchOrder()
            .searchOrderExternalId()
            .onFinish { proceedByResult(it, scanCode) }
            .build()
    }

    private fun proceedByResult(it: GetResultFromCode.CodeResult, scanCode: String) {
        when (val itemObj = it.item) {
            is OrderResponse -> {
                runOnUiThread {
                    filterFragment.setOrderExternalId(itemObj.externalId)
                    filterFragment.setOrderId(itemObj.id.toString())
                    fillAdapter(arrayListOf(itemObj))
                }
            }

            else -> {
                runOnUiThread {
                    if (settingsVm.orderSearchByOrderExtId) {
                        filterFragment.setOrderId("")
                        filterFragment.setOrderExternalId(scanCode)
                    } else {
                        filterFragment.setItemEan(scanCode)
                    }
                    getOrders()
                }
            }
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    @SuppressLint("MissingSuperCall")
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        closeKeyboard(this)

        isFinishingByUser = true
        setResult(RESULT_CANCELED)
        finish()
    }

    private val menuItemManualCode = 999001
    private val menuItemRandomEan = 999002
    private val menuItemRandomIt = 999003
    private val menuItemRandomItUrl = 999004
    private val menuItemRandomOnListL = 999005
    private val menuItemRandomOrder = 999006
    private val menuItemRandomRack = 999007
    private val menuItemRandomWa = 999008

    @SuppressLint("RestrictedApi")
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
            menu.add(Menu.NONE, menuItemRandomOnListL, Menu.NONE, "Random item on list")
            menu.add(Menu.NONE, menuItemRandomOrder, Menu.NONE, "Random order")
            menu.add(Menu.NONE, menuItemRandomRack, Menu.NONE, "Random rack")
            menu.add(Menu.NONE, menuItemRandomWa, Menu.NONE, "Random area")
        }

        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_visibility)
        binding.topAppbar.overflowIcon = drawable

        // Opciones de visibilidad del menú
        val allControls = SettingsRepository.getAllSelectOrderVisibleControls()
        val visibleFilters = filterFragment.getVisibleFilters()
        allControls.forEach { p ->
            menu.add(0, p.key.hashCode(), menu.size(), p.description)
                .setChecked(visibleFilters.contains(p)).isCheckable = true
        }

        for (y in allControls) {
            var tempIndex = 0
            for (i in visibleFilters) {
                if (i != y) continue

                val item = menu.getItem(tempIndex)
                if (item?.itemId != i.key.hashCode()) {
                    continue
                }

                // Keep the popup menu open
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
                item.actionView = View(this)
                item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                        return false
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                        return false
                    }
                })

                tempIndex++
            }
        }

        return true
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
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
                (adapter?.fullList ?: ArrayList()).mapTo(codes) { "$PREFIX_ORDER${it.id}#" }
                if (codes.any()) scannerCompleted(codes[Random().nextInt(codes.count())])
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
        }

        item.isChecked = !item.isChecked
        val sv = settingsVm
        when (id) {
            settingsRepository.orderSearchByOrderId.key.hashCode() -> {
                filterFragment.setOrderIdVisibility(if (item.isChecked) View.VISIBLE else GONE)
                sv.orderSearchByOrderId = item.isChecked
            }

            settingsRepository.orderSearchByOrderExtId.key.hashCode() -> {
                filterFragment.setOrderExtIdVisibility(if (item.isChecked) View.VISIBLE else GONE)
                sv.orderSearchByOrderExtId = item.isChecked
            }

            settingsRepository.orderSearchByOrderDescription.key.hashCode() -> {
                filterFragment.setAreaVisibility(if (item.isChecked) View.VISIBLE else GONE)
                sv.orderSearchByOrderDescription = item.isChecked
            }

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onFilterChanged(orderId: String, orderExternalId: String, orderDescription: String) {
        filterOrderId = orderId
        filterOrderExternalId = orderExternalId
        filterOrderDescription = orderDescription

        Handler(Looper.getMainLooper()).postDelayed({ getOrders() }, 200)
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
                if (event.action == KeyEvent.ACTION_DOWN && (event?.keyCode == KeyEvent.KEYCODE_ENTER || event?.keyCode == KeyEvent.KEYCODE_UNKNOWN || event?.keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
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

    override fun onCheckedChanged(isChecked: Boolean, pos: Int) {
        fillSummaryFragment()
    }

    override fun onDataSetChanged() {
        Handler(Looper.getMainLooper()).postDelayed({
            fillSummaryFragment()
        }, 100)
    }

    private fun fillSummaryFragment() {
        runOnUiThread {
            Handler(Looper.getMainLooper()).postDelayed({
                summaryFragment
                    .firstLabel(getString(R.string.total))
                    .first(adapter?.totalVisible() ?: 0)
                    .secondLabel(getString(R.string.total_count))
                    .second(countChecked)
                    .fill()
            }, 100)
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
        const val ARG_HIDE_FILTER_PANEL = "hideFilterPanel"
        const val ARG_SHOW_SELECT_BUTTON = "showSelectButton"
        const val ARG_REPACK_ORDER_ID = "orderId"
    }
}
