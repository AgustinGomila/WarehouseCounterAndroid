package com.dacosys.warehouseCounter.ui.activities.orderRequest

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.text.format.DateFormat
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.adapter.orderRequest.OrcAdapter
import com.dacosys.warehouseCounter.databinding.OrderRequestActivityBothPanelsCollapsedBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.closeKeyboard
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.misc.objects.status.ConfirmStatus
import com.dacosys.warehouseCounter.misc.objects.status.ConfirmStatus.CREATOR.confirm
import com.dacosys.warehouseCounter.misc.objects.status.ConfirmStatus.CREATOR.modify
import com.dacosys.warehouseCounter.moshi.log.Log
import com.dacosys.warehouseCounter.moshi.log.LogContent
import com.dacosys.warehouseCounter.moshi.orderRequest.*
import com.dacosys.warehouseCounter.moshi.orderRequest.Item.CREATOR.fromItemRoom
import com.dacosys.warehouseCounter.room.dao.item.ItemCoroutines
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
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import org.parceler.Parcels
import java.io.UnsupportedEncodingException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class OrderRequestContentActivity : AppCompatActivity(), OrcAdapter.DataSetChangedListener,
    OrcAdapter.EditQtyListener, OrcAdapter.EditDescriptionListener, Scanner.ScannerListener,
    SwipeRefreshLayout.OnRefreshListener, Rfid.RfidDeviceListener {
    override fun onEditDescriptionRequired(position: Int, orc: OrderRequestContent) {
        showDialogForItemDescription(orc)
    }

    private fun onCheckCodeEnded(it: CheckCode.CheckCodeEnded) {
        val orc = it.orc ?: return
        itemCode = it.itemCode

        // El código fue chequeado, se agrega y se seleciona en la lista
        orcAdapter?.add(arrayListOf(orc))

        // Definir las cantidades según el modo de ingreso actual
        if (binding.requiredDescCheckBox.isChecked &&
            orc.item?.itemDescription == context().getString(R.string.unknown_item)
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

        if (!settingViewModel().useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        if (BuildConfig.DEBUG || Statics.testMode) {
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
                //val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0987654321"
                //val s = Random().ints(10, 0, source.length)
                //    .asSequence()
                //    .map(source::get)
                //    .joinToString("")
                //scannerCompleted(s)
                //return super.onOptionsItemSelected(item)
                JotterListener.trigger(this)
                return super.onOptionsItemSelected(item)
            }
            R.id.action_read_barcode -> {
                JotterListener.toggleCameraFloatingWindowVisibility(this)
                return super.onOptionsItemSelected(item)
            }
            menuItemRandomOnListL -> {
                val codes: ArrayList<String> = ArrayList()
                (orcAdapter?.getAll()
                    ?: ArrayList<OrderRequestContent>().filter { it.item != null && !it.item?.codeRead.isNullOrEmpty() }).mapTo(
                    codes
                ) {
                    it.item?.codeRead ?: ""
                }
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
            menuRegexItem -> {
                scannerCompleted("0SM20220721092826007792261002857001038858")
                return super.onOptionsItemSelected(item)
            }
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

    private fun setQty(orc: OrderRequestContent) {
        if (lastRegexResult != null) {
            val qty = lastRegexResult?.qty?.toDouble()
            if (qty == null) makeText(this, getString(R.string.null_quantity_in_regex), ERROR)

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
        val item = orc.item ?: return
        if ((item.itemId ?: 0) > 0 && item.lotEnabled == true) {
            if (orc.lot != null || !lastRegexResult?.lot.isNullOrEmpty()) {
                setLotCode(orc, lastRegexResult?.lot ?: "")
                lastRegexResult = null
            } else {
                lotCodeDialog(orc)
            }
        }
    }

    override fun onDataSetChanged() {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                fillSummaryRow()
            }
        }, 100)
    }

    override fun onEditQtyRequired(
        position: Int,
        orc: OrderRequestContent,
        initialQty: Double,
        minValue: Double,
        maxValue: Double,
        multiplier: Int,
    ) {
        qtySelectorDialog(
            orc = orc,
            initialQty = initialQty,
            minValue = minValue,
            maxValue = maxValue,
            multiplier = multiplier,
            total = true
        )
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshOrc.isRefreshing = false
            }
        }, 100)
    }

// region Variables privadas de la actividad

    private var tempTitle = context().getString(R.string.count)

    private var panelBottomIsExpanded = false
    private var panelTopIsExpanded = false

    private var itemCode: ItemCode? = null
    private var orderRequest: OrderRequest? = null
    private var orcArray: ArrayList<OrderRequestContent> = ArrayList()
    private var checkedIdArray: ArrayList<Int> = ArrayList()
    private var isNew: Boolean = false

    private var partial = true
    private var partialBlock = true
    private var divided = false
    private var dividedBlock = false

    private var orcAdapter: OrcAdapter? = null
    private var lastSelected: OrderRequestContent? = null
    private var firstVisiblePos: Int? = null
    private var log: Log? = null

    private var allowClicks = true
    private var rejectNewInstances = false

// endregion Variables privadas de la actividad

    override fun onDestroy() {
        saveSharedPreferences()
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        orcAdapter?.refreshListeners(null, null, null, null)
    }

    private fun saveSharedPreferences() {
        settingViewModel().scanModeCount = when {
            !partialBlock && !partial -> 1 // Parcial auto
            partialBlock -> 2 // Parcial bloqueado
            partial -> 0 // Manual
            else -> 1
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        saveBundleValues(savedInstanceState)
    }

    private fun saveBundleValues(b: Bundle) {
        if (orcAdapter != null) {
            b.putParcelable("lastSelected", orcAdapter?.currentOrc())
            b.putInt("firstVisiblePos", orcAdapter?.firstVisiblePos() ?: 0)
            b.putParcelableArrayList("orcArray", orcAdapter?.getAll())
            b.putIntegerArrayList("checkedIdArray", orcAdapter?.getAllCheckedAsInt() ?: ArrayList())
        }

        b.putParcelable("itemCode", itemCode)
        b.putParcelable("orderRequest", orderRequest)

        b.putBoolean("isNew", isNew)

        b.putParcelable("log", log)

        b.putBoolean("panelTopIsExpanded", panelTopIsExpanded)
        b.putBoolean("panelBottomIsExpanded", panelBottomIsExpanded)

        b.putBoolean("partial", partial)
        b.putBoolean("partialBlock", partialBlock)
        b.putBoolean("divided", divided)
        b.putBoolean("dividedBlock", dividedBlock)

        b.putParcelable("lastRegexResult", lastRegexResult)

        b.putBoolean("tempCompleted", tempCompleted)
    }

    private fun loadBundleValues(b: Bundle) {
        // region Recuperar el título de la ventana
        val t1 = b.getString("title")
        if (t1 != null && t1.isNotEmpty()) tempTitle = t1
        // endregion

        itemCode = b.getParcelable("itemCode")
        orderRequest = b.getParcelable("orderRequest")
        orcArray =
            b.getParcelableArrayList<OrderRequestContent>("orcArray") as ArrayList<OrderRequestContent>
        checkedIdArray = b.getIntegerArrayList("checkedIdArray") ?: ArrayList()
        lastSelected = b.getParcelable("lastSelected")
        firstVisiblePos = if (b.containsKey("firstVisiblePos")) b.getInt("firstVisiblePos") else -1

        isNew = b.getBoolean("isNew")

        log = b.getParcelable("log")

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
        val t1 = extras.getString("title")
        if (t1 != null && t1.isNotEmpty()) tempTitle = t1

        isNew = extras.getBoolean("isNew")
        orderRequest = Parcels.unwrap<OrderRequest>(extras.getParcelable("orderRequest"))

        loadDefaultValues()
    }

    private fun loadDefaultValues() {
        val or = orderRequest ?: return
        orcArray = ArrayList(orderRequest?.content ?: ArrayList())

        log = Log(
            or.clientId,
            Statics.currentUserId,
            or.description,
            DateFormat.format("yyyy-MM-dd hh:mm:ss", System.currentTimeMillis()).toString(),
            ArrayList()
        )

        when (settingViewModel().scanModeCount) {
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
        Statics.setScreenRotation(this)
        binding = OrderRequestActivityBothPanelsCollapsedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState != null) {
            // Recuperar el estado previo de la actividad con los datos guardados
            loadBundleValues(savedInstanceState)
        } else {
            // Inicializar la actividad. EXTRAS: Parámetros que recibe la actividad
            val extras = intent.extras
            if (extras != null) loadExtraBundleValues(extras)
        }

        // Nunca debería ser NULL.
        if (orderRequest != null) {
            if (orderRequest!!.description.isNotEmpty()) {
                tempTitle = "${context().getString(R.string.count)}: ${orderRequest!!.description}"
            }

            if (orderRequest!!.orderRequestedType != null) {
                binding.orTypeTextView.text = when (orderRequest!!.orderRequestedType) {
                    OrderRequestType.deliveryAudit -> OrderRequestType.deliveryAudit.description
                    OrderRequestType.prepareOrder -> OrderRequestType.prepareOrder.description
                    OrderRequestType.receptionAudit -> OrderRequestType.receptionAudit.description
                    OrderRequestType.stockAudit -> OrderRequestType.stockAudit.description
                    OrderRequestType.stockAuditFromDevice -> OrderRequestType.stockAuditFromDevice.description
                    else -> context().getString(R.string.counted)
                }
            }
        }

        title = tempTitle

        binding.swipeRefreshOrc.setOnRefreshListener(this)
        binding.swipeRefreshOrc.setColorSchemeResources(
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
            settingViewModel().requiredDescription = isChecked
        }

        binding.partialButton.setOnClickListener {
            if (partialBlock) {
                partial = false
                partialBlock = false
            } else if (partial) {
                partialBlock = true
            } else if (!partial) {
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
            } else if (!divided) {
                divided = true
            }
            setupDivided()
        }

        // region Traer de la configuración guardada
        binding.requiredDescCheckBox.isChecked = settingViewModel().requiredDescription

        binding.multiplierButton.setOnClickListener {
            if (allowClicks) {
                allowClicks = false
                multiplierDialog()
            }
        }

        setPanels()

        setupPartial()
        setupDivided()
        setMultiplierButtonText()
    }

    private fun setPanels() {
        val currentLayout = ConstraintSet()
        if (panelBottomIsExpanded) {
            if (panelTopIsExpanded) currentLayout.load(this, R.layout.order_request_activity)
            else currentLayout.load(this, R.layout.order_request_activity_top_panel_collapsed)
        } else {
            if (panelTopIsExpanded) currentLayout.load(
                this, R.layout.order_request_activity_bottom_panel_collapsed
            )
            else currentLayout.load(this, R.layout.order_request_activity_both_panels_collapsed)
        }

        val transition = ChangeBounds()
        transition.interpolator = FastOutSlowInInterpolator()
        transition.addListener(object : Transition.TransitionListener {
            override fun onTransitionResume(transition: Transition?) {}
            override fun onTransitionPause(transition: Transition?) {}
            override fun onTransitionStart(transition: Transition?) {}
            override fun onTransitionEnd(transition: Transition?) {}
            override fun onTransitionCancel(transition: Transition?) {}
        })

        TransitionManager.beginDelayedTransition(binding.orderRequestContent, transition)
        currentLayout.applyTo(binding.orderRequestContent)

        when {
            panelBottomIsExpanded -> binding.expandBottomPanelButton?.text =
                context().getString(R.string.collapse_panel)
            else -> binding.expandBottomPanelButton?.text =
                context().getString(R.string.scan_options)
        }

        when {
            panelTopIsExpanded -> binding.expandTopPanelButton?.text =
                context().getString(R.string.collapse_panel)
            else -> binding.expandTopPanelButton?.text =
                context().getString(R.string.item_operations)
        }
    }

    private fun setBottomPanelAnimation() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            return
        }

        binding.expandBottomPanelButton!!.setOnClickListener {
            val nextLayout = ConstraintSet()
            if (panelBottomIsExpanded) {
                if (panelTopIsExpanded) nextLayout.load(
                    this, R.layout.order_request_activity_bottom_panel_collapsed
                )
                else nextLayout.load(this, R.layout.order_request_activity_both_panels_collapsed)
            } else {
                if (panelTopIsExpanded) nextLayout.load(this, R.layout.order_request_activity)
                else nextLayout.load(this, R.layout.order_request_activity_top_panel_collapsed)
            }

            panelBottomIsExpanded = !panelBottomIsExpanded
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

            TransitionManager.beginDelayedTransition(binding.orderRequestContent, transition)

            when {
                panelBottomIsExpanded -> binding.expandBottomPanelButton?.text =
                    context().getString(R.string.collapse_panel)
                else -> binding.expandBottomPanelButton?.text =
                    context().getString(R.string.scan_options)
            }

            nextLayout.applyTo(binding.orderRequestContent)
        }
    }

    private fun setTopPanelAnimation() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            return
        }

        binding.expandTopPanelButton!!.setOnClickListener {
            val nextLayout = ConstraintSet()
            if (panelBottomIsExpanded) {
                if (panelTopIsExpanded) nextLayout.load(
                    this, R.layout.order_request_activity_top_panel_collapsed
                )
                else nextLayout.load(this, R.layout.order_request_activity)
            } else {
                if (panelTopIsExpanded) nextLayout.load(
                    this, R.layout.order_request_activity_both_panels_collapsed
                )
                else nextLayout.load(this, R.layout.order_request_activity_bottom_panel_collapsed)
            }

            panelTopIsExpanded = !panelTopIsExpanded

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

            TransitionManager.beginDelayedTransition(binding.orderRequestContent, transition)

            when {
                panelTopIsExpanded -> binding.expandTopPanelButton?.text =
                    context().getString(R.string.collapse_panel)
                else -> binding.expandTopPanelButton?.text =
                    context().getString(R.string.item_operations)
            }

            nextLayout.applyTo(binding.orderRequestContent)
        }
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshOrc.isRefreshing = show
            }
        }, 20)
    }

    private fun setupPartial() {
        runOnUiThread {
            var buttonText = ""
            var buttonBackground: Drawable? = null
            var buttonTextColor: Int? = null

            if (!partialBlock && !partial) {
                buttonText = context().getString(R.string.partial_auto)
                buttonBackground = ResourcesCompat.getDrawable(
                    this.resources, R.drawable.rounded_corner_button_ffed65, null
                )
                buttonTextColor = ResourcesCompat.getColor(this.resources, R.color.black, null)
            } else if (partialBlock) {
                buttonText = context().getString(R.string.partial_locked)
                buttonBackground = ResourcesCompat.getDrawable(
                    this.resources, R.drawable.rounded_corner_button_6b2737, null
                )
                buttonTextColor = ResourcesCompat.getColor(this.resources, R.color.whitesmoke, null)
            } else if (partial) {
                buttonText = context().getString(R.string.partial_manual)
                buttonBackground = ResourcesCompat.getDrawable(
                    this.resources, R.drawable.rounded_corner_button_teal, null
                )
                buttonTextColor = ResourcesCompat.getColor(this.resources, R.color.whitesmoke, null)
            }

            if (buttonText.isNotEmpty() && buttonTextColor != null && buttonBackground != null) {
                binding.partialButton.text = buttonText
                binding.partialButton.setTextColor(buttonTextColor)
                binding.partialButton.background = buttonBackground
            }
        }
    }

    private fun setupDivided() {
        runOnUiThread {
            var buttonText = ""
            var buttonBackground: Drawable? = null
            var buttonTextColor: Int? = null

            if (!dividedBlock && !divided) {
                buttonText = context().getString(R.string.without_divider)
                buttonBackground = ResourcesCompat.getDrawable(
                    this.resources, R.drawable.rounded_corner_button_ffed65, null
                )
                buttonTextColor = ResourcesCompat.getColor(this.resources, R.color.black, null)
            } else if (dividedBlock) {
                buttonText = context().getString(R.string.divider_locked)
                buttonBackground = ResourcesCompat.getDrawable(
                    this.resources, R.drawable.rounded_corner_button_6b2737, null
                )
                buttonTextColor = ResourcesCompat.getColor(this.resources, R.color.whitesmoke, null)
            } else if (divided) {
                buttonText = context().getString(R.string.divider)
                buttonBackground = ResourcesCompat.getDrawable(
                    this.resources, R.drawable.rounded_corner_button_teal, null
                )
                buttonTextColor = ResourcesCompat.getColor(this.resources, R.color.whitesmoke, null)
            }

            if (buttonText.isNotEmpty() && buttonTextColor != null && buttonBackground != null) {
                binding.divisorButton.text = buttonText
                binding.divisorButton.setTextColor(buttonTextColor)
                binding.divisorButton.background = buttonBackground
            }
        }
    }

    private fun cancelCount() {
        if ((orcAdapter?.count() ?: 0) <= 0) {
            finish()
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.save_count))
            builder.setMessage(getString(R.string.save_changes_and_continue_later))
            builder.setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            builder.setNegativeButton(R.string.no) { _, _ -> finish() }
            builder.setPositiveButton(R.string.yes) { _, _ ->
                if (processCount(false)) finish() // Salir si no hay error al guardar
            }

            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
    }

    private fun fillOrcAdapter(t: ArrayList<OrderRequestContent>) {
        orcArray = t

        showProgressBar(true)

        runOnUiThread {
            try {
                if (orcAdapter != null) {
                    lastSelected = orcAdapter?.currentOrc()
                    firstVisiblePos = orcAdapter?.firstVisiblePos()
                }

                orcAdapter = OrcAdapter(
                    activity = this,
                    resource = R.layout.orc_row,
                    orcs = orcArray,
                    listView = binding.orcListView,
                    checkedIdArray = checkedIdArray,
                    multiSelect = false,
                    allowEditQty = true,
                    orType = orderRequest?.orderRequestedType
                        ?: OrderRequestType.stockAuditFromDevice,
                    setQtyOnCheckedChanged = false
                )

                orcAdapter?.refreshListeners(
                    checkedChangedListener = null,
                    dataSetChangedListener = this,
                    editQtyListener = this,
                    editDescriptionListener = this
                )

                while (binding.orcListView.adapter == null) {
                    // Horrible wait for full load
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    run {
                        orcAdapter?.setSelectItemAndScrollPos(lastSelected, firstVisiblePos)
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

    private fun fillSummaryRow() {
        runOnUiThread {
            if (orderRequest?.orderRequestedType != OrderRequestType.stockAuditFromDevice) {
                binding.totalLabelTextView.text = context().getString(R.string.total)
                binding.qtyReqLabelTextView.text = context().getString(R.string.cant)
                binding.selectedLabelTextView.visibility = VISIBLE
                binding.selectedLabelTextView.text = context().getString(R.string.checked)

                if (orcAdapter != null) {
                    val cont = orcAdapter!!.qtyRequestedTotal()
                    val t = orcAdapter!!.count
                    binding.totalTextView.text = t.toString()
                    binding.qtyReqTextView.text = Statics.roundToString(cont, 0)
                    binding.selectedTextView.text = orcAdapter!!.countChecked().toString()
                }
            } else {
                binding.totalLabelTextView.text = context().getString(R.string.total)
                binding.qtyReqLabelTextView.text = context().getString(R.string.cont_)
                binding.selectedLabelTextView.visibility = GONE

                if (orcAdapter != null) {
                    val cont = orcAdapter!!.qtyCollectedTotal()
                    val t = orcAdapter!!.count
                    binding.totalTextView.text = t.toString()
                    binding.qtyReqTextView.text = Statics.roundToString(cont, 0)
                }
            }

            if (orcAdapter == null) {
                binding.totalTextView.text = 0.toString()
                binding.qtyReqTextView.text = 0.toString()
                binding.selectedTextView.text = 0.toString()
            }
        }
    }

    private fun setMultiplierButtonText() {
        runOnUiThread {
            binding.multiplierButton.text = String.format(
                context().getString(R.string.multiplier_x), settingViewModel().scanMultiplier
            )
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun removeItem() {
        if (orcAdapter?.currentOrc() == null && (orcAdapter?.countChecked() ?: 0) <= 0) {
            allowClicks = true
            return
        }

        val orcsToRemove =
            if ((orcAdapter?.countChecked() ?: 0) > 0) orcAdapter?.getAllChecked() else arrayListOf(
                orcAdapter?.currentOrc()!!
            )

        var allCodes = ""
        if (orcsToRemove!!.isNotEmpty()) {
            for (w in orcsToRemove) {
                if (w.item != null) {
                    allCodes = "${w.item?.ean}, $allCodes"
                }
            }

            if (allCodes.endsWith(", ")) {
                allCodes = allCodes.substring(0, allCodes.length - 2)
            }

            allCodes = if (orcAdapter!!.countChecked() > 1) {
                String.format(getString(R.string.do_you_want_to_remove_the_items), allCodes)
            } else {
                String.format(getString(R.string.do_you_want_to_remove_the_item), allCodes)
            }
        }

        JotterListener.pauseReaderDevices(this)

        try {
            val adb = AlertDialog.Builder(this)
            adb.setTitle(if (orcsToRemove.count() > 1) R.string.remove_items else R.string.remove_item)
            adb.setMessage(allCodes)
            adb.setNegativeButton(R.string.cancel, null)
            adb.setPositiveButton(R.string.accept) { _, _ ->

                val logContent: ArrayList<LogContent> = ArrayList(log?.content ?: ArrayList())

                for (i in orcsToRemove) {
                    if (i.item != null && i.qty != null) {
                        val itemObj = i.item!!
                        val qtyObj = i.qty!!
                        val lot = i.lot

                        val df = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z")
                        val now = df.format(Calendar.getInstance().time)

                        val variationQty = qtyObj.qtyCollected!! * -1
                        if (variationQty != 0.toDouble()) {
                            logContent.add(
                                LogContent(
                                    itemId = itemObj.itemId,
                                    itemStr = itemObj.itemDescription,
                                    itemCode = itemObj.ean,
                                    lotId = lot?.lotId,
                                    lotCode = lot?.code ?: "",
                                    scannedCode = itemObj.codeRead,
                                    variationQty = variationQty,
                                    finalQty = 0.toDouble(),
                                    date = now
                                )
                            )
                        }
                    }
                }

                log?.content = logContent

                orcAdapter!!.remove(orcsToRemove)
            }
            adb.show()
        } catch (ex: java.lang.Exception) {
            ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())
        }

        JotterListener.resumeReaderDevices(this)
        allowClicks = true
    }

    private fun selectItemDialog() {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val intent = Intent(context(), ItemSelectActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        intent.putExtra("title", context().getString(R.string.select_item))
        intent.putExtra("multiSelect", false)
        resultForItemSelect.launch(intent)
    }

    private val resultForItemSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val items: ArrayList<Int> =
                        data.getIntegerArrayListExtra("ids") ?: return@registerForActivityResult

                    if (items.isEmpty()) return@registerForActivityResult

                    ItemCoroutines().getById(items.first().toLong()) { it2 ->
                        if (it2 == null) return@getById

                        try {
                            addOrc(OrderRequestContent().apply {
                                item = fromItemRoom(it2)
                                lot = null
                                qty = Qty().apply {
                                    qtyCollected = 0.toDouble()
                                    qtyRequested = 0.toDouble()
                                }
                            })
                        } catch (ex: Exception) {
                            val res =
                                context().getString(R.string.an_error_occurred_while_trying_to_add_the_item)
                            makeText(binding.root, res, ERROR)
                            android.util.Log.e(this::class.java.simpleName, res)
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

    private fun finishCountDialog() {
        JotterListener.lockScanner(this, true)

        val intent = Intent(this, OrderRequestConfirmActivity::class.java)

        intent.putExtra("orderRequest", Parcels.wrap<OrderRequest>(orderRequest))
        intent.putParcelableArrayListExtra("orcArray", orcAdapter?.getAll() ?: ArrayList())
        resultForFinishCount.launch(intent)
    }

    private val resultForFinishCount =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    when (Parcels.unwrap<ConfirmStatus>(data.getParcelableExtra("confirmStatus"))) {
                        modify -> {
                            if (processCount(false)) finish()
                        }
                        confirm -> {
                            if (processCount(true)) finish()
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

    // Flag que guarda el estado "completed" en caso de que
// se requiran permisos de escritura y se tenga que retomar la
// actividad después de que el usuario los otorgue.
    private var tempCompleted = false

    private fun processCount(completed: Boolean): Boolean {
        tempCompleted = completed
        val tOrderRequest = orderRequest ?: return false
        var error = false

        // Preparar el conteo
        tOrderRequest.content = orcAdapter!!.getAll()
        tOrderRequest.completed = tempCompleted
        tOrderRequest.finishDate = if (tempCompleted) DateFormat.format(
            "yyyy-MM-dd hh:mm:ss", System.currentTimeMillis()
        ).toString()
        else ""

        if (tOrderRequest.filename.isEmpty()) {
            val df = SimpleDateFormat("yyMMddHHmmssZ", Locale.getDefault())
            tOrderRequest.filename =
                String.format("%s.json", df.format(Calendar.getInstance().time))
        }
        tOrderRequest.log = log ?: Log()

        setImagesJson()

        try {
            orJson = moshi().adapter(OrderRequest::class.java).toJson(tOrderRequest)
            android.util.Log.i(this::class.java.simpleName, orJson)
            orFileName = tOrderRequest.filename.substringAfterLast('/')

            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                error = !Statics.writeJsonToFile(
                    v = binding.root,
                    filename = orFileName,
                    value = orJson,
                    completed = tempCompleted
                )
                if (!error) finish()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_EXTERNAL_STORAGE
                )
            }
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            android.util.Log.e(this::class.java.simpleName, e.message ?: "")
            error = true
        }

        return !error
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

        val intent = Intent(context(), LogContentActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        intent.putExtra("title", context().getString(R.string.count_log))
        intent.putExtra("log", log)
        intent.putExtra("logContent", ArrayList(log?.content ?: ArrayList()))
        resultForLog.launch(intent)
    }

    private val resultForLog =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            gentlyReturn()
        }

    private fun manualAddItem() {
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

    private fun setQtyManually(orc: OrderRequestContent) {
        val multi = if (itemCode == null) settingViewModel().scanMultiplier
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
            if (Statics.demoMode) {
                if ((orcAdapter?.count ?: 0) >= 5) {
                    val res =
                        context().getString(R.string.maximum_amount_of_demonstration_mode_reached)
                    makeText(binding.root, res, ERROR)
                    android.util.Log.e(this::class.java.simpleName, res)
                    return false
                }
            }

            // Nada que hacer, volver
            if (tempOrc == null) {
                return false
            }

            // Buscar primero en el adaptador de la lista
            for (a in 0 until (orcAdapter?.count ?: 0)) {
                val orc = orcAdapter?.getItem(a)
                if (orc != null && orc.item?.itemId == tempOrc.item?.itemId) {
                    setQtyCollected(
                        orc = orc, qty = tempOrc.qty?.qtyCollected ?: 0.toDouble(), manualAdd = true
                    )
                    return true
                }
            }

            // No está en la lista, agregar al adaptador con cantidades 0, después se actualiza y se agrega al Log
            orcAdapter?.add(arrayListOf(tempOrc))

            // Esta pausa permite que el Adapter se actualice a tiempo
            Handler(mainLooper).postDelayed({
                setQty(tempOrc)
            }, 250)

            return true
        } catch (ex: Exception) {
            makeText(binding.root, ex.message.toString(), ERROR)
            android.util.Log.e(this::class.java.simpleName, ex.message.toString())
            return false
        }
    }

    private fun showDialogForItemDescription(orc: OrderRequestContent) {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val intent = Intent(context(), EnterCodeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        intent.putExtra("title", context().getString(R.string.enter_description))
        intent.putExtra("hint", context().getString(R.string.item_description_hint))
        intent.putExtra("orc", orc)
        resultForEnterDescription.launch(intent)
    }

    private val resultForEnterDescription =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (data != null) {
                    val description = data.getStringExtra("code") ?: ""
                    val orc = Parcels.unwrap<OrderRequestContent>(data.getParcelableExtra("orc"))

                    val item = orc.item ?: return@registerForActivityResult

                    if (it.resultCode == RESULT_OK) {
                        if (description.trim()
                                .isEmpty() && binding.requiredDescCheckBox.isChecked
                        ) {
                            orcAdapter?.remove(orc)
                        } else {
                            setDescription(orc, description)
                        }
                    } else {
                        if ((item.itemDescription.isEmpty() || item.itemDescription == context().getString(
                                R.string.unknown_item
                            )) && binding.requiredDescCheckBox.isChecked
                        ) {
                            orcAdapter?.remove(orc)
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

        val intent = Intent(context(), EnterCodeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        intent.putExtra("title", context().getString(R.string.enter_description))
        intent.putExtra("hint", context().getString(R.string.item_description_hint))
        intent.putExtra("orc", orc)
        resultForAddDescription.launch(intent)
    }

    private val resultForAddDescription =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (data != null) {
                    val description = data.getStringExtra("code") ?: ""
                    val orc = Parcels.unwrap<OrderRequestContent>(data.getParcelableExtra("orc"))

                    val item = orc.item ?: return@registerForActivityResult

                    if (it.resultCode == RESULT_OK) {
                        if (description.trim()
                                .isEmpty() && binding.requiredDescCheckBox.isChecked
                        ) {
                            orcAdapter?.remove(orc)
                        } else {
                            setDescription(orc, description)
                            setQty(orc)
                        }
                    } else {
                        if ((item.itemDescription.isEmpty() || item.itemDescription == context().getString(
                                R.string.unknown_item
                            )) && binding.requiredDescCheckBox.isChecked
                        ) {
                            orcAdapter?.remove(orc)
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

        val intent = Intent(context(), EnterCodeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        intent.putExtra("title", context().getString(R.string.enter_lot_code))
        intent.putExtra("hint", context().getString(R.string.lot_code))
        intent.putExtra("orc", orc)
        resultForLotCodeSelect.launch(intent)
    }

    private val resultForLotCodeSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val code = data.getStringExtra("code") ?: ""
                    val orc = Parcels.unwrap<OrderRequestContent>(data.getParcelableExtra("orc"))
                    setLotCode(orc, code)
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

    public override fun onStart() {
        super.onStart()

        thread { fillOrcAdapter(orcArray) }
        gentlyReturn()
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

        intent.putExtra("orderRequestContent", Parcels.wrap(orc))
        intent.putExtra("partial", (partial || partialBlock))

        intent.putExtra("initialValue", initialQty)
        intent.putExtra("multiplier", multiplier.toLong())
        intent.putExtra("minValue", minValue)
        intent.putExtra("maxValue", maxValue)
        if (total) resultForTotalSelect.launch(intent)
        else resultForPartialSelect.launch(intent)
    }

    private val resultForTotalSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val qty = data.getDoubleExtra("qty", 0.toDouble())
                    val orc =
                        Parcels.unwrap<OrderRequestContent>(data.getParcelableExtra("orderRequestContent"))

                    // La descripción puede modificarse desde el selector de cantidades
                    setDescription(
                        orc = orc, description = orc.item?.itemDescription ?: "", updateDb = false
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
                    val qty = data.getDoubleExtra("qty", 0.toDouble())
                    val orc =
                        Parcels.unwrap<OrderRequestContent>(data.getParcelableExtra("orderRequestContent"))

                    // La descripción puede modificarse desde el selector de cantidades
                    setDescription(
                        orc = orc, description = orc.item?.itemDescription ?: "", updateDb = false
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

        val multiplier = settingViewModel().scanMultiplier
        val intent = Intent(context(), MultiplierSelectActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra("title", context().getString(R.string.select_multiplier))
        intent.putExtra("multiplier", multiplier)
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

    private fun setLotCode(orc: OrderRequestContent, lotId: String) {
        val lotCode = orc.item?.codeRead ?: ""
        if (lotCode == "") return

        val longLotId: Long
        try {
            longLotId = lotId.toLong()
        } catch (ex: Exception) {
            return
        }

        if (longLotId < 0) return

        for (i in 0 until (orcAdapter?.count ?: 0)) {
            val t = orcAdapter?.getItem(i)
            if (t?.lot != null) {
                if (equals(t.lot?.lotId, longLotId)) {
                    // Encontrado, volver!
                    return
                }
            }
        }

        val lot = Lot(longLotId, lotCode, true)

        for (i in 0 until (orcAdapter?.count ?: 0)) {
            val t = orcAdapter?.getItem(i)
            if (t?.item != null) {
                if (equals(t.item, orc.item)) {
                    t.lot = lot
                    break
                }
            }
        }
    }

    private fun setDescription(
        orc: OrderRequestContent,
        description: String,
        updateDb: Boolean = true,
    ) {
        val item = orc.item ?: return
        if (updateDb) {
            val itemId = item.itemId ?: return
            ItemCoroutines().updateDescription(itemId, description) {
                orcAdapter?.updateDescription(itemId, description)
            }
        } else {
            orcAdapter?.updateDescription(item, description)
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
                    null -> settingViewModel().scanMultiplier.toLong()
                    else -> itemCode?.qty?.toLong() ?: 0
                }
            }
        }

        val initialQty = orc.qty?.qtyCollected ?: 0.0
        val logContent: ArrayList<LogContent> = ArrayList(log?.content ?: ArrayList())

        for (i in 0 until (orcAdapter?.count ?: 0)) {
            val tempOrc = orcAdapter?.getItem(i)
            if (tempOrc != null && tempOrc == orc) {
                if (tempOrc.item != null && tempOrc.qty != null) {
                    val itemObj = tempOrc.item ?: return
                    val qtyObj = tempOrc.qty ?: return
                    val lot = tempOrc.lot

                    val tempQty: Double = if (!total) {
                        qty * multi + qtyObj.qtyCollected!!
                    } else {
                        qty * multi
                    }

                    if (tempQty <= 0) {
                        removeItem()
                        lastRegexResult = null
                        return
                    }

                    orcAdapter?.updateQtyCollected(tempOrc, tempQty)

                    // Unless is blocked, unlock the partial
                    if (!partialBlock) {
                        partial = false
                        partialBlock = false
                        setupPartial()
                    }

                    val df = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z")
                    val now = df.format(Calendar.getInstance().time)

                    val variationQty = (qtyObj.qtyCollected ?: 0.0) - initialQty
                    if (variationQty != 0.toDouble()) {
                        logContent.add(
                            LogContent(
                                itemId = itemObj.itemId,
                                itemStr = itemObj.itemDescription,
                                itemCode = itemObj.ean,
                                lotId = lot?.lotId,
                                lotCode = lot?.code ?: "",
                                scannedCode = itemObj.codeRead,
                                variationQty = variationQty,
                                finalQty = qtyObj.qtyCollected,
                                date = now
                            )
                        )
                    }

                    checkLotEnabled(tempOrc)
                    break
                }
            }
        }

        log?.content = logContent
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
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        makeText(
                            this, getString(R.string.cannot_write_images_to_external_storage), ERROR
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
        if (settingViewModel().showScannedCode) makeText(binding.root, scanCode, INFO)

        JotterListener.lockScanner(this, true)

        var code: String

        ItemRegex.tryToRegex(scanCode) { it ->
            if (!it.any()) {
                // No coincide con los Regex de la DB, lo
                // usamos como un código corriente.
                code = scanCode
            } else {
                // region Regex Founded
                if (it.count() > 1) {
                    // Mostrar advertencia.
                    makeText(this, getString(R.string.there_are_multiple_regex_matches), INFO)
                }

                // Utilizamos la primer coincidencia
                lastRegexResult = it.first()

                // Si la cantidad no es NULL procesiguimos con el Regex
                if (lastRegexResult!!.qty != null) {
                    CheckCode(callback = { onCheckCodeEnded(it) },
                        scannedCode = lastRegexResult!!.ean,
                        adapter = orcAdapter!!,
                        onEventData = { showSnackBar(it) }).execute()

                    return@tryToRegex
                }

                // Qty NULL ->
                //    Actualizar scanned code y proseguir como
                //    un código corriente pero advertir sobre el error.

                // Mostrar advertencia.
                makeText(this, "Cantidad nula en Regex", INFO)
                code = lastRegexResult!!.ean

                // endregion Regex Founded
            }

            if (divided) {
                val splitCode = scanCode.split(settingViewModel().divisionChar.toRegex())
                    .dropLastWhile { it2 -> it2.isEmpty() }
                if (splitCode.any()) {
                    code = splitCode.toTypedArray()[0]
                }
            }

            CheckCode(callback = { onCheckCodeEnded(it) },
                scannedCode = code,
                adapter = orcAdapter!!,
                onEventData = { it2 -> showSnackBar(it2) }).execute()

            gentlyReturn()
        }
    }

    private fun showSnackBar(it: SnackBarEventData) {
        makeText(binding.root, it.text, it.snackBarType)
    }

    private fun gentlyReturn() {
        // Unless is blocked, unlock the partial
        if (!dividedBlock) {
            divided = false
            dividedBlock = false
            setupDivided()
        }

        closeKeyboard(this)
        allowClicks = true
        JotterListener.lockScanner(this, false)
        rejectNewInstances = false
    }

    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 3001

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }

        private var orFileName = ""
        private var orJson = ""
    }
}