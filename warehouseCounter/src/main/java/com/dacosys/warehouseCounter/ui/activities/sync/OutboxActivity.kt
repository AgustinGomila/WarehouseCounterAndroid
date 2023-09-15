package com.dacosys.warehouseCounter.ui.activities.sync

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.writeToFile
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest.CREATOR.getCompletedOrders
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestContent
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.SendOrder
import com.dacosys.warehouseCounter.databinding.OutboxActivityBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.completeCompletedPath
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.activities.order.OrderPrintLabelActivity
import com.dacosys.warehouseCounter.ui.activities.orderRequest.OrderRequestDetailActivity
import com.dacosys.warehouseCounter.ui.adapter.orderRequest.OrderRequestAdapter
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.SUCCESS
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelableArrayList
import com.dacosys.warehouseCounter.ui.utils.Screen
import java.io.File
import java.io.UnsupportedEncodingException
import kotlin.concurrent.thread
import kotlin.io.path.Path

class OutboxActivity : AppCompatActivity() {

    private var isListViewFilling = false
    private var multiSelect = true
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
            else settingViewModel.outboxShowCheckBoxes
        set(value) {
            settingViewModel.outboxShowCheckBoxes = value
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

    private lateinit var binding: OutboxActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = OutboxActivityBinding.inflate(layoutInflater)
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

        var tempTitle = getString(R.string.completed_counts)

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

                multiSelect = extras.getBoolean(ARG_MULTISELECT, true)
            }
        }

        binding.topAppbar.title = tempTitle

        binding.sendButton.setOnClickListener { sendDialog() }
        binding.detailButton.setOnClickListener { showDetail() }
        binding.removeResetButton.setOnClickListener { removeResetDialog() }

        Screen.setupUI(binding.root, this)
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

    private fun showDetail() {
        val filename = currentItem?.filename ?: return
        val completePath = Path(completeCompletedPath, filename).toString()

        val intent = Intent(context, OrderRequestDetailActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra(OrderRequestDetailActivity.ARG_FILENAME, completePath)
        startActivity(intent)
    }

    private fun sendDialog() {
        val checked = countChecked
        val orderRequest = currentItem

        val toSend = when {
            checked > 0 -> allChecked
            orderRequest != null -> arrayListOf(orderRequest)
            else -> return
        }

        runOnUiThread {
            val alert = AlertDialog.Builder(this)
            alert.setTitle(getString(R.string.send_counts))
            alert.setMessage(
                if (toSend.count() > 1) {
                    getString(R.string.do_you_want_to_send_the_selected_counts)
                } else {
                    getString(R.string.do_you_want_to_send_the_selected_count)
                }
            )
            alert.setNegativeButton(R.string.cancel, null)
            alert.setPositiveButton(R.string.ok) { _, _ ->
                sendSelected(toSend)
            }
            alert.show()
        }
    }

    private fun sendSelected(orArray: ArrayList<OrderRequest>) {
        runOnUiThread {
            SendOrder(
                orders = orArray,
                onEvent = {
                    if (it.snackBarType != SUCCESS) {
                        showSnackBar(it.text, it.snackBarType)
                    }
                },
                onFinish = {
                    fillAdapter()
                    if (settingViewModel.autoPrint) {
                        launchOrderPrintLabelsActivity(it)
                    }
                }
            )
        }
    }

    private fun launchOrderPrintLabelsActivity(ids: ArrayList<Long>) {
        val intent = Intent(baseContext, OrderPrintLabelActivity::class.java)
        intent.putExtra(OrderPrintLabelActivity.ARG_TITLE, getString(R.string.print_order_labels))
        intent.putExtra(OrderPrintLabelActivity.ARG_IDS, ids)
        intent.putExtra(OrderPrintLabelActivity.ARG_MULTI_SELECT, true)
        intent.putExtra(OrderPrintLabelActivity.ARG_HIDE_FILTER_PANEL, true)
        intent.putExtra(OrderPrintLabelActivity.ARG_SHOW_SELECT_BUTTON, false)
        startActivity(intent)
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    private fun removeResetDialog() {
        val or = currentItem
        val toRemoveReset = when {
            countChecked > 0 -> allChecked
            or != null -> arrayListOf(or)
            else -> return
        }

        val toRemove: ArrayList<OrderRequest> = ArrayList()
        val toReset: ArrayList<OrderRequest> = ArrayList()
        for (remOrder in toRemoveReset) {
            if (remOrder.orderRequestType == OrderRequestType.stockAuditFromDevice) {
                toRemove.add(remOrder)
            } else {
                toReset.add(remOrder)
            }
        }

        if (!toRemove.any() && !toReset.any()) return

        val msg = when {
            toReset.isNotEmpty() && toRemove.isNotEmpty() -> getString(R.string.do_you_want_to_delete_or_reset_the_selected_counts)
            else -> when {
                toReset.count() > 1 -> getString(R.string.do_you_want_to_reset_the_selected_counts)
                toRemove.count() > 1 -> getString(R.string.do_you_want_to_delete_the_selected_counts)
                toReset.count() == 1 -> getString(R.string.do_you_want_to_reset_the_selected_count)
                else -> getString(R.string.do_you_want_to_delete_the_selected_count)
            }
        }

        runOnUiThread {
            val alert = AlertDialog.Builder(this)
            alert.setTitle(getString(R.string.cancel_count))
            alert.setMessage(msg)
            alert.setNegativeButton(R.string.cancel, null)
            alert.setPositiveButton(R.string.ok) { _, _ ->
                if (toRemove.isNotEmpty()) {
                    removeSelected(toRemove)
                }
                if (toReset.isNotEmpty()) {
                    resetSelected(toReset)
                }
            }
            alert.show()
        }
    }

    private fun removeSelected(toRemove: ArrayList<OrderRequest>) {
        var isOk = true
        for (i in toRemove) {
            val filePath = Path(completeCompletedPath, i.filename)
            val fl = File(filePath.toString())
            isOk = !fl.delete()
            if (!isOk) break
        }

        if (!isOk) {
            showSnackBar(getString(R.string.an_error_occurred_while_trying_to_delete_the_count), ERROR)
        }

        thread { fillAdapter() }
    }

    private fun resetSelected(toReset: ArrayList<OrderRequest>) {
        var isOk = true
        for (orderRequest in toReset) {
            val orcToRemove: ArrayList<OrderRequestContent> = ArrayList()

            orderRequest.completed = null
            orderRequest.finishDate = null
            orderRequest.userId = null

            for (content in orderRequest.contents) {
                if (content.qtyRequested == null || content.qtyRequested == 0.toDouble()) {
                    orcToRemove.add(content)
                    continue
                }
                content.qtyCollected = null
            }

            val logContent: ArrayList<OrderRequestContent> = ArrayList(orderRequest.contents)
            logContent.removeAll(orcToRemove.toSet())

            orderRequest.contents = logContent
            orderRequest.logs = arrayListOf()

            try {
                orJson = json.encodeToString(OrderRequest.serializer(), orderRequest)
                orFileName = orderRequest.filename.substringAfterLast('/')

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                    PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    isOk = write(orFileName, orJson)
                    if (!isOk) break
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_EXTERNAL_STORAGE
                    )
                }
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                Log.e(this::class.java.simpleName, e.message ?: "")
            }
        }

        if (isOk) {
            val data = Intent()
            data.putStringArrayListExtra(ARG_ORDER_REQUEST_FILENAMES, ArrayList(toReset.map { it.filename }))
            setResult(RESULT_OK, data)
            finish()
        } else {
            val res = getString(R.string.an_error_occurred_while_trying_to_reset_the_count)
            showSnackBar(res, ERROR)
            Log.e(this::class.java.simpleName, res)
        }
    }

    private fun write(filename: String, value: String): Boolean {
        val path = Statics.getPendingPath()
        return if (writeToFile(fileName = filename, data = value, directory = path)) {
            val completedPath = Path(completeCompletedPath, filename)
            val fl = File(completedPath.toString())
            fl.delete()
        } else {
            false
        }
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshItem.isRefreshing = show
        }, 20)
    }

    private fun fillAdapter(t: ArrayList<OrderRequest> = ArrayList()) {
        if (isListViewFilling) return
        isListViewFilling = true

        Handler(Looper.getMainLooper()).post { Screen.closeKeyboard(this) }

        var temp = t
        if (!temp.any()) {
            temp = getCompletedOrders()
            if (temp.isEmpty()) {
                showSnackBar(
                    this.getString(R.string.there_are_no_completed_counts), SnackBarType.INFO
                )
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
        val set = settingViewModel.orderRequestVisibleStatus
        for (i in set) {
            if (i.trim().isEmpty()) continue
            visibleStatusArray.add(OrderRequestType.getById(i.toLong()))
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
                    colors[i.id.toInt()], BlendModeCompat.SRC_IN
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (adapter == null) {
            return false
        }

        val id = item.itemId
        if (id == R.id.home || id == android.R.id.home) {
            @Suppress("DEPRECATION") onBackPressed()
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
        settingViewModel.orderRequestVisibleStatus = set

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_EXTERNAL_STORAGE -> {
                // If the request is canceled, the result arrays are empty.
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showSnackBar(
                        getString(R.string.cannot_write_to_external_storage), ERROR
                    )
                    return
                }
                write(orFileName, orJson)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        Screen.closeKeyboard(this)

        setResult(RESULT_CANCELED)
        finish()
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_MULTISELECT = "multiSelect"
        const val ARG_ORDER_REQUEST_FILENAMES = "filenames"

        private const val REQUEST_EXTERNAL_STORAGE = 5001

        private var orFileName = ""
        private var orJson = ""
    }
}
