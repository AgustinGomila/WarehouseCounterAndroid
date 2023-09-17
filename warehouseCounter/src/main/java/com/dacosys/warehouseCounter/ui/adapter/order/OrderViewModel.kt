package com.dacosys.warehouseCounter.ui.adapter.order

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV2
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.OrderPagingSource
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.ViewOrder.Companion.defaultAction
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiActionParam
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {
    val searchedText = MutableLiveData("")

    fun setSearchedText(searchedText: String) {
        this.searchedText.postValue(searchedText)
    }

    private fun getSearchedText(): String {
        return searchedText.value ?: ""
    }

    val actions = MutableLiveData<List<ApiActionParam>>(defaultAction)

    fun setActions(actions: ArrayList<ApiActionParam>) {
        this.actions.postValue(actions)
    }

    private fun getActions(): List<ApiActionParam> {
        return actions.value ?: listOf()
    }

    val filters = MutableLiveData<List<ApiFilterParam>>()
    fun setFilters(filters: ArrayList<ApiFilterParam>) {
        this.filters.postValue(filters)
    }

    private fun getFilters(): List<ApiFilterParam> {
        return filters.value ?: listOf()
    }

    private val loading = MutableLiveData<Boolean>()

    val orderList = Pager(PagingConfig(PAGE_SIZE)) {
        OrderPagingSource.Builder()
            .action(getActions())
            .filter(getFilters())
            .build()
    }.flow
        // .map { pagingData ->
        //     val st = searchedText.value.toString()
        //     when {
        //         st.isEmpty() -> pagingData
        //         else -> {
        //             pagingData.filter {
        //                 it.description.contains(st, true) ||
        //                         it.externalId.contains(st, true) ||
        //                         it.id.toString().contains(st, true)
        //             }
        //         }
        //     }
        // }
        .cachedIn(viewModelScope)

    private val orderDetails = MutableLiveData<OrderResponse>()

    fun loadOrderDetails(id: Long) = viewModelScope.launch {
        loading.postValue(true)
        val apiResponse = apiServiceV2.viewOrderResponse(id, defaultAction)
        val tempOrder: OrderResponse? = apiResponse.response
        if (tempOrder != null) {
            orderDetails.postValue(tempOrder!!)
        }

        loading.postValue(false)
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
