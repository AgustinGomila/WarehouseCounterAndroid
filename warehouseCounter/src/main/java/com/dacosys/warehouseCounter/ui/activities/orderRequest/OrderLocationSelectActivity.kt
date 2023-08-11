package com.dacosys.warehouseCounter.ui.activities.orderRequest

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
import android.view.View
import android.view.View.GONE
import android.widget.EditText
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
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.OrderLocationActivityTopPanelCollapsedBinding
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderLocation
import com.dacosys.warehouseCounter.ktor.v2.functions.GetOrderLocation
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiFilterParam
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiFilterParam.Companion.EXTENSION_ITEM_CODE
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiFilterParam.Companion.EXTENSION_ITEM_DESCRIPTION
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiFilterParam.Companion.EXTENSION_ITEM_EAN
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiFilterParam.Companion.EXTENSION_ORDER_EXTERNAL_ID
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiFilterParam.Companion.EXTENSION_ORDER_ID
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiFilterParam.Companion.EXTENSION_ORDER_LOCATION_AREA_ID
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiFilterParam.Companion.EXTENSION_ORDER_LOCATION_RACK_ID
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiFilterParam.Companion.EXTENSION_ORDER_LOCATION_WAREHOUSE_ID
import com.dacosys.warehouseCounter.misc.ParcelLong
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.settings.SettingsRepository
import com.dacosys.warehouseCounter.ui.adapter.FilterOptions
import com.dacosys.warehouseCounter.ui.adapter.orderRequest.OrderLocationRecyclerAdapter
import com.dacosys.warehouseCounter.ui.fragments.orderRequest.OrderLocationFilterFragment
import com.dacosys.warehouseCounter.ui.fragments.orderRequest.OrderSummaryFragment
import com.dacosys.warehouseCounter.ui.fragments.orderRequest.SearchTextFragment
import com.dacosys.warehouseCounter.ui.fragments.print.PrintLabelFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.utils.Screen.Companion.closeKeyboard
import java.util.*
import kotlin.concurrent.thread

class OrderLocationSelectActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    Scanner.ScannerListener, Rfid.RfidDeviceListener,
    OrderLocationFilterFragment.OnFilterChangedListener, OrderLocationRecyclerAdapter.CheckedChangedListener,
    PrintLabelFragment.FragmentListener, OrderLocationRecyclerAdapter.DataSetChangedListener,
    SearchTextFragment.OnSearchTextFocusChangedListener, SearchTextFragment.OnSearchTextChangedListener {
    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        /*
        TODO: Usar tabla temporal para guardar listas largas.
        */
        if (isFinishingByUser) {
            // Borramos los Ids temporales que se usaron en la actividad.
            // ItemDbHelper().deleteTemp()
        }

        adapter?.refreshListeners()
        orderLocationFilterFragment.onDestroy()
        printLabelFragment.onDestroy()
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

    private var multiSelect = false
    private var adapter: OrderLocationRecyclerAdapter? = null
    private var lastSelected: OrderLocation? = null
    private var firstVisiblePos: Int? = null
    private var currentScrollPosition: Int = 0

    // Se usa para saber si estamos en onStart luego de onCreate
    private var fillRequired = false

    private var panelBottomIsExpanded = true
    private var panelTopIsExpanded = false

    private var hideFilterPanel = false

    private var searchText: String = ""

    private var completeList: ArrayList<OrderLocation> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()

    private var showCheckBoxes
        get() =
            if (!multiSelect) false
            else settingViewModel.itemSelectShowCheckBoxes
        set(value) {
            settingViewModel.itemSelectShowCheckBoxes = value
        }

    private lateinit var orderLocationFilterFragment: OrderLocationFilterFragment
    private lateinit var printLabelFragment: PrintLabelFragment
    private lateinit var summaryFragment: OrderSummaryFragment
    private lateinit var searchTextFragment: SearchTextFragment

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        saveBundleValues(savedInstanceState)
    }

    private fun saveBundleValues(b: Bundle) {
        b.putString(ARG_TITLE, title.toString())
        b.putBoolean(ARG_MULTI_SELECT, multiSelect)
        b.putBoolean(ARG_HIDE_FILTER_PANEL, hideFilterPanel)
        b.putBoolean("panelTopIsExpanded", panelTopIsExpanded)
        b.putBoolean("panelBottomIsExpanded", panelBottomIsExpanded)

        if (adapter != null) {
            b.putParcelable("lastSelected", (adapter ?: return).currentItem())
            b.putInt("firstVisiblePos", (adapter ?: return).firstVisiblePos())
            b.putParcelableArrayList("completeList", adapter?.fullList)
            b.putLongArray("checkedIdArray", adapter?.getAllChecked()?.map { it.uniqueId }?.toLongArray())
            b.putInt("currentScrollPosition", currentScrollPosition)
        }

        b.putString("searchText", searchText)
    }

    private fun loadBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.order_location)

        multiSelect = b.getBoolean(ARG_MULTI_SELECT, multiSelect)
        hideFilterPanel = b.getBoolean(ARG_HIDE_FILTER_PANEL, hideFilterPanel)
        panelBottomIsExpanded = b.getBoolean("panelBottomIsExpanded")
        panelTopIsExpanded = b.getBoolean("panelTopIsExpanded")

        searchText = b.getString("searchText") ?: ""

        // Adapter
        checkedIdArray = (b.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(ArrayList())
        completeList = b.getParcelableArrayList("completeList") ?: ArrayList()
        lastSelected = b.getParcelable("lastSelected")
        firstVisiblePos = if (b.containsKey("firstVisiblePos")) b.getInt("firstVisiblePos") else -1
        currentScrollPosition = b.getInt("currentScrollPosition")
    }

    private fun loadExtrasBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.order_location)

        orderLocationFilterFragment.itemCode = b.getString(OrderLocationFilterFragment.ARG_ITEM_CODE) ?: ""
        orderLocationFilterFragment.itemDescription =
            b.getString(OrderLocationFilterFragment.ARG_ITEM_DESCRIPTION) ?: ""
        orderLocationFilterFragment.itemEan = b.getString(OrderLocationFilterFragment.ARG_ITEM_EAN) ?: ""
        orderLocationFilterFragment.orderId = b.getString(OrderLocationFilterFragment.ARG_ORDER_ID) ?: ""
        orderLocationFilterFragment.orderExternalId =
            b.getString(OrderLocationFilterFragment.ARG_ORDER_EXTERNAL_ID) ?: ""
        orderLocationFilterFragment.warehouse = b.getString(OrderLocationFilterFragment.ARG_WAREHOUSE) ?: ""
        orderLocationFilterFragment.warehouseArea = b.getString(OrderLocationFilterFragment.ARG_WAREHOUSE_AREA) ?: ""
        orderLocationFilterFragment.rack = b.getString(OrderLocationFilterFragment.ARG_RACK) ?: ""
        orderLocationFilterFragment.onlyActive = true

        hideFilterPanel = b.getBoolean(ARG_HIDE_FILTER_PANEL)
        multiSelect = b.getBoolean(ARG_MULTI_SELECT, false)
    }

    private lateinit var binding: OrderLocationActivityTopPanelCollapsedBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = OrderLocationActivityTopPanelCollapsedBinding.inflate(layoutInflater)
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

        orderLocationFilterFragment =
            supportFragmentManager.findFragmentById(R.id.filterFragment) as OrderLocationFilterFragment
        printLabelFragment = supportFragmentManager.findFragmentById(R.id.printFragment) as PrintLabelFragment
        summaryFragment = supportFragmentManager.findFragmentById(R.id.summaryFragment) as OrderSummaryFragment
        searchTextFragment = supportFragmentManager.findFragmentById(R.id.searchTextFragment) as SearchTextFragment

        if (savedInstanceState != null) {
            loadBundleValues(savedInstanceState)
        } else {
            // Inicializar la actividad
            val extras = intent.extras
            if (extras != null) loadExtrasBundleValues(extras)
        }

        title = tempTitle

        orderLocationFilterFragment.setListener(this)
        printLabelFragment.setListener(this)

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

        // OCULTAR PANEL DE CONTROLES DE FILTRADO
        if (hideFilterPanel) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (panelBottomIsExpanded) {
                    binding.expandBottomPanelButton?.performClick()
                }

                runOnUiThread {
                    binding.expandBottomPanelButton?.visibility = GONE
                }
            }
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
            when {
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT && !printQtyIsFocused -> {
                    panelBottomIsExpanded = false
                    panelTopIsExpanded = false
                    setPanels()
                }

                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT && printQtyIsFocused -> {
                    panelBottomIsExpanded = false
                    setPanels()
                }

                resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT && !printQtyIsFocused -> {
                    panelTopIsExpanded = false
                    setPanels()
                }
            }
        }
    }
    // endregion

    private fun itemSelect() {
        closeKeyboard(this)

        if (adapter != null) {
            val data = Intent()

            val item = adapter?.currentItem()
            val countChecked = adapter?.countChecked() ?: 0
            var itemArray: ArrayList<OrderLocation> = ArrayList()

            if (!multiSelect && item != null) {
                data.putParcelableArrayListExtra(ARG_IDS, arrayListOf(ParcelLong(item.uniqueId)))
                setResult(RESULT_OK, data)
            } else if (multiSelect) {
                if (countChecked > 0 || item != null) {
                    if (countChecked > 0) itemArray = adapter?.getAllChecked() ?: ArrayList()
                    else if (adapter?.showCheckBoxes == false) {
                        itemArray = arrayListOf(item!!)
                    }
                    data.putParcelableArrayListExtra(
                        ARG_IDS,
                        itemArray.map { ParcelLong(it.uniqueId) } as ArrayList<ParcelLong>)
                    setResult(RESULT_OK, data)
                } else {
                    setResult(RESULT_CANCELED)
                }
            } else {
                setResult(RESULT_CANCELED)
            }
        } else {
            setResult(RESULT_CANCELED)
        }

        isFinishingByUser = true
        finish()
    }

    private fun setSearchTextFragment() {
        searchTextFragment
            .focusChangedCallback(this)
            .searchTextChangedCallback(this)
            .searchText(searchText)
    }

    override fun onFocusChange(hasFocus: Boolean) {
        searchTextIsFocused = hasFocus
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
            panelTopIsExpanded = false
            setPanels()
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                closeKeyboard(this)
            }, 100)
        }
    }

    override fun onSearchTextChanged(searchText: String) {
        this.searchText = searchText
        adapter?.refreshFilter(FilterOptions(searchText))
    }

    private fun setPanels() {
        runOnUiThread {
            val currentLayout = ConstraintSet()
            if (panelBottomIsExpanded) {
                if (panelTopIsExpanded) currentLayout.load(this, R.layout.order_location_activity)
                else currentLayout.load(this, R.layout.order_location_activity_top_panel_collapsed)
            } else {
                if (panelTopIsExpanded) currentLayout.load(
                    this,
                    R.layout.order_location_activity_bottom_panel_collapsed
                )
                else currentLayout.load(this, R.layout.order_location_activity_both_panels_collapsed)
            }

            currentLayout.applyTo(binding.orderLocation)

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

        binding.expandBottomPanelButton?.setOnClickListener {
            val bottomVisible = panelBottomIsExpanded
            val imeVisible = isKeyboardVisible

            if (!bottomVisible && imeVisible) {
                // Esperar que se cierre el teclado luego de perder el foco el TextView para expandir el panel
                changePanelsStateAtFinish = true
                return@setOnClickListener
            }

            val nextLayout = ConstraintSet()
            if (panelBottomIsExpanded) {
                if (panelTopIsExpanded) nextLayout.load(this, R.layout.order_location_activity_bottom_panel_collapsed)
                else nextLayout.load(this, R.layout.order_location_activity_both_panels_collapsed)
            } else {
                if (panelTopIsExpanded) nextLayout.load(this, R.layout.order_location_activity)
                else nextLayout.load(this, R.layout.order_location_activity_top_panel_collapsed)
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

            TransitionManager.beginDelayedTransition(binding.orderLocation, transition)

            if (panelBottomIsExpanded) binding.expandBottomPanelButton?.text =
                context.getString(R.string.collapse_panel)
            else binding.expandBottomPanelButton?.text = context.getString(R.string.search_options)

            nextLayout.applyTo(binding.orderLocation)
        }
    }

    private fun setTopPanelAnimation() {
        binding.expandTopPanelButton.setOnClickListener {
            val topVisible = panelTopIsExpanded
            val imeVisible = isKeyboardVisible

            if (!topVisible && imeVisible) {
                // Esperar que se cierre el teclado luego de perder el foco el TextView para expandir el panel
                changePanelsStateAtFinish = true
                return@setOnClickListener
            }

            val nextLayout = ConstraintSet()
            if (panelBottomIsExpanded) {
                if (panelTopIsExpanded) nextLayout.load(this, R.layout.order_location_activity_top_panel_collapsed)
                else nextLayout.load(this, R.layout.order_location_activity)
            } else {
                if (panelTopIsExpanded) nextLayout.load(this, R.layout.order_location_activity_both_panels_collapsed)
                else nextLayout.load(this, R.layout.order_location_activity_bottom_panel_collapsed)
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

            TransitionManager.beginDelayedTransition(binding.orderLocation, transition)

            if (panelTopIsExpanded) binding.expandTopPanelButton.text = context.getString(R.string.collapse_panel)
            else binding.expandTopPanelButton.text = context.getString(R.string.print_labels)

            nextLayout.applyTo(binding.orderLocation)
        }
    }

    private fun refreshTextViews() {
        runOnUiThread {
            if (panelTopIsExpanded) {
                printLabelFragment.refreshViews()
            }

            if (panelBottomIsExpanded) {
                orderLocationFilterFragment.refreshTextViews()
            }
        }
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshItem.isRefreshing = show
        }, 20)
    }

    private fun getItems() {
        // Limpiamos los ítems marcados
        checkedIdArray.clear()

        val fr = orderLocationFilterFragment

        val itemCode = fr.itemCode
        val itemDescription = fr.itemDescription
        val itemEan = fr.itemEan
        val orderId = fr.orderId
        val orderExternalId = fr.orderExternalId
        val warehouse = fr.warehouse
        val warehouseArea = fr.warehouseArea
        val rack = fr.rack

        if (itemCode.isEmpty() &&
            itemDescription.isEmpty() &&
            itemEan.isEmpty() &&
            orderId.isEmpty() &&
            orderExternalId.isEmpty() &&
            warehouse.isEmpty() &&
            warehouseArea.isEmpty() &&
            rack.isEmpty()
        ) {
            showProgressBar(false)
            return
        }

        val filter: ArrayList<ApiFilterParam> = arrayListOf()
        if (itemCode.isNotEmpty()) filter.add(ApiFilterParam(EXTENSION_ITEM_CODE, itemCode))
        if (itemDescription.isNotEmpty()) filter.add(ApiFilterParam(EXTENSION_ITEM_DESCRIPTION, itemDescription))
        if (itemEan.isNotEmpty()) filter.add(ApiFilterParam(EXTENSION_ITEM_EAN, itemEan))
        if (orderId.isNotEmpty()) filter.add(ApiFilterParam(EXTENSION_ORDER_ID, orderId))
        if (orderExternalId.isNotEmpty()) filter.add(ApiFilterParam(EXTENSION_ORDER_EXTERNAL_ID, orderExternalId))
        if (warehouse.isNotEmpty()) filter.add(ApiFilterParam(EXTENSION_ORDER_LOCATION_WAREHOUSE_ID, warehouse))
        if (warehouseArea.isNotEmpty()) filter.add(ApiFilterParam(EXTENSION_ORDER_LOCATION_AREA_ID, warehouseArea))
        if (rack.isNotEmpty()) filter.add(ApiFilterParam(EXTENSION_ORDER_LOCATION_RACK_ID, rack))

        try {
            Log.d(this::class.java.simpleName, "Selecting items...")

            GetOrderLocation(
                filter = filter,
                onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it) },
                onFinish = {
                    if (it.any()) fillAdapter(ArrayList(it))
                    else showProgressBar(false)
                }
            ).execute()
        } catch (ex: java.lang.Exception) {
            ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())
            showProgressBar(false)
        }
    }

    private fun fillAdapter(t: ArrayList<OrderLocation>) {
        showProgressBar(true)

        if (!t.any()) {
            getItems()
            return
        }

        completeList = t

        runOnUiThread {
            try {
                if (adapter != null) {
                    // Si el adapter es NULL es porque aún no fue creado.
                    // Por lo tanto, puede ser que los valores de [lastSelected]
                    // sean valores guardados de la instancia anterior y queremos preservarlos.
                    lastSelected = adapter?.currentItem()
                }

                adapter = OrderLocationRecyclerAdapter.Builder()
                    .recyclerView(binding.recyclerView)
                    .fullList(completeList)
                    .checkedIdArray(checkedIdArray)
                    .multiSelect(multiSelect)
                    .showCheckBoxes(showCheckBoxes) { showCheckBoxes = it }
                    .filterOptions(FilterOptions(searchText))
                    .checkedChangedListener(this)
                    .dataSetChangedListener(this)
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
                showProgressBar(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rejectNewInstances = false

        closeKeyboard(this)
        JotterListener.resumeReaderDevices(this)
    }

    public override fun onStart() {
        super.onStart()

        setSearchTextFragment()
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

        orderLocationFilterFragment.itemEan = scanCode
        getItems()
    }

    private fun showSnackBar(it: SnackBarEventData) {
        makeText(binding.root, it.text, it.snackBarType)
    }

    override fun onBackPressed() {
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

        if (!settingViewModel.useBtRfid) {
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
        val allControls = SettingsRepository.getAllSelectOrderLocationVisibleControls()
        val visibleFilters = orderLocationFilterFragment.getVisibleFilters()
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
                (adapter?.fullList ?: ArrayList()).mapTo(codes) { it.itemEan }
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
        when (id) {
            settingRepository.orderLocationSearchByItemDescription.key.hashCode() -> {
                orderLocationFilterFragment.setDescriptionVisibility(if (item.isChecked) View.VISIBLE else GONE)
            }

            settingRepository.orderLocationSearchByItemEan.key.hashCode() -> {
                orderLocationFilterFragment.setEanVisibility(if (item.isChecked) View.VISIBLE else GONE)
            }

            settingRepository.orderLocationSearchByItemCode.key.hashCode() -> {
                orderLocationFilterFragment.setCodeVisibility(if (item.isChecked) View.VISIBLE else GONE)
            }

            settingRepository.orderLocationSearchByOrderId.key.hashCode() -> {
                orderLocationFilterFragment.setOrderIdVisibility(if (item.isChecked) View.VISIBLE else GONE)
            }

            settingRepository.orderLocationSearchByOrderExtId.key.hashCode() -> {
                orderLocationFilterFragment.setOrderExtIdVisibility(if (item.isChecked) View.VISIBLE else GONE)
            }

            settingRepository.orderLocationSearchByWarehouse.key.hashCode() -> {
                orderLocationFilterFragment.setWarehouseVisibility(if (item.isChecked) View.VISIBLE else GONE)
            }

            settingRepository.orderLocationSearchByArea.key.hashCode() -> {
                orderLocationFilterFragment.setAreaVisibility(if (item.isChecked) View.VISIBLE else GONE)
            }

            settingRepository.orderLocationSearchByRack.key.hashCode() -> {
                orderLocationFilterFragment.setRackVisibility(if (item.isChecked) View.VISIBLE else GONE)
            }

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onFilterChanged(
        code: String,
        description: String,
        ean: String,
        orderId: String,
        orderExternalId: String,
        warehouse: String,
        warehouseArea: String,
        rack: String,
        onlyActive: Boolean
    ) {
        Handler(Looper.getMainLooper()).postDelayed({
            closeKeyboard(this)
        }, 100)

        thread {
            getItems()
        }
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
            summaryFragment
                .totalChecked(adapter?.countChecked() ?: 0)
                .fill()
        }
    }

    override fun onFilterChanged(printer: String, qty: Int?) {}

    override fun onPrintRequested(printer: String, qty: Int) {
        /** Acá seleccionamos siguiendo estos criterios:
         *
         * Si NO es multiSelect tomamos el ítem seleccionado de forma simple.
         *
         * Si es multiSelect nos fijamos que o bien estén marcados algunos ítems o
         * bien tengamos un ítem seleccionado de forma simple.
         *
         * Si es así, vamos a devolver los ítems marcados si existen como prioridad.
         *
         * Si no, nos fijamos que NO sean visibles los CheckBoxes, esto quiere
         * decir que el usuario está seleccionado el ítem de forma simple y
         * devolvemos este ítem.
         *
         **/

        val item = adapter?.currentItem()
        val countChecked = adapter?.countChecked() ?: 0
        var itemArray: ArrayList<OrderLocation> = ArrayList()

        if (!multiSelect && item != null) {
            itemArray = arrayListOf(item)
        } else if (multiSelect) {
            if (countChecked > 0 || item != null) {
                if (countChecked > 0) itemArray = adapter?.getAllChecked() ?: ArrayList()
                else if (adapter?.showCheckBoxes == false) {
                    itemArray = arrayListOf(item!!)
                }
            }
        }

        printLabelFragment.printItemById(ArrayList(itemArray.map { it.uniqueId }))
    }

    override fun onQtyTextViewFocusChanged(hasFocus: Boolean) {
        printQtyIsFocused = hasFocus
    }

    override fun onDataSetChanged() {
        runOnUiThread {
            Handler(Looper.getMainLooper()).postDelayed({
                summaryFragment
                    .multiSelect(multiSelect)
                    .totalVisible(adapter?.totalVisible() ?: 0)
                    .totalChecked(adapter?.countChecked() ?: 0)
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
        const val ARG_IDS = "ids"
    }
}