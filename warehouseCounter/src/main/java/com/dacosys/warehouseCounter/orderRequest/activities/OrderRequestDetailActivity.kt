package com.dacosys.warehouseCounter.orderRequest.activities

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.Statics.Companion.decimalPlaces
import com.dacosys.warehouseCounter.client.`object`.Client
import com.dacosys.warehouseCounter.databinding.OrderRequestDetailActivityBinding
import com.dacosys.warehouseCounter.errorLog.ErrorLog
import com.dacosys.warehouseCounter.orderRequest.`object`.OrderRequest
import com.dacosys.warehouseCounter.orderRequest.`object`.OrderRequestContent
import com.dacosys.warehouseCounter.orderRequest.`object`.OrderRequestType
import com.dacosys.warehouseCounter.orderRequest.dbHelper.OrcAdapter
import com.dacosys.warehouseCounter.orderRequest.fragments.OrderRequestHeader
import org.parceler.Parcels

class OrderRequestDetailActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    OrcAdapter.DataSetChangedListener {

    private var panelTopIsExpanded = true

    // Header que depende del tipo de countimiento
    private var headerFragment: androidx.fragment.app.Fragment? = null

    // Adaptador, colección de ítems, fila seleccionada
    private var orcAdapter: OrcAdapter? = null
    private var lastSelected: OrderRequestContent? = null
    private var checkedIdArray: ArrayList<Int> = ArrayList()
    private var firstVisiblePos: Int? = null

    // Cliente
    private var client: Client? = null

    // OrderRequest
    private var orderRequest: OrderRequest? = null

    // Lista completa
    private var orcArray: ArrayList<OrderRequestContent> = ArrayList()

    private var rejectNewInstances = false

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        orcAdapter?.refreshListeners(null, null, null, null)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI(view: View) {
        // Set up touch listener for non-text box views to hide keyboard.
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
            (0 until view.childCount)
                .map { view.getChildAt(it) }
                .forEach { setupUI(it) }
        }
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshOrc.isRefreshing = false
            }
        }, 100)
    }

    override fun onResume() {
        super.onResume()

        rejectNewInstances = false
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        if (orcAdapter != null) {
            savedInstanceState.putParcelable("lastSelected", (orcAdapter ?: return).currentOrc())
            savedInstanceState.putInt("firstVisiblePos", (orcAdapter ?: return).firstVisiblePos())
            savedInstanceState.putParcelableArrayList("orcArray", orcAdapter?.getAll())
            savedInstanceState.putIntegerArrayList(
                "checkedIdArray",
                orcAdapter!!.getAllCheckedAsInt()
            )
        }
        savedInstanceState.putParcelable("client", client)
        savedInstanceState.putParcelable("orderRequest", orderRequest)
    }

    private lateinit var binding: OrderRequestDetailActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Statics.setScreenRotation(this)
        binding = OrderRequestDetailActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var tempTitle = getString(R.string.detail_count)

        if (savedInstanceState != null) {
            // region Recuperar el título de la ventana
            val t2 = savedInstanceState.getString("title")
            if (t2 != null && t2.isNotEmpty()) tempTitle = t2
            // endregion

            client = savedInstanceState.getParcelable("client")
            orderRequest = savedInstanceState.getParcelable("orderRequest")
            orcArray =
                savedInstanceState.getParcelableArrayList<OrderRequestContent>("orcArray") as ArrayList<OrderRequestContent>
            checkedIdArray = savedInstanceState.getIntegerArrayList("checkedIdArray") ?: ArrayList()
            lastSelected = savedInstanceState.getParcelable("lastSelected")
            firstVisiblePos =
                if (savedInstanceState.containsKey("firstVisiblePos")) savedInstanceState.getInt("firstVisiblePos") else -1
        } else {
            // Nueva instancia de la actividad

            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString("title")
                if (t1 != null && t1.isNotEmpty()) tempTitle = t1

                orderRequest =
                    Parcels.unwrap<OrderRequest>(extras.getParcelable("orderRequest"))
                client = Parcels.unwrap<Client>(extras.getParcelable("client"))
                val t2 = extras.getParcelableArrayList<OrderRequestContent>("orcArray")
                if (t2 != null) orcArray = t2
            }
        }

        title = tempTitle

        binding.countTypeActionTextView.text = when (orderRequest!!.orderRequestedType) {
            OrderRequestType.deliveryAudit -> OrderRequestType.deliveryAudit.description
            OrderRequestType.prepareOrder -> OrderRequestType.prepareOrder.description
            OrderRequestType.receptionAudit -> OrderRequestType.receptionAudit.description
            OrderRequestType.stockAudit -> OrderRequestType.stockAudit.description
            OrderRequestType.stockAuditFromDevice -> OrderRequestType.stockAuditFromDevice.description
            else -> getString(R.string.counted)
        }

        binding.swipeRefreshOrc.setOnRefreshListener(this)
        binding.swipeRefreshOrc.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Para expandir y colapsar el panel inferior
        setTopPanelAnimation()

        fillOrcAdapter(orcArray)

        // ESTO SIRVE PARA OCULTAR EL TECLADO EN PANTALLA CUANDO PIERDEN EL FOCO LOS CONTROLES QUE LO NECESITAN
        setupUI(binding.orderRequestContentDetail)

        showProgressBar(false)
    }

    /**
     * Cambia el fragment que aparece en la parte superior de la pantalla
     * dependiendo del tipo de conteo que se está haciendo.
     */
    private fun setHeaderFragment() {
        if (orderRequest == null || orcAdapter == null) {
            return
        }

        val orType = orderRequest?.orderRequestedType
        val newFragment: androidx.fragment.app.Fragment =
            if (orType == OrderRequestType.deliveryAudit ||
                orType == OrderRequestType.prepareOrder ||
                orType == OrderRequestType.stockAudit ||
                orType == OrderRequestType.receptionAudit ||
                orType == OrderRequestType.stockAuditFromDevice
            ) {
                OrderRequestHeader.newInstance(orderRequest, orcAdapter!!.getAll())
            } else {
                return
            }

        var oldFragment: androidx.fragment.app.Fragment? = null
        if (headerFragment != null) {
            oldFragment = headerFragment!!
        }

        var fragmentTransaction = supportFragmentManager.beginTransaction()
        if (oldFragment != null) {
            fragmentTransaction.remove(oldFragment).commitAllowingStateLoss()
        }

        // Close keyboard in transition
        if (currentFocus != null) {
            val inputManager = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(
                currentFocus!!.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }

        runOnUiThread {
            fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.setCustomAnimations(
                R.anim.animation_fade_in,
                R.anim.animation_fade_in
            )
            fragmentTransaction.replace(binding.panelHeader.id, newFragment)
                .commitAllowingStateLoss()
        }

        headerFragment = newFragment
    }

    private fun setTopPanelAnimation() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            return
        }

        binding.expandTopPanelButton.setOnClickListener {
            val nextLayout = ConstraintSet()
            if (panelTopIsExpanded) {
                nextLayout.load(this, R.layout.order_request_detail_activity_top_panel_collapsed)
            } else {
                nextLayout.load(this, R.layout.order_request_detail_activity)
            }

            panelTopIsExpanded = !panelTopIsExpanded

            val transition = ChangeBounds()
            transition.interpolator = FastOutSlowInInterpolator()
            transition.addListener(object : Transition.TransitionListener {
                override fun onTransitionResume(transition: Transition?) {}
                override fun onTransitionPause(transition: Transition?) {}
                override fun onTransitionStart(transition: Transition?) {}
                override fun onTransitionEnd(transition: Transition?) {}
                override fun onTransitionCancel(transition: Transition?) {}
            })

            TransitionManager.beginDelayedTransition(
                binding.orderRequestContentDetail,
                transition
            )

            when {
                panelTopIsExpanded -> {
                    binding.expandTopPanelButton.text = getString(R.string.collapse_panel)
                }
                else -> {
                    binding.expandTopPanelButton.text = getString(R.string.expand_panel)
                }
            }

            nextLayout.applyTo(binding.orderRequestContentDetail)
        }

        when {
            panelTopIsExpanded -> {
                binding.expandTopPanelButton.text = getString(R.string.collapse_panel)
            }
            else -> {
                binding.expandTopPanelButton.text = getString(R.string.expand_panel)
            }
        }
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshOrc.isRefreshing = show
            }
        }, 20)
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

    private val countItems: Int
        get() {
            return orcAdapter?.count() ?: 0
        }

    private val totalCollected: Double
        get() {
            return orcAdapter?.totalCollected() ?: 0.toDouble()
        }

    private val totalRequested: Double
        get() {
            return orcAdapter?.totalRequested() ?: 0.toDouble()
        }

    private fun setCountedTextBox() {
        runOnUiThread {
            binding.countedTextView.text = String.format(
                "%s %s (%s %s %s %s %s)",
                countItems,
                if (countItems > 1)
                    getString(R.string.items)
                else
                    getString(R.string.item),
                Statics.roundToString(totalCollected, decimalPlaces),
                if (totalCollected > 1)
                    getString(R.string.counted_plural)
                else
                    getString(R.string.counted),
                getString(R.string.of),
                Statics.roundToString(totalRequested, decimalPlaces),
                if (totalRequested > 1)
                    getString(R.string.requested_plural)
                else
                    getString(R.string.requested)
            )
        }
    }

    private fun fillOrcAdapter(t: ArrayList<OrderRequestContent>) {
        orcArray = t

        showProgressBar(true)

        runOnUiThread {
            try {
                if (orcAdapter != null) {
                    lastSelected = (orcAdapter ?: return@runOnUiThread).currentOrc()
                    firstVisiblePos = (orcAdapter ?: return@runOnUiThread).firstVisiblePos()
                }

                val orcDiffArray = orcArray.indices
                    .filter {
                        orcArray[it].qty!!.qtyCollected!! < orcArray[it].qty!!.qtyRequested!! ||
                                orcArray[it].qty!!.qtyCollected!! > orcArray[it].qty!!.qtyRequested!!
                    }
                    .map { orcArray[it] }

                orcAdapter = OrcAdapter(
                    activity = this,
                    resource = R.layout.orc_row,
                    orcs = orcDiffArray as ArrayList<OrderRequestContent>,
                    listView = binding.orcListView,
                    checkedIdArray = checkedIdArray,
                    multiSelect = false,
                    allowEditQty = false,
                    orType = orderRequest?.orderRequestedType
                        ?: OrderRequestType.stockAuditFromDevice,
                    setQtyOnCheckedChanged = false
                )

                orcAdapter?.refreshListeners(
                    checkedChangedListener = null,
                    dataSetChangedListener = this,
                    editQtyListener = null,
                    editDescriptionListener = null
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

    companion object {
        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == R.id.home || id == android.R.id.home) {
            onBackPressed()
            true
        } else super.onOptionsItemSelected(item)
    }

    override fun onDataSetChanged() {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                setHeaderFragment()
                setCountedTextBox()
            }
        }, 100)
    }
}