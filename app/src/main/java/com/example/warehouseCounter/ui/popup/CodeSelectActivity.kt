package com.example.warehouseCounter.ui.popup

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CheckedTextView
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.example.warehouseCounter.data.room.entity.item.Item
import com.example.warehouseCounter.databinding.CodeSelectActivityBinding
import com.example.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.example.warehouseCounter.scanners.Scanner
import com.example.warehouseCounter.scanners.deviceLifecycle.ScannerManager
import com.example.warehouseCounter.ui.adapter.generic.GenericDropDownAdapter
import com.example.warehouseCounter.ui.adapter.generic.getViewTotalHeight
import com.example.warehouseCounter.ui.adapter.generic.toItemStatus
import com.example.warehouseCounter.ui.adapter.item.ItemStatus
import com.example.warehouseCounter.ui.adapter.item.toGenericStatus
import com.example.warehouseCounter.ui.adapter.item.toGenericStatusList
import com.example.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.example.warehouseCounter.ui.snackBar.SnackBarType
import com.example.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.example.warehouseCounter.ui.utils.Screen
import com.example.warehouseCounter.ui.utils.TextViewUtils.Companion.isActionDone
import com.example.warehouseCounter.ui.views.ContractsAutoCompleteTextView
import kotlinx.coroutines.launch

class CodeSelectActivity : androidx.appcompat.app.AppCompatActivity(), Scanner.ScannerListener,
    ContractsAutoCompleteTextView.OnContractsAvailability {

    val tag = this::class.java.simpleName

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        Screen.closeKeyboard(this)
        binding.autoCompleteTextView.setOnContractsAvailability(null)
        binding.autoCompleteTextView.setAdapter(null)
    }

    private fun showMessage(msg: String, type: SnackBarType) {
        if (isFinishing || isDestroyed) return
        if (type == ERROR) Log.e(tag, msg)
        makeText(binding.root, msg, type)
    }

    override fun onStart() {
        super.onStart()

        refreshText(cleanText = false, focus = true)
        fillAdapter()
    }

    override fun onStop() {
        super.onStop()
    }

    private var tempTitle: String = ""
    private var ean: String = ""
    private var onlyContainer: Boolean = false
    private var partial: Boolean = false

    private fun restoreSavedInstance(savedInstanceState: Bundle?) {
        tempTitle = getString(R.string.search_by_code)
        if (savedInstanceState != null) {
            ean = savedInstanceState.getString("ean").orEmpty()
            onlyContainer = savedInstanceState.getBoolean("onlyContainer")
            partial = savedInstanceState.getBoolean("partial")
        } else {
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString("title")
                if (!t1.isNullOrEmpty()) tempTitle = t1

                ean = extras.getString("ean").orEmpty()
                onlyContainer = extras.getBoolean("onlyContainer")
                partial = extras.getBoolean("partial")
            }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("title", title.toString())
        outState.putString("ean", currentText)
        outState.putBoolean("onlyContainer", onlyContainer)
        outState.putBoolean("partial", partial)
    }

    private fun isBackPressed() {
        closeDialog(RESULT_CANCELED)
    }

    @Suppress("UNCHECKED_CAST")
    private val iAdapter: GenericDropDownAdapter<Item>?
        get() = binding.autoCompleteTextView.adapter as? GenericDropDownAdapter<Item>

    private val itemCount get() = iAdapter?.count ?: 0

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

        restoreSavedInstance(savedInstanceState)

        title = tempTitle

        setClickedItem()
        setUpItemTextView()
    }

    private fun setClickedItem() {
        binding.codeSelect.setOnClickListener { isBackPressed() }
        binding.codeClearImageView.setOnClickListener { setCode() }
    }

    private fun setUpItemTextView() {
        setupAutoCompleteTextView(
            view = binding.autoCompleteTextView,
            hintResId = R.string.item,
            onItemClicked = { pos ->
                setCode(iAdapter?.getItem(pos)?.ean)
            },
            onFocusChange = { hasFocus, isPopupShowing, reached ->
                if (hasFocus) {
                    showItemPopup(reached, isPopupShowing)
                }
            },
            onViewTouched = { reached ->
                showItemPopup(reached, binding.autoCompleteTextView.isPopupShowing)
            },
            onSearchItem = { text ->
                if (partial) {
                    setCode(text)
                } else {
                    val foundItem = getInAdapter(text)
                    foundItem?.let {
                        setCode(it.ean)
                    }
                }
            }
        )
    }

    private fun showItemPopup(reached: Boolean, isPopupShowing: Boolean) {
        if (reached) {
            if (!isPopupShowing && itemCount > 0) {
                topLayout()
                binding.autoCompleteTextView.showDropDown()
                adjustPopupHeight()
            }
        } else centerLayout()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupAutoCompleteTextView(
        view: ContractsAutoCompleteTextView,
        hintResId: Int,
        onItemClicked: (pos: Int) -> Unit = {},
        onViewTouched: (thresholdReached: Boolean) -> Unit = {},
        onFocusChange: (hasFocus: Boolean, isPopupShowing: Boolean, thresholdReached: Boolean) -> Unit = { _, _, _ -> },
        onSearchItem: (text: String) -> Unit = {},
    ) {
        view.setOnEditorActionListener(null)
        view.onItemClickListener = null
        view.setOnTouchListener(null)
        view.onFocusChangeListener = null
        view.setOnDismissListener(null)

        view.threshold = 1
        view.hint = getString(hintResId)
        view.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            onItemClicked(position)
        }

        view.setOnContractsAvailability(this)
        view.tag = ContractType.Item

        view.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            onFocusChange(hasFocus, view.isPopupShowing, view.isThresholdReached())
        }

        view.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    onViewTouched(view.isThresholdReached())
                    false
                }

                MotionEvent.BUTTON_BACK -> {
                    closeDialog(RESULT_CANCELED)
                    true
                }

                else -> false
            }
        }

        view.setOnEditorActionListener { _, keyCode, keyEvent ->
            if (isActionDone(keyCode, keyEvent)) {
                val text = view.text.toString().trim()
                if (view.isThresholdReached()) onSearchItem(text)
                true
            } else {
                false
            }
        }

        view.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) centerLayout()
        }
    }

    private fun getInAdapter(text: String): Item? {
        val adapter = iAdapter ?: return null

        return (0 until adapter.count)
            .mapNotNull { adapter.getItem(it) }
            .firstOrNull {
                it.ean.isNotEmpty() &&
                        (it.ean.startsWith(text, true) || it.ean.contains(text, true)) ||
                        it.description.isNotEmpty() &&
                        (it.description.startsWith(text, true) || it.description.contains(text, true))
            }
    }

    private fun setCode(c: String? = null) {
        ean = c.orEmpty()
        refreshText(cleanText = c == null, focus = c == null)
        if (ean.isNotEmpty()) {
            closeDialog(RESULT_OK)
        }
    }

    private fun closeDialog(resultCode: Int) {
        if (resultCode == RESULT_OK) {
            val data = Intent()
            data.putExtra("ean", ean)
            setResult(resultCode, data)
        } else {
            setResult(RESULT_CANCELED, null)
        }
        finish()
    }

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.animation_fade_in, R.anim.animation_fade_out)
    }

    private fun showProgressBar(visibility: Int) {
        binding.progressBar.postDelayed({
            binding.progressBar.visibility = visibility
        }, 20)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupWindowInsetsAnimation()
    }

    private fun setupWindowInsetsAnimation() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        implWindowInsetsAnimation()
    }

    private fun implWindowInsetsAnimation() {
        val rootView = binding.root

        ViewCompat.setWindowInsetsAnimationCallback(
            rootView,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    paddingBottomView(rootView, insets)

                    adjustLayoutAndPopup()

                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    val isIme = animation.typeMask and WindowInsetsCompat.Type.ime() != 0
                    if (!isIme) return

                    adjustLayoutAndPopup()

                    super.onEnd(animation)
                }
            }
        )
    }

    private fun paddingBottomView(rootView: ConstraintLayout, insets: WindowInsetsCompat) {
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

        // NOTA: Si en el futuro queremos conocer estos valores dentro de la clase
        val keypadHeight = imeInsets.bottom
        val isKeyboardVisible = keypadHeight > 0

        rootView.setPadding(
            rootView.paddingLeft,
            rootView.paddingTop,
            rootView.paddingRight,
            imeInsets.bottom  // Solo el padding del teclado
        )

        Log.d(tag, "IME Visible: $isKeyboardVisible IME Size: $keypadHeight")
    }

    private fun availableSpace(): Int {
        val rect = Rect()
        val view = window.decorView
        view.getWindowVisibleDisplayFrame(rect)
        return rect.bottom - rect.top
    }

    private fun getMaxHeight(view: View): Int {
        return availableSpace() - (view.y + view.height).toInt()
    }

    private val topAppBarHeight: Int by lazy { resources.getDimensionPixelSize(R.dimen.top_app_bar_height) }
    private var isAdjustingLayout = false

    private fun somePopupVisible(): Boolean {
        return binding.autoCompleteTextView.isPopupShowing
    }

    private fun centerLayout() {
        adjustLayout(false)
    }

    private fun topLayout() {
        adjustLayout(true)
    }

    private fun adjustLayout(isTop: Boolean) {
        if (isAdjustingLayout) return
        isAdjustingLayout = true

        val set = ConstraintSet().apply {
            clone(binding.codeSelect)

            clear(binding.gralLayout.id, ConstraintSet.BOTTOM)
            clear(binding.gralLayout.id, ConstraintSet.TOP)

            if (isTop) {
                // Ancla a la parte superior
                connect(
                    binding.gralLayout.id,
                    ConstraintSet.TOP,
                    binding.codeSelect.id,
                    ConstraintSet.TOP,
                    topAppBarHeight,
                )
            } else {
                connect(
                    binding.gralLayout.id,
                    ConstraintSet.TOP,
                    binding.codeSelect.id,
                    ConstraintSet.TOP,
                    0,
                )
                connect(
                    binding.gralLayout.id,
                    ConstraintSet.BOTTOM,
                    binding.codeSelect.id,
                    ConstraintSet.BOTTOM,
                    0
                )
            }
        }

        set.applyTo(binding.codeSelect)
        isAdjustingLayout = false
    }

    private var isAdjustingPopup = false

    private fun adjustAndShowDropDown(view: ContractsAutoCompleteTextView, popUpHeight: Int) {
        view.dropDownHeight = popUpHeight
        view.showDropDown()
    }

    private fun adjustPopupHeight() {
        if (isAdjustingPopup) return
        isAdjustingPopup = true

        val adapter = iAdapter ?: run {
            isAdjustingPopup = false
            return
        }

        if (adapter.count == 0) {
            isAdjustingPopup = false
            return
        }

        val view = binding.autoCompleteTextView
        val availableHeight = getMaxHeight(view)
        val viewHeight = adapter.getViewTotalHeight(view.parent as ViewGroup, availableHeight)

        val isPopupShowing = view.isPopupShowing
        if (isPopupShowing) {
            lifecycleScope.launch {
                adjustAndShowDropDown(view, viewHeight)
            }
        }

        isAdjustingPopup = false
    }

    private fun adjustLayoutAndPopup() {
        when {
            binding.autoCompleteTextView.isPopupShowing -> adjustPopupHeight()
            else -> centerLayout()
        }
    }

    sealed class ContractType(val tag: String) {
        object Item : ContractType("item")
    }

    override fun contractsRetrieved(tag: Any?, count: Int) {
        when (tag) {
            ContractType.Item -> if (count > 0) {
                topLayout()
                adjustPopupHeight()
            }
        }

        if (!somePopupVisible()) {
            centerLayout()
        }
    }

    private val currentText: String
        get() {
            return binding.autoCompleteTextView.text.toString().trim()
        }

    private fun refreshText(cleanText: Boolean, focus: Boolean) {
        runOnUiThread {
            val textToSet = ean.takeIf { it.isNotBlank() } ?: if (cleanText) "" else currentText

            binding.autoCompleteTextView.setText(textToSet)

            binding.autoCompleteTextView.post {
                binding.autoCompleteTextView.setSelection(binding.autoCompleteTextView.length())
            }

            if (focus) {
                binding.autoCompleteTextView.requestFocus()
            }
        }
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

    private fun handleAdapterError(ex: Exception) {
        Log.e(tag, "Error filling adapter", ex)
        ErrorLog.writeLog(this, tag, ex)
        showProgressBar(GONE)
        showMessage("Error loading items", ERROR)
    }

    private fun createAndSetAdapter(itemList: List<Item>): GenericDropDownAdapter<Item> {
        val visibleStatus = listOf(ItemStatus.ACTIVE_LOT_DISABLED, ItemStatus.ACTIVE_LOT_DISABLED)

        val adapter = GenericDropDownAdapter.create(
            resource = R.layout.item_row_simple,
            itemList = itemList,
            visibleStatus = visibleStatus.toGenericStatusList(),
            bindView = { view, item, _ ->
                val descTextView: CheckedTextView = view.findViewById(R.id.description)
                val codeTextView: CheckedTextView = view.findViewById(R.id.ean)

                descTextView.text = item.description
                codeTextView.text = item.ean
            },
            getStatus = { item ->
                item.itemStatus.toGenericStatus()
            },
            styleView = { view, status ->
                val (backgroundColorResId, textColorResId) = colorsMap[status?.toItemStatus()]
                    ?: Pair(R.color.whitesmoke, R.color.black) // Valores por defecto

                view.setBackgroundColor(ContextCompat.getColor(view.context, backgroundColorResId))
                val descTextView: CheckedTextView = view.findViewById(R.id.description)
                val codeTextView: CheckedTextView = view.findViewById(R.id.ean)

                descTextView.setTextColor(ContextCompat.getColor(view.context, textColorResId))
                codeTextView.setTextColor(ContextCompat.getColor(view.context, textColorResId))
            },
            filterPredicate = { item, filterString ->
                item.ean.contains(filterString, true) ||
                        item.description.contains(filterString, true)
            },
            comparator = ItemComparator(),
            onReady = { refreshCodeText(cleanText = false, focus = false) }
        )

        return adapter
    }

    private val colorsMap = mapOf(
        ItemStatus.ACTIVE_LOT_ENABLED to Pair(R.color.whitesmoke, R.color.black),
        ItemStatus.ACTIVE_LOT_DISABLED to Pair(R.color.whitesmoke, R.color.black),
        ItemStatus.INACTIVE_LOT_ENABLED to Pair(R.color.lightgray, R.color.dimgray),
        ItemStatus.INACTIVE_LOT_DISABLED to Pair(R.color.lightgray, R.color.dimgray),
    )

    internal class ItemComparator : Comparator<Item> {
        override fun compare(o1: Item, o2: Item): Int {
            val eanComp = o1.ean.compareTo(o2.ean, ignoreCase = true)
            if (eanComp != 0) return eanComp
            return o1.description.compareTo(o2.description, ignoreCase = true)
        }
    }

    override fun scannerCompleted(scanCode: String) {
        ScannerManager.lockScanner(this, true)
        if (settingsVm.showScannedCode) showMessage(scanCode, SnackBarType.INFO)

        try {
            ean = scanCode
            refreshCodeText(cleanText = false, focus = true)
        } catch (ex: Exception) {
            ex.printStackTrace()
            showMessage(ex.message.toString(), ERROR)
            ErrorLog.writeLog(this, tag, ex)
        } finally {
            ScannerManager.lockScanner(this, false)
        }
    }

    private fun refreshCodeText(cleanText: Boolean, focus: Boolean) {
        runOnUiThread {
            binding.autoCompleteTextView.setText(ean.ifEmpty {
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
}