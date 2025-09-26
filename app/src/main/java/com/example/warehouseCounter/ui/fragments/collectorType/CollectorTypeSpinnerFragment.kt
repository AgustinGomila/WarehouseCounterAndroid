package com.example.warehouseCounter.ui.fragments.collectorType

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.warehouseCounter.R
import com.example.warehouseCounter.databinding.FragmentSpinnerBinding
import com.example.warehouseCounter.scanners.collector.CollectorType
import com.example.warehouseCounter.ui.utils.ParcelUtils.parcelableArrayList

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [CollectorTypeSpinnerFragment.OnItemSelectedListener] interface
 * to handle interaction events.
 * Use the [CollectorTypeSpinnerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CollectorTypeSpinnerFragment : Fragment() {
    private var allCollectorType: ArrayList<CollectorType> = arrayListOf()
        set(value) {
            field = value
            fillAdapter()
        }
    private var showGeneralLevel = true
    private var mCallback: OnItemSelectedListener? = null

    var selectedCollectorType: CollectorType?
        get() {
            val temp = binding.fragmentSpinner.selectedItem
            return if (temp != null) {
                temp as CollectorType
            } else null
        }
        set(collectorType) {
            if (collectorType == null) {
                this.requireActivity().runOnUiThread {
                    binding.fragmentSpinner.setSelection(0)
                }
                return
            }

            val adapter = binding.fragmentSpinner.adapter as ArrayAdapter<*>
            for (i in 0 until adapter.count) {
                if (equals(collectorType, adapter.getItem(i))) {
                    this.requireActivity().runOnUiThread {
                        binding.fragmentSpinner.setSelection(i)
                    }
                    break
                }
            }
        }

    val count: Int
        get() = when {
            binding.fragmentSpinner.adapter != null -> binding.fragmentSpinner.adapter.count
            else -> 0
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            allCollectorType =
                requireArguments().parcelableArrayList<CollectorType>(ARG_ALL_COLLECTOR_TYPE) ?: arrayListOf()
            showGeneralLevel = requireArguments().getBoolean(ARG_SHOW_GENERAL_LEVEL)
        }
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
        val view = binding.root

        // Llenar el binding.fragmentSpinner
        allCollectorType = ArrayList(CollectorType.getAll())

        return view
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
                    mCallback?.onItemSelected(parent.getItemAtPosition(position) as CollectorType)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    mCallback?.onItemSelected(null)
                }
            }
    }

    override fun onStart() {
        super.onStart()
        try {
            mCallback = activity as OnItemSelectedListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement OnItemSelectedListener")
        }
    }

    private fun fillAdapter() {
        allCollectorType =
            ArrayList(allCollectorType!!.sortedWith(compareBy { it.description }).reversed())
        val arrayAdapter = ArrayAdapter(
            this.requireContext(),
            R.layout.custom_spinner_dropdown_item,
            allCollectorType!!
        )
        binding.fragmentSpinner.adapter = arrayAdapter
    }

    // Container Activity must implement this interface
    interface OnItemSelectedListener {
        fun onItemSelected(collectorType: CollectorType?)
    }

    companion object {
        // The fragment initialization parameters
        private const val ARG_ALL_COLLECTOR_TYPE = "allCollectorType"
        private const val ARG_SHOW_GENERAL_LEVEL = "showGeneralLevel"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param allCollectorType Parameter 1.
         * @return A new instance of fragment collectorType_binding.fragmentSpinner.
         */
        fun newInstance(
            allCollectorType: ArrayList<CollectorType>,
            showGeneralLevel: Boolean,
        ): CollectorTypeSpinnerFragment {
            val fragment = CollectorTypeSpinnerFragment()

            val args = Bundle()
            args.putParcelableArrayList(ARG_ALL_COLLECTOR_TYPE, allCollectorType)
            args.putBoolean(ARG_SHOW_GENERAL_LEVEL, showGeneralLevel)

            fragment.arguments = args
            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}
