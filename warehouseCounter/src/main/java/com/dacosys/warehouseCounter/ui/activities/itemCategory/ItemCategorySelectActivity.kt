package com.dacosys.warehouseCounter.ui.activities.itemCategory

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
import android.view.View.GONE
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.data.room.dao.itemCategory.ItemCategoryCoroutines
import com.dacosys.warehouseCounter.data.room.entity.itemCategory.ItemCategory
import com.dacosys.warehouseCounter.databinding.CodeSelectActivityBinding
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.adapter.itemCategory.ItemCategoryAdapter
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.views.ContractsAutoCompleteTextView
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import kotlin.concurrent.thread

class ItemCategorySelectActivity : AppCompatActivity(),
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

    private var fillRequired = false
    private var itemCategory: ItemCategory? = null

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putParcelable(ARG_ITEM_CATEGORY, itemCategory)
    }

    private lateinit var binding: CodeSelectActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = CodeSelectActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Permite finalizar la actividad si se toca la pantalla
        // fuera de la ventana. Esta actividad se ve como un diálogo.
        setFinishOnTouchOutside(true)

        var tempTitle = getString(R.string.search_by_category)
        if (savedInstanceState != null) {
            // Dejo de escuchar estos eventos hasta pasar los valores guardados
            // más adelante se reconectan
            binding.autoCompleteTextView.setOnEditorActionListener(null)
            binding.autoCompleteTextView.onItemClickListener = null
            binding.autoCompleteTextView.setOnTouchListener(null)
            binding.autoCompleteTextView.onFocusChangeListener = null
            binding.autoCompleteTextView.setOnDismissListener(null)

            itemCategory = savedInstanceState.parcelable(ARG_ITEM_CATEGORY)
        } else {
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString(ARG_TITLE)
                if (!t1.isNullOrEmpty()) tempTitle = t1

                itemCategory = extras.parcelable(ARG_ITEM_CATEGORY)
            }
        }

        // Para el llenado en el onStart siguiente de onCreate
        fillRequired = true

        title = tempTitle

        binding.codeSelect.setOnClickListener {
            @Suppress("DEPRECATION") onBackPressed()
        }

        binding.codeClearImageView.setOnClickListener {
            itemCategory = null
            refreshItemCategoryText(cleanText = true, focus = true)
        }

        // region Setup CATEGORY_CATEGORY ID AUTOCOMPLETE
        // Set an itemCategory click checkedChangedListener for auto complete text view
        binding.autoCompleteTextView.threshold = 1
        binding.autoCompleteTextView.hint = tempTitle
        binding.autoCompleteTextView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val adapter = binding.autoCompleteTextView.adapter
                if (adapter is ItemCategoryAdapter) {
                    val it = adapter.getItem(position)
                    if (it != null) {
                        itemCategory = it
                    }
                }
                itemCategorySelected()
            }
        binding.autoCompleteTextView.setOnContractsAvailability(this)
        binding.autoCompleteTextView.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus && binding.autoCompleteTextView.text.trim().length >= binding.autoCompleteTextView.threshold &&
                    (binding.autoCompleteTextView.adapter?.count ?: 0) > 0 &&
                    !binding.autoCompleteTextView.isPopupShowing
                ) {
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
                val adapter = binding.autoCompleteTextView.adapter
                if (adapter is ItemCategoryAdapter) {
                    if (binding.autoCompleteTextView.text.trim().length >= binding.autoCompleteTextView.threshold) {
                        val all = adapter.getAll()
                        if (all.any()) {
                            var founded = false
                            for (a in all) {
                                if (a.description.startsWith(
                                        binding.autoCompleteTextView.text.toString().trim(), true
                                    )
                                ) {
                                    itemCategory = a
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
                                        itemCategory = a
                                        break
                                    }
                                }
                            }
                        }
                    }
                    itemCategorySelected()
                }
                true
            } else {
                false
            }
        }
        // endregion

        KeyboardVisibilityEvent.registerEventListener(this, this)
        refreshItemCategoryText(cleanText = false, focus = true)
    }

    override fun onStart() {
        super.onStart()

        if (fillRequired) {
            fillRequired = false
            thread { fillAdapter() }
        }
    }

    @Suppress("SameParameterValue")
    private fun showProgressBar(visibility: Int) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progressBar.visibility = visibility
        }, 20)
    }

    private fun refreshItemCategoryText(cleanText: Boolean, focus: Boolean) {
        runOnUiThread {
            binding.autoCompleteTextView.setText(
                if (itemCategory == null) {
                    if (cleanText) {
                        ""
                    } else {
                        binding.autoCompleteTextView.text.toString()
                    }
                } else {
                    itemCategory?.description ?: ""
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

    private fun itemCategorySelected() {
        Screen.closeKeyboard(this)

        val data = Intent()
        data.putExtra(ARG_ITEM_CATEGORY, itemCategory)
        setResult(RESULT_OK, data)
        finish()
    }

    var isFilling = false
    private fun fillAdapter() {
        if (isFilling) return
        isFilling = true

        try {
            Log.d(this::class.java.simpleName, "Selecting categories...")

            ItemCategoryCoroutines.get {
                val adapter = ItemCategoryAdapter(
                    activity = this,
                    resource = R.layout.item_category_row,
                    itemCategoryArray = it,
                    suggestedList = ArrayList()
                )

                runOnUiThread {
                    binding.autoCompleteTextView.setAdapter(adapter)
                    (binding.autoCompleteTextView.adapter as ItemCategoryAdapter).notifyDataSetChanged()

                    while (binding.autoCompleteTextView.adapter == null) {
                        // Wait for complete loaded
                    }
                    refreshItemCategoryText(cleanText = false, focus = false)
                    showProgressBar(GONE)
                }
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
        } finally {
            isFilling = false
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

    //region SOFT KEYBOARD AND DROPDOWN ISSUES
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

        val viewHeight = settingViewModel.categoryViewHeight
        val maxNeeded = (binding.autoCompleteTextView.adapter?.count ?: 0) * viewHeight
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

        set.clone(binding.codeSelect)
        set.clear(binding.gralLayout.id, ConstraintSet.BOTTOM)
        set.applyTo(binding.codeSelect)

        isTopping = false
    }

    private var isCentring = false
    private fun centerLayout() {
        if (isCentring) return

        isCentring = true
        val set = ConstraintSet()

        set.clone(binding.codeSelect)
        set.connect(
            binding.gralLayout.id,
            ConstraintSet.BOTTOM,
            binding.codeSelect.id,
            ConstraintSet.BOTTOM,
            0
        )
        set.applyTo(binding.codeSelect)

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
        const val ARG_ITEM_CATEGORY = "itemCategory"
    }
}
