package com.dacosys.warehouseCounter.ui.activities.sync

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.adapter.orderRequest.OrderRequestAdapter
import com.dacosys.warehouseCounter.adapter.orderRequest.OrderRequestAdapter.Companion.getPrefVisibleStatus
import com.dacosys.warehouseCounter.databinding.OutboxActivityBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.writeToFile
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.moshi.log.Log
import com.dacosys.warehouseCounter.moshi.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.moshi.orderRequest.OrderRequest.CREATOR.getCompletedOrders
import com.dacosys.warehouseCounter.moshi.orderRequest.OrderRequestContent
import com.dacosys.warehouseCounter.moshi.orderRequest.OrderRequestType
import com.dacosys.warehouseCounter.retrofit.functions.SendOrder
import com.dacosys.warehouseCounter.ui.activities.orderRequest.OrderRequestDetailActivity
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import org.parceler.Parcels
import java.io.File
import java.io.UnsupportedEncodingException
import kotlin.concurrent.thread

class OutboxActivity : AppCompatActivity() {

    private var isListViewFilling = false
    private var multiSelect = true
    private var arrayAdapter: OrderRequestAdapter? = null
    private var lastSelected: OrderRequest? = null
    private var firstVisiblePos: Int? = null
    private var completeList: ArrayList<OrderRequest> = ArrayList()
    private var checkedIdArray: ArrayList<Int> = ArrayList()
    // endregion

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
        arrayAdapter?.refreshListeners(checkedChangedListener = null, dataSetChangedListener = null)
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString("title", title.toString())
        savedInstanceState.putBoolean("multiSelect", multiSelect)
        if (arrayAdapter != null) {
            savedInstanceState.putParcelableArrayList(
                "completeList", (arrayAdapter ?: return).getAll()
            )
            savedInstanceState.putIntegerArrayList(
                "checkedIdArray", arrayAdapter!!.getAllCheckedAsInt()
            )
            savedInstanceState.putParcelable("lastSelected", (arrayAdapter ?: return).currentItem())
            savedInstanceState.putInt("firstVisiblePos", (arrayAdapter ?: return).firstVisiblePos())
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI(view: View) {
// Set up touch checkedChangedListener for non-text box views to hide keyboard.
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
            (0 until view.childCount).map { view.getChildAt(it) }.forEach { setupUI(it) }
        }
    }

    private lateinit var binding: OutboxActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Statics.setScreenRotation(this)
        binding = OutboxActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var tempTitle = getString(R.string.completed_counts)

        if (savedInstanceState != null) {
            // region Recuperar el título de la ventana
            val t1 = savedInstanceState.getString("title")
            if (t1 != null && t1.isNotEmpty()) tempTitle = t1
            // endregion

            multiSelect = savedInstanceState.getBoolean("multiSelect", multiSelect)
            checkedIdArray = savedInstanceState.getIntegerArrayList("checkedIdArray") ?: ArrayList()
            completeList = savedInstanceState.getParcelableArrayList("completeList") ?: ArrayList()
            lastSelected = savedInstanceState.getParcelable("lastSelected")
            firstVisiblePos =
                if (savedInstanceState.containsKey("firstVisiblePos")) savedInstanceState.getInt("firstVisiblePos") else -1
        } else {
            // Inicializar la actividad

            // Traer los parámetros que recibe la actividad
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString("title")
                if (t1 != null && t1.isNotEmpty()) tempTitle = t1

                multiSelect = extras.getBoolean("multiSelect", true)
            }
        }

        title = tempTitle

        binding.sendButton.setOnClickListener { sendDialog() }
        binding.detailButton.setOnClickListener { showDetail() }
        binding.removeResetButton.setOnClickListener { removeResetDialog() }

        fillAdapter(completeList)

        // ESTO SIRVE PARA OCULTAR EL TECLADO EN PANTALLA CUANDO PIERDEN EL FOCO LOS CONTROLES QUE LO NECESITAN
        setupUI(binding.outbox)
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

    private fun showDetail() {
        if (arrayAdapter != null && arrayAdapter!!.currentItem() != null) {
            val intent = Intent(context(), OrderRequestDetailActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

            intent.putExtra(
                "orderRequest", Parcels.wrap<OrderRequest>(arrayAdapter!!.currentItem())
            )

            // Valid content
            intent.putParcelableArrayListExtra(
                "orcArray", ArrayList(arrayAdapter!!.currentItem()!!.content)
            )

            startActivity(intent)
        }
    }

    private fun sendDialog() {
        val toSend = when {
            ((arrayAdapter?.countChecked()) ?: 0) > 0 -> arrayAdapter?.getAllChecked()!!
            arrayAdapter?.currentItem() != null -> arrayListOf(arrayAdapter!!.currentItem()!!)
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
            (arrayAdapter?.countChecked() ?: 0) > 0 -> arrayAdapter?.getAllChecked()!!
            arrayAdapter?.currentItem() != null -> arrayListOf(arrayAdapter!!.currentItem()!!)
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
                orJson = moshi().adapter(OrderRequest::class.java).toJson(orderRequest)
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

        Handler(Looper.getMainLooper()).post { run { Statics.closeKeyboard(this) } }

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
                if (arrayAdapter != null) {
                    lastSelected = (arrayAdapter ?: return@runOnUiThread).currentItem()
                    firstVisiblePos = (arrayAdapter ?: return@runOnUiThread).firstVisiblePos()
                }

                arrayAdapter = OrderRequestAdapter(
                    activity = this,
                    resource = R.layout.order_request_row,
                    itemList = completeList,
                    suggestedList = completeList,
                    checkedIdArray = checkedIdArray,
                    listView = binding.itemListView,
                    multiSelect = multiSelect
                )

                arrayAdapter?.refreshListeners(
                    checkedChangedListener = null, dataSetChangedListener = null
                )

                while (binding.itemListView.adapter == null) {
                    // Horrible wait for full load
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    run {
                        arrayAdapter?.setSelectItemAndScrollPos(lastSelected, firstVisiblePos)
                    }
                }, 20)
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                showProgressBar(false)
                isListViewFilling = false
            }
        }
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
        val green = Color.parseColor("#FF009688")
        val yellow = Color.parseColor("#FFFFC107")
        val blue = Color.parseColor("#FF2196F3")
        val orange = Color.parseColor("#FFFF5722")
        val green2 = Color.parseColor("#FF4CAF50")

        val colors: ArrayList<Int> = ArrayList()
        colors.add(green)
        colors.add(blue)
        colors.add(orange)
        colors.add(green2)
        colors.add(yellow)
        //endregion Icon colors

        for (i in OrderRequestType.getAll()) {
            val icon = ResourcesCompat.getDrawable(context().resources, R.drawable.ic_lens, null)
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
        if (arrayAdapter == null) {
            return false
        }

        val id = item.itemId
        if (id == R.id.home || id == android.R.id.home) {
            onBackPressed()
            return true
        }

        item.isChecked = !item.isChecked
        val visibleStatus = arrayAdapter!!.getVisibleStatus()

        when (id) {
            OrderRequestType.deliveryAudit.id.toInt() -> if (item.isChecked && !visibleStatus.contains(
                    OrderRequestType.deliveryAudit
                )
            ) {
                arrayAdapter!!.addVisibleStatus(OrderRequestType.deliveryAudit)
            } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.deliveryAudit)) {
                arrayAdapter!!.removeVisibleStatus(OrderRequestType.deliveryAudit)
            }
            OrderRequestType.prepareOrder.id.toInt() -> if (item.isChecked && !visibleStatus.contains(
                    OrderRequestType.prepareOrder
                )
            ) {
                arrayAdapter!!.addVisibleStatus(OrderRequestType.prepareOrder)
            } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.prepareOrder)) {
                arrayAdapter!!.removeVisibleStatus(OrderRequestType.prepareOrder)
            }
            OrderRequestType.receptionAudit.id.toInt() -> if (item.isChecked && !visibleStatus.contains(
                    OrderRequestType.receptionAudit
                )
            ) {
                arrayAdapter!!.addVisibleStatus(OrderRequestType.receptionAudit)
            } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.receptionAudit)) {
                arrayAdapter!!.removeVisibleStatus(OrderRequestType.receptionAudit)
            }
            OrderRequestType.stockAudit.id.toInt() -> if (item.isChecked && !visibleStatus.contains(
                    OrderRequestType.stockAudit
                )
            ) {
                arrayAdapter!!.addVisibleStatus(OrderRequestType.stockAudit)
            } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.stockAudit)) {
                arrayAdapter!!.removeVisibleStatus(OrderRequestType.stockAudit)
            }
            OrderRequestType.stockAuditFromDevice.id.toInt() -> if (item.isChecked && !visibleStatus.contains(
                    OrderRequestType.stockAuditFromDevice
                )
            ) {
                arrayAdapter!!.addVisibleStatus(OrderRequestType.stockAuditFromDevice)
            } else if (!item.isChecked && visibleStatus.contains(OrderRequestType.stockAuditFromDevice)) {
                arrayAdapter!!.removeVisibleStatus(OrderRequestType.stockAuditFromDevice)
            }
            else -> return super.onOptionsItemSelected(item)
        }

        // Guardar los valores en las preferencias
        val set = HashSet<String>()
        for (i in visibleStatus) {
            set.add(i.id.toString())
        }
        settingViewModel().orderRequestVisibleStatus = set

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
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
        Statics.closeKeyboard(this)

        setResult(RESULT_CANCELED)
        finish()
    }
}