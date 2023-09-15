package com.dacosys.warehouseCounter.ui.fragments.common

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Rack
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Warehouse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam.Companion.ACTION_CONDITIONAL_IN
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam.Companion.ACTION_CONDITIONAL_LIKE
import com.dacosys.warehouseCounter.data.room.entity.itemCategory.ItemCategory
import com.dacosys.warehouseCounter.data.settings.Preference
import com.dacosys.warehouseCounter.databinding.SelectFilterFragmentBinding
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.activities.itemCategory.ItemCategorySelectActivity
import com.dacosys.warehouseCounter.ui.activities.location.LocationSelectActivity
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SelectFilterFragment.OnFilterChangedListener] interface
 * to handle interaction events.
 */
class SelectFilterFragment private constructor(builder: Builder) : Fragment() {

    /**
     * Required constructor for Fragments
     */
    constructor() : this(Builder())

    var description: String

    var itemCode: String
    var itemEan: String
    var itemCategory: ItemCategory?

    var orderId: String
    var orderExternalId: String

    var warehouse: Warehouse?
    var warehouseArea: WarehouseArea?
    var rack: Rack?
    var onlyActive: Boolean

    private var searchByItemCode: Boolean
    private var pSearchByItemCode: Preference? = null

    private var searchByItemDescription: Boolean
    private var pSearchByItemDescription: Preference? = null

    private var searchByItemEan: Boolean
    private var pSearchByItemEan: Preference? = null

    private var searchByCategory: Boolean
    private var pSearchByCategory: Preference? = null

    private var searchByOrderId: Boolean
    private var pSearchByOrderId: Preference? = null

    private var searchByOrderExtId: Boolean
    private var pSearchByOrderExtId: Preference? = null

    private var searchByOrderDescription: Boolean
    private var pSearchByOrderDescription: Preference? = null

    private var searchByWarehouse: Boolean
    private var pSearchByWarehouse: Preference? = null

    private var searchByArea: Boolean
    private var pSearchByArea: Preference? = null

    private var searchByRack: Boolean
    private var pSearchByRack: Preference? = null

    private var searchByOnlyActive: Boolean
    private var pSearchByOnlyActive: Preference? = null

    private var filterItemChangedListener: OnFilterItemChangedListener? = null
    private var filterLocationChangedListener: OnFilterLocationChangedListener? = null
    private var filterOrderChangedListener: OnFilterOrderChangedListener? = null
    private var filterOrderLocationChangedListener: OnFilterOrderLocationChangedListener? = null

    // Container Activity must implement this interface
    interface OnFilterLocationChangedListener {
        fun onFilterChanged(
            warehouse: Warehouse?,
            warehouseArea: WarehouseArea?,
            rack: Rack?,
            onlyActive: Boolean,
        )
    }

    interface OnFilterOrderLocationChangedListener {
        fun onFilterChanged(
            externalId: String,
            description: String,
            ean: String,
            itemCategory: ItemCategory?,
            orderId: String,
            orderExternalId: String,
            warehouse: Warehouse?,
            warehouseArea: WarehouseArea?,
            rack: Rack?,
            onlyActive: Boolean,
        )
    }

    interface OnFilterItemChangedListener {
        fun onFilterChanged(
            code: String,
            description: String,
            ean: String,
            itemCategory: ItemCategory?,
            onlyActive: Boolean,
        )
    }

    interface OnFilterOrderChangedListener {
        fun onFilterChanged(
            orderId: String,
            orderExternalId: String,
            orderDescription: String
        )
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString(ARG_ITEM_DESCRIPTION, description)

        savedInstanceState.putString(ARG_ITEM_CODE, itemCode)
        savedInstanceState.putString(ARG_ITEM_EAN, itemEan)
        savedInstanceState.putParcelable(ARG_ITEM_CATEGORY, itemCategory)

        savedInstanceState.putString(ARG_ORDER_ID, orderId)
        savedInstanceState.putString(ARG_ORDER_EXTERNAL_ID, orderExternalId)

        savedInstanceState.putParcelable(ARG_WAREHOUSE, warehouse)
        savedInstanceState.putParcelable(ARG_WAREHOUSE_AREA, warehouseArea)
        savedInstanceState.putParcelable(ARG_RACK, rack)
        savedInstanceState.putBoolean(ARG_ONLY_ACTIVE, onlyActive)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        if (args != null) {
            loadSavedValues(args)
        }
    }

    private fun loadSavedValues(b: Bundle) {
        description = b.getString(ARG_ITEM_DESCRIPTION) ?: ""

        itemCode = b.getString(ARG_ITEM_CODE) ?: ""
        itemEan = b.getString(ARG_ITEM_EAN) ?: ""
        itemCategory = b.parcelable(ARG_ITEM_CATEGORY)

        orderId = b.getString(ARG_ORDER_ID) ?: ""
        orderExternalId = b.getString(ARG_ORDER_EXTERNAL_ID) ?: ""

        warehouse = b.parcelable(ARG_WAREHOUSE)
        warehouseArea = b.parcelable(ARG_WAREHOUSE_AREA)
        rack = b.parcelable(ARG_RACK)
        onlyActive = b.getBoolean(ARG_ONLY_ACTIVE)
    }

    private var _binding: SelectFilterFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SelectFilterFragmentBinding.inflate(inflater, container, false)
        val view = binding.root

        if (savedInstanceState != null) {
            loadSavedValues(savedInstanceState)
        }

        // ONLY ACTIVE
        binding.onlyActiveCheckBox.setOnCheckedChangeListener(null)
        binding.onlyActiveCheckBox.setOnCheckedChangeListener { _, isChecked ->
            onlyActive = isChecked
            onFilterChanged()
        }

        // CODE
        binding.codeTextView.setOnClickListener {
            enterText(getString(R.string.enter_code), itemCode, getString(R.string.code)) {
                itemCode = it
                setCodeText()
                onFilterChanged()
            }
        }
        binding.codeSearchImageView.setOnClickListener { binding.codeTextView.performClick() }
        binding.codeClearImageView.setOnClickListener {
            itemCode = ""
            setCodeText()
            onFilterChanged()
        }

        // DESCRIPTION
        binding.descriptionTextView.setOnClickListener {
            enterText(getString(R.string.enter_description), description, getString(R.string.description)) {
                description = it
                setDescriptionText()
                onFilterChanged()
            }
        }
        binding.descriptionSearchImageView.setOnClickListener { binding.descriptionTextView.performClick() }
        binding.descriptionClearImageView.setOnClickListener {
            description = ""
            setDescriptionText()
            onFilterChanged()
        }

        // EAN
        binding.eanTextView.setOnClickListener {
            enterText(getString(R.string.enter_ean), itemEan, getString(R.string.ean)) {
                itemEan = it
                setEanText()
                onFilterChanged()
            }
        }
        binding.eanSearchImageView.setOnClickListener { binding.eanTextView.performClick() }
        binding.eanClearImageView.setOnClickListener {
            itemEan = ""
            setEanText()
            onFilterChanged()
        }

        // ITEM CATEGORY
        binding.categoryTextView.setOnClickListener {
            val intent = Intent(requireContext(), ItemCategorySelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra(ItemCategorySelectActivity.ARG_ITEM_CATEGORY, itemCategory)
            intent.putExtra(ItemCategorySelectActivity.ARG_TITLE, getString(R.string.select_category))
            resultForCategorySelect.launch(intent)
        }
        binding.categorySearchImageView.setOnClickListener { binding.categoryTextView.performClick() }
        binding.categoryClearImageView.setOnClickListener {
            itemCategory = null
            setCategoryText()
            onFilterChanged()
        }

        // ORDER ID
        binding.orderTextView.setOnClickListener {
            enterText(getString(R.string.enter_order_id), orderId, getString(R.string.order)) {
                orderId = it
                setOrderIdText()
                onFilterChanged()
            }
        }
        binding.orderSearchImageView.setOnClickListener { binding.orderTextView.performClick() }
        binding.orderClearImageView.setOnClickListener {
            orderId = ""
            setOrderIdText()
            onFilterChanged()
        }

        // ORDER EXTERNAL ID
        binding.orderExternalIdTextView.setOnClickListener {
            enterText(getString(R.string.enter_order_external_id), orderExternalId, getString(R.string.external_id)) {
                orderExternalId = it
                setOrderExternalIdText()
                onFilterChanged()
            }
        }
        binding.orderExternalIdSearchImageView.setOnClickListener { binding.orderExternalIdTextView.performClick() }
        binding.orderExternalIdClearImageView.setOnClickListener {
            orderExternalId = ""
            setOrderExternalIdText()
            onFilterChanged()
        }

        // WAREHOUSE
        binding.warehouseTextView.setOnClickListener {
            val intent = Intent(requireActivity(), LocationSelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra(LocationSelectActivity.ARG_WAREHOUSE, warehouse)
            intent.putExtra(LocationSelectActivity.ARG_WAREHOUSE_AREA_VISIBLE, false)
            intent.putExtra(LocationSelectActivity.ARG_RACK_VISIBLE, false)
            intent.putExtra(LocationSelectActivity.ARG_TITLE, requireContext().getString(R.string.select_warehouse))
            resultForWarehouseSelect.launch(intent)
        }
        binding.warehouseSearchImageView.setOnClickListener { binding.warehouseTextView.performClick() }
        binding.warehouseClearImageView.setOnClickListener {
            warehouse = null
            setWarehouseText()
            onFilterChanged()
        }

        // WAREHOUSE AREA
        binding.areaTextView.setOnClickListener {
            val intent = Intent(requireActivity(), LocationSelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra(LocationSelectActivity.ARG_WAREHOUSE_AREA, warehouseArea)
            intent.putExtra(LocationSelectActivity.ARG_WAREHOUSE_VISIBLE, false)
            intent.putExtra(LocationSelectActivity.ARG_RACK_VISIBLE, false)
            intent.putExtra(LocationSelectActivity.ARG_TITLE, requireContext().getString(R.string.select_area))
            resultForAreaSelect.launch(intent)
        }

        binding.areaSearchImageView.setOnClickListener { binding.areaTextView.performClick() }
        binding.areaClearImageView.setOnClickListener {
            warehouseArea = null
            setAreaText()
            onFilterChanged()
        }

        // RACK
        binding.rackTextView.setOnClickListener {
            val intent = Intent(requireActivity(), LocationSelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra(LocationSelectActivity.ARG_RACK, rack)
            intent.putExtra(LocationSelectActivity.ARG_WAREHOUSE_VISIBLE, false)
            intent.putExtra(LocationSelectActivity.ARG_WAREHOUSE_AREA_VISIBLE, false)
            intent.putExtra(LocationSelectActivity.ARG_TITLE, requireContext().getString(R.string.select_rack))
            resultForRackSelect.launch(intent)
        }
        binding.rackSearchImageView.setOnClickListener { binding.rackTextView.performClick() }
        binding.rackClearImageView.setOnClickListener {
            rack = null
            setRackText()
            onFilterChanged()
        }

        return view
    }

    private val resultForAreaSelect = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it?.data
        try {
            if (it?.resultCode == RESULT_OK && data != null) {
                warehouseArea = data.parcelable<WarehouseArea>(LocationSelectActivity.ARG_WAREHOUSE_AREA)
                setAreaText()
                onFilterChanged()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(requireActivity(), this::class.java.simpleName, ex)
        }
    }

    private val resultForRackSelect = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it?.data
        try {
            if (it?.resultCode == RESULT_OK && data != null) {
                rack = data.parcelable<Rack>(LocationSelectActivity.ARG_RACK)
                setRackText()
                onFilterChanged()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(requireActivity(), this::class.java.simpleName, ex)
        }
    }

    private val resultForCategorySelect = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it?.data
        try {
            if (it?.resultCode == RESULT_OK && data != null) {
                itemCategory = data.parcelable(ItemCategorySelectActivity.ARG_ITEM_CATEGORY)
                setCategoryText()
                onFilterChanged()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(requireActivity(), this::class.java.simpleName, ex)
        }
    }

    private val resultForWarehouseSelect = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it?.data
        try {
            if (it?.resultCode == RESULT_OK && data != null) {
                warehouse = data.parcelable<Warehouse>(LocationSelectActivity.ARG_WAREHOUSE)
                setWarehouseText()
                onFilterChanged()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(requireActivity(), this::class.java.simpleName, ex)
        }
    }

    private fun showKeyboard(editText: View) {
        val inputMethodManager =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun enterText(title: String, text: String, hint: String, onResult: (String) -> Unit) {
        with(requireContext()) {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_layout, null)
            val editText = dialogView.findViewById<EditText>(R.id.editText)
            editText.setText(text)
            editText.isFocusable = true
            editText.isFocusableInTouchMode = true
            editText.hint = hint
            editText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
                editText.post {
                    if (hasFocus) {
                        showKeyboard(editText)
                    }
                }
            }

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle(title)
                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                    val t = editText.text.toString()
                    onResult.invoke(t)
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()

            editText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val t = editText.text.toString()
                    onResult.invoke(t)
                    dialog.dismiss()
                    true
                } else {
                    false
                }
            }
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                    val t = editText.text.toString()
                    onResult.invoke(t)
                    dialog.dismiss()
                    true
                } else {
                    false
                }
            }

            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            dialog.show()
            editText.requestFocus()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setVisibleFilters()
        refreshViews()
    }

    private fun onFilterChanged() {
        when (requireActivity()) {
            is OnFilterItemChangedListener -> onFilterItemChanged()
            is OnFilterLocationChangedListener -> onFilterLocationChanged()
            is OnFilterOrderChangedListener -> onFilterOrderChanged()
            is OnFilterOrderLocationChangedListener -> onFilterOrderLocationChanged()
        }
    }

    private fun onFilterItemChanged() {
        filterItemChangedListener?.onFilterChanged(
            code = itemCode,
            description = description,
            ean = itemEan,
            itemCategory = itemCategory,
            onlyActive = onlyActive
        )
    }

    private fun onFilterLocationChanged() {
        filterLocationChangedListener?.onFilterChanged(
            warehouse = warehouse,
            warehouseArea = warehouseArea,
            rack = rack,
            onlyActive = onlyActive
        )
    }

    private fun onFilterOrderChanged() {
        filterOrderChangedListener?.onFilterChanged(
            orderId = orderId,
            orderExternalId = orderExternalId,
            orderDescription = description
        )
    }

    private fun onFilterOrderLocationChanged() {
        filterOrderLocationChangedListener?.onFilterChanged(
            externalId = itemCode,
            description = description,
            ean = itemEan,
            itemCategory = itemCategory,
            orderId = orderId,
            orderExternalId = orderExternalId,
            warehouse = warehouse,
            warehouseArea = warehouseArea,
            rack = rack,
            onlyActive = onlyActive
        )
    }

    @Suppress("unused", "MemberVisibilityCanBePrivate")
    fun refreshViews() {
        activity?.runOnUiThread {
            when (requireActivity()) {
                is OnFilterItemChangedListener -> setItemTexts()
                is OnFilterLocationChangedListener -> setLocationTexts()
                is OnFilterOrderChangedListener -> setOrderTexts()
                is OnFilterOrderLocationChangedListener -> setOrderLocationTexts()
            }
        }
    }

    private fun setItemTexts() {
        setCodeText()
        setDescriptionText()
        setEanText()
        setCategoryText()
        binding.onlyActiveCheckBox.isChecked = onlyActive
    }

    private fun setLocationTexts() {
        setWarehouseText()
        setAreaText()
        setRackText()
    }

    private fun setOrderTexts() {
        setDescriptionText()
        setOrderIdText()
        setOrderExternalIdText()
    }

    private fun setOrderLocationTexts() {
        setExternalIdText()
        setEanText()
        setCategoryText()
        setDescriptionText()
        setOrderIdText()
        setOrderExternalIdText()
        setWarehouseText()
        setAreaText()
        setRackText()
        binding.onlyActiveCheckBox.isChecked = onlyActive
    }

    private fun setCodeText() {
        activity?.runOnUiThread {
            if (itemCode.isEmpty()) {
                binding.codeTextView.typeface = Typeface.DEFAULT
                binding.codeTextView.text = getString(R.string.search_by_item_code)
            } else {
                binding.codeTextView.typeface = Typeface.DEFAULT_BOLD
                binding.codeTextView.text = itemCode
            }
        }
    }

    private fun setExternalIdText() {
        activity?.runOnUiThread {
            if (itemCode.isEmpty()) {
                binding.codeTextView.typeface = Typeface.DEFAULT
                binding.codeTextView.text = getString(R.string.search_by_external_id)
            } else {
                binding.codeTextView.typeface = Typeface.DEFAULT_BOLD
                binding.codeTextView.text = itemCode
            }
        }
    }

    private fun setEanText() {
        activity?.runOnUiThread {
            if (itemEan.isEmpty()) {
                binding.eanTextView.typeface = Typeface.DEFAULT
                binding.eanTextView.text = getString(R.string.search_by_item_ean)
            } else {
                binding.eanTextView.typeface = Typeface.DEFAULT_BOLD
                binding.eanTextView.text = itemEan
            }
        }
    }

    private fun setDescriptionText() {
        activity?.runOnUiThread {
            if (description.isEmpty()) {
                binding.descriptionTextView.typeface = Typeface.DEFAULT
                binding.descriptionTextView.text = getString(R.string.search_by_description)
            } else {
                binding.descriptionTextView.typeface = Typeface.DEFAULT_BOLD
                binding.descriptionTextView.text = description
            }
        }
    }

    private fun setCategoryText() {
        activity?.runOnUiThread {
            if (itemCategory == null) {
                binding.categoryTextView.typeface = Typeface.DEFAULT
                binding.categoryTextView.text = getString(R.string.search_by_category)
            } else {
                binding.categoryTextView.typeface = Typeface.DEFAULT_BOLD
                binding.categoryTextView.text = itemCategory!!.description
            }
        }
    }

    private fun setOrderIdText() {
        activity?.runOnUiThread {
            if (orderId.isEmpty()) {
                binding.orderTextView.typeface = Typeface.DEFAULT
                binding.orderTextView.text = getString(R.string.search_by_order_id)
            } else {
                binding.orderTextView.typeface = Typeface.DEFAULT_BOLD
                binding.orderTextView.text = orderId
            }
        }
    }

    private fun setOrderExternalIdText() {
        activity?.runOnUiThread {
            if (orderExternalId.isEmpty()) {
                binding.orderExternalIdTextView.typeface = Typeface.DEFAULT
                binding.orderExternalIdTextView.text = getString(R.string.search_by_order_external_id)
            } else {
                binding.orderExternalIdTextView.typeface = Typeface.DEFAULT_BOLD
                binding.orderExternalIdTextView.text = orderExternalId
            }
        }
    }

    private fun setWarehouseText() {
        activity?.runOnUiThread {
            if (warehouse == null) {
                binding.warehouseTextView.typeface = Typeface.DEFAULT
                binding.warehouseTextView.text = getString(R.string.search_by_warehouse)
            } else {
                binding.warehouseTextView.typeface = Typeface.DEFAULT_BOLD
                binding.warehouseTextView.text = warehouse?.description
            }
        }
    }

    private fun setAreaText() {
        activity?.runOnUiThread {
            if (warehouseArea == null) {
                binding.areaTextView.typeface = Typeface.DEFAULT
                binding.areaTextView.text = getString(R.string.search_by_area)
            } else {
                binding.areaTextView.typeface = Typeface.DEFAULT_BOLD
                binding.areaTextView.text = warehouseArea?.description
            }
        }
    }

    private fun setRackText() {
        activity?.runOnUiThread {
            if (rack == null) {
                binding.rackTextView.typeface = Typeface.DEFAULT
                binding.rackTextView.text = getString(R.string.search_by_rack)
            } else {
                binding.rackTextView.typeface = Typeface.DEFAULT_BOLD
                binding.rackTextView.text = rack?.code
            }
        }
    }

    // Se llama cuando el fragmento está visible ante el usuario.
    // Obviamente, depende del método onStart() de la actividad para saber si la actividad se está mostrando.
    override fun onStart() {
        super.onStart()
        when (requireActivity()) {
            is OnFilterItemChangedListener -> filterItemChangedListener = activity as OnFilterItemChangedListener
            is OnFilterLocationChangedListener -> filterLocationChangedListener =
                activity as OnFilterLocationChangedListener

            is OnFilterOrderChangedListener -> filterOrderChangedListener = activity as OnFilterOrderChangedListener
            is OnFilterOrderLocationChangedListener -> filterOrderLocationChangedListener =
                activity as OnFilterOrderLocationChangedListener
        }
    }

    // Se llama cuando el fragmento ya no está asociado a la actividad anfitriona.
    override fun onDetach() {
        super.onDetach()
        filterItemChangedListener = null
        filterOrderChangedListener = null
        filterLocationChangedListener = null
        filterOrderLocationChangedListener = null
    }

    // region Visibility functions
    fun getVisibleFilters(): ArrayList<Preference> {
        val r: ArrayList<Preference> = ArrayList()

        if (searchByItemCode && pSearchByItemCode != null) r.add(pSearchByItemCode!!)
        if (searchByItemDescription && pSearchByItemDescription != null) r.add(pSearchByItemDescription!!)
        if (searchByItemEan && pSearchByItemEan != null) r.add(pSearchByItemEan!!)
        if (searchByCategory && pSearchByCategory != null) r.add(pSearchByCategory!!)
        if (searchByOrderId && pSearchByOrderId != null) r.add(pSearchByOrderId!!)
        if (searchByOrderExtId && pSearchByOrderExtId != null) r.add(pSearchByOrderExtId!!)
        if (searchByOrderDescription && pSearchByOrderDescription != null) r.add(pSearchByOrderDescription!!)
        if (searchByWarehouse && pSearchByWarehouse != null) r.add(pSearchByWarehouse!!)
        if (searchByArea && pSearchByArea != null) r.add(pSearchByArea!!)
        if (searchByRack && pSearchByRack != null) r.add(pSearchByRack!!)
        if (searchByOnlyActive && pSearchByOnlyActive != null) r.add(pSearchByOnlyActive!!)

        return r
    }

    private fun setVisibleFilters() {
        //Retrieve the values
        if (searchByItemCode) setCodeVisibility(View.VISIBLE)
        else setCodeVisibility(View.GONE)

        if (searchByItemDescription || searchByOrderDescription) setDescriptionVisibility(View.VISIBLE)
        else setDescriptionVisibility(View.GONE)

        if (searchByItemEan) setEanVisibility(View.VISIBLE)
        else setEanVisibility(View.GONE)

        if (searchByCategory) setCategoryVisibility(View.VISIBLE)
        else setCategoryVisibility(View.GONE)

        if (searchByOrderId) setOrderIdVisibility(View.VISIBLE)
        else setOrderIdVisibility(View.GONE)

        if (searchByOrderExtId) setOrderExtIdVisibility(View.VISIBLE)
        else setOrderExtIdVisibility(View.GONE)

        if (searchByWarehouse) setWarehouseVisibility(View.VISIBLE)
        else setWarehouseVisibility(View.GONE)

        if (searchByArea) setAreaVisibility(View.VISIBLE)
        else setAreaVisibility(View.GONE)

        if (searchByRack) setRackVisibility(View.VISIBLE)
        else setRackVisibility(View.GONE)

        if (searchByOnlyActive) setOnlyActiveVisibility(View.VISIBLE)
        else setOnlyActiveVisibility(View.GONE)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setCodeVisibility(visibility: Int) {
        binding.codePanel.visibility = visibility
        searchByItemCode = visibility == View.VISIBLE
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setDescriptionVisibility(visibility: Int) {
        binding.descriptionPanel.visibility = visibility
        searchByItemDescription = visibility == View.VISIBLE
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setEanVisibility(visibility: Int) {
        binding.eanPanel.visibility = visibility
        searchByItemEan = visibility == View.VISIBLE
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setCategoryVisibility(visibility: Int) {
        binding.categoryPanel.visibility = visibility
        searchByCategory = visibility == View.VISIBLE
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setOrderIdVisibility(visibility: Int) {
        binding.orderPanel.visibility = visibility
        searchByOrderId = visibility == View.VISIBLE
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setOrderExtIdVisibility(visibility: Int) {
        binding.orderExternalIdPanel.visibility = visibility
        searchByOrderExtId = visibility == View.VISIBLE
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setWarehouseVisibility(visibility: Int) {
        binding.warehousePanel.visibility = visibility
        searchByWarehouse = visibility == View.VISIBLE
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setAreaVisibility(visibility: Int) {
        binding.areaPanel.visibility = visibility
        searchByArea = visibility == View.VISIBLE
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setRackVisibility(visibility: Int) {
        binding.rackPanel.visibility = visibility
        searchByRack = visibility == View.VISIBLE
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setOnlyActiveVisibility(visibility: Int) {
        binding.onlyActiveCheckBox.visibility = visibility
        searchByOnlyActive = visibility == View.VISIBLE
    }
    // endregion Visibility functions

    fun validFilters(): Boolean {
        return when (requireActivity()) {
            is OnFilterLocationChangedListener -> return validLocationFilter()
            is OnFilterOrderChangedListener -> return validOrderFilter()
            is OnFilterOrderLocationChangedListener -> return validOrderLocationFilter()
            is OnFilterItemChangedListener -> return validItemFilter()
            else -> false
        }
    }

    private fun validItemFilter(): Boolean {
        return itemCode.isNotEmpty() ||
                description.isNotEmpty() ||
                itemEan.isNotEmpty() ||
                itemCategory != null
    }

    private fun validLocationFilter(): Boolean {
        return warehouse != null ||
                warehouseArea != null ||
                rack != null
    }

    private fun validOrderFilter(): Boolean {
        return description.isNotEmpty() ||
                orderId.isNotEmpty() ||
                orderExternalId.isNotEmpty()
    }

    private fun validOrderLocationFilter(): Boolean {
        return itemCode.isNotEmpty() ||
                description.isNotEmpty() ||
                itemEan.isNotEmpty() ||
                itemCategory != null ||
                orderId.isNotEmpty() ||
                orderExternalId.isNotEmpty() ||
                warehouse != null ||
                warehouseArea != null ||
                rack != null
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun filterIdIn(id: Long): ApiFilterParam {
        return ApiFilterParam(
            columnName = ApiFilterParam.EXTENSION_ID,
            value = id.toString(),
            conditional = ACTION_CONDITIONAL_IN
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterItemExternalId: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ITEM_EXTERNAL_ID,
                value = itemCode,
                conditional = ACTION_CONDITIONAL_LIKE
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterItemDescription: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ITEM_DESCRIPTION,
                value = description,
                conditional = ACTION_CONDITIONAL_LIKE
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterItemEan: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ITEM_EAN,
                value = itemEan,
                conditional = ACTION_CONDITIONAL_LIKE
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterItemCategory: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ITEM_CATEGORY_ID,
                value = itemCategory?.itemCategoryId.toString()
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterOrderId: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ORDER_ORDER_ID,
                value = orderId
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterOrderLocationId: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ORDER_LOCATION_ORDER_ID,
                value = orderId
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterOrderExternalId: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ORDER_EXTERNAL_ID,
                value = orderExternalId,
                conditional = ACTION_CONDITIONAL_LIKE
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterOrderDescription: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ORDER_DESCRIPTION,
                value = description,
                conditional = ACTION_CONDITIONAL_LIKE
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterWarehouse: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ORDER_LOCATION_WAREHOUSE_ID,
                value = warehouse?.id.toString()
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterWarehouseArea: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ORDER_LOCATION_AREA_ID,
                value = warehouseArea?.id.toString()
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterRack: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ORDER_LOCATION_RACK_ID,
                value = rack?.id.toString()
            )
        }

    fun getFilters(): ArrayList<ApiFilterParam> {
        return when (requireActivity()) {
            is OnFilterLocationChangedListener -> getLocationFilters()
            is OnFilterOrderChangedListener -> getOrderFilters()
            is OnFilterOrderLocationChangedListener -> getOrderLocationFilters()
            is OnFilterItemChangedListener -> getItemFilters()
            else -> arrayListOf()
        }
    }

    private fun getOrderLocationFilters(): ArrayList<ApiFilterParam> {
        val filter: ArrayList<ApiFilterParam> = arrayListOf()

        if (validOrderLocationFilter()) {
            if (itemCode.isNotEmpty()) filter.add(filterItemExternalId)
            if (description.isNotEmpty()) filter.add(filterItemDescription)
            if (itemEan.isNotEmpty()) filter.add(filterItemEan)
            if (itemCategory != null) filter.add(filterItemCategory)
            if (orderId.isNotEmpty()) filter.add(filterOrderLocationId)
            if (orderExternalId.isNotEmpty()) filter.add(filterOrderExternalId)
            if (warehouse != null) filter.add(filterWarehouse)
            if (warehouseArea != null) filter.add(filterWarehouseArea)
            if (rack != null) filter.add(filterRack)
        }
        return filter
    }

    private fun getOrderFilters(): ArrayList<ApiFilterParam> {
        val filter: ArrayList<ApiFilterParam> = arrayListOf()

        if (validOrderFilter()) {
            if (description.isNotEmpty()) filter.add(filterOrderDescription)
            if (orderId.isNotEmpty()) filter.add(filterOrderId)
            if (orderExternalId.isNotEmpty()) filter.add(filterOrderExternalId)
        }
        return filter
    }

    private fun getItemFilters(): ArrayList<ApiFilterParam> {
        val filter: ArrayList<ApiFilterParam> = arrayListOf()

        if (validItemFilter()) {
            if (itemCode.isNotEmpty()) filter.add(filterItemExternalId)
            if (description.isNotEmpty()) filter.add(filterItemDescription)
            if (itemEan.isNotEmpty()) filter.add(filterItemEan)
            if (itemCategory != null) filter.add(filterItemCategory)
        }
        return filter
    }

    private fun getLocationFilters(): ArrayList<ApiFilterParam> {
        val filter: ArrayList<ApiFilterParam> = arrayListOf()

        if (validLocationFilter()) {
            if (warehouse != null) filter.add(filterWarehouse)
            if (warehouseArea != null) filter.add(filterWarehouseArea)
            if (rack != null) filter.add(filterRack)
        }
        return filter
    }

    fun clear() {
        description = ""
        itemCode = ""
        itemEan = ""
        itemCategory = null
        orderId = ""
        orderExternalId = ""
        warehouse = null
        warehouseArea = null
        rack = null

        refreshViews()
        onFilterChanged()
    }

    init {
        // Values
        description = builder.description

        itemCode = builder.itemCode
        itemEan = builder.itemEan
        itemCategory = builder.itemCategory

        orderId = builder.orderId
        orderExternalId = builder.orderExternalId

        warehouse = builder.warehouse
        warehouseArea = builder.warehouseArea
        rack = builder.rack
        onlyActive = builder.onlyActive

        // Visibility
        searchByItemCode = builder.searchByItemCode
        searchByItemDescription = builder.searchByItemDescription
        searchByItemEan = builder.searchByItemEan
        searchByCategory = builder.searchByCategory

        searchByOrderId = builder.searchByOrderId
        searchByOrderExtId = builder.searchByOrderExtId
        searchByOrderDescription = builder.searchByOrderDescription

        searchByWarehouse = builder.searchByWarehouse
        searchByArea = builder.searchByArea
        searchByRack = builder.searchByRack
        searchByOnlyActive = builder.searchByOnlyActive

        // Preferences
        pSearchByItemCode = builder.pSearchByItemCode
        pSearchByItemDescription = builder.pSearchByItemDescription
        pSearchByItemEan = builder.pSearchByItemEan
        pSearchByCategory = builder.pSearchByCategory

        pSearchByOrderId = builder.pSearchByOrderId
        pSearchByOrderExtId = builder.pSearchByOrderExtId
        pSearchByOrderDescription = builder.pSearchByOrderDescription

        pSearchByWarehouse = builder.pSearchByWarehouse
        pSearchByArea = builder.pSearchByArea
        pSearchByRack = builder.pSearchByRack
        pSearchByOnlyActive = builder.pSearchByOnlyActive
    }

    class Builder {
        fun build(): SelectFilterFragment {
            return SelectFilterFragment(this)
        }

        internal var description: String = ""

        internal var itemCode: String = ""
        internal var itemEan: String = ""
        internal var itemCategory: ItemCategory? = null

        internal var orderId: String = ""
        internal var orderExternalId: String = ""

        internal var warehouse: Warehouse? = null
        internal var warehouseArea: WarehouseArea? = null
        internal var rack: Rack? = null
        internal var onlyActive: Boolean = true

        internal var searchByItemCode: Boolean = false
        internal var pSearchByItemCode: Preference? = null

        internal var searchByItemDescription: Boolean = false
        internal var pSearchByItemDescription: Preference? = null

        internal var searchByItemEan: Boolean = false
        internal var pSearchByItemEan: Preference? = null

        internal var searchByCategory: Boolean = false
        internal var pSearchByCategory: Preference? = null

        internal var searchByOrderId: Boolean = false
        internal var pSearchByOrderId: Preference? = null

        internal var searchByOrderExtId: Boolean = false
        internal var pSearchByOrderExtId: Preference? = null

        internal var searchByOrderDescription: Boolean = false
        internal var pSearchByOrderDescription: Preference? = null

        internal var searchByWarehouse: Boolean = false
        internal var pSearchByWarehouse: Preference? = null

        internal var searchByArea: Boolean = false
        internal var pSearchByArea: Preference? = null

        internal var searchByRack: Boolean = false
        internal var pSearchByRack: Preference? = null

        internal var searchByOnlyActive: Boolean = false
        internal var pSearchByOnlyActive: Preference? = null

        // Setter methods for variables with chained methods
        @Suppress("unused")
        fun itemCode(`val`: String): Builder {
            itemCode = `val`
            return this
        }

        @Suppress("unused")
        fun itemDescription(value: String): Builder {
            description = value
            return this
        }

        @Suppress("unused")
        fun itemEan(value: String): Builder {
            itemEan = value
            return this
        }

        @Suppress("unused")
        fun itemCategory(value: ItemCategory?): Builder {
            itemCategory = value
            return this
        }

        @Suppress("unused")
        fun orderId(value: String): Builder {
            orderId = value
            return this
        }

        @Suppress("unused")
        fun orderExternalId(value: String): Builder {
            orderExternalId = value
            return this
        }

        @Suppress("unused")
        fun orderDescription(value: String): Builder {
            description = value
            return this
        }

        @Suppress("unused")
        fun warehouse(value: Warehouse?): Builder {
            warehouse = value
            return this
        }

        @Suppress("unused")
        fun warehouseArea(value: WarehouseArea?): Builder {
            warehouseArea = value
            return this
        }

        @Suppress("unused")
        fun rack(value: Rack?): Builder {
            rack = value
            return this
        }

        @Suppress("unused")
        fun onlyActive(value: Boolean): Builder {
            onlyActive = value
            return this
        }

        @Suppress("unused")
        fun searchByItemCode(value: Boolean, pref: Preference? = null): Builder {
            searchByItemCode = value
            pSearchByItemCode = pref
            return this
        }

        @Suppress("unused")
        fun searchByItemDescription(value: Boolean, pref: Preference? = null): Builder {
            searchByItemDescription = value
            pSearchByItemDescription = pref
            return this
        }

        @Suppress("unused")
        fun searchByItemEan(value: Boolean, pref: Preference? = null): Builder {
            searchByItemEan = value
            pSearchByItemEan = pref
            return this
        }

        @Suppress("unused")
        fun searchByCategory(value: Boolean, pref: Preference? = null): Builder {
            searchByCategory = value
            pSearchByCategory = pref
            return this
        }

        @Suppress("unused")
        fun searchByOrderId(value: Boolean, pref: Preference? = null): Builder {
            searchByOrderId = value
            pSearchByOrderId = pref
            return this
        }

        @Suppress("unused")
        fun searchByOrderExtId(value: Boolean, pref: Preference? = null): Builder {
            searchByOrderExtId = value
            pSearchByOrderExtId = pref
            return this
        }

        @Suppress("unused")
        fun searchByOrderDescription(value: Boolean, pref: Preference? = null): Builder {
            searchByOrderDescription = value
            pSearchByOrderDescription = pref
            return this
        }

        @Suppress("unused")
        fun searchByWarehouse(value: Boolean, pref: Preference? = null): Builder {
            searchByWarehouse = value
            pSearchByWarehouse = pref
            return this
        }

        @Suppress("unused")
        fun searchByArea(value: Boolean, pref: Preference? = null): Builder {
            searchByArea = value
            pSearchByArea = pref
            return this
        }

        @Suppress("unused")
        fun searchByRack(value: Boolean, pref: Preference? = null): Builder {
            searchByRack = value
            pSearchByRack = pref
            return this
        }

        @Suppress("unused")
        fun searchByOnlyActive(value: Boolean, pref: Preference? = null): Builder {
            searchByOnlyActive = value
            pSearchByOnlyActive = pref
            return this
        }
    }

    companion object {

        // The fragment initialization parameters
        const val ARG_ITEM_CODE = "itemCode"
        const val ARG_ITEM_DESCRIPTION = "description"
        const val ARG_ITEM_EAN = "itemEan"
        const val ARG_ITEM_CATEGORY = "itemCategory"
        const val ARG_ORDER_ID = "orderId"
        const val ARG_ORDER_EXTERNAL_ID = "orderExternalId"
        const val ARG_WAREHOUSE = "warehouse"
        const val ARG_WAREHOUSE_AREA = "warehouseArea"
        const val ARG_RACK = "rack"
        const val ARG_ONLY_ACTIVE = "onlyActive"
    }
}
