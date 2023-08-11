package com.dacosys.warehouseCounter.ui.fragments.orderRequest

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.OrderLocationSelectFilterFragmentBinding
import com.dacosys.warehouseCounter.settings.Preference


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [OrderLocationFilterFragment.OnFilterChangedListener] interface
 * to handle interaction events.
 * Use the [OrderLocationFilterFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OrderLocationFilterFragment : Fragment() {
    private var mCallback: OnFilterChangedListener? = null

    // Configuración guardada de los controles que se ven o no se ven
    var itemCode: String = ""
    var itemDescription: String = ""
    var itemEan: String = ""
    var orderId: String = ""
    var orderExternalId: String = ""
    var warehouse: String = ""
    var warehouseArea: String = ""
    var rack: String = ""
    var onlyActive: Boolean = true

    // Container Activity must implement this interface
    interface OnFilterChangedListener {
        fun onFilterChanged(
            code: String,
            description: String,
            ean: String,
            orderId: String,
            orderExternalId: String,
            warehouse: String,
            warehouseArea: String,
            rack: String,
            onlyActive: Boolean,
        )
    }

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        this.mCallback = null
    }

    fun setListener(listener: OnFilterChangedListener) {
        this.mCallback = listener
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString(ARG_ITEM_CODE, itemCode)
        savedInstanceState.putString(ARG_ITEM_DESCRIPTION, itemDescription)
        savedInstanceState.putString(ARG_ITEM_EAN, itemEan)
        savedInstanceState.putString(ARG_ORDER_ID, orderId)
        savedInstanceState.putString(ARG_ORDER_EXTERNAL_ID, orderExternalId)
        savedInstanceState.putString(ARG_WAREHOUSE, warehouse)
        savedInstanceState.putString(ARG_WAREHOUSE_AREA, warehouseArea)
        savedInstanceState.putString(ARG_RACK, rack)
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
        itemCode = b.getString(ARG_ITEM_CODE) ?: ""
        itemDescription = b.getString(ARG_ITEM_DESCRIPTION) ?: ""
        itemEan = b.getString(ARG_ITEM_EAN) ?: ""
        orderId = b.getString(ARG_ORDER_ID) ?: ""
        orderExternalId = b.getString(ARG_ORDER_EXTERNAL_ID) ?: ""
        warehouse = b.getString(ARG_WAREHOUSE) ?: ""
        warehouseArea = b.getString(ARG_WAREHOUSE_AREA) ?: ""
        rack = b.getString(ARG_RACK) ?: ""
        onlyActive = b.getBoolean(ARG_ONLY_ACTIVE)
    }

    private var _binding: OrderLocationSelectFilterFragmentBinding? = null

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
        _binding = OrderLocationSelectFilterFragmentBinding.inflate(inflater, container, false)
        val view = binding.root

        if (savedInstanceState != null) {
            loadSavedValues(savedInstanceState)
        }

        binding.onlyActiveCheckBox.setOnCheckedChangeListener(null)
        binding.onlyActiveCheckBox.setOnCheckedChangeListener { _, isChecked ->
            settingViewModel.orderLocationOnlyActive = isChecked
            onlyActive = binding.onlyActiveCheckBox.isChecked
            onFilterChanged()
        }

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

        binding.descriptionTextView.setOnClickListener {
            enterText(getString(R.string.enter_description), itemDescription, getString(R.string.description)) {
                itemDescription = it
                setDescriptionText()
                onFilterChanged()
            }
        }
        binding.descriptionSearchImageView.setOnClickListener { binding.descriptionTextView.performClick() }
        binding.descriptionClearImageView.setOnClickListener {
            itemDescription = ""
            setDescriptionText()
            onFilterChanged()
        }

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

        binding.orderExternalIdTextView.setOnClickListener {
            enterText(getString(R.string.enter_order_external_id), orderExternalId, getString(R.string.external_id)) {
                orderExternalId = it
                setOrderExternalIdText()
                onFilterChanged()
            }
        }
        binding.orderSearchImageView.setOnClickListener { binding.orderExternalIdTextView.performClick() }
        binding.orderClearImageView.setOnClickListener {
            orderExternalId = ""
            setOrderExternalIdText()
            onFilterChanged()
        }

        binding.warehouseTextView.setOnClickListener {
            enterText(getString(R.string.enter_warehouse), warehouse, getString(R.string.warehouse)) {
                warehouse = it
                setWarehouseText()
                onFilterChanged()
            }
        }
        binding.warehouseSearchImageView.setOnClickListener { binding.warehouseTextView.performClick() }
        binding.warehouseClearImageView.setOnClickListener {
            warehouse = ""
            setWarehouseText()
            onFilterChanged()
        }

        binding.areaTextView.setOnClickListener {
            enterText(getString(R.string.enter_area), warehouseArea, getString(R.string.area)) {
                warehouseArea = it
                setAreaText()
                onFilterChanged()
            }
        }
        binding.areaSearchImageView.setOnClickListener { binding.areaTextView.performClick() }
        binding.areaClearImageView.setOnClickListener {
            warehouseArea = ""
            setAreaText()
            onFilterChanged()
        }

        binding.rackTextView.setOnClickListener {
            enterText(getString(R.string.enter_rack), rack, getString(R.string.rack)) {
                rack = it
                setRackText()
                onFilterChanged()
            }
        }
        binding.rackSearchImageView.setOnClickListener { binding.rackTextView.performClick() }
        binding.rackClearImageView.setOnClickListener {
            rack = ""
            setRackText()
            onFilterChanged()
        }

        return view
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
            editText.hint = hint
            editText.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
                editText.post {
                    if (hasFocus) {
                        showKeyboard(editText)
                    }
                }
            }
            editText.requestFocus()

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
            dialog.show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setVisibleFilters()
        refreshTextViews()
    }

    private fun onFilterChanged() {
        mCallback?.onFilterChanged(
            code = itemCode,
            description = itemDescription,
            ean = itemEan,
            orderId = orderId,
            orderExternalId = orderExternalId,
            warehouse = warehouse,
            warehouseArea = warehouseArea,
            rack = rack,
            onlyActive = onlyActive
        )
    }

    fun refreshTextViews() {
        activity?.runOnUiThread {
            binding.onlyActiveCheckBox.isChecked = onlyActive

            setCodeText()
            setDescriptionText()
            setEanText()
            setOrderIdText()
            setOrderExternalIdText()
            setWarehouseText()
            setAreaText()
            setRackText()
        }
    }

    private fun setCodeText() {
        activity?.runOnUiThread {
            if (itemCode.isEmpty()) {
                binding.codeTextView.typeface = Typeface.DEFAULT
                binding.codeTextView.text = getString(R.string.order_location_search_by_item_code)
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
                binding.eanTextView.text = getString(R.string.order_location_search_by_item_ean)
            } else {
                binding.eanTextView.typeface = Typeface.DEFAULT_BOLD
                binding.eanTextView.text = itemEan
            }
        }
    }

    private fun setDescriptionText() {
        activity?.runOnUiThread {
            if (itemDescription.isEmpty()) {
                binding.descriptionTextView.typeface = Typeface.DEFAULT
                binding.descriptionTextView.text = getString(R.string.order_location_search_by_item_description)
            } else {
                binding.descriptionTextView.typeface = Typeface.DEFAULT_BOLD
                binding.descriptionTextView.text = itemDescription
            }
        }
    }

    private fun setOrderIdText() {
        activity?.runOnUiThread {
            if (orderId.isEmpty()) {
                binding.orderTextView.typeface = Typeface.DEFAULT
                binding.orderTextView.text = getString(R.string.order_location_search_by_order_id)
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
                binding.orderExternalIdTextView.text = getString(R.string.order_location_search_by_order_external_id)
            } else {
                binding.orderExternalIdTextView.typeface = Typeface.DEFAULT_BOLD
                binding.orderExternalIdTextView.text = orderExternalId
            }
        }
    }

    private fun setWarehouseText() {
        activity?.runOnUiThread {
            if (warehouse.isEmpty()) {
                binding.warehouseTextView.typeface = Typeface.DEFAULT
                binding.warehouseTextView.text = getString(R.string.order_location_search_by_warehouse)
            } else {
                binding.warehouseTextView.typeface = Typeface.DEFAULT_BOLD
                binding.warehouseTextView.text = itemEan
            }
        }
    }

    private fun setAreaText() {
        activity?.runOnUiThread {
            if (warehouseArea.isEmpty()) {
                binding.areaTextView.typeface = Typeface.DEFAULT
                binding.areaTextView.text = getString(R.string.order_location_search_by_area)
            } else {
                binding.areaTextView.typeface = Typeface.DEFAULT_BOLD
                binding.areaTextView.text = itemEan
            }
        }
    }

    private fun setRackText() {
        activity?.runOnUiThread {
            if (rack.isEmpty()) {
                binding.rackTextView.typeface = Typeface.DEFAULT
                binding.rackTextView.text = getString(R.string.order_location_search_by_rack)
            } else {
                binding.rackTextView.typeface = Typeface.DEFAULT_BOLD
                binding.rackTextView.text = itemEan
            }
        }
    }

    // Se llama cuando el fragmento está visible ante el usuario.
    // Obviamente, depende del método onStart() de la actividad para saber si la actividad se está mostrando.
    override fun onStart() {
        super.onStart()
        if (mCallback is OnFilterChangedListener) {
            mCallback = activity as OnFilterChangedListener
        }
    }

    // Se llama cuando el fragmento ya no está asociado a la actividad anfitriona.
    override fun onDetach() {
        super.onDetach()
        mCallback = null
    }

    // region Visibility functions
    fun getVisibleFilters(): ArrayList<Preference> {
        val sv = settingViewModel
        val sp = settingRepository

        val r: ArrayList<Preference> = ArrayList()

        if (sv.orderLocationSearchByItemCode) r.add(sp.orderLocationSearchByItemCode)
        if (sv.orderLocationSearchByItemDescription) r.add(sp.orderLocationSearchByItemDescription)
        if (sv.orderLocationSearchByItemEan) r.add(sp.orderLocationSearchByItemEan)
        if (sv.orderLocationSearchByOrderId) r.add(sp.orderLocationSearchByOrderId)
        if (sv.orderLocationSearchByOrderExtId) r.add(sp.orderLocationSearchByOrderExtId)
        if (sv.orderLocationSearchByWarehouse) r.add(sp.orderLocationSearchByWarehouse)
        if (sv.orderLocationSearchByArea) r.add(sp.orderLocationSearchByArea)
        if (sv.orderLocationSearchByRack) r.add(sp.orderLocationSearchByRack)
        if (sv.orderLocationSearchByOnlyActive) r.add(sp.orderLocationSearchByOnlyActive)

        return r
    }

    private fun setVisibleFilters() {
        //Retrieve the values
        if (settingViewModel.orderLocationSearchByItemCode) setCodeVisibility(View.VISIBLE)
        else setCodeVisibility(View.GONE)

        if (settingViewModel.orderLocationSearchByItemDescription) setDescriptionVisibility(View.VISIBLE)
        else setDescriptionVisibility(View.GONE)

        if (settingViewModel.orderLocationSearchByItemEan) setEanVisibility(View.VISIBLE)
        else setEanVisibility(View.GONE)

        if (settingViewModel.orderLocationSearchByOrderId) setEanVisibility(View.VISIBLE)
        else setOrderIdVisibility(View.GONE)

        if (settingViewModel.orderLocationSearchByOrderExtId) setEanVisibility(View.VISIBLE)
        else setOrderExtIdVisibility(View.GONE)

        if (settingViewModel.orderLocationSearchByWarehouse) setEanVisibility(View.VISIBLE)
        else setWarehouseVisibility(View.GONE)

        if (settingViewModel.orderLocationSearchByArea) setEanVisibility(View.VISIBLE)
        else setAreaVisibility(View.GONE)

        if (settingViewModel.orderLocationSearchByRack) setEanVisibility(View.VISIBLE)
        else setRackVisibility(View.GONE)

        if (settingViewModel.orderLocationSearchByOnlyActive) setOnlyActiveVisibility(View.VISIBLE)
        else setOnlyActiveVisibility(View.GONE)
    }

    fun setCodeVisibility(visibility: Int) {
        binding.codePanel.visibility = visibility
        settingViewModel.orderLocationSearchByItemCode = visibility == View.VISIBLE
    }

    fun setDescriptionVisibility(visibility: Int) {
        binding.descriptionPanel.visibility = visibility
        settingViewModel.orderLocationSearchByItemDescription = visibility == View.VISIBLE
    }

    fun setEanVisibility(visibility: Int) {
        binding.eanPanel.visibility = visibility
        settingViewModel.orderLocationSearchByItemEan = visibility == View.VISIBLE
    }

    fun setOrderIdVisibility(visibility: Int) {
        binding.orderPanel.visibility = visibility
        settingViewModel.orderLocationSearchByOrderId = visibility == View.VISIBLE
    }

    fun setOrderExtIdVisibility(visibility: Int) {
        binding.orderExternalIdPanel.visibility = visibility
        settingViewModel.orderLocationSearchByOrderExtId = visibility == View.VISIBLE
    }

    fun setWarehouseVisibility(visibility: Int) {
        binding.warehousePanel.visibility = visibility
        settingViewModel.orderLocationSearchByWarehouse = visibility == View.VISIBLE
    }

    fun setAreaVisibility(visibility: Int) {
        binding.areaPanel.visibility = visibility
        settingViewModel.orderLocationSearchByArea = visibility == View.VISIBLE
    }

    fun setRackVisibility(visibility: Int) {
        binding.rackPanel.visibility = visibility
        settingViewModel.orderLocationSearchByRack = visibility == View.VISIBLE
    }

    fun setOnlyActiveVisibility(visibility: Int) {
        binding.onlyActiveCheckBox.visibility = visibility
        settingViewModel.orderLocationSearchByOnlyActive = visibility == View.VISIBLE
    }
    // endregion Visibility functions

    companion object {

        // The fragment initialization parameters
        const val ARG_ITEM_CODE = "itemCode"
        const val ARG_ITEM_DESCRIPTION = "itemDescription"
        const val ARG_ITEM_EAN = "itemEan"
        const val ARG_ORDER_ID = "orderId"
        const val ARG_ORDER_EXTERNAL_ID = "orderExternalId"
        const val ARG_WAREHOUSE = "warehouse"
        const val ARG_WAREHOUSE_AREA = "warehouseArea"
        const val ARG_RACK = "rack"
        const val ARG_ONLY_ACTIVE = "onlyActive"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        fun newInstance(
            itemCode: String = "",
            itemDescription: String = "",
            itemEan: String = "",
            orderId: String = "",
            orderExternalId: String = "",
            warehouse: String = "",
            warehouseArea: String = "",
            rack: String = "",
            onlyActive: Boolean = true
        ): OrderLocationFilterFragment {
            val fragment = OrderLocationFilterFragment()

            val args = Bundle()
            if (itemCode.isNotEmpty()) args.putString(ARG_ITEM_CODE, itemCode)
            if (itemDescription.isNotEmpty()) args.putString(ARG_ITEM_DESCRIPTION, itemDescription)
            if (itemEan.isNotEmpty()) args.putString(ARG_ITEM_EAN, itemEan)
            if (orderId.isNotEmpty()) args.putString(ARG_ORDER_ID, orderId)
            if (orderExternalId.isNotEmpty()) args.putString(ARG_ORDER_EXTERNAL_ID, orderId)
            if (warehouse.isNotEmpty()) args.putString(ARG_WAREHOUSE, warehouse)
            if (warehouseArea.isNotEmpty()) args.putString(ARG_WAREHOUSE_AREA, warehouseArea)
            if (rack.isNotEmpty()) args.putString(ARG_RACK, rack)
            args.putBoolean(ARG_ONLY_ACTIVE, onlyActive)

            fragment.arguments = args
            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}