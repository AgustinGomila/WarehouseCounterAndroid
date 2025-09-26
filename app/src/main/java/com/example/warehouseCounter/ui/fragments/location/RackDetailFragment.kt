package com.example.warehouseCounter.ui.fragments.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.warehouseCounter.R
import com.example.warehouseCounter.data.ktor.v2.dto.location.Rack
import com.example.warehouseCounter.databinding.RackDetailBinding
import com.example.warehouseCounter.ui.utils.ParcelUtils.parcelable


/**
 * A simple [DialogFragment] subclass.
 * Use the [RackDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RackDetailFragment : DialogFragment() {
    private var rack: Rack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            rack = requireArguments().parcelable("rack")
        }
    }

    private var _binding: RackDetailBinding? = null

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
        _binding = RackDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fillControls()
    }

    override fun onStart() {
        super.onStart()

        if (dialog != null) {
            dialog?.setTitle(getString(R.string.rack_detail))

            val params = dialog?.window?.attributes ?: return
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog?.window?.attributes = params
        }
    }

    private fun fillControls() {
        if (_binding == null) return

        val tempRack = rack ?: return

        if (tempRack.code.isEmpty()) {
            binding.codeAutoResizeTextView.text = ""
            binding.codeAutoResizeTextView.visibility = View.GONE
        } else {
            binding.codeAutoResizeTextView.text = tempRack.code
            binding.codeAutoResizeTextView.visibility = View.VISIBLE
        }

        binding.idAutoResizeTextView.text = tempRack.id.toString()
        binding.idAutoResizeTextView.visibility = View.VISIBLE

        if (tempRack.extId.isEmpty()) {
            binding.extIdAutoResizeTextView.text = ""
            binding.extIdAutoResizeTextView.visibility = View.GONE
            binding.extIdTextView.visibility = View.GONE
        } else {
            binding.extIdAutoResizeTextView.text = tempRack.extId
            binding.extIdAutoResizeTextView.visibility = View.VISIBLE
            binding.extIdTextView.visibility = View.VISIBLE
        }

        if (tempRack.warehouseArea == null) {
            binding.warehouseAreaAutoResizeTextView.text = ""
            binding.warehouseAreaAutoResizeTextView.visibility = View.GONE
            binding.warehouseAreaTextView.visibility = View.GONE
        } else {
            binding.warehouseAreaAutoResizeTextView.text = tempRack.warehouseArea?.description
            binding.warehouseAreaAutoResizeTextView.visibility = View.VISIBLE
            binding.warehouseAreaTextView.visibility = View.VISIBLE
        }

        binding.levelsAutoResizeTextView.text = tempRack.levels.toString()
        binding.levelsAutoResizeTextView.visibility = View.VISIBLE
        binding.levelsTextView.visibility = View.VISIBLE
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param rack Parameter 1.
         * @return A new instance of fragment.
         */
        fun newInstance(rack: Rack): RackDetailFragment {
            val fragment = RackDetailFragment()

            val args = Bundle()
            args.putParcelable("rack", rack)

            fragment.arguments = args
            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}