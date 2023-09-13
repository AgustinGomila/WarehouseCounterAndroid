package com.dacosys.warehouseCounter.ui.adapter.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dacosys.warehouseCounter.databinding.LoadMoreBinding

class LoadMoreAdapter(private val retry: () -> Unit, private val isLoading: (Boolean) -> Unit) :
    LoadStateAdapter<LoadMoreAdapter.ViewHolder>() {

    private lateinit var binding: LoadMoreBinding

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ViewHolder {
        binding = LoadMoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(retry)
    }

    override fun onBindViewHolder(holder: ViewHolder, loadState: LoadState) {
        holder.setData(loadState)
    }

    inner class ViewHolder(retry: () -> Unit) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.retryButton.setOnClickListener { retry() }
        }

        fun setData(state: LoadState) {
            binding.apply {
                isLoading(state is LoadState.Loading)
                loadMoteTextView.isVisible = state is LoadState.Error
                retryButton.isVisible = state is LoadState.Error
            }
        }
    }
}
