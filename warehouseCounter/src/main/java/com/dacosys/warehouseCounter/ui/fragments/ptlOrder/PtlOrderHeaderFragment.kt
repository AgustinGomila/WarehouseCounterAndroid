package com.dacosys.warehouseCounter.ui.fragments.ptlOrder

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlOrder
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.databinding.PtlOrderHeaderBinding
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.activities.ptlOrder.PtlOrderSelectActivity
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelableArrayList

/**
 * Ptl order header fragment
 */
class PtlOrderHeaderFragment private constructor(builder: Builder) : Fragment() {
    /**
     * Required constructor for Fragments
     */
    constructor() : this(Builder())

    interface OrderChangedListener {
        fun onOrderChanged(ptlOrder: PtlOrder?)
    }

    private var orderChangedListener: OrderChangedListener? = null

    var ptlOrder: PtlOrder? = null
    var warehouseArea: WarehouseArea? = null

    private var showOrderPanel: Boolean = true
    private var showChangeOrderButton: Boolean = true
    private var showLocationPanel = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            ptlOrder = requireArguments().parcelable(ARG_PTL_ORDER)
            warehouseArea = requireArguments().parcelable(ARG_WAREHOUSE_AREA)
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putBoolean("showChangeOrderButton", showChangeOrderButton)
        savedInstanceState.putBoolean("showOrderPanel", showOrderPanel)
        savedInstanceState.putBoolean("showLocationPanel", showLocationPanel)

        savedInstanceState.putParcelable(ARG_PTL_ORDER, ptlOrder)
        savedInstanceState.putParcelable(ARG_WAREHOUSE_AREA, warehouseArea)
    }

    @Suppress("unused")
    fun setChangeOrderListener(listener: OrderChangedListener) {
        orderChangedListener = listener
    }

    @Suppress("unused")
    fun showOrderPanel(show: Boolean) {
        showOrderPanel = show
        setButtonPanelVisibility()
    }

    @Suppress("unused")
    fun showChangeOrderButton(show: Boolean) {
        showChangeOrderButton = show
        setButtonPanelVisibility()
    }

    @Suppress("unused")
    fun setOrderTitle(title: String) {
        if (_binding == null) return
        binding.orderTitleTextView.text = title
    }

    /**
     * Set order
     *
     * @param order PTL Order.
     * @param location Warehouse area location.
     */
    fun setOrder(order: PtlOrder?, location: WarehouseArea?) {
        ptlOrder = order
        warehouseArea = location

        refreshViews()

        orderChangedListener?.onOrderChanged(ptlOrder)
    }

    fun refreshViews() {
        val orderNbrText: String = ptlOrder?.id?.toString() ?: ""
        val clientText: String = ptlOrder?.client?.first()?.name ?: ""
        val waDescription: String = warehouseArea?.description ?: ""

        if (_binding != null) {
            binding.orderNbrTextView.text = orderNbrText
            TooltipCompat.setTooltipText(binding.orderNbrTextView, orderNbrText)

            binding.clientTextView.text = clientText
            TooltipCompat.setTooltipText(binding.clientTextView, clientText)

            binding.locationTextView.text = waDescription
            TooltipCompat.setTooltipText(binding.locationTextView, waDescription)
        }
    }

    // region DESTINATION CONTAINER

    @Suppress("unused")
    fun showLocationPanel(show: Boolean) {
        showLocationPanel = show
        setButtonPanelVisibility()
    }

    @Suppress("unused")
    fun setLocationTitle(title: String) {
        if (_binding == null) return
        binding.locationTitleTextView.text = title
    }

    // endregion DESTINATION LOCATION

    private var _binding: PtlOrderHeaderBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Se llama cuando el fragmento ya no está asociado a la actividad anfitriona.
    override fun onDetach() {
        super.onDetach()
        orderChangedListener = null
    }

    override fun onStart() {
        super.onStart()
        if (requireActivity() is OrderChangedListener) {
            orderChangedListener = activity as OrderChangedListener
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = PtlOrderHeaderBinding.inflate(inflater, container, false)
        val view = binding.root

        if (savedInstanceState != null) {
            ptlOrder = savedInstanceState.parcelable(ARG_PTL_ORDER)
            warehouseArea = savedInstanceState.parcelable(ARG_WAREHOUSE_AREA)

            showChangeOrderButton = savedInstanceState.getBoolean("showChangeOrderButton")
            showOrderPanel = savedInstanceState.getBoolean("showOrderPanel")
            showLocationPanel = savedInstanceState.getBoolean("showLocationPanel")
        }

        setButtonPanelVisibility()

        binding.changeOrderButton.setOnClickListener { changeOrder() }
        TooltipCompat.setTooltipText(
            binding.changeOrderButton, getString(R.string.change_order)
        )

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshViews()
    }

    private fun setButtonPanelVisibility() {
        if (_binding == null) return

        binding.orderPanel.visibility = if (showOrderPanel) View.VISIBLE else View.GONE
        binding.locationPanel.visibility = if (showLocationPanel) View.VISIBLE else View.GONE

        if (showOrderPanel) {
            binding.changeOrderPanel.visibility =
                if (showChangeOrderButton) View.VISIBLE else View.GONE
        }
    }

    private fun changeOrder() {
        val intent = Intent(requireContext(), PtlOrderSelectActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra(PtlOrderSelectActivity.ARG_TITLE, getString(R.string.select_order))
        // TODO: Ver si es necesario pasar la orden seleccionada
        // intent.putExtra(PtlOrderSelectActivity.ARG_PTL_ORDER, ptlOrder)
        resultForOrderSelect.launch(intent)
    }

    private val resultForOrderSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == AppCompatActivity.RESULT_OK && data != null) {
                    val p =
                        data.parcelableArrayList<PtlOrder>(PtlOrderSelectActivity.ARG_PTL_ORDER)?.firstOrNull()
                    if (p != null) {
                        setOrder(order = p, location = warehouseArea)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(requireActivity(), this::class.java.simpleName, ex)
            }
        }

    init {
        orderChangedListener = builder.orderChangedListener
        ptlOrder = builder.ptlOrder
        warehouseArea = builder.warehouseArea
        showOrderPanel = builder.showOrderPanel
        showChangeOrderButton = builder.showChangeOrderButton
        showLocationPanel = builder.showLocationPanel
    }

    class Builder {
        fun build(): PtlOrderHeaderFragment {
            return PtlOrderHeaderFragment(this)
        }

        internal var orderChangedListener: OrderChangedListener? = null
        internal var ptlOrder: PtlOrder? = null
        internal var warehouseArea: WarehouseArea? = null
        internal var showOrderPanel: Boolean = true
        internal var showChangeOrderButton: Boolean = true
        internal var showLocationPanel = true

        @Suppress("unused")
        fun orderChangedListener(listener: OrderChangedListener): Builder {
            this.orderChangedListener = listener
            return this
        }

        @Suppress("unused")
        fun ptlOrder(ptlOrder: PtlOrder?): Builder {
            this.ptlOrder = ptlOrder
            return this
        }

        @Suppress("unused")
        fun warehouseArea(warehouseArea: WarehouseArea?): Builder {
            this.warehouseArea = warehouseArea
            return this
        }

        @Suppress("unused")
        fun showOrderPanel(show: Boolean): Builder {
            this.showOrderPanel = show
            return this
        }

        @Suppress("unused")
        fun showChangeOrderButton(show: Boolean): Builder {
            this.showChangeOrderButton = show
            return this
        }

        @Suppress("unused")
        fun showLocationPanel(show: Boolean): Builder {
            this.showLocationPanel = show
            return this
        }
    }

    companion object {
        const val ARG_PTL_ORDER = "ptlOrder"
        const val ARG_WAREHOUSE_AREA = "warehouseArea"
    }
}
