package com.dacosys.warehouseCounter.ui.activities.barcodeLabel

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
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelTemplate
import com.dacosys.warehouseCounter.data.ktor.v2.functions.template.GetBarcodeLabelTemplate
import com.dacosys.warehouseCounter.databinding.CodeSelectActivityBinding
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.adapter.barcodeLabel.BarcodeLabelTemplateAdapter
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.serializable
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.views.ContractsAutoCompleteTextView
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import kotlin.concurrent.thread


class TemplateSelectActivity : AppCompatActivity(),
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
    private var template: BarcodeLabelTemplate? = null
    private var templateTypeIdList: ArrayList<Long> = arrayListOf()

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putParcelable(ARG_TEMPLATE, template)
        savedInstanceState.putSerializable(ARG_TEMPLATE_TYPE_ID_LIST, templateTypeIdList)
        savedInstanceState.putString(ARG_TITLE, title.toString())
    }

    private fun loadFromBundle(b: Bundle) {
        val t1 = b.getString(ARG_TITLE)
        title = if (!t1.isNullOrEmpty()) t1
        else getString(R.string.search_by_template)

        template = b.parcelable(ARG_TEMPLATE)
        val temp =
            if (b.containsKey(ARG_TEMPLATE_TYPE_ID_LIST)) b.serializable<ArrayList<Long>>(ARG_TEMPLATE_TYPE_ID_LIST) as ArrayList<*>
            else ArrayList<Long>()
        if (temp.firstOrNull() is Long) {
            @Suppress("UNCHECKED_CAST")
            templateTypeIdList = temp as ArrayList<Long>
        }
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

        if (savedInstanceState != null) {
            // Dejo de escuchar estos eventos hasta pasar los valores guardados
            // más adelante se reconectan
            binding.autoCompleteTextView.setOnEditorActionListener(null)
            binding.autoCompleteTextView.onItemClickListener = null
            binding.autoCompleteTextView.setOnTouchListener(null)
            binding.autoCompleteTextView.onFocusChangeListener = null
            binding.autoCompleteTextView.setOnDismissListener(null)

            loadFromBundle(savedInstanceState)
        } else {
            val extras = intent.extras
            if (extras != null) {
                loadFromBundle(extras)
            }
        }

        // Para el llenado en el onStart siguiente de onCreate
        fillRequired = true

        binding.codeSelect.setOnClickListener {
            @Suppress("DEPRECATION") onBackPressed()
        }

        binding.codeClearImageView.setOnClickListener {
            template = null
            refreshText(cleanText = true, focus = true)
        }

        // region Setup CATEGORY_CATEGORY ID AUTOCOMPLETE
        // Set an barcodeLabelTemplate click checkedChangedListener for auto complete text view
        binding.autoCompleteTextView.threshold = 1
        binding.autoCompleteTextView.hint = title
        binding.autoCompleteTextView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val adapter = binding.autoCompleteTextView.adapter
                if (adapter is BarcodeLabelTemplateAdapter) {
                    val it = adapter.getItem(position)
                    if (it != null) {
                        template = it
                    }
                }
                itemSelected()
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
            val adapter = binding.autoCompleteTextView.adapter
            if (adapter is BarcodeLabelTemplateAdapter) {
                if (actionId == EditorInfo.IME_ACTION_DONE || event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                    if (binding.autoCompleteTextView.text.trim().length >= binding.autoCompleteTextView.threshold) {
                        val all = adapter.getAll()
                        if (all.any()) {
                            var founded = false
                            for (a in all) {
                                if (a.description.startsWith(
                                        binding.autoCompleteTextView.text.toString().trim(),
                                        true
                                    )
                                ) {
                                    template = a
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
                                        template = a
                                        break
                                    }
                                }
                            }
                        }
                    }
                    itemSelected()
                }
                true
            } else {
                false
            }
        }
        // endregion

        KeyboardVisibilityEvent.registerEventListener(this, this)
        refreshText(cleanText = false, focus = true)
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

    private fun refreshText(cleanText: Boolean, focus: Boolean) {
        runOnUiThread {
            binding.autoCompleteTextView.setText(
                if (template == null) {
                    if (cleanText) {
                        ""
                    } else {
                        binding.autoCompleteTextView.text.toString()
                    }
                } else {
                    template?.description ?: ""
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

    private fun itemSelected() {
        Screen.closeKeyboard(this)

        val data = Intent()
        data.putExtra(ARG_TEMPLATE, template)
        setResult(RESULT_OK, data)
        finish()
    }

    var isFilling = false
    private fun fillAdapter() {
        if (isFilling) return
        isFilling = true

        try {
            thread {
                GetBarcodeLabelTemplate(
                    onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it.text, it.snackBarType) },
                    onFinish = {
                        val allTemplates =
                            ArrayList(it.mapNotNull { t -> if (t.barcodeLabelTypeId in templateTypeIdList) t else null }
                                .toList())
                        fillBarcodeLabelTemplate(allTemplates)
                        showProgressBar(View.GONE)
                        isFilling = false
                    }).execute()
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
        } finally {
            isFilling = false
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    private fun fillBarcodeLabelTemplate(it: ArrayList<BarcodeLabelTemplate>) {
        val adapter = BarcodeLabelTemplateAdapter(
            activity = this,
            resource = R.layout.item_category_row, // TODO: Hacer un row específico
            templateArray = ArrayList(it)
        )

        runOnUiThread {
            binding.autoCompleteTextView.setAdapter(adapter)
            (binding.autoCompleteTextView.adapter as BarcodeLabelTemplateAdapter).notifyDataSetChanged()

            while (binding.autoCompleteTextView.adapter == null) {
                // Wait for complete loaded
            }

            refreshText(cleanText = false, focus = false)
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
        topLayout()

        val viewHeight = settingsVm.templateViewHeight
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
            centerLayout()
        }
    }
    // endregion SOFT KEYBOARD AND DROPDOWN ISSUES

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_TEMPLATE = "template"
        const val ARG_TEMPLATE_TYPE_ID_LIST = "templateTypeIdList"
    }
}
