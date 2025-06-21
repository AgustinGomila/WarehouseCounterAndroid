package com.dacosys.warehouseCounter.ui.fragments.common

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.WindowManager
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
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam.Companion.ACTION_OPERATOR_LIKE
import com.dacosys.warehouseCounter.data.room.entity.itemCategory.ItemCategory
import com.dacosys.warehouseCounter.data.settings.Preference
import com.dacosys.warehouseCounter.databinding.SelectFilterFragmentBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.activities.itemCategory.ItemCategorySelectActivity
import com.dacosys.warehouseCounter.ui.activities.location.LocationSelectActivity
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.TextViewUtils.Companion.isActionDone

class SelectFilterFragment private constructor(builder: Builder) : Fragment() {

    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    /**
     * Required constructor for Fragments
     */
    constructor() : this(Builder())

    private var description: String

    private var allowPartialSearch: Boolean = false

    @Suppress("unused")
    fun setDescription(description: String) {
        this.description = description
        refreshViews()
    }

    fun getDescription(): String {
        return description
    }

    private var itemExternalId: String

    @Suppress("unused")
    fun setItemExternalId(itemExternalId: String) {
        this.itemExternalId = itemExternalId
        refreshViews()
    }

    fun getItemExternalId(): String {
        return itemExternalId
    }

    private var itemEan: String

    fun setItemEan(itemEan: String) {
        this.itemEan = itemEan
        refreshViews()
    }

    fun getItemEan(): String {
        return itemEan
    }

    private var itemCategory: ItemCategory?

    @Suppress("unused")
    fun setItemCategory(itemCategory: ItemCategory?) {
        this.itemCategory = itemCategory
        refreshViews()
    }

    fun getItemCategory(): ItemCategory? {
        return itemCategory
    }

    private var orderId: String

    fun setOrderId(orderId: String) {
        this.orderId = orderId
        refreshViews()
    }

    fun addOrderId(orderId: String) {
        var allOrders: MutableList<String> = mutableListOf()
        if (this.orderId.isNotEmpty())
            allOrders = this.orderId.split(Statics.LIST_SEPARATOR).toMutableList()

        if (allOrders.contains(orderId)) return

        allOrders.add(orderId)

        if (allOrders.count() == 1) this.orderId = allOrders.first()
        else this.orderId = allOrders.joinToString(Statics.LIST_SEPARATOR.toString())

        refreshViews()
    }

    fun removeOrderId(ids: List<String>) {
        var allOrders: MutableList<String> = mutableListOf()
        if (this.orderId.isNotEmpty())
            allOrders = this.orderId.split(Statics.LIST_SEPARATOR).toMutableList()

        if (allOrders.isEmpty()) return

        allOrders.removeAll(ids)

        this.orderId = allOrders.joinToString(Statics.LIST_SEPARATOR.toString())

        refreshViews()
    }

    @Suppress("unused")
    fun removeOrderId(orderId: String) {
        removeOrderId(arrayListOf(orderId))
    }

    @Suppress("unused")
    fun getOrderId(): String {
        return orderId
    }

    private var orderExternalId: String

    fun setOrderExternalId(orderExternalId: String) {
        this.orderExternalId = orderExternalId
        refreshViews()
    }

    @Suppress("unused")
    fun getOrderExternalId(): String {
        return orderExternalId
    }

    private var warehouse: Warehouse?

    fun setWarehouse(warehouse: Warehouse?) {
        this.warehouse = warehouse
        refreshViews()
    }

    fun getWarehouse(): Warehouse? {
        return warehouse
    }

    private var warehouseArea: WarehouseArea?

    fun setWarehouseArea(warehouseArea: WarehouseArea?) {
        this.warehouseArea = warehouseArea
        refreshViews()
    }

    fun getWarehouseArea(): WarehouseArea? {
        return warehouseArea
    }

    private var rack: Rack?

    fun setRack(rack: Rack?) {
        this.rack = rack
        refreshViews()
    }

    fun getRack(): Rack? {
        return rack
    }

    private var onlyActive: Boolean

    @Suppress("unused")
    fun setOnlyActive(onlyActive: Boolean) {
        this.onlyActive = onlyActive
        refreshViews()
    }

    @Suppress("unused")
    fun getOnlyActive(): Boolean {
        return onlyActive
    }

    private var searchByItemExternalId: Boolean
    private var pSearchByItemExternalId: Preference? = null

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
            orderId: String,
            orderExternalId: String,
            warehouseArea: WarehouseArea?,
            rack: Rack?,
            onlyActive: Boolean,
        )
    }

    interface OnFilterItemChangedListener {
        fun onFilterChanged(
            externalId: String,
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

        savedInstanceState.putString(ARG_ITEM_EXTERNAL_ID, itemExternalId)
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

        itemExternalId = b.getString(ARG_ITEM_EXTERNAL_ID) ?: ""
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

        return view
    }

    private val resultForWarehouseSelect = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it?.data
        try {
            if (it?.resultCode == RESULT_OK && data != null) {
                var value = ""
                if (allowPartialSearch) {
                    value = data.getStringExtra(LocationSelectActivity.ARG_SEARCH_STRING_VALUE) ?: ""
                } else {
                    warehouse = data.parcelable<Warehouse>(LocationSelectActivity.ARG_WAREHOUSE)
                    value = warehouse?.description ?: ""
                }
                setWarehouseText(value)
                onFilterChanged()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(requireActivity(), tag, ex)
        }
    }

    private val resultForAreaSelect = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it?.data
        try {
            if (it?.resultCode == RESULT_OK && data != null) {
                var value = ""
                if (allowPartialSearch) {
                    value = data.getStringExtra(LocationSelectActivity.ARG_SEARCH_STRING_VALUE) ?: ""
                } else {
                    warehouseArea = data.parcelable<WarehouseArea>(LocationSelectActivity.ARG_WAREHOUSE_AREA)
                    value = warehouseArea?.description ?: ""
                }
                setAreaText(value)
                onFilterChanged()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(requireActivity(), tag, ex)
        }
    }

    private val resultForRackSelect = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it?.data
        try {
            if (it?.resultCode == RESULT_OK && data != null) {
                var value = ""
                if (allowPartialSearch) {
                    value = data.getStringExtra(LocationSelectActivity.ARG_SEARCH_STRING_VALUE) ?: ""
                } else {
                    rack = data.parcelable<Rack>(LocationSelectActivity.ARG_RACK)
                    value = rack?.code ?: ""
                }
                setRackText(value)
                onFilterChanged()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(requireActivity(), tag, ex)
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
            ErrorLog.writeLog(requireActivity(), tag, ex)
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

            editText.setOnKeyListener { _, _, event ->
                if (isActionDone(event)) {
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

        // ONLY ACTIVE
        binding.onlyActiveCheckBox.setOnCheckedChangeListener(null)
        binding.onlyActiveCheckBox.setOnCheckedChangeListener { _, isChecked ->
            onlyActive = isChecked
            onFilterChanged()
        }

        // EXTERNAL ID
        binding.codeTextView.setOnClickListener {
            enterText(getString(R.string.enter_item_external_id), itemExternalId, getString(R.string.external_id)) {
                itemExternalId = it
                setExternalIdText()
                onFilterChanged()
            }
        }
        binding.codeSearchImageView.setOnClickListener { binding.codeTextView.performClick() }
        binding.codeClearImageView.setOnClickListener {
            itemExternalId = ""
            setExternalIdText()
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
            intent.putExtra(LocationSelectActivity.ARG_ALLOW_PARTIAL, allowPartialSearch)
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
            intent.putExtra(LocationSelectActivity.ARG_ALLOW_PARTIAL, allowPartialSearch)
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
            intent.putExtra(LocationSelectActivity.ARG_ALLOW_PARTIAL, allowPartialSearch)
            resultForRackSelect.launch(intent)
        }
        binding.rackSearchImageView.setOnClickListener { binding.rackTextView.performClick() }
        binding.rackClearImageView.setOnClickListener {
            rack = null
            setRackText()
            onFilterChanged()
        }

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
            externalId = itemExternalId,
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
            externalId = itemExternalId,
            description = description,
            ean = itemEan,
            orderId = orderId,
            orderExternalId = orderExternalId,
            warehouseArea = warehouseArea,
            rack = rack,
            onlyActive = onlyActive
        )
    }

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
        setExternalIdText()
        setDescriptionText()
        setEanText()
        setCategoryText()
        binding.onlyActiveCheckBox.isChecked = onlyActive
    }

    private fun setLocationTexts() {
        setWarehouseText(warehouse?.description ?: "")
        setAreaText(warehouseArea?.description ?: "")
        setRackText(rack?.code ?: "")
    }

    private fun setOrderTexts() {
        setDescriptionText()
        setOrderIdText()
        setOrderExternalIdText()
    }

    private fun setOrderLocationTexts() {
        setExternalIdText()
        setEanText()
        setDescriptionText()
        setOrderIdText()
        setOrderExternalIdText()
        setAreaText()
        setRackText()
        binding.onlyActiveCheckBox.isChecked = onlyActive
    }

    private fun setExternalIdText() {
        activity?.runOnUiThread {
            if (itemExternalId.isEmpty()) {
                binding.codeTextView.typeface = Typeface.DEFAULT
                binding.codeTextView.text = getString(R.string.search_by_external_id)
            } else {
                binding.codeTextView.typeface = Typeface.DEFAULT_BOLD
                binding.codeTextView.text = itemExternalId
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

    private fun setWarehouseText(value: String = "") {
        activity?.runOnUiThread {
            if (value.trim().isEmpty()) {
                binding.warehouseTextView.typeface = Typeface.DEFAULT
                binding.warehouseTextView.text = getString(R.string.search_by_warehouse)
            } else {
                binding.warehouseTextView.typeface = Typeface.DEFAULT_BOLD
                binding.warehouseTextView.text = value
            }
        }
    }

    private fun setAreaText(value: String = "") {
        activity?.runOnUiThread {
            if (value.trim().isEmpty()) {
                binding.areaTextView.typeface = Typeface.DEFAULT
                binding.areaTextView.text = getString(R.string.search_by_area)
            } else {
                binding.areaTextView.typeface = Typeface.DEFAULT_BOLD
                binding.areaTextView.text = value
            }
        }
    }

    private fun setRackText(value: String = "") {
        activity?.runOnUiThread {
            if (value.trim().isEmpty()) {
                binding.rackTextView.typeface = Typeface.DEFAULT
                binding.rackTextView.text = getString(R.string.search_by_rack)
            } else {
                binding.rackTextView.typeface = Typeface.DEFAULT_BOLD
                binding.rackTextView.text = value
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

        if (searchByItemExternalId && pSearchByItemExternalId != null) r.add(pSearchByItemExternalId!!)
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
        if (searchByItemExternalId) setCodeVisibility(View.VISIBLE)
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

    fun setCodeVisibility(visibility: Int) {
        binding.codePanel.visibility = visibility
        searchByItemExternalId = visibility == View.VISIBLE
    }

    fun setDescriptionVisibility(visibility: Int) {
        binding.descriptionPanel.visibility = visibility
        searchByItemDescription = visibility == View.VISIBLE
    }

    fun setEanVisibility(visibility: Int) {
        binding.eanPanel.visibility = visibility
        searchByItemEan = visibility == View.VISIBLE
    }

    fun setCategoryVisibility(visibility: Int) {
        binding.categoryPanel.visibility = visibility
        searchByCategory = visibility == View.VISIBLE
    }

    fun setOrderIdVisibility(visibility: Int) {
        binding.orderPanel.visibility = visibility
        searchByOrderId = visibility == View.VISIBLE
    }

    fun setOrderExtIdVisibility(visibility: Int) {
        binding.orderExternalIdPanel.visibility = visibility
        searchByOrderExtId = visibility == View.VISIBLE
    }

    fun setWarehouseVisibility(visibility: Int) {
        binding.warehousePanel.visibility = visibility
        searchByWarehouse = visibility == View.VISIBLE
    }

    fun setAreaVisibility(visibility: Int) {
        binding.areaPanel.visibility = visibility
        searchByArea = visibility == View.VISIBLE
    }

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
        return itemExternalId.isNotEmpty() ||
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
        return itemExternalId.isNotEmpty() ||
                description.isNotEmpty() ||
                itemEan.isNotEmpty() ||
                orderId.isNotEmpty() ||
                orderExternalId.isNotEmpty() ||
                warehouseArea != null ||
                rack != null
    }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterItemExternalId: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ITEM_EXTERNAL_ID,
                value = itemExternalId,
                conditional = ACTION_OPERATOR_LIKE
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterOrderLocationItemExternalId: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ORDER_LOCATION_EXTERNAL_ID,
                value = itemExternalId,
                conditional = ACTION_OPERATOR_LIKE
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterItemDescription: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ITEM_DESCRIPTION,
                value = description,
                conditional = ACTION_OPERATOR_LIKE
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterOrderLocationItemDescription: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ORDER_LOCATION_ITEM_DESCRIPTION,
                value = description,
                conditional = ACTION_OPERATOR_LIKE
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterItemEan: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ITEM_EAN,
                value = itemEan,
                conditional = ACTION_OPERATOR_LIKE
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterOrderLocationItemEan: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ORDER_LOCATION_EAN,
                value = itemEan,
                conditional = ACTION_OPERATOR_LIKE
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

    val filterOrderId: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ORDER_ORDER_ID,
                value = orderId
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterOrderIdArray: ArrayList<ApiFilterParam>
        get() {
            val allOrdersId = orderId.split(Statics.LIST_SEPARATOR)
            val filter: java.util.ArrayList<ApiFilterParam> = arrayListOf()
            for (id in allOrdersId) {
                filter.add(
                    ApiFilterParam(
                        columnName = ApiFilterParam.EXTENSION_ORDER_ORDER_ID,
                        value = id,
                        conditional = ApiFilterParam.ACTION_OPERATOR_IN
                    )
                )
            }
            return filter
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
                conditional = ACTION_OPERATOR_LIKE
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterOrderLocationExternalId: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ORDER_LOCATION_EXTERNAL_ID,
                value = orderExternalId,
                conditional = ACTION_OPERATOR_LIKE
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterOrderDescription: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ORDER_DESCRIPTION,
                value = description,
                conditional = ACTION_OPERATOR_LIKE
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterLocationWarehouseArea: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_LOCATION_AREA_ID,
                value = warehouseArea?.id.toString()
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterLocationRack: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_LOCATION_RACK_ID,
                value = rack?.id.toString()
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterOrderLocationWarehouseArea: ApiFilterParam
        get() {
            return ApiFilterParam(
                columnName = ApiFilterParam.EXTENSION_ORDER_LOCATION_AREA_ID,
                value = warehouseArea?.id.toString()
            )
        }

    @Suppress("MemberVisibilityCanBePrivate")
    val filterOrderLocationRack: ApiFilterParam
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
            if (itemExternalId.isNotEmpty()) filter.add(filterOrderLocationItemExternalId)
            if (description.isNotEmpty()) filter.add(filterOrderLocationItemDescription)
            if (itemEan.isNotEmpty()) filter.add(filterOrderLocationItemEan)
            if (orderId.isNotEmpty()) filter.add(filterOrderLocationId)
            if (orderExternalId.isNotEmpty()) filter.add(filterOrderLocationExternalId)
            if (warehouseArea != null) filter.add(filterOrderLocationWarehouseArea)
            if (rack != null) filter.add(filterOrderLocationRack)
        }
        return filter
    }

    private fun getOrderFilters(): ArrayList<ApiFilterParam> {
        val filter: ArrayList<ApiFilterParam> = arrayListOf()

        if (validOrderFilter()) {
            if (description.isNotEmpty()) filter.add(filterOrderDescription)
            if (orderId.isNotEmpty()) filter.addAll(filterOrderIdArray)
            if (orderExternalId.isNotEmpty()) filter.add(filterOrderExternalId)
        }
        return filter
    }

    private fun getItemFilters(): ArrayList<ApiFilterParam> {
        val filter: ArrayList<ApiFilterParam> = arrayListOf()

        if (validItemFilter()) {
            if (itemExternalId.isNotEmpty()) filter.add(filterItemExternalId)
            if (description.isNotEmpty()) filter.add(filterItemDescription)
            if (itemEan.isNotEmpty()) filter.add(filterItemEan)
            if (itemCategory != null) filter.add(filterItemCategory)
        }
        return filter
    }

    private fun getLocationFilters(): ArrayList<ApiFilterParam> {
        val filter: ArrayList<ApiFilterParam> = arrayListOf()

        if (validLocationFilter()) {
            if (warehouseArea != null) filter.add(filterLocationWarehouseArea)
            if (rack != null) filter.add(filterLocationRack)
        }
        return filter
    }

    fun clear() {
        description = ""
        itemExternalId = ""
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

        itemExternalId = builder.itemExternalId
        itemEan = builder.itemEan
        itemCategory = builder.itemCategory

        orderId = builder.orderId
        orderExternalId = builder.orderExternalId

        warehouse = builder.warehouse
        warehouseArea = builder.warehouseArea
        rack = builder.rack
        onlyActive = builder.onlyActive

        // Visibility
        searchByItemExternalId = builder.searchByItemExternalId
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
        pSearchByItemExternalId = builder.pSearchByItemExternalId
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

        allowPartialSearch = builder.allowPartialSearch
    }

    class Builder {
        fun build(): SelectFilterFragment {
            return SelectFilterFragment(this)
        }

        internal var allowPartialSearch: Boolean = false

        internal var description: String = ""

        internal var itemExternalId: String = ""
        internal var itemEan: String = ""
        internal var itemCategory: ItemCategory? = null

        internal var orderId: String = ""
        internal var orderExternalId: String = ""

        internal var warehouse: Warehouse? = null
        internal var warehouseArea: WarehouseArea? = null
        internal var rack: Rack? = null
        internal var onlyActive: Boolean = true

        internal var searchByItemExternalId: Boolean = false
        internal var pSearchByItemExternalId: Preference? = null

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
        fun itemExternalId(`val`: String): Builder {
            itemExternalId = `val`
            return this
        }

        fun itemDescription(value: String): Builder {
            description = value
            return this
        }

        fun itemEan(value: String): Builder {
            itemEan = value
            return this
        }

        fun itemCategory(value: ItemCategory?): Builder {
            itemCategory = value
            return this
        }

        fun orderId(value: String): Builder {
            orderId = value
            return this
        }

        fun orderExternalId(value: String): Builder {
            orderExternalId = value
            return this
        }

        fun orderDescription(value: String): Builder {
            description = value
            return this
        }

        fun warehouse(value: Warehouse?): Builder {
            warehouse = value
            return this
        }

        fun warehouseArea(value: WarehouseArea?): Builder {
            warehouseArea = value
            return this
        }

        fun rack(value: Rack?): Builder {
            rack = value
            return this
        }

        fun onlyActive(value: Boolean): Builder {
            onlyActive = value
            return this
        }

        @Suppress("unused")
        fun allowPartialSearch(): Builder {
            allowPartialSearch = true
            return this
        }

        fun searchByItemExternalId(value: Boolean, pref: Preference? = null): Builder {
            searchByItemExternalId = value
            pSearchByItemExternalId = pref
            return this
        }

        fun searchByItemDescription(value: Boolean, pref: Preference? = null): Builder {
            searchByItemDescription = value
            pSearchByItemDescription = pref
            return this
        }

        fun searchByItemEan(value: Boolean, pref: Preference? = null): Builder {
            searchByItemEan = value
            pSearchByItemEan = pref
            return this
        }

        fun searchByCategory(value: Boolean, pref: Preference? = null): Builder {
            searchByCategory = value
            pSearchByCategory = pref
            return this
        }

        fun searchByOrderId(value: Boolean, pref: Preference? = null): Builder {
            searchByOrderId = value
            pSearchByOrderId = pref
            return this
        }

        fun searchByOrderExtId(value: Boolean, pref: Preference? = null): Builder {
            searchByOrderExtId = value
            pSearchByOrderExtId = pref
            return this
        }

        fun searchByOrderDescription(value: Boolean, pref: Preference? = null): Builder {
            searchByOrderDescription = value
            pSearchByOrderDescription = pref
            return this
        }

        fun searchByWarehouse(value: Boolean, pref: Preference? = null): Builder {
            searchByWarehouse = value
            pSearchByWarehouse = pref
            return this
        }

        fun searchByArea(value: Boolean, pref: Preference? = null): Builder {
            searchByArea = value
            pSearchByArea = pref
            return this
        }

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
        const val ARG_ITEM_EXTERNAL_ID = "itemExternalId"
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
