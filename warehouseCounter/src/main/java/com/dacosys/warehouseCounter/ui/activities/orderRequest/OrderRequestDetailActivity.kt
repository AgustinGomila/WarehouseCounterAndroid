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
import com.dacosys.warehouseCounter.databinding.OrderRequestDetailActivityBinding
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequestContent
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.decimalPlaces
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.room.dao.orderRequest.OrderRequestCoroutines
import com.dacosys.warehouseCounter.room.entity.client.Client
import com.dacosys.warehouseCounter.ui.adapter.orderRequest.OrcAdapter
import com.dacosys.warehouseCounter.ui.fragments.orderRequest.OrderRequestHeader
import com.dacosys.warehouseCounter.ui.utils.Screen
import org.parceler.Parcels

class OrderRequestDetailActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    OrcAdapter.DataSetChangedListener {

    private var panelTopIsExpanded = true

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

        if (adapter != null) {
            savedInstanceState.putParcelable("lastSelected", adapter?.currentItem())
            savedInstanceState.putInt("firstVisiblePos", adapter?.firstVisiblePos() ?: RecyclerView.NO_POSITION)
            savedInstanceState.putInt("currentScrollPosition", currentScrollPosition)
        }
        savedInstanceState.putLong(ARG_ID, orderRequestId)
        savedInstanceState.putParcelable(ARG_CLIENT, client)
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

        var tempTitle = getString(R.string.detail_count)

        if (savedInstanceState != null) {
            // region Recuperar el título de la ventana
            val t2 = savedInstanceState.getString(ARG_TITLE)
            if (!t2.isNullOrEmpty()) tempTitle = t2
            // endregion

            orderRequestId = savedInstanceState.getLong(ARG_ID)
            client = savedInstanceState.getParcelable(ARG_CLIENT)
            checkedIdArray =
                (savedInstanceState.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(ArrayList())
            lastSelected = savedInstanceState.getParcelable("lastSelected")
            firstVisiblePos =
                if (savedInstanceState.containsKey("firstVisiblePos")) savedInstanceState.getInt("firstVisiblePos")
                else -1
            currentScrollPosition = savedInstanceState.getInt("currentScrollPosition")
        } else {
            // Nueva instancia de la actividad

            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString(ARG_TITLE)
                if (!t1.isNullOrEmpty()) tempTitle = t1

                client = Parcels.unwrap<Client>(extras.getParcelable(ARG_CLIENT))
                orderRequestId = extras.getLong(ARG_ID)
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
        setTopPanelAnimation()

        // ESTO SIRVE PARA OCULTAR EL TECLADO EN PANTALLA CUANDO PIERDEN EL FOCO LOS CONTROLES QUE LO NECESITAN
        Screen.setupUI(binding.orderRequestContentDetail, this)

        showProgressBar(false)
    }

    override fun onStart() {
        super.onStart()

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
                orType == OrderRequestType.stockAuditFromDevice
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

    private fun setTopPanelAnimation() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) return

        binding.expandTopPanelButton.setOnClickListener {
            val nextLayout = ConstraintSet()
            if (panelTopIsExpanded) nextLayout.load(this, R.layout.order_request_detail_activity_top_panel_collapsed)
            else nextLayout.load(this, R.layout.order_request_detail_activity)

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

            if (panelTopIsExpanded) binding.expandTopPanelButton.text = getString(R.string.collapse_panel)
            else binding.expandTopPanelButton.text = getString(R.string.expand_panel)

            nextLayout.applyTo(binding.orderRequestContentDetail)
        }

        if (panelTopIsExpanded) binding.expandTopPanelButton.text = getString(R.string.collapse_panel)
        else binding.expandTopPanelButton.text = getString(R.string.expand_panel)
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
