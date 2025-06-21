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
 */
class SearchTextFragment private constructor(builder: Builder) : Fragment() {

    /**
     * Required constructor for Fragments
     */
    constructor() : this(Builder())

    var searchText: String
    private var textChangedCallback: OnSearchTextChangedListener?
    private var focusChangedCallback: OnSearchTextFocusChangedListener?

    interface OnSearchTextChangedListener {
        fun onSearchTextChanged(
            searchText: String
        )
    }

    interface OnSearchTextFocusChangedListener {
        fun onSearchTextFocusChange(
            hasFocus: Boolean
        )
    }

    private fun onTextChanged(searchText: String) {
        textChangedCallback?.onSearchTextChanged(searchText)
    }

    private fun onFocusChanged(hasFocus: Boolean) {
        focusChangedCallback?.onSearchTextFocusChange(hasFocus)
    }

    private var _binding: SearchTextFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Se llama cuando el fragmento ya no está asociado a la actividad anfitriona.
    override fun onDetach() {
        super.onDetach()
        textChangedCallback = null
        focusChangedCallback = null
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString(ARG_SEARCH_TEXT, searchText)
    }

    private fun loadSavedValues(b: Bundle) {
        searchText = b.getString(ARG_SEARCH_TEXT) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SearchTextFragmentBinding.inflate(inflater, container, false)
        val view = binding.root

        if (savedInstanceState != null) {
            loadSavedValues(savedInstanceState)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setPanel()
        refreshViews()
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
            onTextChanged(searchText)
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

    fun refreshViews() {
        binding.searchEditText.removeTextChangedListener(customWatcher)
        binding.searchEditText.setText(searchText, TextView.BufferType.EDITABLE)
        binding.searchEditText.addTextChangedListener(customWatcher)
    }

    init {
        searchText = builder.searchText
        this.textChangedCallback = builder.textChangedCallback
        this.focusChangedCallback = builder.focusChangedCallback
    }

    class Builder {
        fun build(): SearchTextFragment {
            return SearchTextFragment(this)
        }

        internal var searchText: String = ""
        internal var textChangedCallback: OnSearchTextChangedListener? = null
        internal var focusChangedCallback: OnSearchTextFocusChangedListener? = null

        fun setSearchText(searchText: String): Builder {
            this.searchText = searchText
            return this
        }

        fun searchTextChangedCallback(callback: OnSearchTextChangedListener?): Builder {
            this.textChangedCallback = callback
            return this
        }

        fun focusChangedCallback(callback: OnSearchTextFocusChangedListener?): Builder {
            this.focusChangedCallback = callback
            return this
        }
    }

    companion object {
        const val ARG_SEARCH_TEXT = "searchText"
    }
}
