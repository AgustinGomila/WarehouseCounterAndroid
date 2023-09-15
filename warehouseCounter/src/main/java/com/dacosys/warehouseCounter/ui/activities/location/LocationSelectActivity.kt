package com.dacosys.warehouseCounter.ui.activities.location

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Rack
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Warehouse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.GetRack
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.GetWarehouse
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.GetWarehouseArea
import com.dacosys.warehouseCounter.databinding.LocationSelectActivityBinding
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.ui.adapter.location.RackAdapter
import com.dacosys.warehouseCounter.ui.adapter.location.WarehouseAdapter
import com.dacosys.warehouseCounter.ui.adapter.location.WarehouseAreaAdapter
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.views.ContractsAutoCompleteTextView
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import org.parceler.Parcels
import kotlin.concurrent.thread

class LocationSelectActivity : AppCompatActivity(), ContractsAutoCompleteTextView.OnContractsAvailability,
    KeyboardVisibilityEventListener {
    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        Screen.closeKeyboard(this)
        binding.warehouse.setOnContractsAvailability(null)
        binding.warehouse.setAdapter(null)
        binding.warehouseArea.setOnContractsAvailability(null)
        binding.warehouseArea.setAdapter(null)
        binding.rackCode.setOnContractsAvailability(null)
        binding.rackCode.setAdapter(null)
        Screen.closeKeyboard(this)
    }

    private var fillRequired = false

    private var warehouse: Warehouse? = null
    private var warehouseArea: WarehouseArea? = null
    private var rack: Rack? = null

    private var warehouseVisible: Boolean = true
    private var warehouseAreaVisible: Boolean = true
    private var rackVisible: Boolean = true

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString(ARG_TITLE, title.toString())

        savedInstanceState.putParcelable(ARG_WAREHOUSE, warehouse)
        savedInstanceState.putParcelable(ARG_WAREHOUSE_AREA, warehouseArea)
        savedInstanceState.putParcelable(ARG_RACK, rack)

        savedInstanceState.putBoolean(ARG_WAREHOUSE_VISIBLE, warehouseVisible)
        savedInstanceState.putBoolean(ARG_WAREHOUSE_AREA_VISIBLE, warehouseAreaVisible)
        savedInstanceState.putBoolean(ARG_RACK_VISIBLE, rackVisible)
    }

    private fun loadSavedValues(b: Bundle) {
        val t1 = b.getString(ARG_TITLE)
        title = if (!t1.isNullOrEmpty()) t1
        else getString(R.string.select_area)

        warehouse = b.getParcelable(ARG_WAREHOUSE)
        warehouseArea = b.getParcelable(ARG_WAREHOUSE_AREA)
        rack = b.getParcelable(ARG_RACK)

        warehouseAreaVisible =
            if (b.containsKey(ARG_WAREHOUSE_AREA_VISIBLE)) b.getBoolean(ARG_WAREHOUSE_AREA_VISIBLE)
            else true

        rackVisible =
            if (b.containsKey(ARG_RACK_VISIBLE)) b.getBoolean(ARG_RACK_VISIBLE)
            else true

        warehouseVisible =
            if (b.containsKey(ARG_WAREHOUSE_VISIBLE)) b.getBoolean(ARG_WAREHOUSE_VISIBLE)
            else true
    }

    private lateinit var binding: LocationSelectActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = LocationSelectActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Permite finalizar la actividad si se toca la pantalla
        // fuera de la ventana. Esta actividad se ve como un diálogo.
        setFinishOnTouchOutside(true)

        if (savedInstanceState != null) {
            loadSavedValues(savedInstanceState)

            binding.warehouse.setOnEditorActionListener(null)
            binding.warehouse.onItemClickListener = null
            binding.warehouse.onFocusChangeListener = null
            binding.warehouse.setOnDismissListener(null)

            binding.warehouseArea.setOnEditorActionListener(null)
            binding.warehouseArea.onItemClickListener = null
            binding.warehouseArea.onFocusChangeListener = null
            binding.warehouseArea.setOnDismissListener(null)

            binding.rackCode.setOnEditorActionListener(null)
            binding.rackCode.onItemClickListener = null
            binding.rackCode.onFocusChangeListener = null
            binding.rackCode.setOnDismissListener(null)
        } else {
            val extras = intent.extras
            if (extras != null) {
                loadSavedValues(extras)
            }
        }

        // To fill the control in the onStart after this onCreate.
        fillRequired = true

        binding.root.setOnClickListener { onBackPressed() }

        binding.warehouseClearImageView.setOnClickListener { setWarehouse(null) }
        binding.warehouseAreaClearImageView.setOnClickListener { setWarehouseArea(null) }
        binding.rackClearImageView.setOnClickListener { setRack(null) }

        binding.selectButton.setOnClickListener { locationSelect() }
        binding.scanButton.setOnClickListener {
            JotterListener.toggleCameraFloatingWindowVisibility(this)
        }

        // region Setup WAREHOUSE ID AUTOCOMPLETE
        binding.warehouse.threshold = 1
        binding.warehouse.hint = getString(R.string.warehouse)
        binding.warehouse.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val adapter = binding.warehouse.adapter
            if (adapter is WarehouseAdapter) {
                setWarehouse(adapter.getItem(position))
                if (!warehouseAreaVisible) {
                    locationSelect()
                }
            }
        }
        binding.warehouse.setOnContractsAvailability(this)
        binding.warehouse.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            val adapter = binding.warehouse.adapter
            if (adapter is WarehouseAdapter) {
                if (hasFocus && binding.warehouse.text.trim().length >= binding.warehouse.threshold && adapter.count > 0 && !binding.warehouse.isPopupShowing) {
                    // Display the suggestion dropdown on focus
                    Handler(Looper.getMainLooper()).post {
                        run {
                            adjustAndShowWarehouseDropDown()
                        }
                    }
                }
            }
        }
        binding.warehouse.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                Screen.showKeyboard(this)
                adjustDropDownHeight()
                return@setOnTouchListener false
            } else if (motionEvent.action == MotionEvent.BUTTON_BACK) {
                Screen.closeKeyboard(this)

                setResult(RESULT_CANCELED, null)
                finish()
                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }
        binding.warehouse.setOnEditorActionListener { _, keyCode, keyEvent ->
            if (keyCode == EditorInfo.IME_ACTION_DONE || (keyEvent.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_UNKNOWN || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER))) {
                val adapter = binding.warehouse.adapter
                if (adapter is WarehouseAdapter) {
                    if (binding.warehouse.text.trim().length >= binding.warehouse.threshold) {
                        val all = adapter.getAll()
                        if (all.any()) {
                            var founded = false
                            for (a in all) {
                                if (a.description.isEmpty()) {
                                    continue
                                }
                                if (a.description.startsWith(binding.warehouse.text.toString().trim(), true)) {
                                    setWarehouse(a)
                                    founded = true
                                    break
                                }
                            }
                            if (!founded) {
                                for (a in all) {
                                    if (a.description.isEmpty()) {
                                        continue
                                    }
                                    if (a.description.contains(binding.warehouse.text.toString().trim(), true)) {
                                        setWarehouse(a)
                                        break
                                    }
                                }
                            }
                        }
                    }
                    if (!warehouseAreaVisible) {
                        locationSelect()
                    }
                }
                true
            } else {
                false
            }
        }
        // endregion

        // region Setup WAREHOUSE_AREA ID AUTOCOMPLETE
        binding.warehouseArea.threshold = 1
        binding.warehouseArea.hint = getString(R.string.area)
        binding.warehouseArea.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val adapter = binding.warehouseArea.adapter
            if (adapter is WarehouseAreaAdapter) {
                setWarehouseArea(adapter.getItem(position))
                if (!rackVisible) {
                    locationSelect()
                }
            }
        }
        binding.warehouseArea.setOnContractsAvailability(this)
        binding.warehouseArea.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            val adapter = binding.warehouseArea.adapter
            if (adapter is WarehouseAreaAdapter) {
                if (hasFocus && binding.warehouseArea.text.trim().length >= binding.warehouseArea.threshold && adapter.count > 0 && !binding.warehouseArea.isPopupShowing) {
                    // Display the suggestion dropdown on focus
                    Handler(Looper.getMainLooper()).post {
                        run {
                            adjustAndShowWarehouseAreaDropDown()
                        }
                    }
                }
            }
        }
        binding.warehouseArea.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                Screen.showKeyboard(this)
                adjustDropDownHeight()
                return@setOnTouchListener false
            } else if (motionEvent.action == MotionEvent.BUTTON_BACK) {
                Screen.closeKeyboard(this)

                setResult(RESULT_CANCELED, null)
                finish()
                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }
        binding.warehouseArea.setOnEditorActionListener { _, keyCode, keyEvent ->
            if (keyCode == EditorInfo.IME_ACTION_DONE || (keyEvent.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_UNKNOWN || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER))) {
                val adapter = binding.warehouseArea.adapter
                if (adapter is WarehouseAreaAdapter) {
                    if (binding.warehouseArea.text.trim().length >= binding.warehouseArea.threshold) {
                        val all = adapter.getAll()

                        if (all.any()) {
                            var founded = false
                            for (a in all) {
                                if (a.description.isEmpty()) {
                                    continue
                                }
                                if (a.description.startsWith(binding.warehouseArea.text.toString().trim(), true)) {
                                    setWarehouseArea(a)
                                    founded = true
                                    break
                                }
                            }
                            if (!founded) {
                                for (a in all) {
                                    if (a.description.isEmpty()) {
                                        continue
                                    }
                                    if (a.description.contains(
                                            binding.warehouseArea.text.toString().trim(), true
                                        )
                                    ) {
                                        setWarehouseArea(a)
                                        break
                                    }
                                }
                            }
                        }
                    }
                    if (!rackVisible) {
                        locationSelect()
                    }
                }
                true
            } else {
                false
            }
        }
        // endregion

        // region Setup RACK ID AUTOCOMPLETE
        binding.rackCode.threshold = 1
        binding.rackCode.hint = getString(R.string.rack_code)
        binding.rackCode.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val adapter = binding.rackCode.adapter
            if (adapter is RackAdapter) {
                setRack(adapter.getItem(position))
                locationSelect()
            }
        }
        binding.rackCode.setOnContractsAvailability(this)
        binding.rackCode.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            val adapter = binding.rackCode.adapter
            if (adapter is RackAdapter) {
                if (hasFocus && binding.rackCode.text.trim().length >= binding.rackCode.threshold && adapter.count > 0 && !binding.rackCode.isPopupShowing) {
                    // Display the suggestion dropdown on focus
                    Handler(Looper.getMainLooper()).post {
                        run {
                            adjustAndShowRackDropDown()
                        }
                    }
                }
            }
        }
        binding.rackCode.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                Screen.showKeyboard(this)
                adjustDropDownHeight()
                return@setOnTouchListener false
            } else if (motionEvent.action == MotionEvent.BUTTON_BACK) {
                Screen.closeKeyboard(this)

                setResult(RESULT_CANCELED, null)
                finish()
                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }
        binding.rackCode.setOnEditorActionListener { _, keyCode, keyEvent ->
            if (keyCode == EditorInfo.IME_ACTION_DONE || (keyEvent.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_UNKNOWN || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER))) {
                val adapter = binding.rackCode.adapter
                if (adapter is RackAdapter) {
                    if (binding.rackCode.text.trim().length >= binding.rackCode.threshold) {
                        val all = adapter.getAll()

                        if (all.any()) {
                            var founded = false
                            for (a in all) {
                                if (a.code.isEmpty()) {
                                    continue
                                }
                                if (a.code.startsWith(binding.rackCode.text.toString().trim(), true)) {
                                    setRack(a)
                                    founded = true
                                    break
                                }
                            }
                            if (!founded) {
                                for (a in all) {
                                    if (a.code.isEmpty()) {
                                        continue
                                    }
                                    if (a.code.contains(binding.rackCode.text.toString().trim(), true)) {
                                        setRack(a)
                                        break
                                    }
                                }
                            }
                        }
                    }
                    locationSelect()
                }
                true
            } else {
                false
            }
        }
        // endregion

        binding.warehouse.visibility = if (warehouseVisible) VISIBLE else View.GONE
        binding.warehouseProgressBar.visibility = if (warehouseVisible) VISIBLE else View.GONE
        binding.warehouseClearImageView.visibility = if (warehouseVisible) VISIBLE else View.GONE

        binding.warehouseArea.visibility = if (warehouseAreaVisible) VISIBLE else View.GONE
        binding.warehouseAreaProgressBar.visibility = if (warehouseAreaVisible) VISIBLE else View.GONE
        binding.warehouseAreaClearImageView.visibility = if (warehouseAreaVisible) VISIBLE else View.GONE

        binding.rackCode.visibility = if (rackVisible) VISIBLE else View.GONE
        binding.rackCodeProgressBar.visibility = if (rackVisible) VISIBLE else View.GONE
        binding.rackClearImageView.visibility = if (rackVisible) VISIBLE else View.GONE

        KeyboardVisibilityEvent.registerEventListener(this, this)
        refreshWarehouseText(cleanText = false, focus = warehouseVisible && !warehouseAreaVisible)
        refreshWarehouseAreaText(cleanText = false, focus = warehouseAreaVisible)
        refreshRackText(cleanText = false, focus = rackVisible && !warehouseAreaVisible)
    }

    private fun setWarehouse(w: Warehouse?) {
        warehouse = w
        refreshWarehouseText(cleanText = w == null, focus = w == null)

        if (warehouseAreaVisible && binding.warehouseArea.adapter != null) {
            warehouseArea = null
            (binding.warehouseArea.adapter as WarehouseAreaAdapter).setFilterWarehouse(warehouse)
            refreshWarehouseAreaText(cleanText = true, focus = w != null)
        }

        if (rackVisible && binding.rackCode.adapter != null) {
            rack = null
            (binding.rackCode.adapter as RackAdapter).setFilterWarehouseArea(warehouseArea)
            refreshRackText(cleanText = true, focus = false)
        }
    }

    private fun setWarehouseArea(wa: WarehouseArea?) {
        warehouseArea = wa
        refreshWarehouseAreaText(cleanText = wa == null, focus = wa == null)

        if (rackVisible && binding.rackCode.adapter != null) {
            rack = null
            (binding.rackCode.adapter as RackAdapter).setFilterWarehouseArea(warehouseArea)
            refreshRackText(cleanText = true, focus = wa != null)
        }
    }

    private fun setRack(r: Rack?) {
        rack = r
        refreshRackText(cleanText = r == null, focus = r == null)
    }

    private fun locationSelect() {
        Screen.closeKeyboard(this)

        val data = Intent()
        data.putExtra(ARG_RACK, Parcels.wrap(rack))
        data.putExtra(ARG_WAREHOUSE_AREA, Parcels.wrap(warehouseArea))
        data.putExtra(ARG_WAREHOUSE, Parcels.wrap(warehouse))
        setResult(RESULT_OK, data)
        finish()
    }

    private fun refreshWarehouseText(cleanText: Boolean, focus: Boolean) {
        runOnUiThread {
            binding.warehouse.setText(
                if (warehouse == null) {
                    if (cleanText) {
                        ""
                    } else {
                        binding.warehouse.text.toString()
                    }
                } else {
                    warehouse?.description ?: ""
                }
            )

            binding.warehouse.post {
                binding.warehouse.setSelection(binding.warehouse.length())
            }

            if (focus) {
                binding.warehouse.requestFocus()
            }
        }
    }

    private fun refreshWarehouseAreaText(cleanText: Boolean, focus: Boolean) {
        runOnUiThread {
            binding.warehouseArea.setText(
                if (warehouseArea == null) {
                    if (cleanText) {
                        ""
                    } else {
                        binding.warehouseArea.text.toString()
                    }
                } else {
                    warehouseArea?.description ?: ""
                }
            )

            binding.warehouseArea.post {
                binding.warehouseArea.setSelection(binding.warehouseArea.length())
            }

            if (focus) {
                binding.warehouseArea.requestFocus()
            }
        }
    }

    private fun refreshRackText(cleanText: Boolean, focus: Boolean) {
        runOnUiThread {
            binding.rackCode.setText(
                if (rack == null) {
                    if (cleanText) {
                        ""
                    } else {
                        binding.rackCode.text.toString()
                    }
                } else {
                    rack?.code ?: ""
                }
            )

            binding.rackCode.post {
                binding.rackCode.setSelection(binding.rackCode.length())
            }

            if (focus) {
                binding.rackCode.requestFocus()
            }
        }
    }

    private fun showWarehouseProgressBar(visibility: Int) {
        runOnUiThread {
            binding.warehouseProgressBar.visibility = visibility
        }
    }

    private fun showWarehouseAreaProgressBar(visibility: Int) {
        runOnUiThread {
            binding.warehouseAreaProgressBar.visibility = visibility
        }
    }

    private fun showRackProgressBar(visibility: Int) {
        runOnUiThread {
            binding.rackCodeProgressBar.visibility = visibility
        }
    }

    override fun onStart() {
        super.onStart()

        if (fillRequired) {
            fillRequired = false
            fillAdapter()
        }
    }

    var isFilling = false
    private fun fillAdapter() {
        if (isFilling) return
        isFilling = true

        if (warehouseVisible) getWarehouse()
        if (warehouseAreaVisible) getWarehouseArea()
        if (rackVisible) getRack()
    }

    private fun getWarehouse() {
        showWarehouseProgressBar(VISIBLE)
        thread {
            GetWarehouse(onEvent = {
                if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(
                    it.text,
                    it.snackBarType
                )
            }, onFinish = {
                fillWarehouse(it)
                showWarehouseProgressBar(View.GONE)
                isFilling = false
            }).execute()
        }
    }

    private fun getWarehouseArea() {
        showWarehouseAreaProgressBar(VISIBLE)
        thread {
            GetWarehouseArea(onEvent = {
                if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(
                    it.text,
                    it.snackBarType
                )
            }, onFinish = {
                fillWarehouseArea(it)
                showWarehouseAreaProgressBar(View.GONE)
                isFilling = false
            }).execute()
        }
    }

    private fun getRack() {
        showRackProgressBar(VISIBLE)
        thread {
            GetRack(
                onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it.text, it.snackBarType) },
                onFinish = {
                    fillRack(it)
                    showRackProgressBar(View.GONE)
                    isFilling = false
                }).execute()
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    private var isWFilling = false
    private fun fillWarehouse(it: ArrayList<Warehouse>) {
        if (isWFilling) return
        isWFilling = true

        val adapter = WarehouseAdapter(
            activity = this,
            resource = R.layout.warehouse_row,
            warehouseArray = ArrayList(it),
        )

        runOnUiThread {
            binding.warehouse.setAdapter(adapter)
            (binding.warehouse.adapter as WarehouseAdapter).notifyDataSetChanged()

            while (binding.warehouse.adapter == null) {
                // Wait for complete loaded
            }
            refreshWarehouseText(cleanText = false, focus = false)
            showWarehouseProgressBar(View.GONE)
        }

        isWFilling = false
    }

    private var isWaFilling = false
    private fun fillWarehouseArea(it: ArrayList<WarehouseArea>) {
        if (isWaFilling) return
        isWaFilling = true

        val adapter = WarehouseAreaAdapter(
            activity = this,
            resource = R.layout.warehouse_area_row,
            filterWarehouse = warehouse,
            warehouseAreaArray = ArrayList(it)
        )

        runOnUiThread {
            binding.warehouseArea.setAdapter(adapter)
            (binding.warehouseArea.adapter as WarehouseAreaAdapter).notifyDataSetChanged()

            while (binding.warehouseArea.adapter == null) {
                // Wait for complete loaded
            }
            refreshWarehouseAreaText(cleanText = false, focus = false)
            showWarehouseAreaProgressBar(View.GONE)
        }

        isWaFilling = false
    }

    private var isRFilling = false
    private fun fillRack(it: ArrayList<Rack>) {
        if (isRFilling) return
        isRFilling = true

        val adapter = RackAdapter(
            activity = this,
            resource = R.layout.rack_row,
            filterWarehouseArea = warehouseArea,
            rackArray = ArrayList(it)
        )

        runOnUiThread {
            binding.rackCode.setAdapter(adapter)
            (binding.rackCode.adapter as RackAdapter).notifyDataSetChanged()

            while (binding.rackCode.adapter == null) {
                // Wait for complete loaded
            }
            refreshRackText(cleanText = false, focus = false)
            showRackProgressBar(View.GONE)
        }

        isRFilling = false
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        Screen.closeKeyboard(this)

        setResult(RESULT_CANCELED)
        finish()
    }

    // region SOFT KEYBOARD AND DROPDOWN ISSUES
    override fun onVisibilityChanged(isOpen: Boolean) {
        adjustDropDownHeight()
    }

    private fun calculateDropDownHeight(): Int {
        val r = Rect()
        val mRootWindow = window
        val view: View = mRootWindow.decorView
        view.getWindowVisibleDisplayFrame(r)
        return r.bottom - r.top
    }

    private fun adjustAndShowRackDropDown(c: Int = 0) {
        // TOP LAYOUT
        topLayout()

        val viewHeight = settingViewModel.locationViewHeight
        val maxNeeded = (binding.rackCode.adapter?.count ?: 0) * viewHeight
        val availableHeight = calculateDropDownHeight() - (binding.rackCode.y + binding.rackCode.height).toInt()
        var newHeight = availableHeight / viewHeight * viewHeight
        if (maxNeeded < newHeight) {
            newHeight = maxNeeded
        }
        binding.rackCode.dropDownHeight = newHeight
        binding.rackCode.showDropDown()
    }

    private fun adjustAndShowWarehouseAreaDropDown(c: Int = 0) {
        // TOP LAYOUT
        topLayout()

        val viewHeight = settingViewModel.locationViewHeight
        val maxNeeded = (binding.warehouseArea.adapter?.count ?: 0) * viewHeight
        val availableHeight =
            calculateDropDownHeight() - (binding.warehouseArea.y + binding.warehouseArea.height).toInt()
        var newHeight = availableHeight / viewHeight * viewHeight
        if (maxNeeded < newHeight) {
            newHeight = maxNeeded
        }
        binding.warehouseArea.dropDownHeight = newHeight
        binding.warehouseArea.showDropDown()
    }

    private fun adjustAndShowWarehouseDropDown(c: Int = 0) {
        // TOP LAYOUT
        topLayout()

        val viewHeight = settingViewModel.locationViewHeight
        val maxNeeded = (binding.warehouse.adapter?.count ?: 0) * viewHeight
        val availableHeight = calculateDropDownHeight() - (binding.warehouse.y + binding.warehouse.height).toInt()
        var newHeight = availableHeight / viewHeight * viewHeight
        if (maxNeeded < newHeight) {
            newHeight = maxNeeded
        }
        binding.warehouse.dropDownHeight = newHeight
        binding.warehouse.showDropDown()
    }

    private var isTopping = false
    private fun topLayout() {
        if (isTopping) return
        isTopping = true
        val set = ConstraintSet()

        set.clone(binding.root)
        set.clear(binding.gralLayout.id, ConstraintSet.BOTTOM)
        set.applyTo(binding.root)

        isTopping = false
    }

    private var isCentring = false
    private fun centerLayout() {
        if (isCentring) return

        isCentring = true
        val set = ConstraintSet()

        set.clone(binding.root)
        set.connect(
            binding.gralLayout.id, ConstraintSet.BOTTOM, binding.root.id, ConstraintSet.BOTTOM, 0
        )
        set.applyTo(binding.root)

        isCentring = false
    }

    private fun adjustDropDownHeight(c: Int = 0) {
        runOnUiThread {
            when {
                binding.warehouse.isPopupShowing -> {
                    adjustAndShowWarehouseDropDown(c)
                }

                binding.warehouseArea.isPopupShowing -> {
                    adjustAndShowWarehouseAreaDropDown(c)
                }

                binding.rackCode.isPopupShowing -> {
                    adjustAndShowRackDropDown(c)
                }

                else -> {
                    centerLayout()
                }
            }
        }
    }

    override fun contractsRetrieved(count: Int) {
        if (count > 0) {
            adjustDropDownHeight(count)
        } else {
            // CENTER LAYOUT
            centerLayout()
        }
    }
    // endregion SOFT KEYBOARD AND DROPDOWN ISSUES

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_WAREHOUSE = "warehouse"
        const val ARG_WAREHOUSE_AREA = "warehouseArea"
        const val ARG_RACK = "rack"
        const val ARG_WAREHOUSE_AREA_VISIBLE = "warehouseAreaVisible"
        const val ARG_WAREHOUSE_VISIBLE = "warehouseVisible"
        const val ARG_RACK_VISIBLE = "rackVisible"
    }
}
