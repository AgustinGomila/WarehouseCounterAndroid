package com.dacosys.warehouseCounter.ui.fragments.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.databinding.WarehouseAreaDetailBinding
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable


/**
 * A simple [DialogFragment] subclass.
 * Use the [WarehouseAreaDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WarehouseAreaDetailFragment : DialogFragment() {
    private var warehouseArea: WarehouseArea? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            warehouseArea = requireArguments().parcelable("warehouseArea")
        }
    }

    private var _binding: WarehouseAreaDetailBinding? = null

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
        _binding = WarehouseAreaDetailBinding.inflate(inflater, container, false)
        val view = binding.root

        fillControls()
        return view
    }

    override fun onStart() {
        super.onStart()

        if (dialog != null) {
            dialog?.setTitle(getString(R.string.warehouse_area_detail))

            val params = dialog?.window?.attributes ?: return
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog?.window?.attributes = params
        }
    }

    private fun fillControls() {
        val tempWa = warehouseArea ?: return

        if (tempWa.description.isEmpty()) {
            binding.descriptionAutoResizeTextView.text = ""
            binding.descriptionAutoResizeTextView.visibility = View.GONE
        } else {
            binding.descriptionAutoResizeTextView.text = tempWa.description
            binding.descriptionAutoResizeTextView.visibility = View.VISIBLE
        }

        binding.idAutoResizeTextView.text = tempWa.id.toString()
        binding.idAutoResizeTextView.visibility = View.VISIBLE

        if (tempWa.externalId.isEmpty()) {
            binding.extIdAutoResizeTextView.text = ""
            binding.extIdAutoResizeTextView.visibility = View.GONE
            binding.extIdTextView.visibility = View.GONE
        } else {
            binding.extIdAutoResizeTextView.text = tempWa.externalId
            binding.extIdAutoResizeTextView.visibility = View.VISIBLE
            binding.extIdTextView.visibility = View.VISIBLE
        }

        if (tempWa.warehouse == null) {
            binding.warehouseAutoResizeTextView.text = ""
            binding.warehouseAutoResizeTextView.visibility = View.GONE
            binding.warehouseTextView.visibility = View.GONE
        } else {
            binding.warehouseAutoResizeTextView.text = tempWa.warehouse?.description
            binding.warehouseAutoResizeTextView.visibility = View.VISIBLE
            binding.warehouseTextView.visibility = View.VISIBLE
        }

        if (tempWa.acronym.isEmpty()) {
            binding.acronymAutoResizeTextView.text = ""
            binding.acronymAutoResizeTextView.visibility = View.GONE
        } else {
            binding.acronymAutoResizeTextView.text = tempWa.acronym.toString()
            binding.acronymAutoResizeTextView.visibility = View.VISIBLE
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param warehouseArea Parameter 1.
         * @return A new instance of fragment.
         */
        fun newInstance(warehouseArea: WarehouseArea): WarehouseAreaDetailFragment {
            val fragment = WarehouseAreaDetailFragment()

            val args = Bundle()
            args.putParcelable("warehouseArea", warehouseArea)

            fragment.arguments = args
            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}