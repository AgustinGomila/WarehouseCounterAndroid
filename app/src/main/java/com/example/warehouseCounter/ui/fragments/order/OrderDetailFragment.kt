package com.example.warehouseCounter.ui.fragments.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.example.warehouseCounter.databinding.OrderDetailBinding
import com.example.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.example.warehouseCounter.ui.snackBar.SnackBarType
import com.example.warehouseCounter.ui.utils.ParcelUtils.parcelable

/**
 * A simple [Fragment] subclass.
 * Use the [OrderDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OrderDetailFragment : Fragment() {
    private var order: OrderResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            order = requireArguments().parcelable(ARG_ORDER)
        }
    }

    private var _binding: OrderDetailBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(ARG_ORDER, order)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = OrderDetailBinding.inflate(inflater, container, false)
        val view = binding.root

        if (savedInstanceState != null) {
            order = savedInstanceState.parcelable(ARG_ORDER)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fillControls()
    }

    private fun fillControls() {
        val it = this.order ?: return

        if (it.description.isEmpty()) {
            binding.descriptionAutoResizeTextView.text = ""
            binding.descriptionAutoResizeTextView.visibility = GONE
        } else {
            binding.descriptionAutoResizeTextView.text = it.description
            binding.descriptionAutoResizeTextView.visibility = VISIBLE
        }

        if (it.clientId == null) {
            binding.clientAutoResizeTextView.text = ""
            binding.clientAutoResizeTextView.visibility = GONE
        } else {
            binding.clientAutoResizeTextView.text = it.clientId?.toString()
            binding.clientAutoResizeTextView.visibility = VISIBLE
        }

        binding.zoneAutoResizeTextView.text = it.zone
        binding.zoneAutoResizeTextView.visibility = VISIBLE

        binding.idAutoResizeTextView.text = it.id.toString()
        binding.idAutoResizeTextView.visibility = VISIBLE

        if (it.externalId.isEmpty()) {
            binding.extIdAutoResizeTextView.text = ""
            binding.extIdAutoResizeTextView.visibility = GONE
            binding.extIdTextView.visibility = GONE
        } else {
            binding.extIdAutoResizeTextView.text = it.externalId
            binding.extIdAutoResizeTextView.visibility = VISIBLE
            binding.extIdTextView.visibility = VISIBLE
        }

        binding.orderTypeAutoResizeTextView.text = it.orderType.toString()
        binding.orderTypeAutoResizeTextView.visibility = VISIBLE
        binding.orderTypeTextView.visibility = VISIBLE

        binding.statusAutoResizeTextView.text = it.status.description
        binding.statusAutoResizeTextView.visibility = VISIBLE
        binding.statusTextView.visibility = VISIBLE

        val startDate = it.startDate
        val finishDate = it.finishDate
        val receivedDate = it.receivedDate
        val processedDate = it.processedDate

        if (startDate != null || finishDate != null || receivedDate != null || processedDate != null) {
            binding.datesConstraintLayout.visibility = VISIBLE

            if (startDate != null) {
                binding.startDateAutoResizeTextView.text = startDate
                binding.startDateAutoResizeTextView.visibility = VISIBLE
                binding.startDateTextView.visibility = VISIBLE
            } else {
                binding.startDateAutoResizeTextView.visibility = GONE
                binding.startDateTextView.visibility = GONE
            }

            if (finishDate != null) {
                binding.finishDateAutoResizeTextView.text = finishDate
                binding.finishDateAutoResizeTextView.visibility = VISIBLE
                binding.finishDateTextView.visibility = VISIBLE
            } else {
                binding.finishDateAutoResizeTextView.visibility = GONE
                binding.finishDateTextView.visibility = GONE
            }

            if (receivedDate != null) {
                binding.receivedDateAutoResizeTextView.text = receivedDate
                binding.receivedDateAutoResizeTextView.visibility = VISIBLE
                binding.receivedDateTextView.visibility = VISIBLE
            } else {
                binding.receivedDateAutoResizeTextView.visibility = GONE
                binding.receivedDateTextView.visibility = GONE
            }

            if (processedDate != null) {
                binding.processedDateAutoResizeTextView.text = processedDate
                binding.processedDateAutoResizeTextView.visibility = VISIBLE
                binding.processedDateTextView.visibility = VISIBLE
            } else {
                binding.processedDateAutoResizeTextView.visibility = GONE
                binding.processedDateTextView.visibility = GONE
            }
        } else {
            binding.datesConstraintLayout.visibility = GONE
        }
    }

    private fun showMessage(text: String, snackBarType: SnackBarType) {
        if (_binding == null) return
        makeText(binding.root, text, snackBarType)
    }

    companion object {
        const val ARG_ORDER = "order"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param item Parameter 1.
         * @return A new instance of fragment strOption_spinner.
         */
        fun newInstance(item: OrderResponse): OrderDetailFragment {
            val fragment = OrderDetailFragment()

            val args = Bundle()
            args.putParcelable(ARG_ORDER, item)

            fragment.arguments = args
            return fragment
        }
    }
}
