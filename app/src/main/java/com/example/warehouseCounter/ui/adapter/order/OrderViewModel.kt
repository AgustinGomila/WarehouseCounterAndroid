package com.example.warehouseCounter.ui.adapter.order

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.example.warehouseCounter.WarehouseCounterApp.Companion.apiRequest
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.example.warehouseCounter.data.ktor.v2.functions.order.OrderPagingSource
import com.example.warehouseCounter.data.ktor.v2.functions.order.ViewOrder.Companion.defaultAction
import com.example.warehouseCounter.data.ktor.v2.impl.ApiActionParam
import com.example.warehouseCounter.data.ktor.v2.impl.ApiFilterParam
import com.example.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.ORDER_PATH
import com.example.warehouseCounter.data.ktor.v2.service.APIResponse
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
        val apiResponse = viewOrderResponse(id, defaultAction)
        val tempOrder: OrderResponse? = apiResponse.response
        if (tempOrder != null) {
            orderDetails.postValue(tempOrder!!)
        }

        loading.postValue(false)
    }

    @get:Synchronized
    private var isProcessDone = false

    @Synchronized
    private fun getProcessState(): Boolean {
        return isProcessDone
    }

    @Synchronized
    private fun setProcessState(state: Boolean) {
        isProcessDone = state
    }

    private suspend fun viewOrderResponse(
        id: Long, action: ArrayList<ApiActionParam>
    ): APIResponse<OrderResponse> {
        setProcessState(false)
        var response: APIResponse<OrderResponse> = APIResponse()

        apiRequest.view<OrderResponse>(
            objPath = ORDER_PATH,
            id = id,
            action = action,
            callback = {
                response = it
                setProcessState(true)
            }
        )

        val startTime = System.currentTimeMillis()
        while (!getProcessState()) {
            if (System.currentTimeMillis() - startTime == (settingsVm.connectionTimeout * 1000).toLong()) {
                setProcessState(true)
            }
        }

        return response
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
