package com.dacosys.warehouseCounter.ui.activities.orderRequest

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.text.format.DateFormat
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.OrderRequestActivityBothPanelsCollapsedBinding
import com.dacosys.warehouseCounter.ktor.v2.dto.order.Log
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequestContent
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.misc.ParcelLong
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.DATE_FORMAT
import com.dacosys.warehouseCounter.misc.Statics.Companion.FILENAME_DATE_FORMAT
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.misc.objects.status.ConfirmStatus
import com.dacosys.warehouseCounter.misc.objects.status.ConfirmStatus.CREATOR.confirm
import com.dacosys.warehouseCounter.misc.objects.status.ConfirmStatus.CREATOR.modify
import com.dacosys.warehouseCounter.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.room.dao.orderRequest.LogCoroutines
import com.dacosys.warehouseCounter.room.dao.orderRequest.OrderRequestCoroutines
import com.dacosys.warehouseCounter.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.room.entity.itemRegex.ItemRegex
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.ui.activities.common.EnterCodeActivity
import com.dacosys.warehouseCounter.ui.activities.common.MultiplierSelectActivity
import com.dacosys.warehouseCounter.ui.activities.common.QtySelectorActivity
import com.dacosys.warehouseCounter.ui.activities.item.ItemSelectActivity
import com.dacosys.warehouseCounter.ui.activities.log.LogContentActivity
import com.dacosys.warehouseCounter.ui.adapter.orderRequest.OrcAdapter
import com.dacosys.warehouseCounter.ui.fragments.common.SummaryFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.dacosys.warehouseCounter.ui.utils.Screen
import org.parceler.Parcels
import java.io.UnsupportedEncodingException
import java.text.SimpleDateFormat
import java.util.*

class OrderRequestContentActivity : AppCompatActivity(), OrcAdapter.DataSetChangedListener,
    OrcAdapter.EditQtyListener, OrcAdapter.EditDescriptionListener,
    Scanner.ScannerListener,
    SwipeRefreshLayout.OnRefreshListener, Rfid.RfidDeviceListener {
    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshItem.isRefreshing = false
        }, 100)
    }

    // region Variables privadas de la actividad

    private var tempTitle = context.getString(R.string.count)

    private var panelBottomIsExpanded = false
    private var panelTopIsExpanded = false

    private lateinit var orderRequest: OrderRequest

    private var itemCode: ItemCode? = null
    private var orderRequestId: Long = 0L
    private var completeList: ArrayList<OrderRequestContent> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()
    private var isNew: Boolean = false

    private var partial = true
    private var partialBlock = true
    private var divided = false
    private var dividedBlock = false

    private var adapter: OrcAdapter? = null
    private var lastSelected: OrderRequestContent? = null
    private var firstVisiblePos: Int? = null
    private var currentScrollPosition: Int = 0

    private lateinit var summaryFragment: SummaryFragment

    private val itemCount: Int
        get() {
            return adapter?.itemCount ?: 0
        }

    private val countChecked: Int
        get() {
            return adapter?.countChecked() ?: 0
        }

    private val totalCollected: Int
        get() {
            return (adapter?.qtyCollectedTotal() ?: 0).toInt()
        }

    private val totalRequested: Int
        get() {
            return (adapter?.qtyRequestedTotal() ?: 0).toInt()
        }

    private val currentItem: OrderRequestContent?
        get() {
            return adapter?.currentItem()
        }

    private var allowClicks = true
    private var rejectNewInstances = false

    // Se usa para saber si estamos en onStart luego de onCreate
    private var fillRequired = false

    // endregion Variables privadas de la actividad

    override fun onDestroy() {
        saveSharedPreferences()
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        adapter?.refreshListeners()
    }

    private fun saveSharedPreferences() {
        settingViewModel.scanModeCount = when {
            !partialBlock && !partial -> 1 // Parcial auto
            partialBlock -> 2 // Parcial bloqueado
            else -> 0 // Manual
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        saveBundleValues(savedInstanceState)
    }

    private fun saveBundleValues(b: Bundle) {
        if (adapter != null) {
            b.putParcelable("lastSelected", currentItem)
            b.putInt("firstVisiblePos", adapter?.firstVisiblePos() ?: RecyclerView.NO_POSITION)
            b.putLongArray("checkedIdArray", adapter?.checkedIdArray?.map { it }?.toLongArray())
            b.putInt("currentScrollPosition", currentScrollPosition)
        }

        b.putLong(ARG_ID, orderRequestId)
        b.putBoolean(ARG_IS_NEW, isNew)

        b.putParcelable("itemCode", itemCode)

        b.putBoolean("panelTopIsExpanded", panelTopIsExpanded)
        b.putBoolean("panelBottomIsExpanded", panelBottomIsExpanded)

        b.putBoolean("partial", partial)
        b.putBoolean("partialBlock", partialBlock)
        b.putBoolean("divided", divided)
        b.putBoolean("dividedBlock", dividedBlock)

        b.putParcelable("lastRegexResult", lastRegexResult)

        b.putBoolean("tempCompleted", tempCompleted)

        // Guardar en Room la orden
        saveTempOrder()
    }

    private fun loadBundleValues(b: Bundle) {
        // region Recuperar el título de la ventana
        val t1 = b.getString(ARG_TITLE)
        if (!t1.isNullOrEmpty()) tempTitle = t1
        // endregion

        itemCode = b.getParcelable("itemCode")
        checkedIdArray = (b.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(ArrayList())
        lastSelected = b.getParcelable("lastSelected")
        firstVisiblePos = if (b.containsKey("firstVisiblePos")) b.getInt("firstVisiblePos") else -1
        currentScrollPosition = b.getInt("currentScrollPosition")

        isNew = b.getBoolean(ARG_IS_NEW)
        orderRequestId = b.getLong(ARG_ID)

        partial = b.getBoolean("partial")
        partialBlock = b.getBoolean("partialBlock")
        divided = b.getBoolean("divided")
        dividedBlock = b.getBoolean("dividedBlock")
        binding.requiredDescCheckBox.isChecked = b.getBoolean("requiredDescription")

        panelBottomIsExpanded = b.getBoolean("panelBottomIsExpanded")
        panelTopIsExpanded = b.getBoolean("panelTopIsExpanded")

        lastRegexResult = b.getParcelable("lastRegexResult")

        tempCompleted = b.getBoolean("tempCompleted")
    }

    private fun loadExtraBundleValues(extras: Bundle) {
        val t1 = extras.getString(ARG_TITLE)
        if (!t1.isNullOrEmpty()) tempTitle = t1

        isNew = extras.getBoolean(ARG_IS_NEW)
        orderRequestId = extras.getLong(ARG_ID)

        loadDefaultValues()
    }

    private fun loadOrderRequest() {
        OrderRequestCoroutines.getOrderRequestById(
            id = orderRequestId,
            onResult = {
                if (it != null) {
                    orderRequest = it
                    completeList = ArrayList(it.contents)

                    fillHeader()
                    fillAdapter(completeList)
                }
            }
        )
    }

    private fun loadDefaultValues() {
        when (settingViewModel.scanModeCount) {
            1 -> {
                partialBlock = false
                partial = false // Parcial auto
            }

            2 -> partialBlock = true // Parcial bloqueado
            0 -> {
                partialBlock = false
                partial = true // Manual
            }
        }
    }

    private lateinit var binding: OrderRequestActivityBothPanelsCollapsedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = OrderRequestActivityBothPanelsCollapsedBinding.inflate(layoutInflater)
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

        summaryFragment = supportFragmentManager.findFragmentById(R.id.summaryFragment) as SummaryFragment

        if (savedInstanceState != null) {
            // Recuperar el estado previo de la actividad con los datos guardados
            loadBundleValues(savedInstanceState)
        } else {
            // Inicializar la actividad. EXTRAS: Parámetros que recibe la actividad
            val extras = intent.extras
            if (extras != null) loadExtraBundleValues(extras)
        }

        binding.swipeRefreshItem.setOnRefreshListener(this)
        binding.swipeRefreshItem.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Para expandir y colapsar el panel inferior
        setBottomPanelAnimation()
        setTopPanelAnimation()

        binding.okButton.setOnClickListener {
            if (allowClicks) {
                allowClicks = false
                finishCountDialog()
            }
        }

        binding.logButton.setOnClickListener {
            if (allowClicks) {
                allowClicks = false
                showLogDialog()
            }
        }

        binding.addButton.setOnClickListener {
            if (allowClicks) {
                allowClicks = false
                selectItemDialog()
            }
        }

        binding.removeButton.setOnClickListener {
            if (allowClicks) {
                allowClicks = false
                removeItem()
            }
        }

        binding.manualCodeButton.setOnClickListener {
            if (allowClicks) {
                allowClicks = false
                manualAddItem()
            }
        }

        binding.requiredDescCheckBox.setOnCheckedChangeListener(null)
        binding.requiredDescCheckBox.setOnCheckedChangeListener { _, isChecked ->
            settingViewModel.requiredDescription = isChecked
        }

        binding.partialButton.setOnClickListener {
            if (partialBlock) {
                partial = false
                partialBlock = false
            } else if (partial) {
                partialBlock = true
            } else {
                partial = true
            }
            setupPartial()
        }

        binding.divisorButton.setOnClickListener {
            if (dividedBlock) {
                divided = false
                dividedBlock = false
            } else if (divided) {
                dividedBlock = true
            } else {
                divided = true
            }
            setupDivided()
        }

        // region Traer de la configuración guardada
        binding.requiredDescCheckBox.isChecked = settingViewModel.requiredDescription

        binding.multiplierButton.setOnClickListener {
            if (allowClicks) {
                allowClicks = false
                multiplierDialog()
            }
        }

        setupPartial()
        setupDivided()
        setMultiplierButtonText()
    }

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
            return R.layout.order_request_activity
        }

    private val layoutBothPanelsCollapsed: Int
        get() {
            return R.layout.order_request_activity_both_panels_collapsed
        }

    private val layoutTopPanelCollapsed: Int
        get() {
            return R.layout.order_request_activity_top_panel_collapsed
        }

    private val layoutBottomPanelCollapsed: Int
        get() {
            return R.layout.order_request_activity_bottom_panel_collapsed
        }

    private fun setPanels() {
        runOnUiThread {
            val currentLayout = ConstraintSet()
            currentLayout.load(this, requiredLayout)
            currentLayout.applyTo(binding.root)

            if (panelBottomIsExpanded) binding.expandBottomPanelButton?.text =
                context.getString(R.string.collapse_panel)
            else binding.expandBottomPanelButton?.text = context.getString(R.string.scan_options)

            if (panelTopIsExpanded) binding.expandTopPanelButton?.text = context.getString(R.string.collapse_panel)
            else binding.expandTopPanelButton?.text = context.getString(R.string.item_operations)
        }
    }

    private fun setBottomPanelAnimation() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) return

        binding.expandBottomPanelButton!!.setOnClickListener {
            panelBottomIsExpanded = !panelBottomIsExpanded
            val nextLayout = ConstraintSet()
            nextLayout.load(this, requiredLayout)

            val transition = ChangeBounds()
            transition.interpolator = FastOutSlowInInterpolator()
            transition.addListener(object : Transition.TransitionListener {
                override fun onTransitionResume(transition: Transition?) {}
                override fun onTransitionPause(transition: Transition?) {}
                override fun onTransitionStart(transition: Transition?) {}
                override fun onTransitionEnd(transition: Transition?) {
                    binding.requiredDescCheckBox.isChecked = binding.requiredDescCheckBox.isChecked
                }

                override fun onTransitionCancel(transition: Transition?) {}
            })

            TransitionManager.beginDelayedTransition(binding.root, transition)
            nextLayout.applyTo(binding.root)

            if (panelBottomIsExpanded) binding.expandBottomPanelButton?.text =
                context.getString(R.string.collapse_panel)
            else binding.expandBottomPanelButton?.text = context.getString(R.string.scan_options)
        }
    }

    private fun setTopPanelAnimation() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) return

        binding.expandTopPanelButton!!.setOnClickListener {
            panelTopIsExpanded = !panelTopIsExpanded
            val nextLayout = ConstraintSet()
            nextLayout.load(this, requiredLayout)

            val transition = ChangeBounds()
            transition.interpolator = FastOutSlowInInterpolator()
            transition.addListener(object : Transition.TransitionListener {
                override fun onTransitionResume(transition: Transition?) {}
                override fun onTransitionPause(transition: Transition?) {}
                override fun onTransitionStart(transition: Transition?) {}
                override fun onTransitionEnd(transition: Transition?) {
                    binding.requiredDescCheckBox.isChecked = binding.requiredDescCheckBox.isChecked
                }

                override fun onTransitionCancel(transition: Transition?) {}
            })

            TransitionManager.beginDelayedTransition(binding.root, transition)
            nextLayout.applyTo(binding.root)

            if (panelTopIsExpanded) binding.expandTopPanelButton?.text = context.getString(R.string.collapse_panel)
            else binding.expandTopPanelButton?.text = context.getString(R.string.item_operations)
        }
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshItem.isRefreshing = show
        }, 20)
    }

    private fun setupPartial() {
        runOnUiThread {
            val buttonText: String
            val buttonBackground: Drawable?
            val buttonTextColor: Int?

            if (!partialBlock && !partial) {
                buttonText = context.getString(R.string.partial_auto)
                buttonBackground =
                    ResourcesCompat.getDrawable(this.resources, R.drawable.rounded_corner_button_ffed65, null)
                buttonTextColor = ResourcesCompat.getColor(this.resources, R.color.black, null)
            } else if (partialBlock) {
                buttonText = context.getString(R.string.partial_locked)
                buttonBackground =
                    ResourcesCompat.getDrawable(this.resources, R.drawable.rounded_corner_button_6b2737, null)
                buttonTextColor = ResourcesCompat.getColor(this.resources, R.color.whitesmoke, null)
            } else {
                buttonText = context.getString(R.string.partial_manual)
                buttonBackground =
                    ResourcesCompat.getDrawable(this.resources, R.drawable.rounded_corner_button_teal, null)
                buttonTextColor = ResourcesCompat.getColor(this.resources, R.color.whitesmoke, null)
            }

            if (buttonText.isNotEmpty() && buttonBackground != null) {
                binding.partialButton.text = buttonText
                binding.partialButton.setTextColor(buttonTextColor)
                binding.partialButton.background = buttonBackground
            }
        }
    }

    private fun setupDivided() {
        runOnUiThread {
            val buttonText: String
            val buttonBackground: Drawable?
            val buttonTextColor: Int?

            if (!dividedBlock && !divided) {
                buttonText = context.getString(R.string.without_divider)
                buttonBackground =
                    ResourcesCompat.getDrawable(this.resources, R.drawable.rounded_corner_button_ffed65, null)
                buttonTextColor = ResourcesCompat.getColor(this.resources, R.color.black, null)
            } else if (dividedBlock) {
                buttonText = context.getString(R.string.divider_locked)
                buttonBackground =
                    ResourcesCompat.getDrawable(this.resources, R.drawable.rounded_corner_button_6b2737, null)
                buttonTextColor = ResourcesCompat.getColor(this.resources, R.color.whitesmoke, null)
            } else {
                buttonText = context.getString(R.string.divider)
                buttonBackground =
                    ResourcesCompat.getDrawable(this.resources, R.drawable.rounded_corner_button_teal, null)
                buttonTextColor = ResourcesCompat.getColor(this.resources, R.color.whitesmoke, null)
            }

            if (buttonText.isNotEmpty() && buttonBackground != null) {
                binding.divisorButton.text = buttonText
                binding.divisorButton.setTextColor(buttonTextColor)
                binding.divisorButton.background = buttonBackground
            }
        }
    }

    private fun removeOrder(onFinish: () -> Unit) {
        OrderRequestCoroutines.removeById(
            id = orderRequestId,
            onResult = { onFinish() })
    }

    private fun cancelCount() {
        if (itemCount <= 0) {
            removeOrder(onFinish = { finish() })
            return
        }

        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.save_count))
            builder.setMessage(getString(R.string.save_changes_and_continue_later))
            builder.setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            builder.setNegativeButton(R.string.no) { _, _ ->
                removeOrder(onFinish = { finish() })
            }
            builder.setPositiveButton(R.string.yes) { _, _ ->
                processCount(completed = false)
            }

            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
    }

    private fun fillAdapter(t: ArrayList<OrderRequestContent>) {
        completeList = t

        showProgressBar(true)

        runOnUiThread {
            try {
                if (adapter != null) {
                    // Si el adapter es NULL es porque aún no fue creado.
                    // Por lo tanto, puede ser que los valores de [lastSelected]
                    // sean valores guardados de la instancia anterior y queremos preservarlos.
                    lastSelected = currentItem
                }

                adapter = OrcAdapter.Builder()
                    .recyclerView(binding.recyclerView)
                    .fullList(completeList)
                    .allowEditQty(true, this)
                    .allowEditDescription(true, this)
                    .checkedIdArray(checkedIdArray)
                    .orType(orderRequest.orderRequestType)
                    .dataSetChangedListener(this)
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
                showProgressBar(false)
                gentlyReturn()
            }
        }
    }

    private fun setMultiplierButtonText() {
        runOnUiThread {
            binding.multiplierButton.text = String.format(
                context.getString(R.string.multiplier_x), settingViewModel.scanMultiplier
            )
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun removeItem() {
        val currentItem = currentItem
        if (currentItem == null && countChecked <= 0) {
            allowClicks = true
            return
        }

        val orcsToRemove =
            if (countChecked > 0) adapter?.getAllChecked()
            else if (currentItem != null) arrayListOf(currentItem)
            else arrayListOf()

        var allCodes = ""
        if (orcsToRemove!!.isNotEmpty()) {
            for (w in orcsToRemove) {
                allCodes = "${w.ean}, $allCodes"
            }

            if (allCodes.endsWith(", ")) {
                allCodes = allCodes.substring(0, allCodes.length - 2)
            }

            allCodes = if (countChecked > 1) {
                String.format(getString(R.string.do_you_want_to_remove_the_items), allCodes)
            } else {
                String.format(getString(R.string.do_you_want_to_remove_the_item), allCodes)
            }
        }

        JotterListener.pauseReaderDevices(this)

        runOnUiThread {
            try {
                val adb = AlertDialog.Builder(this)
                adb.setTitle(if (orcsToRemove.count() > 1) R.string.remove_items else R.string.remove_item)
                adb.setMessage(allCodes)
                adb.setNegativeButton(R.string.cancel, null)
                adb.setPositiveButton(R.string.accept) { _, _ ->
                    var isDone = false
                    for ((index, i) in orcsToRemove.withIndex()) {
                        val df = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z")
                        val now = df.format(Calendar.getInstance().time)

                        val variationQty = i.qtyCollected!! * -1
                        if (variationQty != 0.toDouble()) {

                            LogCoroutines.add(
                                orderId = orderRequestId,
                                log = Log(
                                    clientId = orderRequest.clientId,
                                    userId = Statics.currentUserId,
                                    itemId = i.itemId,
                                    itemDescription = i.itemDescription,
                                    itemCode = i.ean,
                                    scannedCode = i.codeRead,
                                    variationQty = variationQty,
                                    finalQty = 0.toDouble(),
                                    date = now
                                ),
                                onResult = { isDone = index == orcsToRemove.lastIndex }
                            )
                        }
                    }

                    val startTime = System.currentTimeMillis()
                    while (!isDone) {
                        if (System.currentTimeMillis() - startTime == settingViewModel.connectionTimeout.toLong())
                            isDone = true
                    }
                    adapter?.remove(orcsToRemove)
                }
                adb.show()
            } catch (ex: java.lang.Exception) {
                ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())
            }
        }

        JotterListener.resumeReaderDevices(this)
        allowClicks = true
    }

    private fun selectItemDialog() {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val intent = Intent(context, ItemSelectActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra(ARG_TITLE, context.getString(R.string.select_item))
        intent.putExtra(ItemSelectActivity.ARG_MULTI_SELECT, false)
        resultForItemSelect.launch(intent)
    }

    private val resultForItemSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val items: ArrayList<ParcelLong> =
                        data.getParcelableArrayListExtra(ItemSelectActivity.ARG_IDS) ?: arrayListOf()

                    if (items.isNotEmpty()) {
                        ItemCoroutines.getById(items.first().value) { it2 ->
                            if (it2 != null) {
                                addOrc(OrderRequestContent().apply {
                                    itemId = it2.itemId
                                    itemDescription = it2.description
                                    ean = it2.ean
                                    price = it2.price?.toDouble()
                                    itemActive = it2.active == 1
                                    externalId = it2.externalId
                                    itemCategoryId = it2.itemCategoryId
                                    lotEnabled = it2.lotEnabled == 1
                                    qtyCollected = 0.toDouble()
                                    qtyRequested = 0.toDouble()
                                })
                            }
                        }
                        gentlyReturn()
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
                gentlyReturn()
            }
        }

    private fun finishCountDialog() {
        JotterListener.lockScanner(this, true)

        saveTempOrder {
            launchFinishDialog()
        }
    }

    private fun saveTempOrder(onFinish: () -> Unit = {}) {
        val fullList = adapter?.fullList?.toList() ?: listOf()
        OrderRequestCoroutines.update(
            orderRequest = orderRequest,
            contents = fullList,
            onResult = { onFinish() })
    }

    private fun launchFinishDialog() {
        val intent = Intent(this, OrderRequestConfirmActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra(OrderRequestConfirmActivity.ARG_ID, orderRequestId)
        resultForFinishCount.launch(intent)
    }

    private val resultForFinishCount =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    when (Parcels.unwrap<ConfirmStatus>(data.getParcelableExtra(OrderRequestConfirmActivity.ARG_CONFIRM_STATUS))) {
                        modify -> processCount(completed = false)
                        confirm -> processCount(completed = true)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                gentlyReturn()
            }
        }

    // Flag que guarda el estado "completed" en caso de que
    // se requerirán permisos de escritura y se tenga que retomar la
    // actividad después de que el usuario los otorgue.
    private var tempCompleted = false

    private fun processCount(completed: Boolean) {
        tempCompleted = completed

        // Preparar el conteo
        orderRequest.contents = adapter?.fullList?.toList() ?: listOf()
        orderRequest.completed = tempCompleted
        orderRequest.finishDate =
            if (tempCompleted) DateFormat.format(DATE_FORMAT, System.currentTimeMillis()).toString()
            else ""

        if (orderRequest.filename.isEmpty()) {
            val df = SimpleDateFormat(FILENAME_DATE_FORMAT, Locale.getDefault())
            orderRequest.filename = String.format("%s.json", df.format(Calendar.getInstance().time))
        }

        setImagesJson()

        LogCoroutines.getByOrderId(
            orderId = orderRequestId,
            onResult = {
                orderRequest.logs = it.toList()
                proceed()
            }
        )
    }

    private fun proceed() {
        val error: Boolean
        try {
            orJson = json.encodeToString(OrderRequest.serializer(), orderRequest)
            android.util.Log.i(this::class.java.simpleName, orJson)

            orFileName = orderRequest.filename.substringAfterLast('/')

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                error = !Statics.writeJsonToFile(
                    v = binding.root,
                    filename = orFileName,
                    value = orJson,
                    completed = tempCompleted
                )

                if (!error) {
                    finish()
                }
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_EXTERNAL_STORAGE
                )
            }
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            android.util.Log.e(this::class.java.simpleName, e.message ?: "")
        }
    }

    private fun setImagesJson() {
        /*
        // IMAGE CONTROL
        // LUEGO, CUANDO FINALMENTE SE ENVÍA LA RECOLECCIÓN,
        // EL SERVIDOR DEVUELVE LOS ID'S NECESARIOS PARA COMPLETAR LOS DATOS DEL DOCUMENTO
        // Y SUBIRLO AL FTP
        if (docContObjArrayList.count() > 0) {
            val docs: ArrayList<Document> = ArrayList()
            docContObjArrayList.forEach { t ->

                val x = Document()
                x.description = t.description
                x.observations = t.obs
                x.reference = t.reference

                val y = DocumentContent(
                    t.filename_original,
                    DocumentType.Jpg
                )

                x.content = arrayListOf(y)
                docs.add(x)
            }

            orderRequest!!.docArray = docs
        }
        */
    }

    private fun showLogDialog() {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val intent = Intent(context, LogContentActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra(LogContentActivity.ARG_TITLE, context.getString(R.string.count_log))
        intent.putExtra(LogContentActivity.ARG_ID, orderRequestId)
        resultForLog.launch(intent)
    }

    private val resultForLog =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            gentlyReturn()
        }

    private fun manualAddItem() {
        runOnUiThread {
            val input = EditText(this)
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.manual_code)
            builder.setMessage(R.string.enter_the_code)
            builder.setView(input)
            builder.setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            builder.setPositiveButton(R.string.ok) { _, _ ->
                scannerCompleted(input.text.toString())
            }

            val alert = builder.create()
            alert.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            alert.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            alert.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

            input.isFocusable = true
            input.isFocusableInTouchMode = true
            input.maxLines = 1
            input.inputType = InputType.TYPE_CLASS_TEXT
            input.filters = arrayOf<InputFilter>(InputFilter.AllCaps())
            input.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            alert.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
                        }
                    }
                }
                false
            }

            alert.show()
            input.requestFocus()
            allowClicks = true
        }
    }

    private fun setQtyManually(orc: OrderRequestContent) {
        val multi = if (itemCode == null) settingViewModel.scanMultiplier
        else itemCode?.qty ?: 0

        qtySelectorDialog(
            orc = orc,
            initialQty = 1.toDouble(),
            minValue = 0.toDouble(),
            maxValue = 999999.toDouble(),
            multiplier = multi.toInt(),
            total = false
        )
    }

    private fun addOrc(tempOrc: OrderRequestContent?): Boolean {
        try {
            val count = adapter?.itemCount ?: 0
            if (Statics.DEMO_MODE) {
                if (count >= 5) {
                    val res =
                        context.getString(R.string.maximum_amount_of_demonstration_mode_reached)
                    showSnackBar(res, ERROR)
                    android.util.Log.e(this::class.java.simpleName, res)
                    return false
                }
            }

            // Nada que hacer, volver
            if (tempOrc == null) {
                return false
            }

            // Buscar primero en el adaptador de la lista
            for (a in 0 until count) {
                val orc = adapter?.currentList?.get(a) ?: continue
                if (orc.itemId == tempOrc.itemId) {
                    val qtyCollected = tempOrc.qtyCollected ?: 0.toDouble()
                    setQtyCollected(orc = orc, qty = qtyCollected, manualAdd = true)
                    return true
                }
            }

            // No está en la lista, agregar al adaptador con cantidades 0, después se actualiza y se agrega al Log
            runOnUiThread {
                adapter?.add(arrayListOf(tempOrc))

                // Esta pausa permite que el Adapter se actualice a tiempo
                Handler(mainLooper).postDelayed({
                    setQty(tempOrc)
                }, 250)
            }

            return true
        } catch (ex: Exception) {
            showSnackBar(ex.message.toString(), ERROR)
            android.util.Log.e(this::class.java.simpleName, ex.message.toString())
            return false
        }
    }

    private fun showDialogForItemDescription(orc: OrderRequestContent) {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val intent = Intent(context, EnterCodeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        intent.putExtra(ARG_TITLE, context.getString(R.string.enter_description))
        intent.putExtra(EnterCodeActivity.ARG_HINT, context.getString(R.string.item_description_hint))
        intent.putExtra(EnterCodeActivity.ARG_ORC, orc)
        resultForEnterDescription.launch(intent)
    }

    private val resultForEnterDescription =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (data != null) {
                    val description = data.getStringExtra(EnterCodeActivity.ARG_CODE) ?: ""
                    val orc = Parcels.unwrap<OrderRequestContent>(data.getParcelableExtra(EnterCodeActivity.ARG_ORC))

                    val item = orc ?: return@registerForActivityResult

                    if (it.resultCode == RESULT_OK) {
                        if (description.trim().isEmpty() && binding.requiredDescCheckBox.isChecked) {
                            adapter?.remove(orc)
                        } else {
                            setDescription(orc, description)
                        }
                    } else {
                        if ((item.itemDescription.isEmpty() || item.itemDescription == context.getString(
                                R.string.unknown_item
                            )) &&
                            binding.requiredDescCheckBox.isChecked
                        ) {
                            adapter?.remove(orc)
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                gentlyReturn()
            }
        }

    private fun showDialogForAddDescription(orc: OrderRequestContent) {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val intent = Intent(context, EnterCodeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        intent.putExtra(ARG_TITLE, context.getString(R.string.enter_description))
        intent.putExtra(EnterCodeActivity.ARG_HINT, context.getString(R.string.item_description_hint))
        intent.putExtra(EnterCodeActivity.ARG_ORC, orc)
        resultForAddDescription.launch(intent)
    }

    private val resultForAddDescription =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (data != null) {
                    val description = data.getStringExtra(EnterCodeActivity.ARG_CODE) ?: ""
                    val orc = Parcels.unwrap<OrderRequestContent>(data.getParcelableExtra(EnterCodeActivity.ARG_ORC))

                    val item = orc ?: return@registerForActivityResult

                    if (it.resultCode == RESULT_OK) {
                        if (description.trim().isEmpty() && binding.requiredDescCheckBox.isChecked) {
                            adapter?.remove(orc)
                        } else {
                            setDescription(orc, description)
                            setQty(orc)
                        }
                    } else {
                        if ((item.itemDescription.isEmpty() || item.itemDescription == context.getString(
                                R.string.unknown_item
                            )) &&
                            binding.requiredDescCheckBox.isChecked
                        ) {
                            adapter?.remove(orc)
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                gentlyReturn()
            }
        }

    private fun lotCodeDialog(orc: OrderRequestContent) {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val intent = Intent(context, EnterCodeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra(ARG_TITLE, context.getString(R.string.enter_lot_code))
        intent.putExtra(EnterCodeActivity.ARG_HINT, context.getString(R.string.lot_code))
        intent.putExtra(EnterCodeActivity.ARG_ORC, orc)
        resultForLotCodeSelect.launch(intent)
    }

    private val resultForLotCodeSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val code = data.getStringExtra(EnterCodeActivity.ARG_CODE) ?: ""
                    val orc = Parcels.unwrap<OrderRequestContent>(data.getParcelableExtra(EnterCodeActivity.ARG_ORC))
                    setLotCode(orc = orc, lotCode = code)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                gentlyReturn()
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

    public override fun onStart() {
        super.onStart()

        setPanels()

        if (fillRequired) {
            fillRequired = false

            loadOrderRequest()
        } else {
            gentlyReturn()
        }
    }

    private fun fillHeader() {
        runOnUiThread {
            if (orderRequest.description.isNotEmpty()) {
                tempTitle = "${context.getString(R.string.count)}: ${orderRequest.description}"
            }

            if (orderRequest.orderRequestType != OrderRequestType.notDefined) {
                binding.orTypeTextView.text = when (orderRequest.orderRequestType) {
                    OrderRequestType.deliveryAudit -> OrderRequestType.deliveryAudit.description
                    OrderRequestType.prepareOrder -> OrderRequestType.prepareOrder.description
                    OrderRequestType.receptionAudit -> OrderRequestType.receptionAudit.description
                    OrderRequestType.stockAudit -> OrderRequestType.stockAudit.description
                    OrderRequestType.stockAuditFromDevice -> OrderRequestType.stockAuditFromDevice.description
                    else -> context.getString(R.string.counted)
                }
            }
            binding.topAppbar.title = tempTitle
        }
    }

    override fun onBackPressed() {
        cancelCount()
    }

    private fun qtySelectorDialog(
        orc: OrderRequestContent,
        initialQty: Double,
        minValue: Double,
        maxValue: Double,
        multiplier: Int,
        total: Boolean,
    ) {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val intent = Intent(this, QtySelectorActivity::class.java)
        intent.putExtra(QtySelectorActivity.ARG_ORDER_REQUEST_CONTENT, Parcels.wrap(orc))
        intent.putExtra(QtySelectorActivity.ARG_PARTIAL, (partial || partialBlock))
        intent.putExtra(QtySelectorActivity.ARG_INITIAL_VALUE, initialQty)
        intent.putExtra(QtySelectorActivity.ARG_MULTIPLIER, multiplier.toLong())
        intent.putExtra(QtySelectorActivity.ARG_MIN_VALUE, minValue)
        intent.putExtra(QtySelectorActivity.ARG_MAX_VALUE, maxValue)
        if (total) resultForTotalSelect.launch(intent)
        else resultForPartialSelect.launch(intent)
    }

    private val resultForTotalSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val qty = data.getDoubleExtra(QtySelectorActivity.ARG_QTY, 0.toDouble())
                    val orc =
                        Parcels.unwrap<OrderRequestContent>(data.getParcelableExtra(QtySelectorActivity.ARG_ORDER_REQUEST_CONTENT))

                    // La descripción puede modificarse desde el selector de cantidades
                    setDescription(
                        orc = orc,
                        description = orc.itemDescription,
                        updateDb = false
                    )

                    setQtyCollected(orc = orc, qty = qty, manualAdd = true, total = true)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                gentlyReturn()
            }
        }

    private val resultForPartialSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val qty = data.getDoubleExtra(QtySelectorActivity.ARG_QTY, 0.toDouble())
                    val orc =
                        Parcels.unwrap<OrderRequestContent>(data.getParcelableExtra(QtySelectorActivity.ARG_ORDER_REQUEST_CONTENT))

                    // La descripción puede modificarse desde el selector de cantidades
                    setDescription(
                        orc = orc,
                        description = orc.itemDescription,
                        updateDb = false
                    )

                    setQtyCollected(orc = orc, qty = qty, manualAdd = true, total = false)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                gentlyReturn()
            }
        }

    private fun multiplierDialog() {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val multiplier = settingViewModel.scanMultiplier
        val intent = Intent(context, MultiplierSelectActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra(MultiplierSelectActivity.ARG_TITLE, context.getString(R.string.select_multiplier))
        intent.putExtra(MultiplierSelectActivity.ARG_MULTIPLIER, multiplier)
        resultForMultiplierSelect.launch(intent)
    }

    private val resultForMultiplierSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    setMultiplierButtonText()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                gentlyReturn()
            }
        }

    private fun setLotCode(orc: OrderRequestContent, lotCode: String) {
        if (lotCode.isEmpty()) return

        val itemId = orc.itemId ?: return

        adapter?.updateLotCode(itemId, lotCode)
    }

    private fun setDescription(
        orc: OrderRequestContent,
        description: String,
        updateDb: Boolean = true,
    ) {
        if (updateDb) {
            val itemId = orc.itemId ?: return
            ItemCoroutines.updateDescription(itemId, description) {
                adapter?.updateDescription(itemId, description)
            }
        } else {
            adapter?.updateDescription(orc, description)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun setQtyCollected(
        orc: OrderRequestContent?,
        qty: Double,
        manualAdd: Boolean = false,
        total: Boolean = false,
    ) {
        if (orc == null || qty <= 0 && !manualAdd) {
            lastRegexResult = null
            return
        }

        // Si viene del selector de cantidad manual ya está aplicado el multiplicador
        // Usar el valor del multiplicador si no se escaneó un código con cantidad determinada (itemCode)
        val multi: Long = when {
            manualAdd -> 1L
            else -> {
                when (itemCode) {
                    null -> settingViewModel.scanMultiplier.toLong()
                    else -> itemCode?.qty?.toLong() ?: 0
                }
            }
        }

        val initialQty = orc.qtyCollected ?: 0.0
        val itemId = orc.itemId ?: 0L
        val tempOrc = adapter?.fullList?.firstOrNull { it.itemId == itemId } ?: return

        val tempQty: Double = if (!total) {
            qty * multi + (tempOrc.qtyCollected ?: 0.toDouble())
        } else {
            qty * multi
        }

        if (tempQty <= 0) {
            removeItem()
            lastRegexResult = null
            return
        }

        adapter?.updateQtyCollected(tempOrc, tempQty)

        // Unless is blocked, unlock the partial
        if (!partialBlock) {
            partial = false
            partialBlock = false
            setupPartial()
        }

        val df = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z")
        val now = df.format(Calendar.getInstance().time)

        val variationQty = (tempOrc.qtyCollected ?: 0.0) - initialQty
        if (variationQty != 0.toDouble()) {
            LogCoroutines.add(
                orderId = orderRequestId,
                log = Log(
                    clientId = orderRequest.clientId,
                    userId = Statics.currentUserId,
                    itemId = tempOrc.itemId,
                    itemDescription = tempOrc.itemDescription,
                    itemCode = tempOrc.ean,
                    scannedCode = tempOrc.codeRead,
                    variationQty = variationQty,
                    finalQty = tempOrc.qtyCollected,
                    date = now
                )
            )
        }

        checkLotEnabled(tempOrc)
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
        else {
            when (requestCode) {
                REQUEST_EXTERNAL_STORAGE -> {
                    // If the request is canceled, the result arrays are empty.
                    if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        showSnackBar(
                            getString(R.string.cannot_write_images_to_external_storage), ERROR
                        )
                    } else {
                        val error = !Statics.writeJsonToFile(
                            v = binding.root,
                            filename = orFileName,
                            value = orJson,
                            completed = tempCompleted
                        )
                        if (!error) finish()
                    }
                }
            }
        }
    }

    // Este flag lo vamos a usar para saber si estamos tratando con un
    private var lastRegexResult: ItemRegex.Companion.RegexResult? = null

    override fun scannerCompleted(scanCode: String) {
        if (settingViewModel.showScannedCode) showSnackBar(scanCode, INFO)

        JotterListener.lockScanner(this, true)

        var code: String
        val fullList = adapter?.fullList ?: ArrayList()

        ItemRegex.tryToRegex(scanCode) { it ->
            if (!it.any()) {
                // No coincide con los Regex de la DB, lo
                // usamos como un código corriente.
                code = scanCode
            } else {
                // region Regex Founded
                if (it.count() > 1) {
                    // Mostrar advertencia.
                    showSnackBar(
                        getString(R.string.there_are_multiple_regex_matches), INFO
                    )
                }

                // Utilizamos la primera coincidencia
                val regexRes = it.firstOrNull()
                lastRegexResult = regexRes
                if (regexRes == null) return@tryToRegex

                // Si la cantidad no es NULL proseguimos con el Regex
                if (regexRes.qty != null) {
                    CheckCode(callback = { onCheckCodeEnded(it) },
                        scannedCode = regexRes.ean,
                        list = fullList,
                        onEventData = { showSnackBar(it.text, it.snackBarType) }).execute()

                    return@tryToRegex
                }

                // Qty NULL →
                //    Actualizar scanned code y proseguir como
                //    un código corriente pero advertir sobre el error.

                // Mostrar advertencia.
                showSnackBar("Cantidad nula en Regex", INFO)
                code = regexRes.ean

                // endregion Regex Founded
            }

            if (divided) {
                val splitCode = scanCode.split(settingViewModel.divisionChar.toRegex())
                    .dropLastWhile { it2 -> it2.isEmpty() }
                if (splitCode.any()) {
                    code = splitCode.toTypedArray()[0]
                }
            }

            CheckCode(callback = { onCheckCodeEnded(it) },
                scannedCode = code,
                list = fullList,
                onEventData = { it2 -> showSnackBar(it2.text, it2.snackBarType) }).execute()

            gentlyReturn()
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    private fun gentlyReturn() {
        // Unless is blocked, unlock the partial
        if (!dividedBlock) {
            divided = false
            dividedBlock = false
            setupDivided()
        }

        Screen.closeKeyboard(this)
        allowClicks = true
        JotterListener.lockScanner(this, false)
        rejectNewInstances = false
    }

    override fun onEditDescriptionRequired(position: Int, orc: OrderRequestContent) {
        showDialogForItemDescription(orc)
    }

    private fun onCheckCodeEnded(it: CheckCode.CheckCodeEnded) {
        val orc = it.orc ?: return
        itemCode = it.itemCode

        // El código fue chequeado, se agrega y se selecciona en la lista
        runOnUiThread { adapter?.add(arrayListOf(orc)) }

        // Definir las cantidades según el modo de ingreso actual
        if (binding.requiredDescCheckBox.isChecked &&
            orc.itemDescription == context.getString(
                R.string.unknown_item
            )
        ) {
            // Antes agregar descripción si es obligatorio.
            // La función itemDescriptionDialog(orc) al final llama a setQty(orc)
            showDialogForAddDescription(orc)
            return
        }

        // Esta pausa permite que el Adapter se actualice a tiempo
        Handler(mainLooper).postDelayed({
            setQty(orc)
        }, 250)
        gentlyReturn()
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

        if (BuildConfig.DEBUG || Statics.TEST_MODE) {
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
                /*
                val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0987654321"
                val s = Random().ints(10, 0, source.length)
                    .asSequence()
                    .map(source::get)
                    .joinToString("")
                scannerCompleted(s)
                return super.onOptionsItemSelected(item)
                */
                JotterListener.trigger(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_read_barcode -> {
                JotterListener.toggleCameraFloatingWindowVisibility(this)
                return super.onOptionsItemSelected(item)
            }

            menuItemRandomOnListL -> {
                val codes: ArrayList<String> = ArrayList()

                (adapter?.fullList ?: ArrayList<OrderRequestContent>()
                    .filter { it.codeRead.isNotEmpty() })
                    .mapTo(codes) { it.codeRead }

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

    private fun setQty(orc: OrderRequestContent) {
        if (lastRegexResult != null) {
            val qty = lastRegexResult?.qty?.toDouble()
            if (qty == null) showSnackBar(
                getString(R.string.null_quantity_in_regex), ERROR
            )

            setQtyCollected(orc = orc, qty = qty ?: 1.toDouble())
        } else {
            if (partial || partialBlock) {
                setQtyManually(orc)
            } else {
                setQtyCollected(orc = orc, qty = 1.toDouble())
            }
        }
    }

    private fun checkLotEnabled(orc: OrderRequestContent) {
        // Check LOT ENABLED
        val itemId = orc.itemId ?: 0
        if (itemId <= 0 || orc.lotEnabled != true) return

        val lotIdStr = lastRegexResult?.lot ?: orc.lotId?.toString() ?: ""
        if (lotIdStr.isNotEmpty()) {
            lastRegexResult = null
            setLotCode(orc = orc, lotCode = lotIdStr)
        } else {
            lotCodeDialog(orc)
        }
    }

    override fun onDataSetChanged() {
        runOnUiThread {
            Handler(Looper.getMainLooper()).postDelayed({
                val labelCant: Boolean
                val totalVisible: Int
                if (orderRequest.orderRequestType == OrderRequestType.stockAuditFromDevice) {
                    labelCant = false
                    totalVisible = totalCollected
                } else {
                    labelCant = true
                    totalVisible = totalRequested
                }

                summaryFragment
                    .multiSelect(labelCant) // This is to set fragments captions
                    .totalVisible(totalVisible)
                    .totalChecked(countChecked)
                    .fill()
            }, 100)
        }
    }

    override fun onEditQtyRequired(
        position: Int,
        content: OrderRequestContent,
        initialQty: Double,
        minValue: Double,
        maxValue: Double,
        multiplier: Int,
    ) {
        qtySelectorDialog(
            orc = content,
            initialQty = initialQty,
            minValue = minValue,
            maxValue = maxValue,
            multiplier = multiplier,
            total = true
        )
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_ID = "id"
        const val ARG_IS_NEW = "isNew"

        private const val REQUEST_EXTERNAL_STORAGE = 3001

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }

        private var orFileName = ""
        private var orJson = ""
    }
}
