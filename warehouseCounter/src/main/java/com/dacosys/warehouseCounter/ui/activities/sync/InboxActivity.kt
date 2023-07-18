package com.dacosys.warehouseCounter.ui.activities.sync

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.InboxActivityBinding
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.activities.orderRequest.OrderRequestDetailActivity
import com.dacosys.warehouseCounter.ui.adapter.orderRequest.OrderRequestAdapter
import com.dacosys.warehouseCounter.ui.snackBar.MakeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.Screen
import java.io.File
import kotlin.concurrent.thread

class InboxActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    private var isListViewFilling = false
    private var multiSelect = false
    private var adapter: OrderRequestAdapter? = null
    private var lastSelected: OrderRequest? = null
    private var firstVisiblePos: Int? = null
    private var completeList: ArrayList<OrderRequest> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()
    private var currentScrollPosition: Int = 0

    // Se usa para saber si estamos en onStart luego de onCreate
    private var fillRequired = false

    private var showCheckBoxes
        get() =
            if (!multiSelect) false
            else settingViewModel.inboxShowCheckBoxes
        set(value) {
            settingViewModel.inboxShowCheckBoxes = value
        }

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        adapter?.refreshListeners(checkedChangedListener = null, dataSetChangedListener = null)
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString(ARG_TITLE, title.toString())
        savedInstanceState.putBoolean(ARG_MULTISELECT, multiSelect)
        if (adapter != null) {
            savedInstanceState.putParcelable("lastSelected", (adapter ?: return).currentItem())
            savedInstanceState.putInt("firstVisiblePos", (adapter ?: return).firstVisiblePos())
            savedInstanceState.putParcelableArrayList("completeList", adapter?.fullList)
            savedInstanceState.putLongArray(
                "checkedIdArray",
                adapter?.getAllChecked()?.mapNotNull { it.orderRequestId }?.toLongArray()
            )
            savedInstanceState.putInt("currentScrollPosition", currentScrollPosition)
        }
    }

    private lateinit var binding: InboxActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = InboxActivityBinding.inflate(layoutInflater)
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

        var tempTitle = getString(R.string.pending_counts)

        if (savedInstanceState != null) {
            // region Recuperar el título de la ventana
            val t1 = savedInstanceState.getString(ARG_TITLE)
            if (!t1.isNullOrEmpty()) tempTitle = t1
            // endregion

            multiSelect = savedInstanceState.getBoolean(ARG_MULTISELECT, multiSelect)
            checkedIdArray =
                (savedInstanceState.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(ArrayList())
            completeList =
                savedInstanceState.getParcelableArrayList<OrderRequest>("completeList") as ArrayList<OrderRequest>
            lastSelected = savedInstanceState.getParcelable("lastSelected")
            firstVisiblePos =
                if (savedInstanceState.containsKey("firstVisiblePos")) savedInstanceState.getInt("firstVisiblePos") else -1
            currentScrollPosition = savedInstanceState.getInt("currentScrollPosition")
        } else {
            // Inicializar la actividad
            // Traer los parámetros que recibe la actividad
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString(ARG_TITLE)
                if (!t1.isNullOrEmpty()) tempTitle = t1

                multiSelect = extras.getBoolean(ARG_MULTISELECT, false)
            }
        }

        title = tempTitle

        binding.swipeRefreshItem.setOnRefreshListener(this)
        binding.swipeRefreshItem.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        binding.okButton.setOnClickListener { continueOrder() }
        binding.removeButton.setOnClickListener { removeDialog() }
        binding.detailButton.setOnClickListener { showDetail() }

        // ESTO SIRVE PARA OCULTAR EL TECLADO EN PANTALLA CUANDO PIERDEN EL FOCO LOS CONTROLES QUE LO NECESITAN
        Screen.setupUI(binding.inbox, this)
    }

    override fun onStart() {
        super.onStart()

        if (fillRequired) {
            fillRequired = false
            fillAdapter(completeList)
        }
    }

    // region Inset animation
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupWindowInsetsAnimation()
    }

    private var isKeyboardVisible: Boolean = false

    private fun setupWindowInsetsAnimation() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        adjustRootLayout()

        implWindowInsetsAnimation()
    }

    private fun adjustRootLayout() {
        val rootView = binding.root

        // Adjust root layout to bottom navigation bar
        val windowInsets = window.decorView.rootWindowInsets
        @Suppress("DEPRECATION") rootView.setPadding(
            windowInsets.systemWindowInsetLeft,
            windowInsets.systemWindowInsetTop,
            windowInsets.systemWindowInsetRight,
            windowInsets.systemWindowInsetBottom
        )
    }

    private fun implWindowInsetsAnimation() {
        val rootView = binding.root

        ViewCompat.setWindowInsetsAnimationCallback(
            rootView,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    val isIme = animation.typeMask and WindowInsetsCompat.Type.ime() != 0
                    if (!isIme) return

                    super.onEnd(animation)
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>,
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
    // endregion

    private fun showDetail() {
        val orderRequest = adapter?.currentItem() ?: return
        val intent = Intent(context, OrderRequestDetailActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra(OrderRequestDetailActivity.ARG_ID, orderRequest.orderRequestId)
        startActivity(intent)
    }

    private fun removeDialog() {
        val checked = adapter?.countChecked() ?: 0
        val orderRequest = adapter?.currentItem()

        val toRemove = when {
            checked > 0 -> adapter?.getAllChecked() ?: ArrayList()
            orderRequest != null -> arrayListOf(orderRequest)
            else -> return
        }

        if (!toRemove.any()) return

        val msg = when {
            toRemove.count() > 1 -> getString(R.string.do_you_want_to_delete_the_selected_counts)
            else -> getString(R.string.do_you_want_to_delete_the_selected_count)
        }

        runOnUiThread {
            val alert = AlertDialog.Builder(this)
            alert.setTitle(getString(R.string.cancel_count))
            alert.setMessage(msg)
            alert.setNegativeButton(R.string.cancel, null)
            alert.setPositiveButton(R.string.ok) { _, _ ->
                removeSelected(toRemove)
            }
            alert.show()
        }
    }

    private fun removeSelected(toRemove: ArrayList<OrderRequest>) {
        var isOk = true

        val currentDir = Statics.getPendingPath()
        for (i in toRemove) {
            val filePath = currentDir.absolutePath + File.separator + i.filename
            val fl = File(filePath)
            if (!fl.delete()) {
                isOk = false
                break
            }
        }

        if (!isOk) {
            MakeText.makeText(
                binding.root,
                getString(R.string.an_error_occurred_while_trying_to_delete_the_count),
                SnackBarType.ERROR
            )
        }

        thread { fillAdapter(ArrayList()) }
    }

    private fun continueOrder() {
        val currentItem = adapter?.currentItem()
        val allChecked = adapter?.getAllChecked() ?: arrayListOf()

        if (!multiSelect && currentItem != null) {
            Screen.closeKeyboard(this)

            val data = Intent()
            data.putParcelableArrayListExtra(ARG_ORDER_REQUESTS, arrayListOf(currentItem))
            setResult(RESULT_OK, data)
            finish()
        } else if (multiSelect && allChecked.any()) {
            Screen.closeKeyboard(this)

            val data = Intent()
            data.putParcelableArrayListExtra(ARG_ORDER_REQUESTS, allChecked)
            setResult(RESULT_OK, data)
            finish()
        }
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshItem.isRefreshing = show
        }, 20)
    }

    private fun fillAdapter(t: ArrayList<OrderRequest>) {
        if (isListViewFilling) return
        isListViewFilling = true

        Handler(Looper.getMainLooper()).post { Screen.closeKeyboard(this) }

        var temp = t
        if (!temp.any()) {
            temp = OrderRequest.getPendingOrders()
            if (temp.isEmpty()) {
                MakeText.makeText(binding.root, getString(R.string.there_are_no_pending_counts), SnackBarType.INFO)
            }
        }
        completeList = temp

        showProgressBar(true)

        runOnUiThread {
            try {
                if (adapter != null) {
                    // Si el adapter es NULL es porque aún no fue creado.
                    // Por lo tanto, puede ser que los valores de [lastSelected]
                    // sean valores guardados de la instancia anterior y queremos preservarlos.
                    lastSelected = adapter?.currentItem()
                }

                adapter = OrderRequestAdapter(
                    recyclerView = binding.recyclerView,
                    fullList = completeList,
                    checkedIdArray = checkedIdArray,
                    multiSelect = multiSelect,
                    showCheckBoxes = showCheckBoxes,
                    showCheckBoxesChanged = { showCheckBoxes = it }
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
                isListViewFilling = false
            }
        }
    }

    private fun refreshAdapterListeners() {
        adapter?.refreshListeners(
            checkedChangedListener = null,
            dataSetChangedListener = null
        )
    }

    private fun getPrefVisibleStatus(): ArrayList<OrderRequestType> {
        val visibleStatusArray: ArrayList<OrderRequestType> = ArrayList()
        //Retrieve the values
        val set = settingViewModel.orderRequestVisibleStatus
        for (i in set) {
            if (i.trim().isEmpty()) continue
            val status = OrderRequestType.getById(i.toLong())
            if (status != OrderRequestType.notDefined) {
                visibleStatusArray.add(status)
            }
        }

        return visibleStatusArray
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        // Opciones de visibilidad del menú
        for (i in OrderRequestType.getAll()) {
            menu.add(0, i.id.toInt(), i.id.toInt(), i.description)
                .setChecked(getPrefVisibleStatus().contains(i)).isCheckable = true
        }

        //region Icon colors
        val colors: ArrayList<Int> = ArrayList()
        colors.add(getColor(R.color.status_prepare_order))
        colors.add(getColor(R.color.status_stock_audit_from_device))
        colors.add(getColor(R.color.status_stock_audit))
        colors.add(getColor(R.color.status_reception_audit))
        colors.add(getColor(R.color.status_delivery_audit))
        //endregion Icon colors

        for (i in OrderRequestType.getAll()) {
            val icon = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_lens, null)
            icon?.mutate()?.colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    colors[i.id.toInt() - 1],
                    BlendModeCompat.SRC_IN
                )

            val item = menu.getItem(i.id.toInt() - 1)
            item.icon = icon

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
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (adapter == null) {
            return false
        }

        val id = item.itemId
        if (id == R.id.home || id == android.R.id.home) {
            onBackPressed()
            return true
        }

        item.isChecked = !item.isChecked
        val visibleStatus = adapter?.visibleStatus ?: ArrayList()

        when (id) {
            OrderRequestType.deliveryAudit.id.toInt() -> {
                if (item.isChecked && !visibleStatus.contains(OrderRequestType.deliveryAudit)) {
                    adapter?.addVisibleStatus(OrderRequestType.deliveryAudit)
                } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.deliveryAudit)) {
                    adapter?.removeVisibleStatus(OrderRequestType.deliveryAudit)
                }
            }

            OrderRequestType.prepareOrder.id.toInt() -> {
                if (item.isChecked && !visibleStatus.contains(OrderRequestType.prepareOrder)) {
                    adapter?.addVisibleStatus(OrderRequestType.prepareOrder)
                } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.prepareOrder)) {
                    adapter?.removeVisibleStatus(OrderRequestType.prepareOrder)
                }
            }

            OrderRequestType.receptionAudit.id.toInt() -> {
                if (item.isChecked && !visibleStatus.contains(OrderRequestType.receptionAudit)) {
                    adapter?.addVisibleStatus(OrderRequestType.receptionAudit)
                } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.receptionAudit)) {
                    adapter?.removeVisibleStatus(OrderRequestType.receptionAudit)
                }
            }

            OrderRequestType.stockAudit.id.toInt() -> {
                if (item.isChecked && !visibleStatus.contains(OrderRequestType.stockAudit)) {
                    adapter?.addVisibleStatus(OrderRequestType.stockAudit)
                } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.stockAudit)) {
                    adapter?.removeVisibleStatus(OrderRequestType.stockAudit)
                }
            }

            OrderRequestType.stockAuditFromDevice.id.toInt() -> {
                if (item.isChecked && !visibleStatus.contains(OrderRequestType.stockAuditFromDevice)) {
                    adapter?.addVisibleStatus(OrderRequestType.stockAuditFromDevice)
                } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.stockAuditFromDevice)) {
                    adapter?.removeVisibleStatus(OrderRequestType.stockAuditFromDevice)
                }
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }

        // Guardar los valores en las preferencias
        val set = HashSet<String>()
        for (i in visibleStatus) {
            set.add(i.id.toString())
        }
        settingViewModel.orderRequestVisibleStatus = set

        return true
    }

    override fun onBackPressed() {
        Screen.closeKeyboard(this)

        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshItem.isRefreshing = false
        }, 100)
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_MULTISELECT = "multiSelect"
        const val ARG_ORDER_REQUESTS = "orderRequests"
    }
}