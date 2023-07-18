package com.dacosys.warehouseCounter.ui.activities.ptlOrder

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.databinding.WarehouseAreaSelectActivityBinding
import com.dacosys.warehouseCounter.ktor.v2.dto.location.Warehouse
import com.dacosys.warehouseCounter.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.ktor.v2.functions.GetWarehouse
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiActionParam
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.adapter.ptlOrder.WarehouseAreaAdapter
import com.dacosys.warehouseCounter.ui.snackBar.MakeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.views.ContractsAutoCompleteTextView
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import kotlin.concurrent.thread

class WarehouseAreaSelectActivity : AppCompatActivity(),
    ContractsAutoCompleteTextView.OnContractsAvailability, KeyboardVisibilityEventListener {
    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        Screen.closeKeyboard(this)
        binding.autoCompleteTextView.setOnContractsAvailability(null)
        binding.autoCompleteTextView.setAdapter(null)
    }

    private var warehouseArea: WarehouseArea? = null

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putParcelable(ARG_WAREHOUSE_AREA, warehouseArea)
    }

    private lateinit var binding: WarehouseAreaSelectActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = WarehouseAreaSelectActivityBinding.inflate(layoutInflater)
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
            warehouseArea = savedInstanceState.getParcelable(ARG_WAREHOUSE_AREA)
        } else {
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString(ARG_TITLE)
                if (!t1.isNullOrEmpty()) tempTitle = t1

                warehouseArea = extras.getParcelable(ARG_WAREHOUSE_AREA)
            }
        }

        title = tempTitle
        binding.warehouseAreaSelect.setOnClickListener { onBackPressed() }

        binding.clearImageView.setOnClickListener {
            warehouseArea = null
            refreshWarehouseAreaText(cleanText = true, focus = true)
        }

        // region Setup CATEGORY_CATEGORY ID AUTOCOMPLETE
        // Set an warehouseArea click checkedChangedListener for auto complete text view
        binding.autoCompleteTextView.threshold = 1
        binding.autoCompleteTextView.hint = tempTitle
        binding.autoCompleteTextView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                if (binding.autoCompleteTextView.adapter != null && binding.autoCompleteTextView.adapter is WarehouseAreaAdapter) {
                    val it = (binding.autoCompleteTextView.adapter as WarehouseAreaAdapter).getItem(
                        position
                    )
                    if (it != null) {
                        warehouseArea = it
                    }
                }
                warehouseAreaSelected()
            }
        binding.autoCompleteTextView.setOnContractsAvailability(this)
        binding.autoCompleteTextView.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus && binding.autoCompleteTextView.text.trim().length >= binding.autoCompleteTextView.threshold && binding.autoCompleteTextView.adapter != null && (binding.autoCompleteTextView.adapter as WarehouseAreaAdapter).count > 0 && !binding.autoCompleteTextView.isPopupShowing) {
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
                    if (binding.autoCompleteTextView.adapter != null && binding.autoCompleteTextView.adapter is WarehouseAreaAdapter) {
                        val all =
                            (binding.autoCompleteTextView.adapter as WarehouseAreaAdapter).getAll()
                        if (all.any()) {
                            var founded = false
                            for (a in all) {
                                if (a.description.startsWith(
                                        binding.autoCompleteTextView.text.toString().trim(), true
                                    )
                                ) {
                                    warehouseArea = a
                                    founded = true
                                    break
                                }
                            }
                            if (!founded) {
                                for (a in all) {
                                    if (a.description.contains(
                                            binding.autoCompleteTextView.text.toString().trim(),
                                            true
                                        )
                                    ) {
                                        warehouseArea = a
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
                warehouseAreaSelected()
                true
            } else {
                false
            }
        }
        // endregion

        KeyboardVisibilityEvent.registerEventListener(this, this)

        refreshWarehouseAreaText(cleanText = false, focus = true)

        fillAdapter()
    }

    private fun showProgressBar(visibility: Int) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progressBar.visibility = visibility
        }, 20)
    }

    private fun refreshWarehouseAreaText(cleanText: Boolean, focus: Boolean) {
        runOnUiThread {
            binding.autoCompleteTextView.setText(
                if (warehouseArea == null) {
                    if (cleanText) {
                        ""
                    } else {
                        binding.autoCompleteTextView.text.toString()
                    }
                } else {
                    warehouseArea?.description ?: ""
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

    private fun warehouseAreaSelected() {
        Screen.closeKeyboard(this)

        val data = Intent()
        data.putExtra(ARG_WAREHOUSE_AREA, warehouseArea)
        setResult(RESULT_OK, data)
        finish()
    }

    var isFilling = false
    private fun fillAdapter() {
        if (isFilling) return
        isFilling = true

        try {
            Log.d(this::class.java.simpleName, "Selecting areas...")

            thread {
                GetWarehouse(
                    action = action,
                    onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it) },
                    onFinish = {
                        fill(it)

                        showProgressBar(View.GONE)
                        isFilling = false
                    }
                ).execute()
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            isFilling = false
        }
    }

    private val action: ArrayList<ApiActionParam> by lazy {
        arrayListOf(
            ApiActionParam(
                action = ApiActionParam.ACTION_EXPAND,
                extension = setOf(
                    ApiActionParam.EXTENSION_WAREHOUSE_AREA_LIST,
                    ApiActionParam.EXTENSION_STATUS
                )
            )
        )
    }

    private fun showSnackBar(it: SnackBarEventData) {
        MakeText.makeText(binding.root, it.text, it.snackBarType)
    }

    private fun fill(it: ArrayList<Warehouse>) {
        val waArray = it.firstOrNull()?.areas ?: listOf()

        val adapter = WarehouseAreaAdapter(
            activity = this,
            resource = R.layout.warehouse_area_row,
            warehouseAreas = ArrayList(waArray),
            suggestedList = ArrayList()
        )

        runOnUiThread {
            binding.autoCompleteTextView.setAdapter(adapter)
            (binding.autoCompleteTextView.adapter as WarehouseAreaAdapter).notifyDataSetChanged()

            while (binding.autoCompleteTextView.adapter == null) {
                // Wait for complete loaded
            }

            refreshWarehouseAreaText(cleanText = false, focus = false)
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

        val adapter = (binding.autoCompleteTextView.adapter!! as WarehouseAreaAdapter)
        val viewHeight = WarehouseAreaAdapter.viewHeight
        val maxNeeded = adapter.count() * viewHeight
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

        set.clone(binding.warehouseAreaSelect)
        set.clear(binding.gralLayout.id, ConstraintSet.BOTTOM)
        set.applyTo(binding.warehouseAreaSelect)

        isTopping = false
    }

    private var isCentring = false
    private fun centerLayout() {
        if (isCentring) return

        isCentring = true
        val set = ConstraintSet()

        set.clone(binding.warehouseAreaSelect)
        set.connect(
            binding.gralLayout.id,
            ConstraintSet.BOTTOM,
            binding.warehouseAreaSelect.id,
            ConstraintSet.BOTTOM,
            0
        )
        set.applyTo(binding.warehouseAreaSelect)

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
        const val ARG_WAREHOUSE_AREA = "warehouseArea"
    }
}