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
import com.dacosys.warehouseCounter.databinding.PtlOrderHeaderBinding
import com.dacosys.warehouseCounter.dto.ptlOrder.PtlOrder
import com.dacosys.warehouseCounter.dto.warehouse.WarehouseArea
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.activities.ptlOrder.PtlOrderSelectActivity

/**
 * A simple [Fragment] subclass.
 * Use the [PtlOrderHeaderFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PtlOrderHeaderFragment : Fragment() {
    interface OrderChangedListener {
        fun onOrderChanged(ptlOrder: PtlOrder?)
    }

    private val resultForOrderSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == AppCompatActivity.RESULT_OK && data != null) {
                    val p = data.getParcelableArrayListExtra<PtlOrder>("ptlOrder")?.firstOrNull()
                    if (p != null) {
                        setOrder(p, warehouseArea)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(requireActivity(), this::class.java.simpleName, ex)
            }
        }

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        orderChangedListener = null
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
            ptlOrder = requireArguments().getParcelable("ptlOrder")
            warehouseArea = requireArguments().getParcelable("warehouseArea")
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putBoolean("showChangeOrderButton", showChangeOrderButton)
        savedInstanceState.putBoolean("showOrderPanel", showOrderPanel)
        savedInstanceState.putBoolean("showLocationPanel", showLocationPanel)

        savedInstanceState.putParcelable("ptlOrder", ptlOrder)
        savedInstanceState.putParcelable("warehouseArea", warehouseArea)
    }

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

    fun setOrder(order: PtlOrder?, location: WarehouseArea?) {
        ptlOrder = order
        warehouseArea = location

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

        // Evento de cambio de orden
        orderChangedListener?.onOrderChanged(ptlOrder)
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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = PtlOrderHeaderBinding.inflate(inflater, container, false)
        val view = binding.root

        if (savedInstanceState != null) {
            ptlOrder = savedInstanceState.getParcelable("ptlOrder")
            warehouseArea = savedInstanceState.getParcelable("warehouseArea")

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

        setOrder(ptlOrder, warehouseArea)
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

        intent.putExtra("title", getString(R.string.select_order))
        intent.putExtra("ptlOrder", ptlOrder)

        resultForOrderSelect.launch(intent)
    }

    companion object {

        fun newInstance(
            orderChangedListener: OrderChangedListener,
            ptlOrder: PtlOrder,
            warehouseArea: WarehouseArea?,
        ): PtlOrderHeaderFragment {
            val fragment = PtlOrderHeaderFragment()

            val args = Bundle()
            args.putParcelable("ptlOrder", ptlOrder)
            args.putParcelable("warehouseArea", warehouseArea)

            fragment.arguments = args
            fragment.orderChangedListener = orderChangedListener

            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}