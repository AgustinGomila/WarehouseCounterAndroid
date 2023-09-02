package com.dacosys.warehouseCounter.ui.activities.orderRequest

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestContent
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.data.room.dao.orderRequest.OrderRequestCoroutines
import com.dacosys.warehouseCounter.data.room.entity.client.Client
import com.dacosys.warehouseCounter.databinding.OrderRequestDetailActivityBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.decimalPlaces
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.adapter.orderRequest.OrcAdapter
import com.dacosys.warehouseCounter.ui.fragments.orderRequest.OrderRequestHeader
import com.dacosys.warehouseCounter.ui.utils.Screen
import org.parceler.Parcels

class OrderRequestDetailActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    OrcAdapter.DataSetChangedListener {

    private var panelIsExpanded = true

    private var tempTitle: String = ""

    // Header que depende del tipo de arqueo
    private var headerFragment: androidx.fragment.app.Fragment? = null

    // Adaptador, colección de ítems, fila seleccionada
    private var adapter: OrcAdapter? = null
    private var lastSelected: OrderRequestContent? = null
    private var checkedIdArray: ArrayList<Long> = ArrayList()
    private var firstVisiblePos: Int? = null
    private var currentScrollPosition: Int = 0

    // Cliente
    private var client: Client? = null

    // OrderRequest
    private var orderRequestId: Long = 0L
    private lateinit var orderRequest: OrderRequest

    // Lista completa
    private var completeList: ArrayList<OrderRequestContent> = ArrayList()

    private var rejectNewInstances = false

    // Se usa para saber si estamos en onStart luego de onCreate
    private var fillRequired = false

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        adapter?.refreshListeners(null, null, null, null)
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshOrc.isRefreshing = false
        }, 100)
    }

    override fun onResume() {
        super.onResume()
        rejectNewInstances = false
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putBoolean("panelTopIsExpanded", panelIsExpanded)

        if (adapter != null) {
            savedInstanceState.putParcelable("lastSelected", adapter?.currentItem())
            savedInstanceState.putInt("firstVisiblePos", adapter?.firstVisiblePos() ?: RecyclerView.NO_POSITION)
            savedInstanceState.putInt("currentScrollPosition", currentScrollPosition)
        }
        savedInstanceState.putLong(ARG_ID, orderRequestId)
        savedInstanceState.putParcelable(ARG_CLIENT, client)
    }

    private fun loadExtrasBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isEmpty()) tempTitle = getString(R.string.detail_count)

        client = Parcels.unwrap<Client>(b.getParcelable(ARG_CLIENT))
        orderRequestId = b.getLong(ARG_ID)
    }

    private fun loadBundleValues(b: Bundle) {
        tempTitle = b.getString(ARG_TITLE) ?: ""
        if (tempTitle.isNotEmpty()) tempTitle = getString(R.string.detail_count)

        orderRequestId = b.getLong(ARG_ID)
        client = b.getParcelable(ARG_CLIENT)
        checkedIdArray = (b.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(ArrayList())
        lastSelected = b.getParcelable("lastSelected")
        firstVisiblePos =
            if (b.containsKey("firstVisiblePos")) b.getInt("firstVisiblePos")
            else -1
        currentScrollPosition = b.getInt("currentScrollPosition")
        panelIsExpanded = b.getBoolean("panelIsExpanded")
    }

    private lateinit var binding: OrderRequestDetailActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = OrderRequestDetailActivityBinding.inflate(layoutInflater)
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

        if (savedInstanceState != null) {
            loadBundleValues(savedInstanceState)
        } else {
            // Inicializar la actividad
            val extras = intent.extras
            if (extras != null) loadExtrasBundleValues(extras)
        }

        binding.topAppbar.title = tempTitle

        binding.swipeRefreshOrc.setOnRefreshListener(this)
        binding.swipeRefreshOrc.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Para expandir y colapsar el panel inferior
        setPanelAnimation()

        showProgressBar(false)

        // ESTO SIRVE PARA OCULTAR EL TECLADO EN PANTALLA CUANDO PIERDEN EL FOCO LOS CONTROLES QUE LO NECESITAN
        Screen.setupUI(binding.root, this)
    }

    override fun onStart() {
        super.onStart()

        setPanels()

        if (fillRequired) {
            fillRequired = false

            loadOrderRequest()
        }
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

    private fun fillHeader() {
        runOnUiThread {
            binding.countTypeActionTextView.text = when (orderRequest.orderRequestType) {
                OrderRequestType.deliveryAudit -> OrderRequestType.deliveryAudit.description
                OrderRequestType.prepareOrder -> OrderRequestType.prepareOrder.description
                OrderRequestType.receptionAudit -> OrderRequestType.receptionAudit.description
                OrderRequestType.stockAudit -> OrderRequestType.stockAudit.description
                OrderRequestType.stockAuditFromDevice -> OrderRequestType.stockAuditFromDevice.description
                OrderRequestType.packaging -> OrderRequestType.packaging.description
                else -> getString(R.string.counted)
            }
        }
    }

    /**
     * Cambia el fragment que aparece en la parte superior de la pantalla
     * dependiendo del tipo de conteo que se está haciendo.
     */
    private fun setHeaderFragment() {
        if (adapter == null) return

        val orType = orderRequest.orderRequestType
        val newFragment: androidx.fragment.app.Fragment =
            if (orType == OrderRequestType.deliveryAudit ||
                orType == OrderRequestType.prepareOrder ||
                orType == OrderRequestType.stockAudit ||
                orType == OrderRequestType.receptionAudit ||
                orType == OrderRequestType.stockAuditFromDevice ||
                orType == OrderRequestType.packaging
            ) {
                OrderRequestHeader.newInstance(orderRequestId)
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

    private val requiredLayout: Int
        get() {
            val orientation = resources.configuration.orientation
            return if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (panelIsExpanded) layoutPanelCollapsed
                else layoutPanelExpanded
            } else layoutPanelExpanded
        }

    private val layoutPanelExpanded: Int
        get() {
            return R.layout.order_request_detail_activity
        }

    private val layoutPanelCollapsed: Int
        get() {
            return R.layout.order_request_detail_activity_top_panel_collapsed
        }


    private fun setPanels() {
        runOnUiThread {
            val currentLayout = ConstraintSet()
            currentLayout.load(this, requiredLayout)
            currentLayout.applyTo(binding.root)

            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (panelIsExpanded) binding.expandTopPanelButton.text = getString(R.string.collapse_panel)
                else binding.expandTopPanelButton.text = getString(R.string.expand_panel)
            }
        }
    }

    private fun setPanelAnimation() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) return

        binding.expandTopPanelButton.setOnClickListener {
            panelIsExpanded = !panelIsExpanded
            val nextLayout = ConstraintSet()
            nextLayout.load(this, requiredLayout)

            val transition = ChangeBounds()
            transition.interpolator = FastOutSlowInInterpolator()
            transition.addListener(object : Transition.TransitionListener {
                override fun onTransitionResume(transition: Transition?) {}
                override fun onTransitionPause(transition: Transition?) {}
                override fun onTransitionStart(transition: Transition?) {}
                override fun onTransitionEnd(transition: Transition?) {}
                override fun onTransitionCancel(transition: Transition?) {}
            })

            TransitionManager.beginDelayedTransition(binding.root, transition)
            nextLayout.applyTo(binding.root)

            if (panelIsExpanded) binding.expandTopPanelButton.text = getString(R.string.collapse_panel)
            else binding.expandTopPanelButton.text = getString(R.string.expand_panel)
        }
    }


    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshOrc.isRefreshing = show
        }, 20)
    }

    private val countItems: Int
        get() {
            return adapter?.itemCount ?: 0
        }

    private val totalCollected: Double
        get() {
            return adapter?.qtyCollectedTotal() ?: 0.toDouble()
        }

    private val totalRequested: Double
        get() {
            return adapter?.qtyRequestedTotal() ?: 0.toDouble()
        }

    private fun setCountedTextBox() {
        runOnUiThread {
            binding.countedTextView.text = String.format(
                "%s %s (%s %s %s %s %s)",
                countItems,
                if (countItems > 1) getString(R.string.items)
                else getString(R.string.item),
                Statics.roundToString(totalCollected, decimalPlaces),
                if (totalCollected > 1) getString(R.string.counted_plural)
                else getString(R.string.counted),
                getString(R.string.of),
                Statics.roundToString(totalRequested, decimalPlaces),
                if (totalRequested > 1) getString(R.string.requested_plural)
                else getString(R.string.requested)
            )
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
                    lastSelected = adapter?.currentItem()
                }

                val filteredList = ArrayList(completeList.indices
                    .filter {
                        (completeList[it].qtyCollected ?: 0.0) < (completeList[it].qtyRequested ?: 0.0) ||
                                (completeList[it].qtyCollected ?: 0.0) > (completeList[it].qtyRequested
                            ?: 0.0)
                    }
                    .map { completeList[it] })

                adapter = OrcAdapter.Builder()
                    .recyclerView(binding.recyclerView)
                    .fullList(filteredList)
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
            }
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
            setHeaderFragment()
            setCountedTextBox()
        }, 100)
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_ID = "id"
        const val ARG_CLIENT = "client"

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}
