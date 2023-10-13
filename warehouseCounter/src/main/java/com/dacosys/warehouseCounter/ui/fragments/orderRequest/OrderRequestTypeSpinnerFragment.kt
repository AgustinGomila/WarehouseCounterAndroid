package com.dacosys.warehouseCounter.ui.fragments.orderRequest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.databinding.FragmentSpinnerBinding

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [OrderRequestTypeSpinnerFragment.OnItemSelectedListener] interface
 * to handle interaction events.
 */
class OrderRequestTypeSpinnerFragment private constructor(builder: Builder) : Fragment() {

    /**
     * Required constructor for Fragments
     */
    constructor() : this(Builder())

    private var orderRequestType: OrderRequestType?
    private var allOrderRequestType: ArrayList<OrderRequestType>
    private var showGeneralLevel: Boolean
    private var callback: OnItemSelectedListener?

    @Suppress("MemberVisibilityCanBePrivate")
    var selectedOrderRequestType: OrderRequestType?
        get() {
            val temp = binding.fragmentSpinner.selectedItem
            return if (temp != null) {
                temp as OrderRequestType
            } else null
        }
        set(orderRequestType) {
            if (orderRequestType == null) {
                this.requireActivity().runOnUiThread {
                    binding.fragmentSpinner.setSelection(0)
                }
                return
            }

            val adapter = binding.fragmentSpinner.adapter as ArrayAdapter<*>
            for (i in 0 until adapter.count) {
                if (equals(orderRequestType, adapter.getItem(i))) {
                    this.requireActivity().runOnUiThread {
                        binding.fragmentSpinner.setSelection(i)
                    }
                    break
                }
            }
        }

    private fun equals(a: Any?, b: Any?): Boolean {
        return a != null && a == b
    }

    val count: Int
        get() = when {
            binding.fragmentSpinner.adapter != null -> binding.fragmentSpinner.adapter.count
            else -> 0
        }

    private var _binding: FragmentSpinnerBinding? = null

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
        _binding = FragmentSpinnerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    orderRequestType = parent.getItemAtPosition(position) as OrderRequestType
                    callback?.onItemSelected(orderRequestType)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    callback?.onItemSelected(null)
                }
            }
    }

    // Se llama cuando el fragmento está visible ante el usuario.
    // Obviamente, depende del método onStart() de la actividad para saber si la actividad se está mostrando.
    override fun onStart() {
        super.onStart()
        fillAdapter()
    }

    private fun fillAdapter() {
        val arrayAdapter = ArrayAdapter(
            this.requireContext(),
            R.layout.custom_spinner_dropdown_item,
            allOrderRequestType
        )
        binding.fragmentSpinner.adapter = arrayAdapter
        if (orderRequestType != null)
            selectedOrderRequestType = orderRequestType
    }

    // Container Activity must implement this interface
    interface OnItemSelectedListener {
        fun onItemSelected(orderRequestType: OrderRequestType?)
    }

    init {
        orderRequestType = builder.orderRequestType
        allOrderRequestType = ArrayList(builder.allOrderRequestType.sortedWith(compareBy { it.description }))
        showGeneralLevel = builder.showGeneralLevel
        callback = builder.callback
    }

    class Builder {
        fun build(): OrderRequestTypeSpinnerFragment {
            return OrderRequestTypeSpinnerFragment(this)
        }

        internal var orderRequestType: OrderRequestType = OrderRequestType.stockAuditFromDevice
        internal var allOrderRequestType: ArrayList<OrderRequestType> = OrderRequestType.getAll()
        internal var showGeneralLevel = true
        internal var callback: OnItemSelectedListener? = null

        // Setter methods for variables with chained methods
        @Suppress("unused")
        fun orderRequestType(`val`: OrderRequestType): Builder {
            orderRequestType = `val`
            return this
        }

        @Suppress("unused")
        fun allOrderRequestType(`val`: ArrayList<OrderRequestType>): Builder {
            allOrderRequestType = `val`
            return this
        }

        @Suppress("unused")
        fun showGeneralLevel(`val`: Boolean): Builder {
            showGeneralLevel = `val`
            return this
        }

        @Suppress("unused")
        fun callback(`val`: OnItemSelectedListener): Builder {
            callback = `val`
            return this
        }
    }
}
