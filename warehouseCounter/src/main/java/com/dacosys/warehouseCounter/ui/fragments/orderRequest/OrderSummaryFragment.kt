package com.dacosys.warehouseCounter.ui.fragments.orderRequest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.databinding.OrderSummaryFragmentBinding

/**
 * A simple [Fragment] subclass.
 * Use the [OrderSummaryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OrderSummaryFragment : Fragment() {
    private var multiSelect: Boolean = false
    private var totalVisible: Int = 0
    private var totalRequired: Int = 0
    private var totalChecked: Int = 0

    private var _binding: OrderSummaryFragmentBinding? = null

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
        _binding = OrderSummaryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (arguments != null) {
            multiSelect = requireArguments().getBoolean(ARG_MULTISELECT)
            totalVisible = requireArguments().getInt(ARG_TOTAL_VISIBLE)
            totalChecked = requireArguments().getInt(ARG_TOTAL_CHECKED)
            totalRequired = requireArguments().getInt(ARG_TOTAL_REQUIRED)
        }

        fill()
    }

    @Suppress("unused")
    fun multiSelect(`val`: Boolean): OrderSummaryFragment {
        multiSelect = `val`
        return this
    }

    @Suppress("unused")
    fun totalVisible(`val`: Int): OrderSummaryFragment {
        totalVisible = `val`
        return this
    }

    @Suppress("unused")
    fun totalChecked(`val`: Int): OrderSummaryFragment {
        totalChecked = `val`
        return this
    }

    @Suppress("unused")
    fun totalRequired(`val`: Int): OrderSummaryFragment {
        totalRequired = `val`
        return this
    }

    fun fill() {
        if (multiSelect) {
            binding.totalLabelTextView.text = getString(R.string.total)
            binding.qtyReqLabelTextView.text = getString(R.string.cant)
            binding.selectedLabelTextView.text = getString(R.string.checked)

            binding.totalTextView.text = totalVisible.toString()
            binding.qtyReqTextView.text = totalRequired.toString()
            binding.selectedTextView.text = totalChecked.toString()
        } else {
            binding.totalLabelTextView.text = getString(R.string.total)
            binding.qtyReqLabelTextView.text = getString(R.string.cont_)
            binding.selectedLabelTextView.text = getString(R.string.items)

            binding.totalTextView.text = totalVisible.toString()
            binding.qtyReqTextView.text = totalRequired.toString()
            binding.selectedTextView.text = (totalVisible - totalRequired).toString()
        }
    }

    companion object {
        const val ARG_MULTISELECT = "multiSelect"
        const val ARG_TOTAL_VISIBLE = "totalVisible"
        const val ARG_TOTAL_REQUIRED = "totalRequired"
        const val ARG_TOTAL_CHECKED = "totalChecked"

        fun newInstance(
            multiSelect: Boolean,
            totalVisible: Int,
            totalChecked: Int,
            totalRequired: Int
        ): OrderSummaryFragment {
            val fragment = OrderSummaryFragment()

            val args = Bundle()
            args.putBoolean(ARG_MULTISELECT, multiSelect)
            args.putInt(ARG_TOTAL_VISIBLE, totalVisible)
            args.putInt(ARG_TOTAL_REQUIRED, totalRequired)
            args.putInt(ARG_TOTAL_CHECKED, totalChecked)
            fragment.arguments = args

            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}