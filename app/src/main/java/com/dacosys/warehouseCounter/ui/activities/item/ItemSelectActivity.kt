package com.dacosys.warehouseCounter.ui.activities.item

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.WindowManager
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.MenuBuilder
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.doOnNextLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.imageControl.dto.DocumentContent
import com.dacosys.imageControl.dto.DocumentContentRequestResult
import com.dacosys.imageControl.network.common.ProgramData
import com.dacosys.imageControl.network.download.GetImages.Companion.toDocumentContentList
import com.dacosys.imageControl.network.webService.WsFunction
import com.dacosys.imageControl.room.dao.ImageCoroutines
import com.dacosys.imageControl.ui.activities.ImageControlCameraActivity
import com.dacosys.imageControl.ui.activities.ImageControlGridActivity
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelTemplate
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelType
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeParam
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.PrintOps
import com.dacosys.warehouseCounter.data.ktor.v2.functions.barcode.GetItemBarcode
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.GetRack
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.GetWarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.GetOrder
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.data.room.entity.item.Item
import com.dacosys.warehouseCounter.data.room.entity.itemCategory.ItemCategory
import com.dacosys.warehouseCounter.data.settings.Preference
import com.dacosys.warehouseCounter.data.settings.SettingsRepository
import com.dacosys.warehouseCounter.databinding.ItemPrintLabelActivityTopPanelCollapsedBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.devices.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.devices.rfid.Rfid
import com.dacosys.warehouseCounter.scanners.devices.vh75.Vh75Bt
import com.dacosys.warehouseCounter.scanners.jotter.ScannerManager
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode
import com.dacosys.warehouseCounter.ui.adapter.FilterOptions
import com.dacosys.warehouseCounter.ui.adapter.item.ItemRecyclerAdapter
import com.dacosys.warehouseCounter.ui.fragments.common.SearchTextFragment
import com.dacosys.warehouseCounter.ui.fragments.common.SelectFilterFragment
import com.dacosys.warehouseCounter.ui.fragments.common.SummaryFragment
import com.dacosys.warehouseCounter.ui.fragments.print.PrintLabelFragment
import com.dacosys.warehouseCounter.ui.panel.BasePanelActivity
import com.dacosys.warehouseCounter.ui.panel.PanelController
import com.dacosys.warehouseCounter.ui.panel.PanelState
import com.dacosys.warehouseCounter.ui.panel.PanelType
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.dacosys.warehouseCounter.ui.utils.ParcelLong
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelableArrayList
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.utils.Screen.Companion.closeKeyboard
import com.dacosys.warehouseCounter.ui.utils.TextViewUtils.Companion.isActionDone
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.concurrent.thread
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.Item as ItemKtor

class ItemSelectActivity(
) : BasePanelActivity(), SwipeRefreshLayout.OnRefreshListener,
    Scanner.ScannerListener, Rfid.RfidDeviceListener, ItemRecyclerAdapter.CheckedChangedListener,
    PrintLabelFragment.FragmentListener, ItemRecyclerAdapter.DataSetChangedListener,
    ItemRecyclerAdapter.AddPhotoRequiredListener, ItemRecyclerAdapter.AlbumViewRequiredListener,
    SearchTextFragment.OnSearchTextFocusChangedListener, SearchTextFragment.OnSearchTextChangedListener,
    SelectFilterFragment.OnFilterItemChangedListener {

    // region Set Panels
    override val stateConfig: PanelController.PanelStateConfiguration
        get() = PanelController.PanelStateConfiguration(
            initialPanelTopState = PanelState.COLLAPSED,
            initialPanelBottomState = PanelState.EXPANDED,
        )
    override val layoutConfig: PanelController.PanelLayoutConfiguration
        get() = PanelController.PanelLayoutConfiguration(
            topPanelExpandedLayout = R.layout.item_print_label_activity_bottom_panel_collapsed,
            bottomPanelExpandedLayout = R.layout.item_print_label_activity_top_panel_collapsed,
            allPanelsExpandedLayout = R.layout.item_print_label_activity,
            allPanelsCollapsedLayout = R.layout.item_print_label_activity_both_panels_collapsed,
        )
    override val textConfig: PanelController.PanelTextConfiguration
        get() = PanelController.PanelTextConfiguration(
            topButtonText = R.string.print_labels,
            bottomButtonText = R.string.search_options,
        )
    override val animationConfig: PanelController.PanelAnimationConfiguration
        get() = PanelController.PanelAnimationConfiguration(
            postImeShowAnimation = ::postImeShowAnimation,
            postTopPanelAnimation = ::postTopPanelAnimation,
            postBottomPanelAnimation = ::postBottomPanelAnimation,
        )

    override fun provideRootLayout(): ConstraintLayout = binding.root

    override fun provideTopButton(): Button? = binding.expandTopPanelButton

    override fun provideBottomButton(): Button? = binding.expandBottomPanelButton

    private fun postImeShowAnimation() {
        if (printQtyIsFocused) handlePanelState(PanelType.BOTTOM, PanelState.COLLAPSED)
        else handlePanelState(PanelState.COLLAPSED, PanelState.COLLAPSED)
    }

    private fun postBottomPanelAnimation() {
        if (panelBottomState == PanelState.EXPANDED) filterFragment.refreshViews()
    }

    private fun postTopPanelAnimation() {
        if (panelBottomState == PanelState.EXPANDED) printLabelFragment?.refreshViews()
    }
    // endregion Set Panels

    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        adapter?.refreshListeners()
        adapter?.refreshImageControlListeners()
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

    private var multiSelect = false
    private var adapter: ItemRecyclerAdapter? = null
    private var lastSelected: Item? = null
    private var firstVisiblePos: Int? = null
    private var currentScrollPosition: Int = 0

    private var completeList: ArrayList<Item> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()

    // Se usa para saber si estamos en onStart luego de onCreate
    private var fillRequired = false
    private var hideFilterPanel = false

    private val menuItemShowImages = 9999
    private var showImages
        get() = settingsVm.itemSelectShowImages
        set(value) {
            settingsVm.itemSelectShowImages = value
        }

    private var showCheckBoxes
        get() =
            if (!multiSelect) false
            else settingsVm.itemSelectShowCheckBoxes
        set(value) {
            settingsVm.itemSelectShowCheckBoxes = value
        }

    private val countChecked: Int
        get() = adapter?.countChecked() ?: 0

    private val allChecked: ArrayList<Item>
        get() = adapter?.getAllChecked() ?: arrayListOf()

    private val currentItem: Item?
        get() = adapter?.currentItem()

    private lateinit var filterFragment: SelectFilterFragment
    private var printLabelFragment: PrintLabelFragment? = null
    private lateinit var summaryFragment: SummaryFragment
    private lateinit var searchTextFragment: SearchTextFragment

    private var filterItemDescription: String = ""
    private var filterItemEan: String = ""
    private var filterItemCategory: ItemCategory? = null
    private var filterItemExternalId: String = ""
    private var filterOnlyActive: Boolean = true

    private var searchedText: String = ""

    private var currentPrintQty: Int = 1
    private var currentTemplateId: Long = 0L

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveBundleValues(outState)
    }

    private fun saveBundleValues(b: Bundle) {
        b.putString(ARG_TITLE, tempTitle)
        b.putBoolean(ARG_SHOW_SELECT_BUTTON, showSelectButton)
        b.putBoolean(ARG_MULTI_SELECT, multiSelect)
        b.putBoolean(ARG_HIDE_FILTER_PANEL, hideFilterPanel)

        if (adapter != null) {
            b.putParcelable(ARG_LAST_SELECTED, currentItem)
            b.putInt(ARG_FIRST_VISIBLE_POS, adapter?.firstVisiblePos() ?: -1)
            b.putParcelableArrayList(ARG_COMPLETE_LIST, adapter?.fullList)
            b.putLongArray(ARG_CHECKED_ID_ARRAY, allChecked.map { it.itemId }.toLongArray())
            b.putInt(ARG_CURRENT_SCROLL_POSITION, currentScrollPosition)
        }

        b.putString(ARG_FILTER_ITEM_DESCRIPTION, filterItemDescription)
        b.putString(ARG_FILTER_ITEM_EAN, filterItemEan)
        b.putParcelable(ARG_FILTER_ITEM_CATEGORY, filterItemCategory)
        b.putString(ARG_FILTER_ITEM_CODE, filterItemExternalId)
        b.putBoolean(ARG_FILTER_ONLY_ACTIVE, filterOnlyActive)

        b.putString(ARG_SEARCHED_TEXT, searchedText)

        b.putInt(ARG_CURRENT_PRINT_QTY, currentPrintQty)
        b.putLong(ARG_CURRENT_TEMPLATE_ID, currentTemplateId)
    }

    private fun loadBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.select_item)

        showSelectButton = b.getBoolean(ARG_SHOW_SELECT_BUTTON, showSelectButton)

        multiSelect = b.getBoolean(ARG_MULTI_SELECT, multiSelect)
        hideFilterPanel = b.getBoolean(ARG_HIDE_FILTER_PANEL, hideFilterPanel)

        // Adapter
        checkedIdArray = (b.getLongArray(ARG_CHECKED_ID_ARRAY) ?: longArrayOf()).toCollection(ArrayList())
        completeList = b.parcelableArrayList(ARG_COMPLETE_LIST) ?: ArrayList()
        lastSelected = b.parcelable(ARG_LAST_SELECTED)
        firstVisiblePos = if (b.containsKey(ARG_FIRST_VISIBLE_POS)) b.getInt(ARG_FIRST_VISIBLE_POS) else -1
        currentScrollPosition = b.getInt(ARG_CURRENT_SCROLL_POSITION)

        // Filter Fragment
        filterItemDescription = b.getString(ARG_FILTER_ITEM_DESCRIPTION) ?: ""
        filterItemEan = b.getString(ARG_FILTER_ITEM_EAN) ?: ""
        filterItemCategory = b.parcelable(ARG_FILTER_ITEM_CATEGORY)
        filterItemExternalId = b.getString(ARG_FILTER_ITEM_CODE) ?: ""
        filterOnlyActive = b.getBoolean(ARG_FILTER_ONLY_ACTIVE)

        // Search Text Fragment
        searchedText = b.getString(ARG_SEARCHED_TEXT) ?: ""

        // Printer Fragment
        currentPrintQty = b.getInt(ARG_CURRENT_PRINT_QTY)
        currentTemplateId = b.getLong(ARG_CURRENT_TEMPLATE_ID)
    }

    private fun loadExtrasBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.select_item)

        hideFilterPanel = b.getBoolean(ARG_HIDE_FILTER_PANEL)
        multiSelect = b.getBoolean(ARG_MULTI_SELECT, multiSelect)

        showSelectButton = b.getBoolean(ARG_SHOW_SELECT_BUTTON, showSelectButton)
    }

    private lateinit var binding: ItemPrintLabelActivityTopPanelCollapsedBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = ItemPrintLabelActivityTopPanelCollapsedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initPanelController(savedInstanceState)
        setSupportActionBar(binding.topAppbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setBackPressCallback()

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                currentScrollPosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            }
        })

        // Para el llenado en el onStart siguiente de onCreate
        fillRequired = true
        restoreInstanceState(savedInstanceState)

        setFragments()
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

        setButtons()
        setSearchPanelVisibility(!hideFilterPanel)

        Screen.setupUI(binding.root, this)
    }

    private fun setBackPressCallback() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isBackPressed()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun restoreInstanceState(b: Bundle?) {
        if (b != null) {
            loadBundleValues(b)
        } else {
            val extras = intent.extras
            if (extras != null) loadExtrasBundleValues(extras)
        }
        binding.topAppbar.title = tempTitle
    }

    private fun setFragments() {
        filterFragment = supportFragmentManager.findFragmentById(R.id.filterFragment) as SelectFilterFragment
        printLabelFragment = supportFragmentManager.findFragmentById(R.id.printFragment) as PrintLabelFragment
        summaryFragment = supportFragmentManager.findFragmentById(R.id.summaryFragment) as SummaryFragment
        searchTextFragment = supportFragmentManager.findFragmentById(R.id.searchTextFragment) as SearchTextFragment
    }

    private fun setButtons() {
        binding.okButton.setOnClickListener { itemSelect() }
    }

    private fun setSearchPanelVisibility(visible: Boolean) {
        if (!visible) {
            handlePanelState(PanelType.BOTTOM, PanelState.COLLAPSED)
            if (lastOrientation == Configuration.ORIENTATION_PORTRAIT)
                binding.expandBottomPanelButton?.visibility = GONE
        }
    }

    private fun setupPrintLabelFragment() {
        binding.printFragment.visibility = View.VISIBLE

        if (currentTemplateId == 0L) {
            currentTemplateId = settingsVm.defaultItemTemplateId
        }

        printLabelFragment?.saveSharedPreferences()
        val fragment =
            PrintLabelFragment.Builder()
                .setTemplateTypeIdList(arrayListOf(BarcodeLabelType.item.id))
                .setTemplateId(currentTemplateId)
                .setQty(currentPrintQty)
                .build()
        printLabelFragment = fragment
        supportFragmentManager.beginTransaction().replace(R.id.printFragment, fragment).commit()
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
                .searchByItemDescription(sv.itemSearchByItemDescription, sr.itemSearchByItemDescription)
                .searchByItemEan(sv.itemSearchByItemEan, sr.itemSearchByItemEan)
                .searchByItemExternalId(sv.itemSearchByItemExternalId, sr.itemSearchByItemExternalId)
                .searchByCategory(sv.itemSearchByCategory, sr.itemSearchByCategory)
                .itemDescription(filterItemDescription)
                .itemEan(filterItemEan)
                .itemExternalId(filterItemExternalId)
                .itemCategory(filterItemCategory)
                .onlyActive(filterOnlyActive)
                .build()
        supportFragmentManager.beginTransaction().replace(R.id.filterFragment, filterFragment).commit()
    }

    override fun onFilterChanged(
        externalId: String,
        description: String,
        ean: String,
        itemCategory: ItemCategory?,
        onlyActive: Boolean
    ) {
        filterItemDescription = description
        filterItemEan = ean
        filterItemCategory = itemCategory
        filterItemExternalId = externalId
        filterOnlyActive = onlyActive

        Handler(Looper.getMainLooper()).postDelayed({ getItems() }, 200)
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshItem.isRefreshing = show
        }, 20)
    }

    private fun itemSelect() {
        closeKeyboard(this)

        val itemArray = adapter?.selectedItems() ?: arrayListOf()

        if (!itemArray.any()) {
            showMessage(getString(R.string.you_must_select_at_least_one_item), ERROR)
            return
        }

        val data = Intent()
        data.putParcelableArrayListExtra(ARG_IDS, itemArray.map { ParcelLong(it.itemId) } as ArrayList<ParcelLong>)
        setResult(RESULT_OK, data)

        isFinishingByUser = true
        finish()
    }

    private fun getItems() {
        // Limpiamos los ítems marcados
        checkedIdArray.clear()

        val itemEan = filterFragment.getItemEan().trim()
        val itemDescription = filterFragment.getDescription().trim()
        val externalId = filterFragment.getItemExternalId().trim()
        val itemCategory = filterFragment.getItemCategory()

        if (itemEan.isEmpty() && externalId.isEmpty() && itemDescription.isEmpty() && itemCategory == null) {
            fillAdapter(arrayListOf())
            return
        }

        try {
            Log.d(tag, "Selecting items...")
            ItemCoroutines.getByQuery(
                ean = itemEan,
                externalId = externalId,
                description = itemDescription,
                itemCategoryId = itemCategory?.itemCategoryId,
                useLike = true
            ) {
                fillAdapter(it)
            }
        } catch (ex: java.lang.Exception) {
            ErrorLog.writeLog(this, tag, ex.message.toString())
            showProgressBar(false)
        }
    }

    private fun fillAdapter(t: ArrayList<Item>) {
        Screen.closeKeyboard(this)
        handlePanelState(PanelType.BOTTOM, PanelState.COLLAPSED)
        showProgressBar(true)

        completeList = t

        runOnUiThread {
            try {
                // Preservar selección solo si ya existe un adapter
                val preserveSelection = adapter != null
                if (preserveSelection) {
                    lastSelected = currentItem
                }

                adapter = createNewAdapter()
                binding.recyclerView.layoutManager = LinearLayoutManager(this)
                binding.recyclerView.adapter = adapter

                // Programar acciones posteriores al layout
                binding.recyclerView.doOnNextLayout { view ->
                    val targetItem = if (preserveSelection) lastSelected else t.firstOrNull()
                    adapter?.apply {
                        selectItem(targetItem, false)
                        scrollToPos(currentScrollPosition, true)
                    }
                    showProgressBar(false)
                }

                // Forzar layout si no hay cambios que lo triggerearían
                if (binding.recyclerView.isLaidOut) {
                    binding.recyclerView.requestLayout()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, tag, ex)
                showProgressBar(false)
            }
        }
    }

    private fun createNewAdapter(): ItemRecyclerAdapter {
        return ItemRecyclerAdapter.Builder()
            .recyclerView(binding.recyclerView)
            .fullList(completeList)
            .checkedIdArray(checkedIdArray)
            .multiSelect(multiSelect)
            .showCheckBoxes(showCheckBoxes) { showCheckBoxes = it }
            .showImages(showImages) { showImages = it }
            .filterOptions(FilterOptions(searchedText))
            .checkedChangedListener(this)
            .dataSetChangedListener(this)
            .addPhotoRequiredListener(this)
            .albumViewRequiredListener(this)
            .build()
    }

    public override fun onStart() {
        super.onStart()
        rejectNewInstances = false

        closeKeyboard(this)

        if (fillRequired) {
            fillRequired = false
            fillAdapter(completeList)
        }
    }

    override fun onSearchTextFocusChange(hasFocus: Boolean) {
        searchTextIsFocused = hasFocus
        if (!hasFocus) {
            handlePanelState(PanelType.TOP, PanelState.COLLAPSED)
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
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
            ScannerManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun scannerCompleted(scanCode: String) {
        if (settingsVm.showScannedCode) showMessage(scanCode, INFO)

        // Nada que hacer, volver
        if (scanCode.trim().isEmpty()) {
            val res = context.getString(R.string.invalid_code)
            showMessage(res, ERROR)
            ErrorLog.writeLog(this, tag, res)
            return
        }

        ScannerManager.lockScanner(this, true)

        GetResultFromCode.Builder()
            .withCode(scanCode)
            .searchItemId()
            .searchItemCode()
            .searchItemEan()
            .searchItemRegex()
            .searchItemUrl()
            .onFinish {
                ScannerManager.lockScanner(this, false)
                proceedByResult(it)
            }
            .build()
    }

    private fun showMessage(msg: String, type: SnackBarType) {
        if (isFinishing || isDestroyed) return
        if (type == ERROR) Log.e(javaClass.simpleName, msg)
        makeText(binding.root, msg, type)
    }

    private fun isBackPressed() {
        closeKeyboard(this)

        isFinishingByUser = true
        setResult(RESULT_CANCELED)
        finish()
    }

    private val debugMenuItems = listOf(
        DebugMenuItem(999001, "Manual code") { enterCode() },
        DebugMenuItem(999002, "Random EAN") {
            ItemCoroutines.getEanCodes(true) {
                if (it.isNotEmpty()) scannerCompleted(it.random())
            }
        },
        DebugMenuItem(999003, "Random item ID") {
            ItemCoroutines.getIds(true) {
                if (it.isNotEmpty()) scannerCompleted("${GetResultFromCode.PREFIX_ITEM}${it.random()}#")
            }
        },
        DebugMenuItem(999004, "Random item ID URL") {
            ItemCoroutines.getIds(true) {
                if (it.isNotEmpty()) scannerCompleted("${GetResultFromCode.PREFIX_ITEM_URL}${it.random()}")
            }
        },
        DebugMenuItem(999005, "Random item on list") {
            (adapter?.fullList ?: emptyList())
                .map { it.ean }
                .takeIf { it.isNotEmpty() }
                ?.let { scannerCompleted(it.random()) }
        },
        DebugMenuItem(999006, "Random order") {
            GetOrder(onFinish = {
                if (it.isNotEmpty()) scannerCompleted("${GetResultFromCode.PREFIX_ORDER}${it.random().id}#")
            }).execute()
        },
        DebugMenuItem(999007, "Random rack") {
            GetRack(onFinish = {
                if (it.isNotEmpty()) scannerCompleted("${GetResultFromCode.PREFIX_RACK}${it.random().id}#")
            }).execute()
        },
        DebugMenuItem(999008, "Random area") {
            GetWarehouseArea(onFinish = {
                if (it.isNotEmpty()) scannerCompleted("${GetResultFromCode.PREFIX_WA}${it.random().id}#")
            }).execute()
        }
    )

    private data class DebugMenuItem(
        val id: Int,
        val title: String,
        val action: () -> Unit
    )

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_read_activity, menu)

        // Eliminar RFID si no está habilitado
        if (!settingsVm.useRfid) {
            menu.removeItem(R.id.action_rfid_connect)
        }

        // Menú de mostrar imágenes
        setupImageVisibilityMenu(menu)

        // Menús de debug
        if (BuildConfig.DEBUG || Statics.TEST_MODE) {
            debugMenuItems.forEach { item ->
                menu.add(Menu.NONE, item.id, Menu.NONE, item.title)
            }
        }

        // Menú de visibilidad de controles
        setupVisibilityControlsMenu(menu)

        // Icono de overflow personalizado
        binding.topAppbar.overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_visibility)

        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        return true
    }

    private fun setupImageVisibilityMenu(menu: Menu) {
        if (!settingsVm.useImageControl) return

        val iconRes = if (showImages) R.drawable.ic_photo_library else R.drawable.ic_hide_image
        val icon = ContextCompat.getDrawable(context, iconRes)?.apply {
            mutate()
            colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                getColor(R.color.dimgray), BlendModeCompat.SRC_IN
            )
        }

        menu.add(Menu.NONE, menuItemShowImages, Menu.NONE, context.getString(R.string.show_images))
            .apply {
                isCheckable = true
                isChecked = showImages
                this.icon = icon
            }
    }

    private fun setupVisibilityControlsMenu(menu: Menu) {
        val visibleFilters = filterFragment.getVisibleFilters()
        val allControls = SettingsRepository.getAllSelectItemVisibleControls()

        allControls.forEach { control ->
            val menuItem = menu.add(0, control.key.hashCode(), Menu.NONE, control.description).apply {
                isCheckable = true
                isChecked = visibleFilters.contains(control)
            }

            // Configurar para mantener el menú abierto
            if (visibleFilters.contains(control)) {
                menuItem.apply {
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
                    actionView = View(context)
                    setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                        override fun onMenuItemActionExpand(item: MenuItem) = false
                        override fun onMenuItemActionCollapse(item: MenuItem) = false
                    })
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.home, android.R.id.home -> {
                isBackPressed()
                true
            }

            R.id.action_rfid_connect -> {
                ScannerManager.rfidStart(this)
                true
            }

            R.id.action_trigger_scan -> {
                ScannerManager.trigger(this)
                true
            }

            R.id.action_read_barcode -> {
                ScannerManager.toggleCameraFloatingWindowVisibility(this)
                true
            }

            menuItemShowImages -> handleImageVisibilityItem(item)
            else -> handleDebugOrFilterItems(item)
        }
    }

    private fun handleImageVisibilityItem(item: MenuItem): Boolean {
        item.isChecked = !item.isChecked
        showImages = item.isChecked
        adapter?.showImages(item.isChecked)

        val iconRes = if (item.isChecked) R.drawable.ic_photo_library else R.drawable.ic_hide_image
        val icon = ContextCompat.getDrawable(context, iconRes)?.apply {
            mutate()
            colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                getColor(R.color.dimgray), BlendModeCompat.SRC_IN
            )
        }
        item.icon = icon

        return true
    }

    private fun handleDebugOrFilterItems(item: MenuItem): Boolean {
        // Manejar ítems de debug
        debugMenuItems.find { it.id == item.itemId }?.let {
            it.action()
            return true
        }

        // Manejar ítems de filtros
        val control = SettingsRepository.getAllSelectItemVisibleControls()
            .firstOrNull { it.key.hashCode() == item.itemId }

        return control?.let {
            item.isChecked = !item.isChecked
            updateFilterVisibility(it, item.isChecked)
            true
        } ?: super.onOptionsItemSelected(item)
    }

    private fun updateFilterVisibility(control: Preference, isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else GONE
        val settings = settingsVm

        when (control) {
            settingsRepository.itemSearchByItemEan -> {
                filterFragment.setEanVisibility(visibility)
                settings.itemSearchByItemEan = isVisible
            }

            settingsRepository.itemSearchByCategory -> {
                filterFragment.setCategoryVisibility(visibility)
                settings.itemSearchByCategory = isVisible
            }

            settingsRepository.itemSearchByItemDescription -> {
                filterFragment.setDescriptionVisibility(visibility)
                settings.itemSearchByItemDescription = isVisible
            }

            settingsRepository.itemSearchByItemExternalId -> {
                filterFragment.setEanVisibility(visibility)
                settings.itemSearchByItemExternalId = isVisible
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

    private fun proceedByResult(it: GetResultFromCode.CodeResult) {
        val r = it.item
        if (r !is ItemKtor) return

        val item = r.toRoom()
        val pos = adapter?.getIndexById(item.itemId) ?: NO_POSITION

        if (pos != NO_POSITION && pos < (adapter?.itemCount ?: 0)) {
            adapter?.selectItem(item)
        } else {
            runOnUiThread {
                filterFragment.setItemEan(item.ean)
                thread {
                    completeList = arrayListOf(item)
                    checkedIdArray.clear()
                    fillAdapter(completeList)
                }
            }
        }
    }

    override fun onFilterChanged(printer: String, template: BarcodeLabelTemplate?, qty: Int?) {
        currentPrintQty = qty ?: 1
        currentTemplateId = template?.templateId ?: return

        settingsVm.defaultItemTemplateId = currentTemplateId
    }

    override fun onPrintRequested(printer: String, qty: Int) {
        val itemArray = adapter?.selectedItems() ?: arrayListOf()
        val template = printLabelFragment?.template ?: return

        if (!itemArray.any()) {
            showMessage(getString(R.string.you_must_select_at_least_one_item), ERROR)
            return
        }

        GetItemBarcode(
            param = BarcodeParam(
                idList = ArrayList(itemArray.map { it.itemId }),
                templateId = template.templateId,
                printOps = PrintOps.getPrintOps()
            ),
            onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showMessage(it.text, it.snackBarType) },
            onFinish = {
                printLabelFragment?.printBarcodes(labelArray = it, onFinish = {})
            }
        ).execute()
    }

    override fun onQtyTextViewFocusChanged(hasFocus: Boolean) {
        printQtyIsFocused = hasFocus
        if (!hasFocus) {
            handlePanelState(PanelType.TOP, PanelState.COLLAPSED)
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

    override fun onStateChanged(state: Int) {
        if (!::binding.isInitialized || isFinishing || isDestroyed) return
        if (settingsVm.rfidShowConnectedMessage) {
            when (Rfid.vh75State) {
                Vh75Bt.STATE_CONNECTED -> showMessage(getString(R.string.rfid_connected), SnackBarType.SUCCESS)
                Vh75Bt.STATE_CONNECTING -> showMessage(
                    getString(R.string.searching_rfid_reader),
                    SnackBarType.RUNNING
                )

                else -> showMessage(getString(R.string.there_is_no_rfid_device_connected), INFO)
            }
        }
    }

    override fun onReadCompleted(scanCode: String) {
        scannerCompleted(scanCode)
    }
    //endregion READERS Reception

    //region ImageControl
    override fun onAddPhotoRequired(tableId: Int, itemId: Long, description: String) {
        if (!settingsVm.useImageControl) {
            return
        }

        if (!rejectNewInstances) {
            rejectNewInstances = true

            val intent = Intent(this, ImageControlCameraActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra(ImageControlCameraActivity.ARG_PROGRAM_OBJECT_ID, tableId.toLong())
            intent.putExtra(ImageControlCameraActivity.ARG_OBJECT_ID_1, itemId.toString())
            intent.putExtra(ImageControlCameraActivity.ARG_DESCRIPTION, description)
            intent.putExtra(ImageControlCameraActivity.ARG_ADD_PHOTO, settingsVm.autoSend)
            resultForPhotoCapture.launch(intent)
        }
    }

    private val resultForPhotoCapture =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val item = currentItem ?: return@registerForActivityResult
                    adapter?.updateItem(item, true)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                rejectNewInstances = false
            }
        }

    override fun onAlbumViewRequired(tableId: Int, itemId: Long) {
        if (!settingsVm.useImageControl) {
            return
        }

        if (rejectNewInstances) return
        rejectNewInstances = true

        tempObjectId = itemId.toString()
        tempTableId = tableId

        val programData = ProgramData(
            programObjectId = tempTableId.toLong(),
            objId1 = tempObjectId
        )

        ImageCoroutines().get(context = context, programData = programData) {
            val allLocal = toDocumentContentList(images = it, programData = programData)
            if (allLocal.isEmpty()) {
                getFromWebservice()
            } else {
                showPhotoAlbum(allLocal)
            }
        }
    }

    private fun getFromWebservice() {
        WsFunction().documentContentGetBy12(
            programObjectId = tempTableId,
            objectId1 = tempObjectId
        ) { it2 ->
            if (it2 != null) fillResults(it2)
            else {
                showMessage(getString(R.string.no_images), INFO)
                rejectNewInstances = false
            }
        }
    }

    private fun showPhotoAlbum(images: ArrayList<DocumentContent> = ArrayList()) {
        val intent = Intent(this, ImageControlGridActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra(ImageControlGridActivity.ARG_PROGRAM_OBJECT_ID, tempTableId.toLong())
        intent.putExtra(ImageControlGridActivity.ARG_OBJECT_ID_1, tempObjectId)
        intent.putExtra(ImageControlGridActivity.ARG_DOC_CONT_OBJ_ARRAY_LIST, images)
        resultForShowPhotoAlbum.launch(intent)
    }

    private val resultForShowPhotoAlbum =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            rejectNewInstances = false
        }

    private var tempObjectId = ""
    private var tempTableId = 0

    private fun fillResults(docContReqResObj: DocumentContentRequestResult) {
        if (docContReqResObj.documentContentArray.isEmpty()) {
            showMessage(getString(R.string.no_images), INFO)
            rejectNewInstances = false
            return
        }

        val anyAvailable = docContReqResObj.documentContentArray.any { it.available }

        if (!anyAvailable) {
            showMessage(getString(R.string.images_not_yet_processed), INFO)
            rejectNewInstances = false
            return
        }

        showPhotoAlbum()
    }
    //endregion ImageControl

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_MULTI_SELECT = "multiSelect"
        const val ARG_SHOW_SELECT_BUTTON = "showSelectButton"
        const val ARG_HIDE_FILTER_PANEL = "hideFilterPanel"
        const val ARG_IDS = "ids"

        const val ARG_LAST_SELECTED = "lastSelected"
        const val ARG_FIRST_VISIBLE_POS = "firstVisiblePos"
        const val ARG_COMPLETE_LIST = "completeList"
        const val ARG_CHECKED_ID_ARRAY = "checkedIdArray"
        const val ARG_CURRENT_SCROLL_POSITION = "currentScrollPosition"
        const val ARG_FILTER_ITEM_DESCRIPTION = "filterItemDescription"
        const val ARG_FILTER_ITEM_EAN = "filterItemEan"
        const val ARG_FILTER_ITEM_CATEGORY = "filterItemCategory"
        const val ARG_FILTER_ITEM_CODE = "filterItemCode"
        const val ARG_FILTER_ONLY_ACTIVE = "filterOnlyActive"
        const val ARG_SEARCHED_TEXT = "searchedText"
        const val ARG_CURRENT_PRINT_QTY = "currentPrintQty"
        const val ARG_CURRENT_TEMPLATE_ID = "currentTemplateId"
    }
}