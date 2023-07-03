package com.dacosys.warehouseCounter.ui.activities.orderRequest

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.imageControl.ui.fragments.ImageControlButtonsFragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.adapter.orderRequest.OrcAdapter
import com.dacosys.warehouseCounter.databinding.OrderRequestConfirmActivityBinding
import com.dacosys.warehouseCounter.dto.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.dto.orderRequest.OrderRequestContent
import com.dacosys.warehouseCounter.dto.orderRequest.OrderRequestType
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.misc.objects.status.ConfirmStatus
import com.dacosys.warehouseCounter.misc.objects.table.Table
import com.dacosys.warehouseCounter.room.entity.client.Client
import com.dacosys.warehouseCounter.ui.fragments.orderRequest.OrderRequestHeader
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.Screen
import org.parceler.Parcels

class OrderRequestConfirmActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    OrcAdapter.DataSetChangedListener {

    private var panelTopIsExpanded = true
    private var finishOrder: Boolean = true

    // Header que depende del tipo de conteo
    private var headerFragment: Fragment? = null

    // Panel de ImageControl
    private var imageControlFragment: ImageControlButtonsFragment? = null

    // Adaptador, colección de ítems, fila seleccionada
    private var adapter: OrcAdapter? = null
    private var lastSelected: OrderRequestContent? = null
    private var firstVisiblePos: Int? = null
    private var currentScrollPosition: Int = 0
    private var checkedIdArray: ArrayList<Long> = ArrayList()

    // Cliente
    private var client: Client? = null

    // OrderRequest
    private var orderRequest: OrderRequest? = null

    // Lista completa
    private var completeList: ArrayList<OrderRequestContent> = ArrayList()

    private var rejectNewInstances = false

    // Se usa para saber si estamos en onStart luego de onCreate
    private var fillRequired = false

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

        if (adapter != null) {
            savedInstanceState.putParcelable("lastSelected", adapter?.currentItem())
            savedInstanceState.putInt("firstVisiblePos", adapter?.firstVisiblePos() ?: RecyclerView.NO_POSITION)
            savedInstanceState.putParcelableArrayList("completeList", adapter?.fullList)
            savedInstanceState.putLongArray("checkedIdArray", adapter?.checkedIdArray?.map { it }?.toLongArray())
            savedInstanceState.putInt("currentScrollPosition", currentScrollPosition)
        }

        savedInstanceState.putBoolean("finishOrder", finishOrder)
        savedInstanceState.putBoolean("panelTopIsExpanded", panelTopIsExpanded)
        savedInstanceState.putParcelable("client", client)
        savedInstanceState.putParcelable("orderRequest", orderRequest)

        supportFragmentManager.putFragment(
            savedInstanceState, "imageControlFragment", imageControlFragment as Fragment
        )
    }

    override fun onDestroy() {
        saveSharedPreferences()
        destroyLocals()
        super.onDestroy()
    }

    private fun saveSharedPreferences() {
        settingViewModel.finishOrder = binding.finishCheckBox.isChecked
    }

    private fun destroyLocals() {
        adapter?.refreshListeners(null, null, null, null)
        imageControlFragment!!.onDestroy()
    }

    private lateinit var binding: OrderRequestConfirmActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = OrderRequestConfirmActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                currentScrollPosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            }
        })

        // Para el llenado en el onStart siguiente de onCreate
        fillRequired = true

        var tempTitle = getString(R.string.confirm_count)

        if (savedInstanceState != null) {
            finishOrder = savedInstanceState.getBoolean("finishOrder")

            // region Recuperar el título de la ventana
            val t2 = savedInstanceState.getString("title")
            if (!t2.isNullOrEmpty()) tempTitle = t2
            // endregion

            client = savedInstanceState.getParcelable("client")
            orderRequest = savedInstanceState.getParcelable("orderRequest")
            panelTopIsExpanded = savedInstanceState.getBoolean("panelTopIsExpanded")
            completeList =
                savedInstanceState.getParcelableArrayList<OrderRequestContent>("completeList") as ArrayList<OrderRequestContent>
            checkedIdArray =
                (savedInstanceState.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(ArrayList())
            lastSelected = savedInstanceState.getParcelable("lastSelected")
            firstVisiblePos =
                if (savedInstanceState.containsKey("firstVisiblePos")) savedInstanceState.getInt("firstVisiblePos") else -1
            currentScrollPosition = savedInstanceState.getInt("currentScrollPosition")

            //Restore the fragment's instance
            imageControlFragment = supportFragmentManager.getFragment(
                savedInstanceState, "imageControlFragment"
            ) as ImageControlButtonsFragment
        } else {
            // Nueva instancia de la actividad

            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString("title")
                if (!t1.isNullOrEmpty()) tempTitle = t1

                orderRequest = Parcels.unwrap<OrderRequest>(extras.getParcelable("orderRequest"))
                client = Parcels.unwrap<Client>(extras.getParcelable("client"))

                val t2 = extras.getParcelableArrayList<OrderRequestContent>("orcArray")
                if (t2 != null) completeList = t2
            }

            finishOrder = settingViewModel.finishOrder
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

        binding.finishCheckBox.isChecked = finishOrder

        binding.swipeRefreshOrc.setOnRefreshListener(this)
        binding.swipeRefreshOrc.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Para expandir y colapsar el panel inferior
        setTopPanelAnimation()

        binding.confirmButton.setOnClickListener { confirm() }

        // Si no está configurado el WebService de ImageControl ocultar botones
        if (settingViewModel.icWsServer.isEmpty()) {
            binding.imageControlLayout.visibility = View.GONE
        }

        setImageControlFragment()
        setPanels()

        // ESTO SIRVE PARA OCULTAR EL TECLADO EN PANTALLA CUANDO PIERDEN EL FOCO LOS CONTROLES QUE LO NECESITAN
        Screen.setupUI(binding.orderRequestContentConfirm, this)

        showProgressBar(false)
    }

    override fun onStart() {
        super.onStart()

        if (fillRequired) {
            fillRequired = false
            fillAdapter(completeList)
        }
    }

    /**
     * Cambia el fragment que aparece en la parte superior de la pantalla
     * dependiendo del tipo de conteo que se está haciendo.
     */
    private fun setHeaderFragment() {
        if (orderRequest == null || adapter == null) {
            return
        }

        val orType = orderRequest?.orderRequestedType
        val newFragment: Fragment =
            if (orType == OrderRequestType.deliveryAudit || orType == OrderRequestType.prepareOrder || orType == OrderRequestType.stockAudit || orType == OrderRequestType.receptionAudit || orType == OrderRequestType.stockAuditFromDevice) {
                OrderRequestHeader.newInstance(
                    orderRequest = orderRequest, orcArray = adapter?.fullList ?: ArrayList()
                )
            } else {
                return
            }

        var oldFragment: Fragment? = null
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
                currentFocus!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
            )
        }

        runOnUiThread {
            fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.setCustomAnimations(
                R.anim.animation_fade_in, R.anim.animation_fade_in
            )
            fragmentTransaction.replace(binding.panelHeader.id, newFragment)
                .commitAllowingStateLoss()
        }

        headerFragment = newFragment
    }

    private fun setImageControlFragment() {
        val tempOrId = orderRequest?.orderRequestId ?: return

        runOnUiThread {
            if (imageControlFragment == null) {
                val orId = tempOrId.toString()

                imageControlFragment = ImageControlButtonsFragment.newInstance(
                    tableId = Table.orderRequest.tableId.toLong(),
                    objectId1 = orId
                )

                val fm = supportFragmentManager

                fm.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(binding.imageControlLayout.id, imageControlFragment!!)
                    .commit()

                if (!(settingViewModel.useImageControl)) {
                    fm.beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .hide(imageControlFragment as Fragment)
                        .commitAllowingStateLoss()
                } else {
                    fm.beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .show(imageControlFragment!! as Fragment)
                        .commitAllowingStateLoss()
                }
            } else {
                imageControlFragment?.setTableId(Table.orderRequest.tableId)
                imageControlFragment?.setObjectId1(tempOrId)
                imageControlFragment?.setObjectId2(null)
            }
        }
    }

    private fun setPanels() {
        val currentLayout = ConstraintSet()
        if (panelTopIsExpanded) {
            currentLayout.load(this, R.layout.order_request_confirm_activity)
        } else {
            currentLayout.load(this, R.layout.order_request_confirm_activity_top_panel_collapsed)
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

        TransitionManager.beginDelayedTransition(binding.orderRequestContentConfirm, transition)

        currentLayout.applyTo(binding.orderRequestContentConfirm)

        when {
            panelTopIsExpanded -> {
                binding.expandTopPanelButton?.text = getString(R.string.collapse_panel)
            }

            else -> {
                binding.expandTopPanelButton?.text = getString(R.string.client_and_description)
            }
        }
    }

    private fun setTopPanelAnimation() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            return
        }

        binding.expandTopPanelButton!!.setOnClickListener {
            val nextLayout = ConstraintSet()
            if (panelTopIsExpanded) {
                nextLayout.load(this, R.layout.order_request_confirm_activity_top_panel_collapsed)
            } else {
                nextLayout.load(this, R.layout.order_request_confirm_activity)
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

            TransitionManager.beginDelayedTransition(binding.orderRequestContentConfirm, transition)

            when {
                panelTopIsExpanded -> {
                    binding.expandTopPanelButton?.text = getString(R.string.collapse_panel)
                }

                else -> {
                    binding.expandTopPanelButton?.text = getString(R.string.client_and_description)
                }
            }

            nextLayout.applyTo(binding.orderRequestContentConfirm)
        }
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshOrc.isRefreshing = show
            }
        }, 20)
    }

    override fun onBackPressed() {
        Screen.closeKeyboard(this)

        ////////////// IMAGE CONTROL //////////////
        imageControlFragment?.saveImages(false)
        ///////////////////////////////////////////

        val data = Intent()
        data.putExtra("confirmStatus", Parcels.wrap(ConfirmStatus.modify))
        setResult(RESULT_CANCELED, data)

        super.onBackPressed()
    }

    private fun fillSummaryRow() {
        runOnUiThread {
            if (orderRequest?.orderRequestedType != OrderRequestType.stockAuditFromDevice) {
                binding.totalLabelTextView.text = getString(R.string.total)
                binding.qtyReqLabelTextView.text = getString(R.string.cant)
                binding.selectedLabelTextView.visibility = View.VISIBLE
                binding.selectedLabelTextView.text = getString(R.string.checked)

                if (adapter != null) {
                    val cont = adapter?.qtyRequestedTotal() ?: 0.0
                    val t = adapter?.itemCount ?: 0
                    binding.totalTextView.text = t.toString()
                    binding.qtyReqTextView.text = Statics.roundToString(cont, 0)
                    binding.selectedTextView.text = adapter!!.countChecked().toString()
                }
            } else {
                binding.totalLabelTextView.text = getString(R.string.total)
                binding.qtyReqLabelTextView.text = getString(R.string.cont_)
                binding.selectedLabelTextView.visibility = View.GONE

                if (adapter != null) {
                    val cont = adapter?.qtyCollectedTotal() ?: 0.0
                    val t = adapter?.itemCount ?: 0
                    binding.totalTextView.text = t.toString()
                    binding.qtyReqTextView.text = Statics.roundToString(cont, 0)
                }
            }

            if (adapter == null) {
                binding.totalTextView.text = 0.toString()
                binding.qtyReqTextView.text = 0.toString()
                binding.selectedTextView.text = 0.toString()
            }
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
                        (completeList[it].qty?.qtyCollected ?: 0.0) < (completeList[it].qty?.qtyRequested ?: 0.0) ||
                                (completeList[it].qty?.qtyCollected ?: 0.0) > (completeList[it].qty?.qtyRequested
                            ?: 0.0)
                    }
                    .map { completeList[it] })

                adapter = OrcAdapter(
                    recyclerView = binding.recyclerView,
                    fullList = filteredList,
                    checkedIdArray = checkedIdArray,
                    orType = orderRequest?.orderRequestedType
                        ?: OrderRequestType.stockAuditFromDevice,
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
            checkedChangedListener = null,
            dataSetChangedListener = this,
            editQtyListener = null,
            editDescriptionListener = null
        )
    }

    private fun confirm() {
        if (settingViewModel.signMandatory && imageControlFragment?.isSigned == false) {
            makeText(binding.root, getString(R.string.mandatory_sign), SnackBarType.ERROR)
        } else {
            val allowDiff = orderRequest!!.resultAllowDiff ?: true
            val allowMod = orderRequest!!.resultAllowDiff ?: true
            val orDiffQty = orderRequest!!.resultDiffQty ?: false
            val orDiffProduct = orderRequest!!.resultDiffProduct ?: false

            if (!allowDiff && orDiffQty) {
                makeText(
                    binding.root,
                    getString(R.string.there_are_quantities_differences_in_some_items_and_this_count_does_not_allow_these_differences),
                    SnackBarType.ERROR
                )
                return
            }

            if (!allowMod && orDiffProduct) {
                makeText(
                    binding.root,
                    getString(R.string.there_are_items_differences_and_this_count_does_not_allow_these_differences),
                    SnackBarType.ERROR
                )
                return
            }

            Screen.closeKeyboard(this)

            ///////////////////////////////////////////
            ////////////// IMAGE CONTROL //////////////
            if (imageControlFragment != null) {
                imageControlFragment!!.saveImages(false)
            }
            ///////////////////////////////////////////

            val data = Intent()
            data.putExtra(
                "confirmStatus",
                if (binding.finishCheckBox.isChecked) Parcels.wrap(ConfirmStatus.confirm)
                else Parcels.wrap(ConfirmStatus.modify)
            )
            data.putExtra("isSigned", imageControlFragment?.isSigned)
            setResult(RESULT_OK, data)
            finish()
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
                fillSummaryRow()
            }
        }, 100)
    }
}