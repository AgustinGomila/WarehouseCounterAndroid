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
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.databinding.LocationSelectActivityBinding
import com.dacosys.warehouseCounter.ktor.v2.dto.location.*
import com.dacosys.warehouseCounter.ktor.v2.functions.location.GetRack
import com.dacosys.warehouseCounter.ktor.v2.functions.location.GetWarehouse
import com.dacosys.warehouseCounter.ktor.v2.functions.location.GetWarehouseArea
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.adapter.location.RackAdapter
import com.dacosys.warehouseCounter.ui.adapter.location.WarehouseAdapter
import com.dacosys.warehouseCounter.ui.adapter.location.WarehouseAreaAdapter
import com.dacosys.warehouseCounter.ui.snackBar.MakeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.views.ContractsAutoCompleteTextView
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import kotlin.concurrent.thread

class LocationSelectActivity : AppCompatActivity(), ContractsAutoCompleteTextView.OnContractsAvailability,
    KeyboardVisibilityEventListener {
    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        Screen.closeKeyboard(this)
        binding.autoCompleteTextView.setOnContractsAvailability(null)
        binding.autoCompleteTextView.setAdapter(null)
    }

    private var location: Location? = null
    private lateinit var locationType: LocationType

    private fun cleanLocation() {
        location = null
    }

    private fun getLocation(b: Bundle) {
        if (b.containsKey(ARG_LOCATION)) location = b.getParcelable(ARG_LOCATION)
        if (b.containsKey(ARG_LOCATION_TYPE)) locationType = b.get(ARG_LOCATION_TYPE) as LocationType
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        when (location) {
            is Warehouse -> savedInstanceState.putParcelable(ARG_LOCATION, location as Warehouse)
            is WarehouseArea -> savedInstanceState.putParcelable(ARG_LOCATION, location as WarehouseArea)
            is Rack -> savedInstanceState.putParcelable(ARG_LOCATION, location as Rack)
        }
        savedInstanceState.putSerializable(ARG_LOCATION_TYPE, locationType)
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

        var tempTitle = getString(R.string.search_area_)
        if (savedInstanceState != null) {
            // Dejo de escuchar estos eventos hasta pasar los valores guardados
            // más adelante se reconectan
            binding.autoCompleteTextView.setOnEditorActionListener(null)
            binding.autoCompleteTextView.onItemClickListener = null
            binding.autoCompleteTextView.setOnTouchListener(null)
            binding.autoCompleteTextView.onFocusChangeListener = null
            binding.autoCompleteTextView.setOnDismissListener(null)

            getLocation(savedInstanceState)
        } else {
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString(ARG_TITLE)
                if (!t1.isNullOrEmpty()) tempTitle = t1

                getLocation(extras)
            }
        }

        title = tempTitle

        binding.locationSelect.setOnClickListener { onBackPressed() }

        binding.clearImageView.setOnClickListener {
            cleanLocation()
            refreshText(cleanText = true, focus = true)
        }

        // region Setup CATEGORY_CATEGORY ID AUTOCOMPLETE
        // Set an warehouseArea click checkedChangedListener for auto complete text view
        binding.autoCompleteTextView.threshold = 1
        binding.autoCompleteTextView.hint = tempTitle
        binding.autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val adapter = binding.autoCompleteTextView.adapter ?: return@OnItemClickListener
            when (adapter) {
                is WarehouseAdapter -> setLocation(adapter.getItem(position))

                is WarehouseAreaAdapter -> setLocation(adapter.getItem(position))

                is RackAdapter -> setLocation(adapter.getItem(position))
            }
            locationSelected()
        }
        binding.autoCompleteTextView.setOnContractsAvailability(this)
        binding.autoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus &&
                binding.autoCompleteTextView.text.trim().length >= binding.autoCompleteTextView.threshold &&
                !binding.autoCompleteTextView.isPopupShowing
            ) {
                var count = 0
                val adapter = binding.autoCompleteTextView.adapter ?: return@OnFocusChangeListener
                when (adapter) {
                    is WarehouseAdapter -> count = adapter.count()

                    is WarehouseAreaAdapter -> count = adapter.count()

                    is RackAdapter -> count = adapter.count()
                }
                if (count == 0) return@OnFocusChangeListener

                // Display the suggestion dropdown on focus
                Handler(Looper.getMainLooper()).post {
                    adjustAndShowDropDown()
                }
            }
        }
        binding.autoCompleteTextView.setOnTouchListener { _, motionEvent ->
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
        binding.autoCompleteTextView.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                if (binding.autoCompleteTextView.text.trim().length >= binding.autoCompleteTextView.threshold) {
                    val adapter = binding.autoCompleteTextView.adapter ?: return@setOnEditorActionListener false
                    when (adapter) {
                        is WarehouseAdapter -> setLocation(adapter.getAll())

                        is WarehouseAreaAdapter -> setLocation(adapter.getAll())

                        is RackAdapter -> setLocation(adapter.getAll())
                    }
                    locationSelected()
                }
                true
            } else {
                false
            }
        }
        // endregion

        KeyboardVisibilityEvent.registerEventListener(this, this)
    }

    private fun setLocation(it: Location?) {
        if (it != null) location = it
    }

    private fun setLocation(all: List<Location>) {
        if (all.any()) {
            var founded = false
            for (a in all) {
                val desc = a.locationParentStr
                if (desc.startsWith(binding.autoCompleteTextView.text.toString().trim(), true)) {
                    location = a
                    founded = true
                    break
                }
            }

            if (!founded) {
                for (a in all) {
                    val desc = a.locationParentStr
                    if (desc.contains(binding.autoCompleteTextView.text.toString().trim(), true)) {
                        location = a
                        break
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        refreshText(cleanText = false, focus = true)

        fillAdapter()
    }

    private fun showProgressBar(visibility: Int) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progressBar.visibility = visibility
        }, 20)
    }

    private fun refreshText(cleanText: Boolean, focus: Boolean) {
        runOnUiThread {
            binding.autoCompleteTextView.setText(
                if (location == null) {
                    if (cleanText) {
                        ""
                    } else {
                        binding.autoCompleteTextView.text.toString()
                    }
                } else {
                    location?.locationDescription
                }
            )

            binding.autoCompleteTextView.post {
                binding.autoCompleteTextView.setSelection(
                    binding.autoCompleteTextView.length()
                )
            }

            if (focus) {
                binding.autoCompleteTextView.requestFocus()
            }
        }
    }

    private fun locationSelected() {
        Screen.closeKeyboard(this)

        val data = Intent()
        when (location) {
            is Warehouse -> data.putExtra(ARG_LOCATION, location as Warehouse)
            is WarehouseArea -> data.putExtra(ARG_LOCATION, location as WarehouseArea)
            is Rack -> data.putExtra(ARG_LOCATION, location as Rack)
        }
        setResult(RESULT_OK, data)
        finish()
    }

    var isFilling = false
    private fun fillAdapter() {
        if (isFilling) return
        isFilling = true

        try {
            when (locationType) {
                LocationType.WAREHOUSE -> getWarehouse()

                LocationType.WAREHOUSE_AREA -> getWarehouseArea()

                LocationType.RACK -> getRack()
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            isFilling = false
        }
    }

    private fun getWarehouse() {
        thread {
            GetWarehouse(action = GetWarehouse.defaultAction,
                onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it) },
                onFinish = {
                    fillWarehouse(it)
                    showProgressBar(View.GONE)
                    isFilling = false
                }).execute()
        }
    }

    private fun getWarehouseArea() {
        thread {
            GetWarehouseArea(action = GetWarehouseArea.defaultAction,
                onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it) },
                onFinish = {
                    fillWarehouseArea(it)
                    showProgressBar(View.GONE)
                    isFilling = false
                }).execute()
        }
    }

    private fun getRack() {
        thread {
            GetRack(action = GetRack.defaultAction,
                onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it) },
                onFinish = {
                    fillRack(it)
                    showProgressBar(View.GONE)
                    isFilling = false
                }).execute()
        }
    }

    private fun showSnackBar(it: SnackBarEventData) {
        MakeText.makeText(binding.root, it.text, it.snackBarType)
    }

    private fun fillWarehouse(it: ArrayList<Warehouse>) {
        val adapter = WarehouseAdapter(
            activity = this,
            resource = R.layout.warehouse_row,
            warehouseArray = ArrayList(it),
            suggestedList = ArrayList()
        )

        runOnUiThread {
            binding.autoCompleteTextView.setAdapter(adapter)
            (binding.autoCompleteTextView.adapter as WarehouseAdapter).notifyDataSetChanged()

            while (binding.autoCompleteTextView.adapter == null) {
                // Wait for complete loaded
            }

            refreshText(cleanText = false, focus = false)
        }
    }

    private fun fillWarehouseArea(it: ArrayList<WarehouseArea>) {
        val adapter = WarehouseAreaAdapter(
            activity = this,
            resource = R.layout.warehouse_area_row,
            warehouseAreaArray = ArrayList(it),
            suggestedList = ArrayList()
        )

        runOnUiThread {
            binding.autoCompleteTextView.setAdapter(adapter)
            (binding.autoCompleteTextView.adapter as WarehouseAreaAdapter).notifyDataSetChanged()

            while (binding.autoCompleteTextView.adapter == null) {
                // Wait for complete loaded
            }

            refreshText(cleanText = false, focus = false)
        }
    }

    private fun fillRack(it: ArrayList<Rack>) {
        val adapter = RackAdapter(
            activity = this,
            resource = R.layout.rack_row,
            rackArray = ArrayList(it),
            suggestedList = ArrayList()
        )

        runOnUiThread {
            binding.autoCompleteTextView.setAdapter(adapter)
            (binding.autoCompleteTextView.adapter as RackAdapter).notifyDataSetChanged()

            while (binding.autoCompleteTextView.adapter == null) {
                // Wait for complete loaded
            }

            refreshText(cleanText = false, focus = false)
        }
    }

    override fun onBackPressed() {
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

    private fun adjustAndShowDropDown() {
        // TOP LAYOUT
        topLayout()

        val adapter = binding.autoCompleteTextView.adapter
        val viewHeight = when (adapter) {
            is WarehouseAdapter -> WarehouseAdapter.viewHeight

            is WarehouseAreaAdapter -> WarehouseAreaAdapter.viewHeight

            is RackAdapter -> RackAdapter.viewHeight

            else -> return
        }

        val maxNeeded = adapter.count * viewHeight
        val availableHeight =
            calculateDropDownHeight() - (binding.autoCompleteTextView.y + binding.autoCompleteTextView.height).toInt()
        var newHeight = availableHeight / viewHeight * viewHeight
        if (maxNeeded < newHeight) {
            newHeight = maxNeeded
        }

        binding.autoCompleteTextView.dropDownHeight = newHeight
        binding.autoCompleteTextView.showDropDown()
    }

    private var isTopping = false
    private fun topLayout() {
        if (isTopping) return
        isTopping = true
        val set = ConstraintSet()

        set.clone(binding.locationSelect)
        set.clear(binding.gralLayout.id, ConstraintSet.BOTTOM)
        set.applyTo(binding.locationSelect)

        isTopping = false
    }

    private var isCentring = false
    private fun centerLayout() {
        if (isCentring) return

        isCentring = true
        val set = ConstraintSet()

        set.clone(binding.locationSelect)
        set.connect(
            binding.gralLayout.id, ConstraintSet.BOTTOM, binding.locationSelect.id, ConstraintSet.BOTTOM, 0
        )
        set.applyTo(binding.locationSelect)

        isCentring = false
    }

    private fun adjustDropDownHeight() {
        runOnUiThread {
            when {
                binding.autoCompleteTextView.isPopupShowing -> {
                    adjustAndShowDropDown()
                }

                else -> {
                    centerLayout()
                }
            }
        }
    }

    override fun contractsRetrieved(count: Int) {
        if (count > 0) {
            adjustDropDownHeight()
        } else {
            // CENTER LAYOUT
            centerLayout()
        }
    }
    // endregion SOFT KEYBOARD AND DROPDOWN ISSUES

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_LOCATION = "location"
        const val ARG_LOCATION_TYPE = "locationType"
    }
}
