package com.dacosys.warehouseCounter.ui.fragments.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.databinding.SummaryFragmentBinding

/**
 * Summary fragment
 */
class SummaryFragment : Fragment() {
    private var first: Int = 0
    private var second: Int = 0
    private var third: Int = 0
    private var firstLabel: String = ""
    private var secondLabel: String = ""
    private var thirdLabel: String = ""

    private var _binding: SummaryFragmentBinding? = null

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
        _binding = SummaryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments != null) {
            firstLabel = requireArguments().getString(ARG_FIRST_LABEL) ?: ""
            secondLabel = requireArguments().getString(ARG_SECOND_LABEL) ?: ""
            thirdLabel = requireArguments().getString(ARG_THIRD_LABEL) ?: ""
            first = requireArguments().getInt(ARG_FIRST)
            second = requireArguments().getInt(ARG_SECOND)
            third = requireArguments().getInt(ARG_THIRD)
        }

        fill()
    }

    fun firstLabel(`val`: String): SummaryFragment {
        firstLabel = `val`
        return this
    }

    fun secondLabel(`val`: String): SummaryFragment {
        secondLabel = `val`
        return this
    }

    fun thirdLabel(`val`: String): SummaryFragment {
        thirdLabel = `val`
        return this
    }

    fun first(`val`: Int): SummaryFragment {
        first = `val`
        return this
    }

    fun second(`val`: Int): SummaryFragment {
        second = `val`
        return this
    }

    fun third(`val`: Int): SummaryFragment {
        third = `val`
        return this
    }

    fun fill() {
        if (firstLabel.isEmpty()) {
            binding.firstLabelTextView.visibility = View.GONE
            binding.firstTextView.visibility = View.GONE
        } else {
            binding.firstLabelTextView.visibility = View.VISIBLE
            binding.firstTextView.visibility = View.VISIBLE
            binding.firstLabelTextView.text = firstLabel
            binding.firstTextView.text = first.toString()
        }

        if (secondLabel.isEmpty()) {
            binding.secondLabelTextView.visibility = View.GONE
            binding.secondTextView.visibility = View.GONE
        } else {
            binding.secondLabelTextView.visibility = View.VISIBLE
            binding.secondTextView.visibility = View.VISIBLE
            binding.secondLabelTextView.text = secondLabel
            binding.secondTextView.text = second.toString()
        }

        if (thirdLabel.isEmpty()) {
            binding.thirdLabelTextView.visibility = View.GONE
            binding.thirdTextView.visibility = View.GONE
        } else {
            binding.thirdLabelTextView.visibility = View.VISIBLE
            binding.thirdTextView.visibility = View.VISIBLE
            binding.thirdLabelTextView.text = thirdLabel
            binding.thirdTextView.text = third.toString()
        }
    }

    companion object {
        const val ARG_FIRST = "first"
        const val ARG_SECOND = "second"
        const val ARG_THIRD = "third"
        const val ARG_FIRST_LABEL = "firstLabel"
        const val ARG_SECOND_LABEL = "secondLabel"
        const val ARG_THIRD_LABEL = "thirdLabel"
    }
}
