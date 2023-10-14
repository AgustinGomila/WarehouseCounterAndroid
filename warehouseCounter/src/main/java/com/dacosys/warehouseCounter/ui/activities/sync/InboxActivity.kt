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
import androidx.activity.OnBackPressedCallback
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
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.completePendingPath
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.getPendingJsonOrders
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.getPendingPath
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.removeOrdersFiles
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.data.room.dao.orderRequest.OrderRequestCoroutines
import com.dacosys.warehouseCounter.databinding.InboxActivityBinding
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.activities.orderRequest.OrderRequestDetailActivity
import com.dacosys.warehouseCounter.ui.adapter.orderRequest.OrderRequestAdapter
import com.dacosys.warehouseCounter.ui.fragments.common.SummaryFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelableArrayList
import com.dacosys.warehouseCounter.ui.utils.Screen
import java.io.File
import kotlin.concurrent.thread

class InboxActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    OrderRequestAdapter.DataSetChangedListener {

    private var isListViewFilling = false
    private var multiSelect = false
    private var adapter: OrderRequestAdapter? = null
    private var lastSelected: OrderRequest? = null
    private var firstVisiblePos: Int? = null
    private var completeList: ArrayList<OrderRequest> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()
    private var currentScrollPosition: Int = 0

    private lateinit var summaryFragment: SummaryFragment

    // Se usa para saber si estamos en onStart luego de onCreate
    private var fillRequired = false

    private var showCheckBoxes
        get() =
            if (!multiSelect) false
            else settingsVm.inboxShowCheckBoxes
        set(value) {
            settingsVm.inboxShowCheckBoxes = value
        }

    private val countChecked: Int
        get() {
            return adapter?.countChecked() ?: 0
        }

    private val allChecked: ArrayList<OrderRequest>
        get() {
            return adapter?.getAllChecked() ?: arrayListOf()
        }

    private val currentItem: OrderRequest?
        get() {
            return adapter?.currentItem()
        }

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        adapter?.refreshListeners(checkedChangedListener = null, dataSetChangedListener = null)
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString(ARG_TITLE, title.toString())
        savedInstanceState.putBoolean(ARG_MULTISELECT, multiSelect)
        if (adapter != null) {
            savedInstanceState.putParcelable("lastSelected", currentItem)
            savedInstanceState.putInt("firstVisiblePos", adapter?.firstVisiblePos() ?: -1)
            savedInstanceState.putParcelableArrayList("completeList", adapter?.fullList)
            savedInstanceState.putLongArray("checkedIdArray", allChecked.mapNotNull { it.orderRequestId }.toLongArray())
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

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isBackPressed()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        summaryFragment = supportFragmentManager.findFragmentById(R.id.summaryFragment) as SummaryFragment

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
                savedInstanceState.parcelableArrayList<OrderRequest>("completeList") as ArrayList<OrderRequest>
            lastSelected = savedInstanceState.parcelable("lastSelected")
            firstVisiblePos =
                if (savedInstanceState.containsKey("firstVisiblePos")) savedInstanceState.getInt("firstVisiblePos") else -1
            currentScrollPosition = savedInstanceState.getInt("currentScrollPosition")
        } else {
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString(ARG_TITLE)
                if (!t1.isNullOrEmpty()) tempTitle = t1

                multiSelect = extras.getBoolean(ARG_MULTISELECT, false)
            }
        }

        binding.topAppbar.title = tempTitle

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

        Screen.setupUI(binding.root, this)
    }

    override fun onStart() {
        super.onStart()

        if (fillRequired) {
            fillRequired = false
            fillAdapter(completeList)
        }
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
                .fill()
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
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
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

    private fun removeDialog() {
        val checked = countChecked
        val orderRequest = currentItem

        val toRemove = when {
            checked > 0 -> allChecked
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
        val filenames = ArrayList(toRemove.map { it.filename })
        val ids = toRemove.mapNotNull { it.roomId }

        removeOrdersFiles(
            path = getPendingPath(),
            filesToRemove = filenames,
            sendEvent = {
                if (it.snackBarType == SnackBarType.SUCCESS) {
                    OrderRequestCoroutines.removeById(
                        idList = ids,
                        onResult = { thread { fillAdapter(ArrayList()) } })
                } else {
                    showSnackBar(it.text, it.snackBarType)
                }
            })
    }

    private fun continueOrder() {
        Screen.closeKeyboard(this)

        val currentItem = currentItem
        val allChecked = allChecked

        if (!multiSelect && currentItem != null) {
            val data = Intent()
            data.putStringArrayListExtra(ARG_ORDER_REQUEST_FILENAMES, arrayListOf(currentItem.filename))
            setResult(RESULT_OK, data)
            finish()
        } else if (multiSelect && allChecked.any()) {
            val data = Intent()
            data.putStringArrayListExtra(ARG_ORDER_REQUEST_FILENAMES, ArrayList(allChecked.map { it.filename }))
            setResult(RESULT_OK, data)
            finish()
        }
    }

    private fun showDetail() {
        val filename = currentItem?.filename ?: return
        val path: String
        try {
            path = File(completePendingPath, filename).toString()
        } catch (ex: Exception) {
            ex.printStackTrace()
            return
        }

        val intent = Intent(context, OrderRequestDetailActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra(OrderRequestDetailActivity.ARG_ID, currentItem?.roomId)
        intent.putExtra(OrderRequestDetailActivity.ARG_FILENAME, path)
        startActivity(intent)
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
            temp = getPendingJsonOrders()
            if (temp.isEmpty()) {
                showSnackBar(getString(R.string.there_are_no_pending_counts), SnackBarType.INFO)
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
                    lastSelected = currentItem
                }

                adapter = OrderRequestAdapter.Builder()
                    .recyclerView(binding.recyclerView)
                    .fullList(completeList)
                    .checkedIdArray(checkedIdArray)
                    .multiSelect(multiSelect)
                    .showCheckBoxes(`val` = showCheckBoxes, listener = { showCheckBoxes = it })
                    .dataSetChangedListener(this)
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
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                showProgressBar(false)
                isListViewFilling = false
            }
        }
    }

    private fun getPrefVisibleStatus(): ArrayList<OrderRequestType> {
        val visibleStatusArray: ArrayList<OrderRequestType> = ArrayList()
        //Retrieve the values
        val set = settingsVm.orderRequestVisibleStatus
        for (i in set) {
            if (i.trim().isEmpty()) continue
            visibleStatusArray.add(OrderRequestType.getById(i.toLong()))
        }
        return visibleStatusArray
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        // Opciones de visibilidad del menú
        for (i in OrderRequestType.getAll()) {
            menu.add(
                0,
                i.id.toInt(),
                i.id.toInt(),
                i.description
            ).setChecked(getPrefVisibleStatus().contains(i)).isCheckable = true
        }

        //region Icon colors
        val colors: ArrayList<Int> = ArrayList()
        colors.add(getColor(R.color.status_default))
        colors.add(getColor(R.color.status_prepare_order))
        colors.add(getColor(R.color.status_stock_audit_from_device))
        colors.add(getColor(R.color.status_stock_audit))
        colors.add(getColor(R.color.status_reception_audit))
        colors.add(getColor(R.color.status_delivery_audit))
        colors.add(getColor(R.color.status_packaging))
        //endregion Icon colors

        for (i in OrderRequestType.getAll()) {
            val icon = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_lens, null)
            icon?.mutate()?.colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    colors[i.id.toInt()],
                    BlendModeCompat.SRC_IN
                )

            val item = menu.getItem(i.id.toInt())
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
        if (adapter == null) {
            return false
        }

        val id = item.itemId
        if (id == R.id.home || id == android.R.id.home) {
            isBackPressed()
            return true
        }

        val it = OrderRequestType.getById(id.toLong())

        item.isChecked = !item.isChecked
        var visibleStatus = adapter?.visibleStatus ?: ArrayList()

        if (item.isChecked && !visibleStatus.contains(it)) {
            adapter?.addVisibleStatus(it)
        } else if (!item.isChecked && visibleStatus.contains(it)) {
            adapter?.removeVisibleStatus(it)
        }

        // Guardar los valores en las preferencias
        visibleStatus = adapter?.visibleStatus ?: ArrayList()
        val set = HashSet<String>()
        visibleStatus.mapTo(set) { it.id.toString() }
        settingsVm.orderRequestVisibleStatus = set

        return true
    }

    private fun isBackPressed() {
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
        const val ARG_ORDER_REQUEST_FILENAMES = "filenames"
    }
}
