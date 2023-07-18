package com.dacosys.warehouseCounter.ui.fragments.orderRequest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.databinding.OrderRequestHeaderBinding
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.decimalPlaces
import com.dacosys.warehouseCounter.misc.Statics.Companion.lineSeparator
import com.dacosys.warehouseCounter.room.dao.client.ClientCoroutines
import com.dacosys.warehouseCounter.room.dao.orderRequest.OrderRequestCoroutines

/**
 * A simple [Fragment] subclass.
 * Use the [OrderRequestHeader.newInstance] factory method to
 * create an instance of this fragment.
 */
class OrderRequestHeader : Fragment() {
    private lateinit var orderRequest: OrderRequest

    private var _binding: OrderRequestHeaderBinding? = null

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
        _binding = OrderRequestHeaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (arguments == null) return

        val id = requireArguments().getLong(ARG_ID)
        OrderRequestCoroutines.getOrderRequestById(
            id = id,
            onResult = {
                if (it != null) {
                    orderRequest = it

                    fillClientData()
                    fillOrderData()
                }
            }
        )
    }

    private fun fillClientData() {
        val clientId = orderRequest.clientId ?: return
        ClientCoroutines.getById(clientId) {
            when {
                it != null -> {
                    binding.clientTextView.text = it.name
                    TooltipCompat.setTooltipText(binding.clientTextView, it.name)
                }

                else -> {
                    binding.clientTextView.text = getString(R.string.unknown_client)
                    TooltipCompat.setTooltipText(
                        binding.clientTextView, getString(R.string.unknown_client)
                    )
                }
            }
        }
    }

    private fun fillOrderData() {
        // Descripción
        when {
            orderRequest.description.isEmpty() -> {
                binding.descriptionTextView.text = getString(R.string.count_without_description)
                TooltipCompat.setTooltipText(
                    binding.descriptionTextView, getString(R.string.count_without_description)
                )
            }

            else -> {
                binding.descriptionTextView.text = orderRequest.description
                TooltipCompat.setTooltipText(binding.descriptionTextView, orderRequest.description)
            }
        }

        // Resumen
        var resume = ""
        val breakLine = lineSeparator
        val totalItems = orderRequest.contents.count()

        if (orderRequest.orderRequestType != OrderRequestType.stockAuditFromDevice) {
            var unknownItems = 0
            var diffQtyItems = 0
            for (orc in orderRequest.contents) {
                if (orc.qtyRequested == 0.toDouble()) {
                    unknownItems++
                    continue
                }

                if (orc.qtyRequested != orc.qtyCollected) {
                    diffQtyItems++
                }
            }

            if (unknownItems > 0 || diffQtyItems > 0) {
                resume += getString(R.string.differences_exist)
            }

            if (orderRequest.resultDiffProduct!!) {
                resume += breakLine + String.format(
                    getString(R.string._product_differences),
                    Statics.roundToString(unknownItems.toDouble(), decimalPlaces)
                )
            }

            if (orderRequest.resultDiffQty!!) {
                resume += breakLine + String.format(
                    getString(R.string._quantity_differences),
                    Statics.roundToString(diffQtyItems.toDouble(), decimalPlaces)
                )
                resume += breakLine + String.format(
                    getString(R.string._item_differences),
                    Statics.roundToString(totalItems.toDouble(), decimalPlaces)
                )
            }
        }

        var totalQtyCollected = 0.0
        for (a in 0 until orderRequest.contents.count()) {
            val orc = orderRequest.contents[a]
            totalQtyCollected += orc.qtyCollected!!
        }

        resume += breakLine + String.format(
            getString(R.string._total_counted),
            Statics.roundToString(totalQtyCollected, decimalPlaces)
        )

        binding.resumeTextView.text = resume.trim()
        TooltipCompat.setTooltipText(binding.resumeTextView, resume.trim())
    }

    companion object {
        const val ARG_ID = "id"

        fun newInstance(id: Long): OrderRequestHeader {
            val fragment = OrderRequestHeader()

            val args = Bundle()
            args.putLong(ARG_ID, id)
            fragment.arguments = args

            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}