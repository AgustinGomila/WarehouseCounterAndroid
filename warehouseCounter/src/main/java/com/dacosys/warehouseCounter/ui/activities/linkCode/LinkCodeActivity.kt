package com.dacosys.warehouseCounter.ui.activities.linkCode

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
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
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
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
import com.dacosys.warehouseCounter.data.ktor.v2.functions.itemCode.SendItemCodeArray
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.data.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.data.room.entity.item.Item
import com.dacosys.warehouseCounter.data.room.entity.itemCategory.ItemCategory
import com.dacosys.warehouseCounter.data.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.data.settings.SettingsRepository
import com.dacosys.warehouseCounter.databinding.LinkCodeActivityBottomPanelCollapsedBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.lineSeparator
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.scanners.LifecycleListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode
import com.dacosys.warehouseCounter.ui.adapter.FilterOptions
import com.dacosys.warehouseCounter.ui.adapter.item.ItemRecyclerAdapter
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
import com.dacosys.warehouseCounter.ui.utils.TextViewUtils.Companion.isActionDone
import com.dacosys.warehouseCounter.ui.views.CounterHandler
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*
import kotlin.concurrent.thread
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.Item as ItemKtor

class LinkCodeActivity : AppCompatActivity(), Scanner.ScannerListener, Rfid.RfidDeviceListener,
    ItemRecyclerAdapter.SelectedItemChangedListener, ItemRecyclerAdapter.CheckedChangedListener,
    CounterHandler.CounterListener, SelectFilterFragment.OnFilterItemChangedListener,
    SwipeRefreshLayout.OnRefreshListener, ItemRecyclerAdapter.DataSetChangedListener,
    ItemRecyclerAdapter.AddPhotoRequiredListener, ItemRecyclerAdapter.AlbumViewRequiredListener,
    SearchTextFragment.OnSearchTextFocusChangedListener, SearchTextFragment.OnSearchTextChangedListener {

    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        adapter?.refreshListeners()
        adapter?.refreshImageControlListeners()
    }

    override fun onSelectedItemChanged(item: Item?) {
        if (item != null) {
            ItemCodeCoroutines.getByItemId(item.itemId) {
                var allItemCodes = ""
                val breakLine = lineSeparator

                runOnUiThread {
                    if (it.isNotEmpty()) {
                        binding.moreCodesConstraintLayout.visibility = VISIBLE

                        for (i in it) {
                            allItemCodes = String.format(
                                "%s%s (%s)%s", allItemCodes, i.code, i.qty.toString(), breakLine
                            )
                        }
                    } else {
                        binding.moreCodesConstraintLayout.visibility = GONE
                    }

                    binding.moreCodesEditText.setText(
                        allItemCodes.trim(), TextView.BufferType.EDITABLE
                    )
                }
            }
        } else {
            runOnUiThread {
                binding.moreCodesConstraintLayout.visibility = GONE
            }
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

        GetResultFromCode.Builder()
            .withCode(scanCode)
            .searchItemCode()
            .searchItemEan()
            .searchItemId()
            .searchItemRegex()
            .searchItemUrl()
            .onFinish {
                LifecycleListener.lockScanner(this, false)
                proceedByResult(it, scanCode)
            }
            .build()
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    override fun onIncrement(view: View?, number: Double) {
        binding.qtyEditText.setText(number.toString())
    }

    override fun onDecrement(view: View?, number: Double) {
        binding.qtyEditText.setText(number.toString())
    }

    private var rejectNewInstances = false

    private var tempTitle = ""

    private var panelIsExpanded = false
    private var searchTextIsFocused = false

    private var linkCode: String = ""

    private var ch: CounterHandler? = null
    private var multiSelect = false

    private var adapter: ItemRecyclerAdapter? = null
    private var lastSelected: Item? = null
    private var firstVisiblePos: Int? = null
    private var currentScrollPosition: Int = 0

    // Se usa para saber si estamos en onStart luego de onCreate
    private var fillRequired = false

    // region Variables para LoadOnDemand
    private var completeList: ArrayList<Item> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()
    // endregion

    // Fragments
    private lateinit var filterFragment: SelectFilterFragment
    private lateinit var summaryFragment: SummaryFragment
    private lateinit var searchTextFragment: SearchTextFragment

    private var filterItemDescription: String = ""
    private var filterItemEan: String = ""
    private var filterItemCategory: ItemCategory? = null
    private var filterOnlyActive: Boolean = true
    private var filterItemCode: String = ""

    private var searchedText: String = ""

    private val menuItemShowImages = 9999
    private var showImages
        get() = settingsVm.linkCodeShowImages
        set(value) {
            settingsVm.linkCodeShowImages = value
        }

    private var showCheckBoxes
        get() =
            if (!multiSelect) false
            else settingsVm.linkCodeShowCheckBoxes
        set(value) {
            settingsVm.linkCodeShowCheckBoxes = value
        }

    private val countChecked: Int
        get() {
            return adapter?.countChecked() ?: 0
        }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        saveBundleValues(savedInstanceState)
    }

    private fun saveBundleValues(b: Bundle) {
        b.putString(ARG_TITLE, tempTitle)
        b.putBoolean(ARG_MULTISELECT, multiSelect)

        b.putString("linkCode", linkCode)
        b.putBoolean("panelIsExpanded", panelIsExpanded)

        if (adapter != null) {
            b.putParcelable("lastSelected", (adapter ?: return).currentItem())
            b.putInt("firstVisiblePos", (adapter ?: return).firstVisiblePos())
            b.putParcelableArrayList("completeList", adapter?.fullList)
            b.putLongArray("checkedIdArray", adapter?.getAllChecked()?.map { it.itemId }?.toLongArray())
            b.putInt("currentScrollPosition", currentScrollPosition)
        }

        b.putString("filterItemDescription", filterItemDescription)
        b.putString("filterItemEan", filterItemEan)
        b.putParcelable("filterItemCategory", filterItemCategory)
        b.putString("filterItemCode", filterItemCode)
        b.putBoolean("filterOnlyActive", filterOnlyActive)

        b.putString("searchedText", searchedText)
    }

    private fun loadBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.link_code)

        multiSelect = b.getBoolean(ARG_MULTISELECT, multiSelect)
        panelIsExpanded = b.getBoolean("panelIsExpanded")

        linkCode = b.getString("linkCode") ?: ""

        // Adapter
        checkedIdArray = (b.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(ArrayList())
        completeList = b.parcelableArrayList("completeList") ?: ArrayList()
        lastSelected = b.parcelable("lastSelected")
        firstVisiblePos = if (b.containsKey("firstVisiblePos")) b.getInt("firstVisiblePos") else -1
        currentScrollPosition = b.getInt("currentScrollPosition")

        // Filter Fragment
        filterItemDescription = b.getString("filterItemDescription") ?: ""
        filterItemEan = b.getString("filterItemEan") ?: ""
        filterItemCategory = b.parcelable("filterItemCategory")
        filterItemCode = b.getString("filterItemCode") ?: ""
        filterOnlyActive = b.getBoolean("filterOnlyActive")

        // Search Text Fragment
        searchedText = b.getString("searchedText") ?: ""
    }

    private fun loadExtrasBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.link_code)

        multiSelect = b.getBoolean(ARG_MULTISELECT, false)
    }

    private lateinit var binding: LinkCodeActivityBottomPanelCollapsedBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = LinkCodeActivityBottomPanelCollapsedBinding.inflate(layoutInflater)
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

        //// INICIALIZAR CONTROLES
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

        // Esta clase controla el comportamiento de los botones (+) y (-)
        ch = CounterHandler.Builder()
            .incrementalView(binding.moreButton)
            .decrementalView(binding.lessButton).minRange(1.0) // cant go any less than -50
            .maxRange(100.0) // cant go any further than 50
            .isCycle(false) // 49,50,-50,-49 and so on
            .counterDelay(50) // speed of counter
            .startNumber(1.0).counterStep(1L)  // steps e.g. 0,2,4,6...
            .listener(this) // to listen to counter-results and show them in app
            .build()

        binding.qtyEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                // Filtro que devuelve un texto válido
                val validStr = getValidValue(
                    source = s.toString(),
                    maxValue = 100.toDouble(),
                    decimalSeparator = settingsVm.decimalSeparator
                )

                // Si es NULL no hay que hacer cambios en el texto
                // porque está dentro de las reglas del filtro
                if (validStr != null && validStr != s.toString()) {
                    s.clear()
                    s.insert(0, validStr)
                }
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
            }
        })

        binding.qtyEditText.setText(1.toString(), TextView.BufferType.EDITABLE)
        binding.qtyEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                Screen.showKeyboard(this)
            }
        }

        binding.qtyEditText.setOnKeyListener { _, _, event ->
            if (isActionDone(event)) {
                binding.linkButton.performClick()
                true
            } else {
                false
            }
        }
        // Cambia el modo del teclado en pantalla a tipo numérico
        // cuando este control lo necesita.
        binding.qtyEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER)

        binding.codeEditText.setText(linkCode, TextView.BufferType.EDITABLE)

        binding.swipeRefreshItem.setOnRefreshListener(this)
        binding.swipeRefreshItem.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Para expandir y colapsar el panel
        addAnimationOperations()

        binding.linkButton.setOnClickListener { link() }
        binding.unlinkButton.setOnClickListener { unlink() }
        binding.sendButton.setOnClickListener { sendDialog() }

        // Ocultar panel de códigos vinculados al ítem seleccionado
        binding.moreCodesConstraintLayout.visibility = GONE

        Screen.setupUI(binding.root, this)
    }

    private fun setupFilterFragment() {
        val sv = settingsVm
        val sr = settingsRepository
        filterFragment =
            SelectFilterFragment.Builder()
                .searchByItemDescription(sv.linkCodeSearchByItemDescription, sr.linkCodeSearchByItemDescription)
                .searchByItemEan(sv.linkCodeSearchByItemEan, sr.linkCodeSearchByItemEan)
                .searchByCategory(sv.linkCodeSearchByCategory, sr.linkCodeSearchByCategory)
                .itemDescription(filterItemDescription)
                .itemEan(filterItemEan)
                .itemCategory(filterItemCategory)
                .itemExternalId(filterItemCode)
                .onlyActive(filterOnlyActive)
                .build()
        supportFragmentManager.beginTransaction().replace(R.id.filterFragment, filterFragment).commit()
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

                override fun onStart(
                    animation: WindowInsetsAnimationCompat,
                    bounds: WindowInsetsAnimationCompat.BoundsCompat
                ): WindowInsetsAnimationCompat.BoundsCompat {
                    // Ocultamos el panel de asistente para hacer lugar al teclado en pantalla que está por aparecer.
                    if (!isKeyboardVisible) setToolTipVisibility(GONE)
                    return super.onStart(animation, bounds)
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
        if (!isKeyboardVisible) {
            // Visibilidad del panel de ingreso de códigos y cantidades
            setInputPanelVisibility(VISIBLE)

            // Expandir el panel de búsqueda y sumario para darle espacio al teclado
            setSearchAndSummaryPanelVisibility(VISIBLE)

            // Visibilidad del panel superior de guía
            setToolTipVisibility(VISIBLE)
        }

        // Si estamos esperando que termine la animación para ejecutar un cambio de vista
        if (changePanelsStateAtFinish) {
            changePanelsStateAtFinish = false
            setPanels()
        } else if (isKeyboardVisible) {
            val orientation = resources.configuration.orientation
            when {
                !inputPanelIsFocused() -> {
                    panelIsExpanded = false
                    setPanels()

                    // No necesitamos ver el panel de ingreso de códigos y cantidades
                    setInputPanelVisibility(GONE)
                }

                orientation == Configuration.ORIENTATION_PORTRAIT && inputPanelIsFocused() -> {
                    panelIsExpanded = false
                    setPanels()

                    // Colapsar el panel de búsqueda y sumario para darle espacio al teclado
                    setSearchAndSummaryPanelVisibility(GONE)
                }
            }
        }
    }

    private fun inputPanelIsFocused(): Boolean {
        return binding.codeEditText.isFocused || binding.qtyEditText.isFocused
    }
    // endregion

    private fun setSendButtonText() {
        Handler(Looper.getMainLooper()).postDelayed({
            ItemCodeCoroutines.getToUpload {
                runOnUiThread {
                    binding.sendButton.text =
                        String.format("%s%s(%s)", context.getString(R.string.send), lineSeparator, it.count())
                }
            }
        }, 250)
    }

    private fun sendDialog() {
        ItemCodeCoroutines.getToUpload {
            if (it.isNotEmpty()) {
                askForSendItemCodes(it)
            } else {
                showSnackBar(
                    context.getString(R.string.there_are_no_item_codes_to_send), INFO
                )
            }
        }
    }

    private fun askForSendItemCodes(it: ArrayList<ItemCode>) {
        runOnUiThread {
            val alert = AlertDialog.Builder(this)
            alert.setTitle(getString(R.string.send_item_codes))
            alert.setMessage(
                if (it.count() > 1) context.getString(R.string.do_you_want_to_send_the_item_codes)
                else context.getString(R.string.do_you_want_to_send_the_item_code)
            )
            alert.setNegativeButton(R.string.cancel, null)
            alert.setPositiveButton(R.string.ok) { _, _ ->
                try {
                    sendItemCodes(it)
                } catch (ex: Exception) {
                    ErrorLog.writeLog(this, tag, ex.message.toString())
                }
            }
            alert.show()
        }
    }

    private fun sendItemCodes(it: ArrayList<ItemCode>) {
        thread {
            SendItemCodeArray(
                payload = it,
                onEvent = { showSnackBar(it.text, it.snackBarType) },
                onFinish = { setSendButtonText() }
            ).execute()
        }
    }

    private fun link() {
        if (adapter?.currentItem() != null) {
            val item = adapter?.currentItem()
            if (item != null) {
                val tempCode = binding.codeEditText.text.toString()
                if (tempCode.isEmpty()) {
                    showSnackBar(
                        context.getString(R.string.you_must_select_a_code_to_link), ERROR
                    )
                    return
                }

                val tempStrQty = binding.qtyEditText.text.toString()
                if (tempStrQty.isEmpty()) {
                    showSnackBar(
                        context.getString(R.string.you_must_select_an_amount_to_link), ERROR
                    )
                    return
                }

                val tempQty: Double?
                try {
                    tempQty = java.lang.Double.parseDouble(tempStrQty)
                } catch (e: NumberFormatException) {
                    showSnackBar(context.getString(R.string.invalid_amount), ERROR)
                    return
                }

                if (tempQty <= 0) {
                    showSnackBar(
                        context.getString(R.string.you_must_select_a_positive_amount_greater_than_zero), ERROR
                    )
                    return
                }

                // Chequear si ya está vinculado
                ItemCodeCoroutines.getByCode(tempCode) {
                    if (it.size > 0) {
                        showSnackBar(
                            context.getString(R.string.the_code_is_already_linked_to_an_item), ERROR
                        )
                        return@getByCode
                    }

                    // Actualizamos si existe
                    ItemCodeCoroutines.countLink(item.itemId, tempCode) { ic ->
                        if (ic > 0) {
                            ItemCodeCoroutines.updateQty(item.itemId, tempCode, tempQty)
                        } else {
                            ItemCodeCoroutines.add(
                                ItemCode(
                                    itemId = item.itemId,
                                    code = tempCode,
                                    qty = tempQty,
                                    toUpload = 1
                                )
                            )
                        }

                        showSnackBar(
                            String.format(
                                context.getString(R.string.item_linked_to_code),
                                item.itemId,
                                tempCode
                            ), SnackBarType.SUCCESS
                        )

                        runOnUiThread { adapter?.selectItem(item) }
                        setSendButtonText()
                    }
                }
            }
        }
    }

    private fun unlink() {
        if (adapter?.currentItem() != null) {
            val item = adapter?.currentItem()
            if (item != null) {
                val tempCode = binding.codeEditText.text.toString()
                if (tempCode.isEmpty()) {
                    showSnackBar(
                        context.getString(R.string.you_must_select_a_code_to_link), ERROR
                    )
                    return
                }

                ItemCodeCoroutines.unlinkCode(item.itemId, tempCode) {
                    showSnackBar(
                        String.format(getString(R.string.item_unlinked_from_codes), item.itemId),
                        SnackBarType.SUCCESS
                    )

                    runOnUiThread { adapter?.selectItem(item) }
                    setSendButtonText()
                }
            }
        }
    }

    private val requiredLayout: Int
        get() {
            val orientation = resources.configuration.orientation
            val r = if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (panelIsExpanded) layoutBothPanelsExpanded
                else layoutBottomPanelCollapsed
            } else {
                layoutBothPanelsExpanded
            }

            if (BuildConfig.DEBUG) {
                when (r) {
                    layoutBothPanelsExpanded -> println("SELECTED LAYOUT: Both Panels Expanded")
                    layoutBottomPanelCollapsed -> println("SELECTED LAYOUT: Bottom Panel Collapsed")
                }
            }

            return r
        }

    private val layoutBothPanelsExpanded: Int
        get() {
            return R.layout.link_code_activity
        }

    private val layoutBottomPanelCollapsed: Int
        get() {
            return R.layout.link_code_activity_bottom_panel_collapsed
        }

    private fun setPanels() {
        runOnUiThread {
            val currentLayout = ConstraintSet()
            currentLayout.load(this, requiredLayout)
            currentLayout.applyTo(binding.root)

            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (panelIsExpanded) binding.expandButton?.text = context.getString(R.string.collapse_panel)
                else binding.expandButton?.text = context.getString(R.string.search_options)
            }
        }
    }

    private fun addAnimationOperations() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) return

        binding.expandButton!!.setOnClickListener {
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

            if (panelIsExpanded) binding.expandButton?.text = context.getString(R.string.collapse_panel)
            else binding.expandButton?.text = context.getString(R.string.search_options)
        }
    }

    private fun refreshTextViews() {
        if (!panelIsExpanded) return

        runOnUiThread {
            filterFragment.refreshViews()
            binding.moreCodesEditText.setText(binding.moreCodesEditText.text.toString())
            binding.qtyEditText.setText(
                binding.qtyEditText.text.toString(),
                TextView.BufferType.EDITABLE
            )
        }
    }

    private fun fillAdapter(t: ArrayList<Item>) {
        showProgressBar(true)

        completeList = t

        runOnUiThread {
            try {
                if (adapter != null) {
                    // Si el adapter es NULL es porque aún no fue creado.
                    // Por lo tanto, puede ser que los valores de [lastSelected]
                    // sean valores guardados de la instancia anterior y queremos preservarlos.
                    lastSelected = adapter?.currentItem()
                }

                adapter = ItemRecyclerAdapter.Builder()
                    .recyclerView(binding.recyclerView)
                    .fullList(completeList)
                    .checkedIdArray(checkedIdArray)
                    .multiSelect(multiSelect)
                    .showCheckBoxes(`val` = showCheckBoxes, listener = { showCheckBoxes = it })
                    .showImages(`val` = showImages, listener = { showImages = it })
                    .filterOptions(FilterOptions(searchedText))
                    .checkedChangedListener(this)
                    .dataSetChangedListener(this)
                    .selectedItemChangedListener(this)
                    .addPhotoRequiredListener(this)
                    .albumViewRequiredListener(this)
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

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshItem.isRefreshing = show
        }, 20)
    }

    private val menuItemManualCode = 999001
    private val menuItemRandomEan = 999002
    private val menuItemRandomIt = 999003
    private val menuItemRandomItUrl = 999004
    private val menuItemRandomOnListL = 999005

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_read_activity, menu)

        if (!settingsVm.useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        // Opción de visibilidad de Imágenes
        if (settingsVm.useImageControl) {
            menu.add(Menu.NONE, menuItemShowImages, Menu.NONE, context.getString(R.string.show_images))
                .setChecked(showImages).isCheckable = true
            val item = menu.findItem(menuItemShowImages)
            if (showImages)
                item.icon = ContextCompat.getDrawable(context, R.drawable.ic_photo_library)
            else
                item.icon = ContextCompat.getDrawable(context, R.drawable.ic_hide_image)
            item.icon?.mutate()?.colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    getColor(R.color.dimgray), BlendModeCompat.SRC_IN
                )
        }

        if (BuildConfig.DEBUG || Statics.TEST_MODE) {
            menu.add(Menu.NONE, menuItemManualCode, Menu.NONE, "Manual code")
            menu.add(Menu.NONE, menuItemRandomEan, Menu.NONE, "Random EAN")
            menu.add(Menu.NONE, menuItemRandomIt, Menu.NONE, "Random item ID")
            menu.add(Menu.NONE, menuItemRandomItUrl, Menu.NONE, "Random item ID URL")
            menu.add(Menu.NONE, menuItemRandomOnListL, Menu.NONE, "Random item on list")
        }

        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_visibility)
        binding.topAppbar.overflowIcon = drawable

        // Opciones de visibilidad del menú
        val allControls = SettingsRepository.getAllLinkCodeVisibleControls()
        allControls.forEach { p ->
            menu.add(0, p.key.hashCode(), menu.size(), p.description).setChecked(
                filterFragment.getVisibleFilters().contains(p)
            ).isCheckable = true
        }

        for (y in allControls) {
            var tempIndex = 0
            for (i in filterFragment.getVisibleFilters()) {
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
        val id = item.itemId
        when (item.itemId) {
            R.id.home, android.R.id.home -> {
                finish()
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
                (adapter?.fullList ?: ArrayList()).mapTo(codes) { it.ean }
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
                    if (it.any()) scannerCompleted("${GetResultFromCode.PREFIX_ITEM}${it[Random().nextInt(it.count())]}#")
                }
                return super.onOptionsItemSelected(item)
            }

            menuItemRandomItUrl -> {
                ItemCoroutines.getIds(true) {
                    if (it.any()) scannerCompleted("${GetResultFromCode.PREFIX_ITEM_URL}${it[Random().nextInt(it.count())]}")
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
            menuItemShowImages -> {
                adapter?.showImages(item.isChecked)
                if (item.isChecked)
                    item.icon = ContextCompat.getDrawable(context, R.drawable.ic_photo_library)
                else
                    item.icon = ContextCompat.getDrawable(context, R.drawable.ic_hide_image)
                item.icon?.mutate()?.colorFilter =
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                        getColor(R.color.dimgray), BlendModeCompat.SRC_IN
                    )
            }

            settingsRepository.linkCodeSearchByItemEan.key.hashCode() -> {
                filterFragment.setEanVisibility(if (item.isChecked) VISIBLE else GONE)
                sv.linkCodeSearchByItemEan = item.isChecked
            }

            settingsRepository.linkCodeSearchByCategory.key.hashCode() -> {
                filterFragment.setCategoryVisibility(if (item.isChecked) VISIBLE else GONE)
                sv.linkCodeSearchByCategory = item.isChecked
            }

            settingsRepository.linkCodeSearchByItemDescription.key.hashCode() -> {
                filterFragment.setDescriptionVisibility(if (item.isChecked) VISIBLE else GONE)
                sv.linkCodeSearchByItemDescription = item.isChecked
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
                itemCategoryId = itemCategory?.itemCategoryId
            ) {
                fillAdapter(it)
            }
        } catch (ex: java.lang.Exception) {
            ErrorLog.writeLog(this, tag, ex.message.toString())
            showProgressBar(false)
        }
    }
    // endregion

    override fun onResume() {
        super.onResume()
        rejectNewInstances = false
    }

    override fun onStart() {
        super.onStart()
        rejectNewInstances = false

        setSendButtonText()
        setPanels()

        if (fillRequired) {
            fillRequired = false
            fillAdapter(completeList)
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
            panelIsExpanded = false
            setPanels()
        }
    }

    private fun setInputPanelVisibility(visibility: Int) {
        runOnUiThread {
            binding.inputPanel.visibility = visibility
        }
    }

    private fun setSearchAndSummaryPanelVisibility(visibility: Int) {
        runOnUiThread {
            binding.summaryFragment.visibility = visibility
            binding.searchTextFragment.visibility = visibility
        }
    }

    private fun setToolTipVisibility(visibility: Int) {
        runOnUiThread {
            binding.tooltipTextView.visibility = visibility
        }
    }

    override fun onSearchTextChanged(searchText: String) {
        searchedText = searchText
        runOnUiThread {
            adapter?.refreshFilter(FilterOptions(searchedText))
        }
    }

    /**
     * Devuelve una cadena de texto formateada que se ajusta a los parámetros.
     * Devuelve una cadena vacía en caso de Exception.
     * Devuelve null si no es necesario cambiar la cadena ingresada porque ya se ajusta a los parámetros
     *          o porque es igual que la cadena original.
     */
    private fun getValidValue(
        source: String,
        maxIntegerPlaces: Int = 7,
        maxDecimalPlaces: Int = 0,
        maxValue: Double,
        decimalSeparator: Char,
    ): CharSequence? {
        if (source.isEmpty()) {
            return null
        } else {
            // Regex para eliminar caracteres no permitidos.
            var validText = source.replace("[^0-9?!\\" + decimalSeparator + "]".toRegex(), "")

            // Probamos convertir el valor, si no se puede
            // se devuelve una cadena vacía
            val numericValue: Double
            try {
                numericValue = java.lang.Double.parseDouble(validText)
            } catch (e: NumberFormatException) {
                return ""
            }

            // Si el valor numérico es mayor al valor máximo reemplazar el
            // texto válido por el valor máximo
            validText = if (numericValue > maxValue) {
                maxValue.toString()
            } else {
                validText
            }

            // Obtener la parte entera y decimal del valor en forma de texto
            var decimalPart = ""
            val integerPart: String
            if (validText.contains(decimalSeparator)) {
                decimalPart =
                    validText.substring(validText.indexOf(decimalSeparator) + 1, validText.length)
                integerPart = validText.substring(0, validText.indexOf(decimalSeparator))
            } else {
                integerPart = validText
            }

            // Si la parte entera es más larga que el máximo de dígitos permitidos
            // retorna un carácter vacío.
            if (integerPart.length > maxIntegerPlaces) {
                return ""
            }

            // Si la cantidad de espacios decimales permitidos es cero devolver la parte entera
            // si no, concatenar la parte entera con el separador de decimales y
            // la cantidad permitida de decimales.
            val result = if (maxDecimalPlaces == 0) {
                integerPart
            } else integerPart + decimalSeparator + decimalPart.substring(
                0,
                if (decimalPart.length > maxDecimalPlaces) maxDecimalPlaces else decimalPart.length
            )

            // Devolver solo si son valores positivos diferentes a los de originales.
            // NULL si no hay que hacer cambios sobre el texto original.
            return if (result != source) {
                result
            } else null
        }
    }

    override fun onFilterChanged(
        externalId: String,
        description: String,
        ean: String,
        itemCategory: ItemCategory?,
        onlyActive: Boolean
    ) {
        filterItemCode = externalId
        filterItemDescription = description
        filterItemEan = ean
        filterItemCategory = itemCategory
        filterOnlyActive = onlyActive

        Handler(Looper.getMainLooper()).postDelayed({ getItems() }, 200)
    }

    private fun proceedByResult(it: GetResultFromCode.CodeResult, scannedCode: String) {
        val r: Any? = it.item
        if (r != null && r is ItemKtor) {
            val item = r.toRoom()
            val pos = adapter?.getIndexById(item.itemId) ?: NO_POSITION

            if (pos != NO_POSITION) {
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
            return
        }

        runOnUiThread {
            binding.codeEditText.setText(scannedCode, TextView.BufferType.EDITABLE)
            binding.codeEditText.dispatchKeyEvent(
                KeyEvent(
                    0,
                    0,
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_ENTER,
                    0
                )
            )
        }
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshItem.isRefreshing = false
        }, 100)
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
                    val item = adapter?.currentItem() ?: return@registerForActivityResult
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
                showSnackBar(getString(R.string.no_images), INFO)
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
            showSnackBar(getString(R.string.no_images), INFO)
            rejectNewInstances = false
            return
        }

        val anyAvailable = docContReqResObj.documentContentArray.any { it.available }

        if (!anyAvailable) {
            showSnackBar(getString(R.string.images_not_yet_processed), INFO)
            rejectNewInstances = false
            return
        }

        showPhotoAlbum()
    }
    //endregion ImageControl

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_MULTISELECT = "multiSelect"
    }
}
