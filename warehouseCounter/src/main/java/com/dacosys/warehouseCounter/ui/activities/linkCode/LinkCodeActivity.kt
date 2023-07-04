package com.dacosys.warehouseCounter.ui.activities.linkCode

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
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.*
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.adapter.item.ItemRecyclerAdapter
import com.dacosys.warehouseCounter.adapter.ptlOrder.PtlOrderAdapter.Companion.FilterOptions
import com.dacosys.warehouseCounter.databinding.LinkCodeActivityBottomPanelCollapsedBinding
import com.dacosys.warehouseCounter.misc.CounterHandler
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.decimalSeparator
import com.dacosys.warehouseCounter.misc.Statics.Companion.lineSeparator
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.retrofit.functions.SendItemCode
import com.dacosys.warehouseCounter.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.room.entity.item.Item
import com.dacosys.warehouseCounter.room.entity.itemCategory.ItemCategory
import com.dacosys.warehouseCounter.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.settings.SettingsRepository
import com.dacosys.warehouseCounter.ui.activities.item.CheckItemCode
import com.dacosys.warehouseCounter.ui.fragments.item.ItemSelectFilterFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.dacosys.warehouseCounter.ui.utils.Screen
import kotlinx.coroutines.*
import org.parceler.Parcels
import java.util.*
import kotlin.concurrent.thread

class LinkCodeActivity : AppCompatActivity(), Scanner.ScannerListener, Rfid.RfidDeviceListener,
    ItemRecyclerAdapter.SelectedItemChangedListener, ItemRecyclerAdapter.CheckedChangedListener,
    CounterHandler.CounterListener, ItemSelectFilterFragment.OnFilterChangedListener,
    SwipeRefreshLayout.OnRefreshListener, ItemRecyclerAdapter.DataSetChangedListener {
    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        adapter?.refreshListeners()
        adapter?.refreshImageControlListeners()
        itemSelectFilterFragment!!.onDestroy()
    }

    override fun onSelectedItemChanged(item: Item?) {
        if (item != null) {
            ItemCodeCoroutines().getByItemId(item.itemId) {
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

    private fun onTaskSendItemCodeEnded(snackBarEventData: SnackBarEventData) {
        val msg = snackBarEventData.text

        if (snackBarEventData.snackBarType == SnackBarType.SUCCESS) {
            makeText(binding.root, msg, SnackBarType.SUCCESS)
            setSendButtonText()
        } else if (snackBarEventData.snackBarType == ERROR) {
            makeText(binding.root, msg, ERROR)
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

        try {
            CheckItemCode(callback = { onCheckCodeEnded(it) },
                scannedCode = scanCode,
                list = adapter?.fullList ?: ArrayList(),
                onEventData = { showSnackBar(it) }).execute()
        } catch (ex: Exception) {
            ex.printStackTrace()
            makeText(binding.root, ex.message.toString(), ERROR)
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
        } finally {
            // Unless is blocked, unlock the partial
            JotterListener.lockScanner(this, false)
        }
    }

    private fun showSnackBar(it: SnackBarEventData) {
        makeText(binding.root, it.text, it.snackBarType)
    }

    override fun onIncrement(view: View?, number: Double) {
        binding.qtyEditText.setText(number.toString())
    }

    override fun onDecrement(view: View?, number: Double) {
        binding.qtyEditText.setText(number.toString())
    }

    override fun onCheckedChanged(isChecked: Boolean, pos: Int) {
        runOnUiThread {
            binding.selectedTextView.text = adapter?.countChecked().toString()
        }
        adapter?.selectItem(pos)
    }

    private var rejectNewInstances = false

    private var tempTitle = ""

    private var panelIsExpanded = false

    private var itemSelectFilterFragment: ItemSelectFilterFragment? = null

    private var linkCode: String = ""

    private var ch: CounterHandler? = null
    private var multiSelect = false

    private var adapter: ItemRecyclerAdapter? = null
    private var lastSelected: Item? = null
    private var firstVisiblePos: Int? = null
    private var currentScrollPosition: Int = 0

    // Se usa para saber si estamos en onStart luego de onCreate
    private var fillRequired = false

    private var searchText: String = ""

    // region Variables para LoadOnDemand
    private var completeList: ArrayList<Item> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()
    // endregion

    private val menuItemShowImages = 9999
    private var showImages
        get() = settingViewModel.linkCodeShowImages
        set(value) {
            settingViewModel.linkCodeShowImages = value
        }

    private var showCheckBoxes
        get() =
            if (!multiSelect) false
            else settingViewModel.linkCodeShowCheckBoxes
        set(value) {
            settingViewModel.linkCodeShowCheckBoxes = value
        }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        saveBundleValues(savedInstanceState)
    }

    private fun saveBundleValues(b: Bundle) {
        b.putString("title", title.toString())
        b.putBoolean("multiSelect", multiSelect)
        b.putString("linkCode", linkCode)
        b.putBoolean("panelIsExpanded", panelIsExpanded)

        if (adapter != null) {
            b.putParcelable("lastSelected", (adapter ?: return).currentItem())
            b.putInt("firstVisiblePos", (adapter ?: return).firstVisiblePos())
            b.putParcelableArrayList("completeList", adapter?.fullList)
            b.putLongArray("checkedIdArray", adapter?.getAllChecked()?.map { it.itemId }?.toLongArray())
            b.putInt("currentScrollPosition", currentScrollPosition)
        }

        b.putString("searchText", searchText)
    }

    private fun loadBundleValues(b: Bundle) {
        tempTitle = b.getString("title") ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.link_code)

        multiSelect = b.getBoolean("multiSelect", multiSelect)
        panelIsExpanded = b.getBoolean("panelIsExpanded")

        linkCode = b.getString("linkCode") ?: ""

        // Adapter
        checkedIdArray = (b.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(ArrayList())
        completeList = b.getParcelableArrayList("completeList") ?: ArrayList()
        lastSelected = b.getParcelable("lastSelected")
        firstVisiblePos = if (b.containsKey("firstVisiblePos")) b.getInt("firstVisiblePos") else -1
        currentScrollPosition = b.getInt("currentScrollPosition")

        searchText = b.getString("searchText") ?: ""
    }

    private fun loadExtrasBundleValues(b: Bundle) {
        tempTitle = b.getString("title") ?: ""
        if (tempTitle.isEmpty()) tempTitle = context.getString(R.string.link_code)

        itemSelectFilterFragment?.itemCode = b.getString("itemCode") ?: ""
        itemSelectFilterFragment?.itemCategory =
            Parcels.unwrap<ItemCategory>(b.getParcelable("itemCategory"))
        multiSelect = b.getBoolean("multiSelect", false)
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
        itemSelectFilterFragment =
            supportFragmentManager.findFragmentById(R.id.filterFragment) as ItemSelectFilterFragment

        if (savedInstanceState != null) {
            loadBundleValues(savedInstanceState)
        } else {
            // Inicializar la actividad
            val extras = intent.extras
            if (extras != null) loadExtrasBundleValues(extras)
        }

        title = tempTitle

        itemSelectFilterFragment!!.setListener(this)

        // Esta clase controla el comportamiento de los botones (+) y (-)
        ch = CounterHandler.Builder().incrementalView(binding.moreButton)
            .decrementalView(binding.lessButton).minRange(1.0) // cant go any less than -50
            .maxRange(100.0) // cant go any further than 50
            .isCycle(true) // 49,50,-50,-49 and so on
            .counterDelay(50) // speed of counter
            .startNumber(1.0).counterStep(1L)  // steps e.g. 0,2,4,6...
            .listener(this) // to listen counter results and show them in app
            .build()

        binding.qtyEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                // Filtro que devuelve un texto válido
                val validStr = getValidValue(
                    source = s.toString(),
                    maxValue = 100.toDouble(),
                    decimalSeparator = decimalSeparator
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
        binding.qtyEditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        binding.linkButton.performClick()
                    }
                }
            }
            false
        }
        // Cambia el modo del teclado en pantalla a tipo numérico
        // cuando este control lo necesita.
        binding.qtyEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER)

        binding.codeEditText.setText(linkCode, TextView.BufferType.EDITABLE)

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
                adapter?.refreshFilter(FilterOptions(searchText, true))
            }
        })
        binding.searchEditText.setText(searchText, TextView.BufferType.EDITABLE)
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

        setSendButtonText()
        setPanels()

        // Ocultar panel de códigos vinculados al ítem seleccionado
        binding.moreCodesConstraintLayout.visibility = GONE

        Screen.setupUI(binding.linkCode, this)
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
    private var changePanelStateAtFinish: Boolean = false

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
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT && !binding.codeEditText.isFocused -> {
                    collapsePanel()
                }

                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT && binding.codeEditText.isFocused -> {
                    collapsePanel()
                }

                resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT && !binding.codeEditText.isFocused -> {
                    collapsePanel()
                }
            }
        }

        // Si estamos esperando que termine la animación para ejecutar un cambio de vista
        if (changePanelStateAtFinish) {
            changePanelStateAtFinish = false
            binding.expandButton?.performClick()
        }
    }

    private fun collapsePanel() {
        if (panelIsExpanded) {
            runOnUiThread {
                binding.expandButton?.performClick()
            }
        }
    }
    // endregion

    private fun setSendButtonText() {
        ItemCodeCoroutines().getToUpload {
            runOnUiThread {
                binding.sendButton.text = String.format(
                    "%s%s(%s)",
                    context.getString(R.string.send),
                    lineSeparator,
                    it.count()
                )
            }
        }
    }

    private fun sendDialog() {
        ItemCodeCoroutines().getToUpload {
            if (it.isNotEmpty()) {
                sendItemCodes(it)
            } else {
                makeText(
                    binding.root,
                    context.getString(R.string.there_are_no_item_codes_to_send),
                    INFO
                )
            }
        }
    }

    private fun sendItemCodes(it: ArrayList<ItemCode>) {
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
                    thread {
                        SendItemCode(it) { it2 -> onTaskSendItemCodeEnded(it2) }.execute()
                    }
                } catch (ex: Exception) {
                    ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())
                }
            }
            alert.show()
        }
    }

    private fun link() {
        if (adapter?.currentItem() != null) {
            val item = adapter?.currentItem()
            if (item != null) {
                val tempCode = binding.codeEditText.text.toString()
                if (tempCode.isEmpty()) {
                    makeText(
                        binding.root,
                        context.getString(R.string.you_must_select_a_code_to_link),
                        ERROR
                    )
                    return
                }

                val tempStrQty = binding.qtyEditText.text.toString()
                if (tempStrQty.isEmpty()) {
                    makeText(
                        binding.root,
                        context.getString(R.string.you_must_select_an_amount_to_link),
                        ERROR
                    )
                    return
                }

                val tempQty: Double?
                try {
                    tempQty = java.lang.Double.parseDouble(tempStrQty)
                } catch (e: NumberFormatException) {
                    makeText(binding.root, context.getString(R.string.invalid_amount), ERROR)
                    return
                }

                if (tempQty <= 0) {
                    makeText(
                        binding.root,
                        context.getString(R.string.you_must_select_a_positive_amount_greater_than_zero),
                        ERROR
                    )
                    return
                }

                // Chequear si ya está vinculado
                ItemCodeCoroutines().getByCode(tempCode) {
                    if (it.size > 0) {
                        makeText(
                            binding.root,
                            context.getString(R.string.the_code_is_already_linked_to_an_item),
                            ERROR
                        )
                        return@getByCode
                    }

                    // Actualizamos si existe
                    ItemCodeCoroutines().countLink(item.itemId, tempCode) { ic ->
                        if (ic > 0) {
                            ItemCodeCoroutines().updateQty(item.itemId, tempCode, tempQty)
                        } else {
                            ItemCodeCoroutines().add(
                                ItemCode(
                                    itemId = item.itemId,
                                    code = tempCode,
                                    qty = tempQty,
                                    toUpload = 1
                                )
                            )
                        }

                        makeText(
                            binding.root, String.format(
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
                    makeText(
                        binding.root,
                        context.getString(R.string.you_must_select_a_code_to_link),
                        ERROR
                    )
                    return
                }

                ItemCodeCoroutines().unlinkCode(item.itemId, tempCode) {
                    makeText(
                        binding.root,
                        String.format(getString(R.string.item_unlinked_from_codes), item.itemId),
                        SnackBarType.SUCCESS
                    )

                    runOnUiThread { adapter?.selectItem(item) }
                    setSendButtonText()
                }
            }
        }
    }

    private fun setPanels() {
        val currentLayout = ConstraintSet()
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (panelIsExpanded) currentLayout.load(this, R.layout.link_code_activity)
            else currentLayout.load(this, R.layout.link_code_activity_bottom_panel_collapsed)
        } else {
            currentLayout.load(this, R.layout.link_code_activity)
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

        TransitionManager.beginDelayedTransition(binding.linkCode, transition)
        currentLayout.applyTo(binding.linkCode)

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (panelIsExpanded) binding.expandButton?.text = context.getString(R.string.collapse_panel)
            else binding.expandButton?.text = context.getString(R.string.search_options)
        }
    }

    private fun addAnimationOperations() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            return
        }

        binding.expandButton!!.setOnClickListener {
            val bottomVisible = panelIsExpanded
            val imeVisible = isKeyboardVisible

            if (!bottomVisible && imeVisible) {
                // Esperar que se cierre el teclado luego de perder el foco el TextView para expandir el panel
                changePanelStateAtFinish = true
                return@setOnClickListener
            }

            val nextLayout = ConstraintSet()
            if (panelIsExpanded) nextLayout.load(this, R.layout.link_code_activity_bottom_panel_collapsed)
            else nextLayout.load(this, R.layout.link_code_activity)

            panelIsExpanded = !panelIsExpanded
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

            TransitionManager.beginDelayedTransition(binding.linkCode, transition)
            nextLayout.applyTo(binding.linkCode)

            if (panelIsExpanded) binding.expandButton?.text = context.getString(R.string.collapse_panel)
            else binding.expandButton?.text = context.getString(R.string.search_options)
        }
    }

    private fun refreshTextViews() {
        if (!panelIsExpanded) return

        runOnUiThread {
            itemSelectFilterFragment!!.refreshTextViews()
            binding.moreCodesEditText.setText(binding.moreCodesEditText.text.toString())
            binding.qtyEditText.setText(
                binding.qtyEditText.text.toString(),
                TextView.BufferType.EDITABLE
            )
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
                if (adapter != null) {
                    // Si el adapter es NULL es porque aún no fue creado.
                    // Por lo tanto, puede ser que los valores de [lastSelected]
                    // sean valores guardados de la instancia anterior y queremos preservarlos.
                    lastSelected = adapter?.currentItem()
                }

                adapter = ItemRecyclerAdapter(
                    recyclerView = binding.recyclerView,
                    fullList = completeList,
                    checkedIdArray = checkedIdArray,
                    multiSelect = multiSelect,
                    showCheckBoxes = showCheckBoxes,
                    showCheckBoxesChanged = { showCheckBoxes = it },
                    showImages = showImages,
                    showImagesChanged = { showImages = it },
                    filterOptions = FilterOptions(searchText, true)
                )

                refreshAdapterListeners()

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

    private fun refreshAdapterListeners() {
        adapter?.refreshListeners(
            checkedChangedListener = this,
            dataSetChangedListener = this,
            editItemRequiredListener = null,
            selectedItemChangedListener = this
        )
        adapter?.refreshImageControlListeners(null, null)
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshItem.isRefreshing = show
            }
        }, 20)
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

        // Opción de visibilidad de Imágenes
        if (settingViewModel.useImageControl) {
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

        if (BuildConfig.DEBUG || Statics.testMode) {
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
        val allControls = SettingsRepository.getAllSelectItemVisibleControls()
        allControls.forEach { p ->
            menu.add(0, p.key.hashCode(), menu.size(), p.description).setChecked(
                itemSelectFilterFragment!!.getVisibleFilters().contains(p)
            ).isCheckable = true
        }

        for (y in allControls) {
            var tempIndex = 0
            for (i in itemSelectFilterFragment!!.getVisibleFilters()) {
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
                (adapter?.fullList ?: ArrayList()).mapTo(codes) { it.ean }
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

        val sp = settingRepository
        item.isChecked = !item.isChecked
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

            sp.selectItemSearchByItemEan.key.hashCode() -> {
                itemSelectFilterFragment!!.setEanDescriptionVisibility(if (item.isChecked) VISIBLE else GONE)
            }

            sp.selectItemSearchByItemCategory.key.hashCode() -> {
                itemSelectFilterFragment!!.setCategoryVisibility(if (item.isChecked) VISIBLE else GONE)
            }

            else -> return super.onOptionsItemSelected(item)
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
    // endregion

    override fun onResume() {
        super.onResume()
        rejectNewInstances = false
    }

    override fun onStart() {
        super.onStart()
        rejectNewInstances = false

        JotterListener.resumeReaderDevices(this)

        if (fillRequired) {
            fillRequired = false
            fillAdapter(completeList)
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
        code: String,
        itemCategory: ItemCategory?,
        onlyActive: Boolean,
    ) {
        Screen.closeKeyboard(this)
        thread {
            checkedIdArray.clear()
            getItems()
        }
    }

    private fun onCheckCodeEnded(it: CheckItemCode.CheckCodeEnded) {
        val item: Item? = it.item
        val scannedCode: String = it.scannedCode

        if (item != null) {
            if (adapter?.getIndexById(item.itemId) != NO_POSITION) {
                adapter?.selectItem(item)
            } else {
                itemSelectFilterFragment!!.itemCode = item.ean
                thread {
                    completeList = arrayListOf(item)
                    checkedIdArray.clear()
                    fillAdapter(completeList)
                }
            }
        } else {
            runOnUiThread {
                binding.codeEditText.setText(scannedCode, TextView.BufferType.EDITABLE)
                binding.codeEditText.dispatchKeyEvent(
                    KeyEvent(
                        0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0
                    )
                )
            }
        }
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshItem.isRefreshing = false
            }
        }, 100)
    }

    override fun onDataSetChanged() {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                fillSummaryRow()
            }
        }, 100)
    }

    private fun fillSummaryRow() {
        Log.d(this::class.java.simpleName, "fillSummaryRow")
        runOnUiThread {
            if (multiSelect) {
                binding.totalLabelTextView.text = context.getString(R.string.total)
                binding.qtyReqLabelTextView.text = context.getString(R.string.cant)
                binding.selectedLabelTextView.text = context.getString(R.string.checked)

                if (adapter != null) {
                    binding.totalTextView.text = adapter?.totalVisible().toString()
                    binding.qtyReqTextView.text = "0"
                    binding.selectedTextView.text = adapter?.countChecked().toString()
                }
            } else {
                binding.totalLabelTextView.text = context.getString(R.string.total)
                binding.qtyReqLabelTextView.text = context.getString(R.string.cont_)
                binding.selectedLabelTextView.text = context.getString(R.string.items)

                if (adapter != null) {
                    val cont = 0
                    val t = adapter?.totalVisible() ?: 0
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
}


