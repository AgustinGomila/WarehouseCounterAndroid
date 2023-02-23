package com.dacosys.warehouseCounter.ui.fragments.orderRequest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.databinding.OrderRequestHeaderBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.decimalPlaces
import com.dacosys.warehouseCounter.model.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.model.orderRequest.OrderRequestContent
import com.dacosys.warehouseCounter.model.orderRequest.OrderRequestType
import com.dacosys.warehouseCounter.room.dao.client.ClientCoroutines

/**
 * A simple [Fragment] subclass.
 * Use the [OrderRequestHeader.newInstance] factory method to
 * create an instance of this fragment.
 */
class OrderRequestHeader : Fragment() {
    private var orderRequest: OrderRequest? = null
    private var orcArray: ArrayList<OrderRequestContent> = ArrayList()

    // Este método es llamado cuando el fragmento se está creando.
    // En el puedes inicializar todos los componentes que deseas guardar si el fragmento fue pausado o detenido.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            orderRequest = requireArguments().getParcelable("orderRequest")
            orcArray = requireArguments().getParcelableArrayList("orcArray") ?: ArrayList()
        }
    }

    fun fill(
        orderRequest: OrderRequest?,
        orcArray: ArrayList<OrderRequestContent>,
    ) {
        this.orderRequest = orderRequest
        this.orcArray = orcArray

        fillControls()
    }

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
        val view = binding.root

        fillControls()
        return view
    }

    private fun fillControls() {
        val or = orderRequest ?: return
        val clientId = or.clientId ?: return

        ClientCoroutines().getById(clientId) {
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

        // Descripción
        when {
            or.description.isEmpty() -> {
                binding.descriptionTextView.text = getString(R.string.count_without_description)
                TooltipCompat.setTooltipText(
                    binding.descriptionTextView, getString(R.string.count_without_description)
                )
            }
            else -> {
                binding.descriptionTextView.text = or.description
                TooltipCompat.setTooltipText(
                    binding.descriptionTextView, or.description
                )
            }
        }

        // Resumen
        var resume = ""
        val breakLine = System.getProperty("line.separator")
        val totalItems = orcArray.count()

        if (orderRequest?.orderRequestedType != OrderRequestType.stockAuditFromDevice) {
            var unknownItems = 0
            var diffQtyItems = 0
            for (x in orcArray.indices) {
                if (orcArray[x].qty != null) {
                    val qty = orcArray[x].qty!!

                    if (qty.qtyRequested == 0.toDouble()) {
                        unknownItems++
                        continue
                    }

                    if (qty.qtyRequested != qty.qtyCollected) {
                        diffQtyItems++
                    }
                }
            }

            if (unknownItems > 0 || diffQtyItems > 0) {
                resume += getString(R.string.differences_exist)
                // confirmButton!!.isEnabled = or.resultAllowDiff!!
            }

            if (or.resultDiffProduct!!) {
                resume += breakLine!! + String.format(
                    getString(R.string._product_differences),
                    Statics.roundToString(unknownItems.toDouble(), decimalPlaces)
                )
            }

            if (or.resultDiffQty!!) {
                resume += breakLine!! + String.format(
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
        for (a in 0 until orcArray.count()) {
            if (orcArray[a].qty != null) {
                val qty = orcArray[a].qty!!

                totalQtyCollected += qty.qtyCollected!!
            }
        }

        resume += breakLine!! + String.format(
            getString(R.string._total_counted),
            Statics.roundToString(totalQtyCollected, decimalPlaces)
        )

        binding.resumeTextView.text = resume.trim()
        TooltipCompat.setTooltipText(binding.resumeTextView, resume.trim())
    }

    companion object {
        fun newInstance(
            orderRequest: OrderRequest?,
            orcArray: ArrayList<OrderRequestContent>,
        ): OrderRequestHeader {
            val fragment = OrderRequestHeader()

            val args = Bundle()
            args.putParcelable("orderRequest", orderRequest)
            args.putParcelableArrayList("orcArray", orcArray)

            fragment.arguments = args
            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}