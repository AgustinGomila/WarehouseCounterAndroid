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
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.menu.MenuBuilder
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelTemplate
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelType
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeParam
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.PrintOps
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.functions.barcode.GetOrderBarcode
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.GetOrder
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.data.room.dao.pendingLabel.PendingLabelCoroutines
import com.dacosys.warehouseCounter.data.settings.SettingsRepository
import com.dacosys.warehouseCounter.databinding.ItemPrintLabelActivityTopPanelCollapsedBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode
import com.dacosys.warehouseCounter.ui.adapter.FilterOptions
import com.dacosys.warehouseCounter.ui.adapter.order.OrderAdapter
import com.dacosys.warehouseCounter.ui.fragments.common.SearchTextFragment
import com.dacosys.warehouseCounter.ui.fragments.common.SelectFilterFragment
import com.dacosys.warehouseCounter.ui.fragments.common.SummaryFragment
import com.dacosys.warehouseCounter.ui.fragments.print.PrintLabelFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelableArrayList
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.serializable
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.utils.Screen.Companion.closeKeyboard
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*

class OrderPrintLabelActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    Scanner.ScannerListener, Rfid.RfidDeviceListener,
    SelectFilterFragment.OnFilterOrderChangedListener, OrderAdapter.CheckedChangedListener,
    PrintLabelFragment.FragmentListener, OrderAdapter.DataSetChangedListener,
    SearchTextFragment.OnSearchTextFocusChangedListener, SearchTextFragment.OnSearchTextChangedListener {
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

    private var printQtyIsFocused = false
    private var searchTextIsFocused = false

    private var showSelectButton = true
    private var showRemoveButton = true

    private var multiSelect = false
    private var adapter: OrderAdapter? = null
    private var lastSelected: OrderResponse? = null
    private var firstVisiblePos: Int? = null
    private var currentScrollPosition: Int = 0

    private var completeList: ArrayList<OrderResponse> = ArrayList()
    private var checkedHashArray: ArrayList<Int> = ArrayList()
    private var ids: ArrayList<Long> = ArrayList()

    // Se usa para saber si estamos en onStart luego de onCreate
    private var fillRequired = false

    private var panelBottomIsExpanded = true
    private var panelTopIsExpanded = false

    private var hideFilterPanel = false

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

    private lateinit var filterFragment: SelectFilterFragment
    private lateinit var printLabelFragment: PrintLabelFragment
    private lateinit var summaryFragment: SummaryFragment
    private lateinit var searchTextFragment: SearchTextFragment

    private var filterOrderId: String = ""
    private var filterOrderExternalId: String = ""
    private var filterOrderDescription: String = ""

    private var searchedText: String = ""

    private var currentPrintQty: Int = 1
    private var currentTemplateId: Long = 0L

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        saveBundleValues(savedInstanceState)
    }

    private fun saveBundleValues(b: Bundle) {
        b.putString(ARG_TITLE, tempTitle)
        b.putBoolean(ARG_SHOW_SELECT_BUTTON, showSelectButton)
        b.putBoolean(ARG_SHOW_REMOVE_BUTTON, showRemoveButton)
        b.putBoolean(ARG_MULTI_SELECT, multiSelect)
        b.putBoolean(ARG_HIDE_FILTER_PANEL, hideFilterPanel)

        b.putBoolean("panelTopIsExpanded", panelTopIsExpanded)
        b.putBoolean("panelBottomIsExpanded", panelBottomIsExpanded)

        if (adapter != null) {
            b.putParcelable("lastSelected", currentItem)
            b.putInt("firstVisiblePos", adapter?.firstVisiblePos() ?: -1)
            b.putParcelableArrayList("completeList", adapter?.fullList)
            b.putIntArray("checkedHashArray", allChecked.map { it.hashCode }.toIntArray())
            b.putInt("currentScrollPosition", currentScrollPosition)
        }

        b.putString("filterOrderId", filterOrderId)
        b.putString("filterOrderExternalId", filterOrderExternalId)
        b.putString("filterOrderDescription", filterOrderDescription)

        b.putString("searchedText", searchedText)

        b.putInt("currentPrintQty", currentPrintQty)
        b.putLong("currentTemplateId", currentTemplateId)
    }

    private fun loadBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.select_item)

        showSelectButton = b.getBoolean(ARG_SHOW_SELECT_BUTTON, showSelectButton)
        showRemoveButton = b.getBoolean(ARG_SHOW_REMOVE_BUTTON, showRemoveButton)
        multiSelect = b.getBoolean(ARG_MULTI_SELECT, multiSelect)
        hideFilterPanel = b.getBoolean(ARG_HIDE_FILTER_PANEL, hideFilterPanel)

        panelBottomIsExpanded = b.getBoolean("panelBottomIsExpanded")
        panelTopIsExpanded = b.getBoolean("panelTopIsExpanded")

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

        currentPrintQty = b.getInt("currentPrintQty")
        currentTemplateId = b.getLong("currentTemplateId")
    }

    private fun loadExtrasBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.select_item)

        hideFilterPanel = b.getBoolean(ARG_HIDE_FILTER_PANEL)
        multiSelect = b.getBoolean(ARG_MULTI_SELECT, false)

        showSelectButton =
            if (b.containsKey(ARG_SHOW_SELECT_BUTTON)) b.getBoolean(ARG_SHOW_SELECT_BUTTON, true)
            else true

        showRemoveButton =
            if (b.containsKey(ARG_SHOW_REMOVE_BUTTON)) b.getBoolean(ARG_SHOW_REMOVE_BUTTON, false)
            else false

        val temp = b.serializable<ArrayList<Long>>(ARG_IDS) as ArrayList<*>
        if (temp.first() is Long) {
            @Suppress("UNCHECKED_CAST")
            ids = temp as ArrayList<Long>
        }
    }

    private lateinit var binding: ItemPrintLabelActivityTopPanelCollapsedBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = ItemPrintLabelActivityTopPanelCollapsedBinding.inflate(layoutInflater)
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

        filterFragment = supportFragmentManager.findFragmentById(R.id.filterFragment) as SelectFilterFragment
        printLabelFragment = supportFragmentManager.findFragmentById(R.id.printFragment) as PrintLabelFragment
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
        setupPrintLabelFragment()

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

        setFilterPanelVisibility()
        setSelectRemoveButton()

        Screen.setupUI(binding.root, this)
    }

    private fun setSelectRemoveButton() {
        if (showRemoveButton) {
            val textColor =
                getBestContrastColor(ResourcesCompat.getColor(context.resources, R.color.coral, null))
            binding.okButton.text = getString(R.string.remove)
            binding.okButton.setTextColor(textColor)
            binding.okButton.background = AppCompatResources.getDrawable(
                context,
                R.drawable.rounded_corner_button_coral
            )
            binding.okButton.setOnClickListener { itemRemove() }
        } else {
            val textColor =
                getBestContrastColor(ResourcesCompat.getColor(context.resources, R.color.seagreen, null))
            binding.okButton.text = getString(R.string.accept)
            binding.okButton.setTextColor(textColor)
            binding.okButton.background = AppCompatResources.getDrawable(
                context,
                R.drawable.rounded_corner_button_seagreen
            )
            binding.okButton.setOnClickListener { itemSelect() }
        }
    }

    private fun setFilterPanelVisibility() {
        runOnUiThread {
            binding.expandBottomPanelButton?.visibility = if (hideFilterPanel) GONE else VISIBLE
            binding.filterFragment.visibility = if (hideFilterPanel) GONE else VISIBLE
        }
    }

    private fun setupPrintLabelFragment() {
        binding.printFragment.visibility = VISIBLE

        if (currentTemplateId == 0L) {
            currentTemplateId = settingsVm.defaultOrderTemplateId
        }

        printLabelFragment =
            PrintLabelFragment.Builder()
                .setTemplateTypeIdList(arrayListOf(BarcodeLabelType.order.id))
                .setTemplateId(currentTemplateId)
                .setQty(currentPrintQty)
                .build()
        supportFragmentManager.beginTransaction().replace(R.id.printFragment, printLabelFragment).commit()
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

    private val requiredLayout: Int
        get() {
            val r = if (panelBottomIsExpanded) {
                if (panelTopIsExpanded) layoutBothPanelsExpanded
                else layoutTopPanelCollapsed
            } else {
                if (panelTopIsExpanded) layoutBottomPanelCollapsed
                else layoutBothPanelsCollapsed
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
            return if (showSelectButton) R.layout.item_print_label_activity
            else R.layout.item_print_label_activity_wo_select_button
        }

    private val layoutBothPanelsCollapsed: Int
        get() {
            return if (showSelectButton) R.layout.item_print_label_activity_both_panels_collapsed
            else R.layout.item_print_label_activity_both_panels_collapsed_wo_select_button
        }

    private val layoutTopPanelCollapsed: Int
        get() {
            return if (showSelectButton) R.layout.item_print_label_activity_top_panel_collapsed
            else R.layout.item_print_label_activity_top_panel_collapsed_wo_select_button
        }

    private val layoutBottomPanelCollapsed: Int
        get() {
            return if (showSelectButton) R.layout.item_print_label_activity_bottom_panel_collapsed
            else R.layout.item_print_label_activity_bottom_panel_collapsed_wo_select_button
        }

    private fun setPanels() {
        runOnUiThread {
            val currentLayout = ConstraintSet()
            currentLayout.load(this, requiredLayout)
            currentLayout.applyTo(binding.root)

            if (panelBottomIsExpanded) binding.expandBottomPanelButton?.text =
                context.getString(R.string.collapse_panel)
            else binding.expandBottomPanelButton?.text = context.getString(R.string.search_options)

            if (panelTopIsExpanded) binding.expandTopPanelButton.text = context.getString(R.string.collapse_panel)
            else binding.expandTopPanelButton.text = context.getString(R.string.print_labels)

            refreshTextViews()
        }
    }

    private fun setBottomPanelAnimation() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) return

        binding.expandBottomPanelButton!!.setOnClickListener {
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

            if (panelBottomIsExpanded) binding.expandBottomPanelButton?.text =
                context.getString(R.string.collapse_panel)
            else binding.expandBottomPanelButton?.text = context.getString(R.string.search_options)
        }
    }

    private fun setTopPanelAnimation() {
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

            if (panelTopIsExpanded) binding.expandTopPanelButton.text = context.getString(R.string.collapse_panel)
            else binding.expandTopPanelButton.text = context.getString(R.string.print_labels)
        }
    }

    private fun refreshTextViews() {
        runOnUiThread {
            if (panelTopIsExpanded) printLabelFragment.refreshViews()
            if (panelBottomIsExpanded) filterFragment.refreshViews()
        }
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshItem.isRefreshing = show
        }, 20)
    }

    private fun itemRemove() {
        val selectedOrders = adapter?.selectedOrders() ?: arrayListOf()
        if (!selectedOrders.any()) {
            showSnackBar(getString(R.string.you_must_select_at_least_one_order), ERROR)
            return
        }
        val idsToRemove = selectedOrders.map { it.id }
        askForConfirmation(idsToRemove)
    }

    private fun askForConfirmation(idsToRemove: List<Long>) {
        val msg = if (idsToRemove.count() == 1) {
            context.getString(R.string.do_you_want_to_print_the_label_for_this_order)
        } else {
            context.getString(R.string.do_you_want_to_print_the_label_for_these_orders)
        }

        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(context.getString(R.string.remove_pending_label))
            builder.setMessage(msg)
            builder.setPositiveButton(getString(R.string.yes)) { dialogInterface, _ ->

                PendingLabelCoroutines.remove(idsToRemove) {
                    ids.removeAll(idsToRemove.toSet())
                    getOrders()
                }
                dialogInterface.dismiss()
            }
            builder.setNegativeButton(getString(R.string.no)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun itemSelect() {
        closeKeyboard(this)

        val selectedOrders = adapter?.selectedOrders() ?: arrayListOf()
        if (!selectedOrders.any()) {
            showSnackBar(getString(R.string.you_must_select_at_least_one_order), ERROR)
            return
        }

        val data = Intent()
        data.putParcelableArrayListExtra(ARG_SELECTED_ORDERS, selectedOrders)
        setResult(RESULT_OK, data)
        isFinishingByUser = true
        finish()
    }

    private fun getOrders() {
        checkedHashArray.clear()

        var filter: ArrayList<ApiFilterParam> = arrayListOf()
        if (ids.isEmpty()) {
            filter = filterFragment.getFilters()
            if (!filter.any()) {
                fillAdapter(arrayListOf())
                return
            }
        } else {
            for (id in ids) {
                filter.add(
                    ApiFilterParam(
                        columnName = ApiFilterParam.EXTENSION_ID,
                        value = id.toString(),
                        conditional = ApiFilterParam.ACTION_OPERATOR_IN
                    )
                )
            }
        }

        /** Usar pageNum y pageTotal
         * filter.add(ApiFilterParam(EXTENSION_PAGE_NUMBER, pageNum.toString()))
         */

        try {
            Log.d(this::class.java.simpleName, "Selecting orders...")

            GetOrder(
                filter = filter,
                action = GetOrder.defaultAction,
                onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it.text, it.snackBarType) },
                onFinish = {
                    fillAdapter(ArrayList(it))
                }
            ).execute()
        } catch (ex: java.lang.Exception) {
            ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())
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
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                showProgressBar(false)
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        rejectNewInstances = false

        closeKeyboard(this)
        JotterListener.resumeReaderDevices(this)

        setPanels()

        if (fillRequired) {
            fillRequired = false

            getOrders()
        }
    }

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
            panelBottomIsExpanded = false
            panelTopIsExpanded = false
            setPanels()
        }
    }

    override fun onSearchTextChanged(searchText: String) {
        searchedText = searchText
        runOnUiThread {
            adapter?.refreshFilter(FilterOptions(searchedText))
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

    @Suppress("UNCHECKED_CAST")
    override fun scannerCompleted(scanCode: String) {
        // Nada que hacer, volver
        if (scanCode.trim().isEmpty()) {
            val res = context.getString(R.string.invalid_code)
            showSnackBar(res, ERROR)
            ErrorLog.writeLog(this, this::class.java.simpleName, res)
            return
        }

        if (settingsVm.showScannedCode) showSnackBar(scanCode, INFO)
        JotterListener.lockScanner(this, true)

        // Buscar por ubicación
        GetResultFromCode(
            code = scanCode,
            searchOrder = true,
            onFinish = {
                val tList = it.typedObject ?: return@GetResultFromCode
                if (tList is ArrayList<*> && tList.firstOrNull() is OrderResponse) {
                    fillAdapter(tList as ArrayList<OrderResponse>)
                } else return@GetResultFromCode
            }
        )
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        closeKeyboard(this)

        isFinishingByUser = true
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

        if (!settingsVm.useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        if (BuildConfig.DEBUG || Statics.TEST_MODE) {
            menu.add(Menu.NONE, menuItemManualCode, Menu.NONE, "Manual code")
            menu.add(Menu.NONE, menuItemRandomIt, Menu.NONE, "Random item")
            menu.add(Menu.NONE, menuItemRandomOnListL, Menu.NONE, "Random item on list")
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        when (item.itemId) {
            R.id.home, android.R.id.home -> {
                @Suppress("DEPRECATION") onBackPressed()
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
                (adapter?.fullList ?: ArrayList()).mapTo(codes) { it.externalId }
                if (codes.any()) scannerCompleted(codes[Random().nextInt(codes.count())])
                return super.onOptionsItemSelected(item)
            }

            menuItemRandomIt -> {
                ItemCoroutines.getCodes(true) {
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
        val sv = settingsVm
        when (id) {
            settingsRepository.orderSearchByOrderId.key.hashCode() -> {
                filterFragment.setOrderIdVisibility(if (item.isChecked) VISIBLE else GONE)
                sv.orderSearchByOrderId = item.isChecked
            }

            settingsRepository.orderSearchByOrderExtId.key.hashCode() -> {
                filterFragment.setOrderExtIdVisibility(if (item.isChecked) VISIBLE else GONE)
                sv.orderSearchByOrderExtId = item.isChecked
            }

            settingsRepository.orderSearchByOrderDescription.key.hashCode() -> {
                filterFragment.setDescriptionVisibility(if (item.isChecked) VISIBLE else GONE)
                sv.orderSearchByOrderDescription = item.isChecked
            }

            else -> return super.onOptionsItemSelected(item)
        }
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

    override fun onFilterChanged(orderId: String, orderExternalId: String, orderDescription: String) {
        filterOrderId = orderId
        filterOrderExternalId = orderExternalId
        filterOrderDescription = orderDescription

        Handler(Looper.getMainLooper()).postDelayed({ getOrders() }, 200)
    }

    override fun onFilterChanged(printer: String, template: BarcodeLabelTemplate?, qty: Int?) {
        currentPrintQty = qty ?: 1
        currentTemplateId = template?.templateId ?: return

        settingsVm.defaultOrderTemplateId = currentTemplateId
    }

    override fun onPrintRequested(printer: String, qty: Int) {
        val template = printLabelFragment.template
        if (template == null) {
            showSnackBar(context.getString(R.string.you_must_select_a_template), ERROR)
            return
        }

        val selectedOrders = adapter?.selectedOrders() ?: arrayListOf()

        if (!selectedOrders.any()) {
            showSnackBar(getString(R.string.you_must_select_at_least_one_order), ERROR)
            return
        }

        val printOps = PrintOps.getPrintOps()

        val ids = ArrayList(selectedOrders.map { it.id })

        GetOrderBarcode(
            param = BarcodeParam(
                idList = ids,
                templateId = template.templateId,
                printOps = printOps
            ),
            onEvent = {
                if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it.text, it.snackBarType)
            },
            onFinish = {
                if (it.isNotEmpty()) {
                    printLabelFragment.printBarcodes(
                        labelArray = it,
                        onFinish = { success ->
                            if (success) PendingLabelCoroutines.remove(ids)
                        }
                    )
                }
            }
        ).execute()
    }

    override fun onQtyTextViewFocusChanged(hasFocus: Boolean) {
        printQtyIsFocused = hasFocus
        if (hasFocus) {
            /**
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
            summaryFragment
                .firstLabel(getString(R.string.total))
                .first(adapter?.totalVisible() ?: 0)
                .secondLabel(getString(R.string.total_count))
                .second(countChecked)
                .fill()
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
        const val ARG_SHOW_SELECT_BUTTON = "showSelectButton"
        const val ARG_SHOW_REMOVE_BUTTON = "showRemoveButton"
        const val ARG_HIDE_FILTER_PANEL = "hideFilterPanel"
        const val ARG_IDS = "ids"

        const val ARG_SELECTED_ORDERS = "orders"
    }
}
