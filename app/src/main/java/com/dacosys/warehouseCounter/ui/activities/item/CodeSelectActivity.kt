package com.dacosys.warehouseCounter.ui.activities.item

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.data.room.entity.item.Item
import com.dacosys.warehouseCounter.databinding.CodeSelectActivityBinding
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.jotter.ScannerManager
import com.dacosys.warehouseCounter.ui.adapter.item.ItemAdapter
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.utils.TextViewUtils.Companion.isActionDone
import com.dacosys.warehouseCounter.ui.views.ContractsAutoCompleteTextView
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import kotlin.concurrent.thread


class CodeSelectActivity : AppCompatActivity(), Scanner.ScannerListener,
    ContractsAutoCompleteTextView.OnContractsAvailability, KeyboardVisibilityEventListener {

    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        Screen.closeKeyboard(this)
        binding.autoCompleteTextView.setOnContractsAvailability(null)
        binding.autoCompleteTextView.setAdapter(null)
    }

    private var code: String = ""
    private val currentText: String
        get() {
            return binding.autoCompleteTextView.text.toString().trim()
        }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString(ARG_CODE, currentText)
        savedInstanceState.putString(ARG_TITLE, title.toString())
    }

    private fun loadSavedValues(b: Bundle) {
        val t1 = b.getString(ARG_TITLE)
        title =
            if (!t1.isNullOrEmpty()) t1
            else getString(R.string.search_by_code)

        code = b.getString(ARG_CODE) ?: ""
    }

    private var fillRequired = false
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

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isBackPressed()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        if (savedInstanceState != null) {
            // Dejo de escuchar estos eventos hasta pasar los valores guardados
            // más adelante se reconectan
            binding.autoCompleteTextView.setOnEditorActionListener(null)
            binding.autoCompleteTextView.onItemClickListener = null
            binding.autoCompleteTextView.setOnTouchListener(null)
            binding.autoCompleteTextView.onFocusChangeListener = null
            binding.autoCompleteTextView.setOnDismissListener(null)

            loadSavedValues(savedInstanceState)
        } else {
            val extras = intent.extras
            if (extras != null) {
                loadSavedValues(extras)
            }
        }

        // Para el llenado en el onStart siguiente de onCreate
        fillRequired = true

        binding.codeSelect.setOnClickListener {
            isBackPressed()
        }

        binding.codeClearImageView.setOnClickListener {
            code = ""
            refreshCodeText(cleanText = true, focus = true)
        }

        // region Setup ITEM_CATEGORY ID AUTOCOMPLETE
        // Set an item click checkedChangedListener for auto complete text view
        binding.autoCompleteTextView.threshold = 1
        binding.autoCompleteTextView.hint = title
        binding.autoCompleteTextView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val adapter = binding.autoCompleteTextView.adapter
                if (adapter is ItemAdapter) {
                    val it = adapter.getItem(position)
                    if (it != null) {
                        code = it.ean
                    }
                }
                itemSelected()
            }
        binding.autoCompleteTextView.setOnContractsAvailability(this)
        binding.autoCompleteTextView.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                val count = binding.autoCompleteTextView.adapter?.count ?: 0
                val text = currentText

                if (hasFocus &&
                    text.length >= binding.autoCompleteTextView.threshold &&
                    count > 0 &&
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
            if (isActionDone(actionId, event)) {
                if (currentText.length >= binding.autoCompleteTextView.threshold) {
                    code = binding.autoCompleteTextView.text!!.toString().trim()
                }
                itemSelected()
                true
            } else {
                false
            }
        }
        // endregion

        KeyboardVisibilityEvent.registerEventListener(this, this)
        refreshCodeText(cleanText = false, focus = true)
    }

    override fun onStart() {
        super.onStart()

        if (fillRequired) {
            fillRequired = false
            thread { fillAdapter() }
        }
    }

    private fun refreshCodeText(cleanText: Boolean, focus: Boolean) {
        runOnUiThread {
            binding.autoCompleteTextView.setText(code.ifEmpty {
                if (cleanText) "" else currentText
            })

            binding.autoCompleteTextView.post {
                binding.autoCompleteTextView.setSelection(binding.autoCompleteTextView.length())
            }

            if (focus) {
                binding.autoCompleteTextView.requestFocus()
            }
        }
    }

    private fun itemSelected() {
        Screen.closeKeyboard(this)

        val data = Intent()
        data.putExtra(ARG_CODE, code)
        setResult(RESULT_OK, data)
        finish()
    }

    private var isFilling = false
    private val fillAdapterLock = Any()
    private fun fillAdapter() {
        synchronized(fillAdapterLock) {
            if (isFilling) return
            isFilling = true
        }

        try {
            Log.d(tag, "Loading items for adapter...")
            showProgressBar(VISIBLE)

            ItemCoroutines.get { itemList ->
                runOnUiThread {
                    try {
                        createAndSetAdapter(itemList)
                    } catch (ex: Exception) {
                        handleAdapterError(ex)
                    } finally {
                        showProgressBar(GONE)
                        isFilling = false
                    }
                }
            }
        } catch (ex: Exception) {
            handleAdapterError(ex)
            isFilling = false
        }
    }

    private fun createAndSetAdapter(itemList: List<Item>) {
        val adapter = ItemAdapter(
            resource = R.layout.item_row_simple,
            activity = this,
            itemList = ArrayList(itemList),
            suggestedList = arrayListOf()
        )
        val tag = tag

        binding.autoCompleteTextView.apply {
            setAdapter(adapter)

            // Verificación asíncrona
            if (adapter.isEmpty) {
                Log.i(tag, "Adapter created with empty dataset")
            }

            // Operaciones posteriores a la creación
            post {
                // Adapter completamente cargado
                refreshCodeText(cleanText = false, focus = false)
            }
        }
    }

    private fun handleAdapterError(ex: Exception) {
        Log.e(tag, "Error filling adapter", ex)
        ErrorLog.writeLog(this, tag, ex)
        showProgressBar(GONE)
        showMessage("Error loading items", ERROR)
    }

    @Suppress("SameParameterValue")
    private fun showProgressBar(visibility: Int) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progressBar.visibility = visibility
        }, 20)
    }

    private fun isBackPressed() {
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
        topLayout()

        val viewHeight = settingsVm.itemViewHeight
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

    override fun contractsRetrieved(tag: Any?, count: Int) {
        if (count > 0) {
            adjustDropDownHeight()
        } else {
            centerLayout()
        }
    }
    // endregion SOFT KEYBOARD AND DROPDOWN ISSUES

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
            ScannerManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun scannerCompleted(scanCode: String) {
        if (settingsVm.showScannedCode) showMessage(scanCode, SnackBarType.INFO)

        ScannerManager.lockScanner(this, true)

        try {
            code = scanCode
            refreshCodeText(cleanText = false, focus = true)
        } catch (ex: Exception) {
            ex.printStackTrace()
            showMessage(ex.message.toString(), ERROR)
            ErrorLog.writeLog(this, tag, ex)
        } finally {
            // Unless is blocked, unlock the partial
            ScannerManager.lockScanner(this, false)
        }
    }

    private fun showMessage(msg: String, type: SnackBarType) {
        if (isFinishing || isDestroyed) return
        if (type == ERROR) Log.e(javaClass.simpleName, msg)
        makeText(binding.root, msg, type)
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_CODE = "code"
    }
}
