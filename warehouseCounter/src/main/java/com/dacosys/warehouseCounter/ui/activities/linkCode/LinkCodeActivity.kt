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
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.adapter.item.ItemAdapter
import com.dacosys.warehouseCounter.dataBase.item.ItemDbHelper
import com.dacosys.warehouseCounter.dataBase.itemCode.ItemCodeDbHelper
import com.dacosys.warehouseCounter.databinding.LinkCodeActivityBottomPanelCollapsedBinding
import com.dacosys.warehouseCounter.misc.CounterHandler
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.decimalSeparator
import com.dacosys.warehouseCounter.misc.Statics.Companion.showKeyboard
import com.dacosys.warehouseCounter.model.errorLog.ErrorLog
import com.dacosys.warehouseCounter.model.item.Item
import com.dacosys.warehouseCounter.model.itemCategory.ItemCategory
import com.dacosys.warehouseCounter.model.itemCode.ItemCode
import com.dacosys.warehouseCounter.retrofit.functionOld.SendItemCode
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.settings.SettingsRepository
import com.dacosys.warehouseCounter.sync.ProgressStatus
import com.dacosys.warehouseCounter.ui.activities.item.CheckItemCode
import com.dacosys.warehouseCounter.ui.fragments.item.ItemSelectFilterFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import org.parceler.Parcels
import java.util.*
import kotlin.concurrent.thread

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class LinkCodeActivity : AppCompatActivity(), Scanner.ScannerListener, Rfid.RfidDeviceListener,
    ItemAdapter.SelectedItemChangedListener, ItemAdapter.CheckedChangedListener,
    CounterHandler.CounterListener, ItemSelectFilterFragment.OnFilterChangedListener,
    SendItemCode.TaskSendItemCodeEnded, CheckItemCode.CheckCodeEnded,
    SwipeRefreshLayout.OnRefreshListener, ItemAdapter.DataSetChangedListener {
    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        arrayAdapter?.refreshListeners(null, null, null)
        itemSelectFilterFragment!!.onDestroy()
    }

    override fun onSelectedItemChanged(item: Item?, pos: Int) {
        if (item != null) {
            val arrayList = ItemCodeDbHelper().selectByItemId(item.itemId)
            var allItemCodes = ""
            val breakLine = Statics.lineSeparator

            if (arrayList.isNotEmpty()) {
                runOnUiThread {
                    binding.moreCodesConstraintLayout.visibility = VISIBLE
                }

                for (i in arrayList) {
                    allItemCodes = String.format(
                        "%s%s (%s)%s",
                        allItemCodes,
                        i.code,
                        i.qty.toString(),
                        breakLine
                    )
                }
            } else {
                runOnUiThread {
                    binding.moreCodesConstraintLayout.visibility = GONE
                }
            }

            runOnUiThread {
                binding.moreCodesEditText.setText(allItemCodes.trim(), TextView.BufferType.EDITABLE)
            }
        } else {
            runOnUiThread {
                binding.moreCodesConstraintLayout.visibility = GONE
            }
        }
    }

    override fun onTaskSendItemCodeEnded(status: ProgressStatus, msg: String) {
        if (status == ProgressStatus.finished) {
            makeText(binding.root, msg, SnackBarType.SUCCESS)
            setSendButtonText()
        } else if (status == ProgressStatus.crashed) {
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
            this,
            requestCode,
            permissions,
            grantResults
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

        try {
            try {
                val checkCodeTask = CheckItemCode()
                checkCodeTask.addParams(callback = this,
                    scannedCode = scanCode,
                    adapter = arrayAdapter!!,
                    onEventData = { showSnackBar(it) })
                checkCodeTask.execute()
            } catch (ex: Exception) {
                makeText(binding.root, ex.message.toString(), ERROR)
                Log.e(this::class.java.simpleName, ex.message.toString())
            } finally {
                JotterListener.lockScanner(this, false)
            }
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
            binding.selectedTextView.text = arrayAdapter?.countChecked().toString()
        }
        arrayAdapter?.selectItem(pos)
    }

    private var rejectNewInstances = false

    private var tempTitle = ""

    private var panelIsExpanded = false

    private var itemSelectFilterFragment: ItemSelectFilterFragment? = null

    private var linkCode: String = ""

    private var ch: CounterHandler? = null
    private var multiSelect = false

    private var arrayAdapter: ItemAdapter? = null
    private var lastSelected: Item? = null
    private var firstVisiblePos: Int? = null

    private var searchText: String = ""

    // region Variables para LoadOnDemand
    private var completeList: ArrayList<Item> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()
    // endregion

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

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        saveBundleValues(savedInstanceState)
    }

    private fun saveBundleValues(b: Bundle) {
        b.putString("title", title.toString())
        b.putBoolean("multiSelect", multiSelect)
        b.putString("linkCode", linkCode)
        b.putBoolean("panelIsExpanded", panelIsExpanded)

        if (arrayAdapter != null) {
            b.putParcelable("lastSelected", (arrayAdapter ?: return).currentItem())
            b.putInt("firstVisiblePos", (arrayAdapter ?: return).firstVisiblePos())
            b.putParcelableArrayList("completeList", arrayAdapter?.getAll())
            b.putLongArray("checkedIdArray", arrayAdapter?.getAllChecked()?.toLongArray())
        }

        b.putString("searchText", searchText)
    }

    private fun loadBundleValues(b: Bundle) {
        tempTitle = b.getString("title") ?: ""
        if (tempTitle.isEmpty()) tempTitle = context().getString(R.string.link_code)

        multiSelect = b.getBoolean("multiSelect", multiSelect)
        panelIsExpanded = b.getBoolean("panelIsExpanded")

        linkCode = b.getString("linkCode") ?: ""

        // Adapter
        checkedIdArray =
            (b.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(ArrayList())
        completeList = b.getParcelableArrayList<Item>("completeList") as ArrayList<Item>
        lastSelected = b.getParcelable("lastSelected")
        firstVisiblePos = if (b.containsKey("firstVisiblePos")) b.getInt("firstVisiblePos") else -1

        searchText = b.getString("searchText") ?: ""
    }

    private fun loadExtrasBundleValues(b: Bundle) {
        tempTitle = b.getString("title") ?: ""
        if (tempTitle.isEmpty()) tempTitle = context().getString(R.string.link_code)

        itemSelectFilterFragment?.itemCode = b.getString("itemCode") ?: ""
        itemSelectFilterFragment?.itemCategory =
            Parcels.unwrap<ItemCategory>(b.getParcelable("itemCategory"))
        multiSelect = b.getBoolean("multiSelect", false)
    }

    private lateinit var binding: LinkCodeActivityBottomPanelCollapsedBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Statics.setScreenRotation(this)
        binding = LinkCodeActivityBottomPanelCollapsedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
                val validStr = getValidValue(s.toString(), 7, 0, 100.toDouble(), decimalSeparator)

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
                showKeyboard(this)
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

        setupUI(binding.linkCode)
    }

    private fun setSendButtonText() {
        val itemCodeArray = ItemCodeDbHelper().selectToUpload()
        runOnUiThread {
            binding.sendButton.text = String.format(
                "%s%s(%s)",
                context().getString(R.string.send),
                System.getProperty("line.separator"),
                itemCodeArray.count()
            )
        }
    }

    private fun sendDialog() {
        val itemCodeArray = ItemCodeDbHelper().selectToUpload()
        if (itemCodeArray.isNotEmpty()) {
            val alert = AlertDialog.Builder(this)
            alert.setTitle(getString(R.string.send_item_codes))
            alert.setMessage(
                if (itemCodeArray.count() > 1) {
                    context().getString(R.string.do_you_want_to_send_the_item_codes)
                } else {
                    context().getString(R.string.do_you_want_to_send_the_item_code)
                }
            )
            alert.setNegativeButton(R.string.cancel, null)
            alert.setPositiveButton(R.string.ok) { _, _ ->
                try {
                    thread {
                        val taskItemCode = SendItemCode()
                        taskItemCode.addParams(listener = this, itemCodeArray = itemCodeArray)
                        taskItemCode.execute()
                    }
                } catch (ex: Exception) {
                    ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())
                }
            }

            alert.show()
        } else {
            makeText(
                binding.root,
                context().getString(R.string.there_are_no_item_codes_to_send),
                INFO
            )
        }
    }

    private fun link() {
        if (arrayAdapter?.currentItem() != null) {
            val item = arrayAdapter?.currentItem()
            if (item != null) {
                val tempCode = binding.codeEditText.text.toString()
                if (tempCode.isEmpty()) {
                    makeText(
                        binding.root,
                        context().getString(R.string.you_must_select_a_code_to_link),
                        ERROR
                    )
                    return
                }

                val tempStrQty = binding.qtyEditText.text.toString()
                if (tempStrQty.isEmpty()) {
                    makeText(
                        binding.root,
                        context().getString(R.string.you_must_select_an_amount_to_link),
                        ERROR
                    )
                    return
                }

                val tempQty: Double?
                try {
                    tempQty = java.lang.Double.parseDouble(tempStrQty)
                } catch (e: NumberFormatException) {
                    makeText(binding.root, context().getString(R.string.invalid_amount), ERROR)
                    return
                }

                if (tempQty <= 0) {
                    makeText(
                        binding.root,
                        context().getString(R.string.you_must_select_a_positive_amount_greater_than_zero),
                        ERROR
                    )
                    return
                }

                val x = ItemCodeDbHelper()

                // Chequear si ya está vinculado
                val tempItemCode = x.selectByCode(tempCode)
                if (tempItemCode.size > 0) {
                    makeText(
                        binding.root,
                        context().getString(R.string.the_code_is_already_linked_to_an_item),
                        ERROR
                    )
                    return
                }

                // Actualizamos si existe
                if (x.linkExists(item.itemId, tempCode)) {
                    x.updateQty(item.itemId, tempCode, tempQty)
                } else {
                    x.insert(item.itemId, tempCode, tempQty, true)
                }

                makeText(
                    binding.root,
                    String.format(
                        context().getString(R.string.item_linked_to_code),
                        item.itemId,
                        tempCode
                    ),
                    SnackBarType.SUCCESS
                )

                runOnUiThread { arrayAdapter?.forceSelectItem(item) }

                setSendButtonText()
            }
        }
    }

    private fun unlink() {
        if (arrayAdapter?.currentItem() != null) {
            val item = arrayAdapter?.currentItem()
            if (item != null) {
                val tempCode = binding.codeEditText.text.toString()
                if (tempCode.isEmpty()) {
                    makeText(
                        binding.root,
                        context().getString(R.string.you_must_select_a_code_to_link),
                        ERROR
                    )
                    return
                }

                val x = ItemCodeDbHelper()
                if (x.unlinkCode(item.itemId, tempCode)) {
                    makeText(
                        binding.root,
                        String.format(getString(R.string.item_unlinked_from_codes), item.itemId),
                        SnackBarType.SUCCESS
                    )

                    runOnUiThread { arrayAdapter?.forceSelectItem(item) }

                    setSendButtonText()
                } else {
                    makeText(
                        binding.root,
                        context().getString(R.string.the_code_could_not_be_unlinked),
                        ERROR
                    )
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
                if (panelIsExpanded) {
                    runOnUiThread {
                        itemSelectFilterFragment!!.refreshTextViews()
                    }
                }
            }

            override fun onTransitionCancel(transition: Transition?) {}
        })

        TransitionManager.beginDelayedTransition(binding.linkCode, transition)
        currentLayout.applyTo(binding.linkCode)

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            when {
                panelIsExpanded -> binding.expandButton?.text =
                    context().getString(R.string.collapse_panel)
                else -> binding.expandButton?.text = context().getString(R.string.search_options)
            }
        }
    }

    private fun addAnimationOperations() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            return
        }

        binding.expandButton!!.setOnClickListener {
            val nextLayout = ConstraintSet()
            if (panelIsExpanded) nextLayout.load(
                this,
                R.layout.link_code_activity_bottom_panel_collapsed
            )
            else nextLayout.load(this, R.layout.link_code_activity)

            panelIsExpanded = !panelIsExpanded
            val transition = ChangeBounds()
            transition.interpolator = FastOutSlowInInterpolator()
            transition.addListener(object : Transition.TransitionListener {
                override fun onTransitionResume(transition: Transition?) {}
                override fun onTransitionPause(transition: Transition?) {}
                override fun onTransitionStart(transition: Transition?) {}
                override fun onTransitionEnd(transition: Transition?) {
                    if (panelIsExpanded) {
                        runOnUiThread {
                            itemSelectFilterFragment!!.refreshTextViews()
                            binding.moreCodesEditText.setText(binding.moreCodesEditText.text.toString())
                            binding.qtyEditText.setText(
                                binding.qtyEditText.text.toString(),
                                TextView.BufferType.EDITABLE
                            )
                        }
                    }
                }

                override fun onTransitionCancel(transition: Transition?) {}
            })

            TransitionManager.beginDelayedTransition(binding.linkCode, transition)
            nextLayout.applyTo(binding.linkCode)

            when {
                panelIsExpanded -> binding.expandButton?.text =
                    context().getString(R.string.collapse_panel)
                else -> binding.expandButton?.text = context().getString(R.string.search_options)
            }
        }
    }

    private fun fillAdapter(t: ArrayList<Item>) {
        showProgressBar(true)

        var temp = t
        if (!t.any()) {
            checkedIdArray.clear()
            temp = getItems()
        }
        completeList = temp

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
                    selectedItemChangedListener = this
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

        if (!settingViewModel().useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        if (BuildConfig.DEBUG || Statics.testMode) {
            menu.add(Menu.NONE, menuItemManualCode, Menu.NONE, "Manual code")
            menu.add(Menu.NONE, menuItemRandomIt, Menu.NONE, "Random item")
            menu.add(Menu.NONE, menuItemRandomOnListL, Menu.NONE, "Random item on list")
        }

        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        val drawable = ContextCompat.getDrawable(context(), R.drawable.ic_visibility)
        val toolbar = findViewById<Toolbar>(R.id.action_bar)
        toolbar.overflowIcon = drawable

        // Opciones de visibilidad del menú
        val allControls = SettingsRepository.getAllSelectItemVisibleControls()
        allControls.forEach { p ->
            menu.add(0, p.key.hashCode(), menu.size(), p.description)
                .setChecked(
                    itemSelectFilterFragment!!.getVisibleFilters()
                        .contains(p)
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
                (arrayAdapter?.getAll() ?: ArrayList()).mapTo(codes) { it.ean }
                if (codes.any()) scannerCompleted(codes[Random().nextInt(codes.count())])
                return super.onOptionsItemSelected(item)
            }
            menuItemRandomIt -> {
                val codes = ItemDbHelper().selectCodes(true)
                if (codes.any()) scannerCompleted(codes[Random().nextInt(codes.count())])
                return super.onOptionsItemSelected(item)
            }
            menuItemManualCode -> {
                enterCode()
                return super.onOptionsItemSelected(item)
            }
        }

        val sp = settingRepository()
        item.isChecked = !item.isChecked
        when (id) {
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

    private fun getItems(): ArrayList<Item> {
        var itemArray: ArrayList<Item> = ArrayList()

        val itemCode = itemSelectFilterFragment!!.itemCode
        val itemCategory = itemSelectFilterFragment!!.itemCategory

        if (itemCode.isEmpty() && itemCategory == null) {
            return ArrayList()
        }

        try {
            Log.d(this::class.java.simpleName, "Selecting items...")
            val itemDbHelper = ItemDbHelper()

            itemArray = (when {
                itemCode.trim().isEmpty() -> when (itemCategory) {
                    null -> itemDbHelper.select()
                    else -> itemDbHelper.selectByItemCategory(itemCategory)
                }
                else -> when (itemCategory) {
                    null -> itemDbHelper.selectByDescriptionEan(itemCode.trim())
                    else -> itemDbHelper.selectByDescriptionEanItemCategory(
                        itemCode.trim(),
                        itemCategory
                    )
                }
            })
        } catch (ex: java.lang.Exception) {
            ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())
        }

        return itemArray
    }
    // endregion

    override fun onResume() {
        super.onResume()
        rejectNewInstances = false

        JotterListener.resumeReaderDevices(this)

        // Ocultar panel de códigos vinculados al ítem seleccionado
        runOnUiThread {
            binding.moreCodesConstraintLayout.visibility = GONE
        }

        fillAdapter(completeList)
    }

    /**
     * Devuelve una cadena de texto formateada que se ajusta a los parámetros.
     * Devuelve una cadena vacía en caso de Exception.
     * Devuelve null si no es necesario cambiar la cadena ingresada porque ya se ajusta a los parámatros
     *          o porque es igual que la cadena original.
     */
    private fun getValidValue(
        source: String,
        maxIntegerPlaces: Int,
        maxDecimalPlaces: Int,
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

            // Si la parte entera es más larga que el máximo de digitos permitidos
            // retorna un caracter vacío.
            if (integerPart.length > maxIntegerPlaces) {
                return ""
            }

            // Si la cantidad de espacios decimales permitidos es cero devolver la parte entera
            // sino, concatenar la parte entera con el separador de decimales y
            // la cantidad permitida de decimales.
            val result = if (maxDecimalPlaces == 0) {
                integerPart
            } else integerPart + decimalSeparator + decimalPart.substring(
                0,
                if (decimalPart.length > maxDecimalPlaces) maxDecimalPlaces else decimalPart.length
            )

            // Devolver sólo si son valores positivos diferentes a los de originales.
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
        Statics.closeKeyboard(this)
        thread {
            completeList = getItems()
            checkedIdArray.clear()
            fillAdapter(completeList)
        }
    }

    override fun onCheckCodeEnded(scannedCode: String, item: Item?, itemCode: ItemCode?) {
        if (item != null) {
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
        } else {
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