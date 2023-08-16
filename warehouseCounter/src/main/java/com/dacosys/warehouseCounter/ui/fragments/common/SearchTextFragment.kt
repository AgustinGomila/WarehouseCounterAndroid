package com.dacosys.warehouseCounter.ui.fragments.common

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.databinding.SearchTextFragmentBinding

/**
 * A simple [Fragment] subclass.
 * Use the [SearchTextFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchTextFragment : Fragment() {
    private var searchText: String = ""

    private var mSearchTextChangedCallback: OnSearchTextChangedListener? = null
    private var mFocusChangedCallback: OnSearchTextFocusChangedListener? = null

    fun searchTextChangedCallback(`val`: OnSearchTextChangedListener): SearchTextFragment {
        mSearchTextChangedCallback = `val`
        return this
    }

    fun focusChangedCallback(`val`: OnSearchTextFocusChangedListener): SearchTextFragment {
        mFocusChangedCallback = `val`
        return this
    }

    fun searchText(`val`: String): SearchTextFragment {
        searchText = `val`
        setText()
        return this
    }

    interface OnSearchTextChangedListener {
        fun onSearchTextChanged(
            searchText: String
        )
    }

    interface OnSearchTextFocusChangedListener {
        fun onFocusChange(
            hasFocus: Boolean
        )
    }

    private fun onSearchTextChanged(searchText: String) {
        mSearchTextChangedCallback?.onSearchTextChanged(searchText)
    }

    private fun onFocusChanged(hasFocus: Boolean) {
        mFocusChangedCallback?.onFocusChange(hasFocus)
    }

    private var _binding: SearchTextFragmentBinding? = null

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
        _binding = SearchTextFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (arguments != null) {
            searchText = requireArguments().getString(ARG_SEARCH_TEXT) ?: ""
        }

        setText()
        setPanel()
    }

    internal class CustomTextWatcher(private val onTextChangedCallback: (String) -> Unit) : TextWatcher {
        override fun afterTextChanged(s: Editable) {}

        override fun beforeTextChanged(
            s: CharSequence, start: Int,
            count: Int, after: Int,
        ) {
        }

        override fun onTextChanged(
            s: CharSequence, start: Int,
            before: Int, count: Int,
        ) {
            onTextChangedCallback.invoke(s.toString())
        }
    }

    private val customWatcher: CustomTextWatcher by lazy {
        CustomTextWatcher { searchText ->
            onSearchTextChanged(searchText)
        }
    }

    private fun setPanel() {
        binding.searchEditText.setOnFocusChangeListener { _, hasFocus ->
            onFocusChanged(hasFocus)
        }
        binding.searchEditText.addTextChangedListener(customWatcher)
        binding.searchEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)

        binding.searchTextImageView.setOnClickListener { binding.searchEditText.requestFocus() }
        binding.searchTextClearImageView.setOnClickListener { binding.searchEditText.setText("") }
    }

    private fun setText() {
        binding.searchEditText.removeTextChangedListener(customWatcher)
        binding.searchEditText.setText(searchText, TextView.BufferType.EDITABLE)
        binding.searchEditText.addTextChangedListener(customWatcher)
    }

    companion object {
        const val ARG_SEARCH_TEXT = "searchText"

        fun newInstance(searchText: String): SearchTextFragment {
            val fragment = SearchTextFragment()

            val args = Bundle()
            args.putString(ARG_SEARCH_TEXT, searchText)
            fragment.arguments = args

            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}
