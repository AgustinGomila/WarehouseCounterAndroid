package com.dacosys.warehouseCounter.ui.activities.sync

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.adapter.orderRequest.OrderRequestAdapter
import com.dacosys.warehouseCounter.databinding.InboxActivityBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.moshi.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.moshi.orderRequest.OrderRequestType
import com.dacosys.warehouseCounter.ui.activities.orderRequest.OrderRequestDetailActivity
import com.dacosys.warehouseCounter.ui.snackBar.MakeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import org.parceler.Parcels
import java.io.File
import kotlin.concurrent.thread

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class InboxActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    private var isListViewFilling = false
    private var multiSelect = false
    private var arrayAdapter: OrderRequestAdapter? = null
    private var lastSelected: OrderRequest? = null
    private var firstVisiblePos: Int? = null
    private var completeList: ArrayList<OrderRequest> = ArrayList()
    private var checkedIdArray: ArrayList<Int> = ArrayList()
    // endregion

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
                "completeList",
                (arrayAdapter ?: return).getAll()
            )
            savedInstanceState.putIntegerArrayList(
                "checkedIdArray",
                arrayAdapter!!.getAllCheckedAsInt()
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

    private lateinit var binding: InboxActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Statics.setScreenRotation(this)
        binding = InboxActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var tempTitle = getString(R.string.pending_counts)

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

                multiSelect = extras.getBoolean("multiSelect", false)
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

        fillAdapter(completeList)

        // ESTO SIRVE PARA OCULTAR EL TECLADO EN PANTALLA CUANDO PIERDEN EL FOCO LOS CONTROLES QUE LO NECESITAN
        setupUI(binding.inbox)
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
                "orderRequest",
                Parcels.wrap<OrderRequest>(arrayAdapter!!.currentItem())
            )

            // Valid content
            intent.putParcelableArrayListExtra(
                "orcArray",
                ArrayList(arrayAdapter!!.currentItem()!!.content)
            )

            startActivity(intent)
        }
    }

    private fun removeDialog() {
        val toRemove = when {
            ((arrayAdapter?.countChecked()) ?: 0) > 0 -> arrayAdapter?.getAllChecked()!!
            arrayAdapter?.currentItem() != null -> arrayListOf(arrayAdapter!!.currentItem()!!)
            else -> return
        }

        val msg = when {
            toRemove.count() > 1 -> getString(R.string.do_you_want_to_delete_the_selected_counts)
            else -> getString(R.string.do_you_want_to_delete_the_selected_count)
        }

        val alert = AlertDialog.Builder(this)
        alert.setTitle(getString(R.string.cancel_count))
        alert.setMessage(msg)
        alert.setNegativeButton(R.string.cancel, null)
        alert.setPositiveButton(R.string.ok) { _, _ ->
            if (toRemove.isNotEmpty()) {
                removeSelected(toRemove)
            }
        }

        alert.show()
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
        if (!multiSelect && arrayAdapter?.currentItem() != null) {
            Statics.closeKeyboard(this)

            val data = Intent()
            data.putParcelableArrayListExtra(
                "orderRequests",
                arrayListOf(arrayAdapter!!.currentItem())
            )
            setResult(RESULT_OK, data)
            finish()
        } else if (multiSelect && ((arrayAdapter?.countChecked()) ?: 0) > 0) {
            Statics.closeKeyboard(this)

            val data = Intent()
            data.putParcelableArrayListExtra("orderRequests", arrayAdapter!!.getAllChecked())
            setResult(RESULT_OK, data)
            finish()
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
            temp = OrderRequest.getPendingOrders()
            if (temp.isEmpty()) {
                MakeText.makeText(
                    binding.root,
                    getString(R.string.there_are_no_pending_counts),
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
                    checkedChangedListener = null,
                    dataSetChangedListener = null
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
                .setChecked(OrderRequestAdapter.getPrefVisibleStatus().contains(i)).isCheckable =
                true
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

    override fun onBackPressed() {
        Statics.closeKeyboard(this)

        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshItem.isRefreshing = false
            }
        }, 100)
    }
}