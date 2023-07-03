package com.dacosys.warehouseCounter.ui.activities.sync

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
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
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.adapter.orderRequest.OrderRequestAdapter
import com.dacosys.warehouseCounter.databinding.OutboxActivityBinding
import com.dacosys.warehouseCounter.dto.log.Log
import com.dacosys.warehouseCounter.dto.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.dto.orderRequest.OrderRequest.CREATOR.getCompletedOrders
import com.dacosys.warehouseCounter.dto.orderRequest.OrderRequestContent
import com.dacosys.warehouseCounter.dto.orderRequest.OrderRequestType
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.writeToFile
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.retrofit.functions.SendOrder
import com.dacosys.warehouseCounter.ui.activities.orderRequest.OrderRequestDetailActivity
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.utils.Screen
import org.parceler.Parcels
import java.io.File
import java.io.UnsupportedEncodingException
import kotlin.concurrent.thread

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

    private fun onSendOrderEnded(snackBarEventData: SnackBarEventData) {
        val msg = snackBarEventData.text

        if (snackBarEventData.snackBarType == SnackBarType.SUCCESS) {
            thread { fillAdapter(ArrayList()) }
        } else if (snackBarEventData.snackBarType == ERROR) {
            makeText(binding.root, msg, ERROR)
        }
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

        savedInstanceState.putString("title", title.toString())
        savedInstanceState.putBoolean("multiSelect", multiSelect)
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

    private lateinit var binding: OutboxActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = OutboxActivityBinding.inflate(layoutInflater)
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

        var tempTitle = getString(R.string.completed_counts)

        if (savedInstanceState != null) {
            // region Recuperar el título de la ventana
            val t1 = savedInstanceState.getString("title")
            if (!t1.isNullOrEmpty()) tempTitle = t1
            // endregion

            multiSelect = savedInstanceState.getBoolean("multiSelect", multiSelect)
            checkedIdArray =
                (savedInstanceState.getLongArray("checkedIdArray") ?: longArrayOf()).toCollection(java.util.ArrayList())
            completeList =
                savedInstanceState.getParcelableArrayList<OrderRequest>("completeList") as java.util.ArrayList<OrderRequest>
            lastSelected = savedInstanceState.getParcelable("lastSelected")
            firstVisiblePos =
                if (savedInstanceState.containsKey("firstVisiblePos")) savedInstanceState.getInt("firstVisiblePos") else -1
            currentScrollPosition = savedInstanceState.getInt("currentScrollPosition")
        } else {
            // Inicializar la actividad

            // Traer los parámetros que recibe la actividad
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString("title")
                if (!t1.isNullOrEmpty()) tempTitle = t1

                multiSelect = extras.getBoolean("multiSelect", true)
            }
        }

        title = tempTitle

        binding.sendButton.setOnClickListener { sendDialog() }
        binding.detailButton.setOnClickListener { showDetail() }
        binding.removeResetButton.setOnClickListener { removeResetDialog() }

        // ESTO SIRVE PARA OCULTAR EL TECLADO EN PANTALLA CUANDO PIERDEN EL FOCO LOS CONTROLES QUE LO NECESITAN
        Screen.setupUI(binding.outbox, this)
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
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
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

        android.util.Log.d(javaClass.simpleName, "IME Size: ${imeInsets.bottom}")
    }
    // endregion

    private fun showDetail() {
        if (adapter != null && adapter!!.currentItem() != null) {
            val intent = Intent(context, OrderRequestDetailActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

            intent.putExtra("orderRequest", Parcels.wrap<OrderRequest>(adapter!!.currentItem()))

            // Valid content
            intent.putParcelableArrayListExtra("orcArray", ArrayList(adapter!!.currentItem()!!.content))

            startActivity(intent)
        }
    }

    private fun sendDialog() {
        val toSend = when {
            ((adapter?.countChecked()) ?: 0) > 0 -> adapter?.getAllChecked()!!
            adapter?.currentItem() != null -> arrayListOf(adapter!!.currentItem()!!)
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
        try {
            thread {
                SendOrder(orderRequestArray = orArray) { onSendOrderEnded(it) }.execute()
            }
        } catch (ex: Exception) {
            ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())
        }
    }

    private fun removeResetDialog() {
        val toRemoveReset = when {
            (adapter?.countChecked() ?: 0) > 0 -> adapter?.getAllChecked()!!
            adapter?.currentItem() != null -> arrayListOf(adapter!!.currentItem()!!)
            else -> return
        }

        val toRemove: ArrayList<OrderRequest> = ArrayList()
        val toReset: ArrayList<OrderRequest> = ArrayList()
        for (or in toRemoveReset) {
            if (or.orderRequestedType == OrderRequestType.stockAuditFromDevice) {
                toRemove.add(or)
            } else {
                toReset.add(or)
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

        val currentDir = Statics.getCompletedPath()
        for (i in toRemove) {
            val filePath = currentDir.absolutePath + File.separator + i.filename
            val fl = File(filePath)
            if (!fl.delete()) {
                isOk = false
                break
            }
        }

        if (!isOk) {
            makeText(
                binding.root,
                getString(R.string.an_error_occurred_while_trying_to_delete_the_count),
                ERROR
            )
        }

        thread { fillAdapter(ArrayList()) }
    }

    private fun resetSelected(toReset: ArrayList<OrderRequest>) {
        var isOk = true
        for (orderRequest in toReset) {
            val orcToRemove: ArrayList<OrderRequestContent> = ArrayList()

            orderRequest.completed = null
            orderRequest.finishDate = null
            orderRequest.userId = null

            for (orderRequestContent in orderRequest.content) {
                if (orderRequestContent.qty != null) {
                    if (orderRequestContent.qty!!.qtyRequested == null || orderRequestContent.qty!!.qtyRequested == 0.toDouble()) {
                        orcToRemove.add(orderRequestContent)
                        continue
                    }

                    orderRequestContent.qty!!.qtyCollected = null
                }
            }

            val logContent: ArrayList<OrderRequestContent> = ArrayList(orderRequest.content)
            logContent.removeAll(orcToRemove.toSet())

            orderRequest.content = logContent
            orderRequest.log = Log()

            try {
                orJson = moshi.adapter(OrderRequest::class.java).toJson(orderRequest)
                orderRequest.filename.substringAfterLast('/')

                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    write(orFileName, orJson)
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_EXTERNAL_STORAGE
                    )
                }
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                android.util.Log.e(this::class.java.simpleName, e.message ?: "")
            }

            val fl = File(orderRequest.filename)
            if (!fl.delete()) {
                isOk = false
                break
            }
        }

        if (!isOk) {
            makeText(
                binding.root,
                getString(R.string.an_error_occurred_while_trying_to_delete_the_count),
                ERROR
            )
        }

        thread { fillAdapter(ArrayList()) }
    }

    private fun write(filename: String, value: String) {
        val path = Statics.getPendingPath()
        if (writeToFile(fileName = filename, data = value, directory = path)) {
            finish()
        } else {
            val res = getString(R.string.an_error_occurred_while_trying_to_save_the_count)
            makeText(binding.root, res, ERROR)
            android.util.Log.e(this::class.java.simpleName, res)
        }
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshItem.isRefreshing = show
            }
        }, 20)
    }

    private fun fillAdapter(t: ArrayList<OrderRequest>) {
        if (isListViewFilling) return
        isListViewFilling = true

        Handler(Looper.getMainLooper()).post { run { Screen.closeKeyboard(this) } }

        var temp = t
        if (!temp.any()) {
            temp = getCompletedOrders()
            if (temp.isEmpty()) {
                makeText(
                    binding.root,
                    this.getString(R.string.there_are_no_completed_counts),
                    SnackBarType.INFO
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
            if (status != null) {
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
                    colors[i.id.toInt() - 1], BlendModeCompat.SRC_IN
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
                    adapter!!.addVisibleStatus(OrderRequestType.deliveryAudit)
                } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.deliveryAudit)) {
                    adapter!!.removeVisibleStatus(OrderRequestType.deliveryAudit)
                }
            }

            OrderRequestType.prepareOrder.id.toInt() -> {
                if (item.isChecked && !visibleStatus.contains(OrderRequestType.prepareOrder)) {
                    adapter!!.addVisibleStatus(OrderRequestType.prepareOrder)
                } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.prepareOrder)) {
                    adapter!!.removeVisibleStatus(OrderRequestType.prepareOrder)
                }
            }

            OrderRequestType.receptionAudit.id.toInt() -> {
                if (item.isChecked && !visibleStatus.contains(OrderRequestType.receptionAudit)) {
                    adapter!!.addVisibleStatus(OrderRequestType.receptionAudit)
                } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.receptionAudit)) {
                    adapter!!.removeVisibleStatus(OrderRequestType.receptionAudit)
                }
            }

            OrderRequestType.stockAudit.id.toInt() -> {
                if (item.isChecked && !visibleStatus.contains(OrderRequestType.stockAudit)) {
                    adapter!!.addVisibleStatus(OrderRequestType.stockAudit)
                } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.stockAudit)) {
                    adapter!!.removeVisibleStatus(OrderRequestType.stockAudit)
                }
            }

            OrderRequestType.stockAuditFromDevice.id.toInt() -> {
                if (item.isChecked && !visibleStatus.contains(OrderRequestType.stockAuditFromDevice)) {
                    adapter!!.addVisibleStatus(OrderRequestType.stockAuditFromDevice)
                } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.stockAuditFromDevice)) {
                    adapter!!.removeVisibleStatus(OrderRequestType.stockAuditFromDevice)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_EXTERNAL_STORAGE -> {
                // If the request is canceled, the result arrays are empty.
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    makeText(
                        binding.root, getString(R.string.cannot_write_to_external_storage), ERROR
                    )
                } else {
                    write(orFileName, orJson)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 5001

        private var orFileName = ""
        private var orJson = ""
    }

    override fun onBackPressed() {
        Screen.closeKeyboard(this)

        setResult(RESULT_CANCELED)
        finish()
    }
}