package com.example.warehouseCounter.data.ktor.v2.functions.order

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.apiRequest
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.data.ktor.v2.dto.apiParam.ListResponse
import com.example.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.example.warehouseCounter.data.ktor.v2.impl.ApiActionParam
import com.example.warehouseCounter.data.ktor.v2.impl.ApiFilterParam
import com.example.warehouseCounter.data.ktor.v2.impl.ApiPaginationParam
import com.example.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.ORDER_PATH
import com.example.warehouseCounter.data.ktor.v2.service.APIResponse

class OrderPagingSource private constructor(builder: Builder) : PagingSource<Int, OrderResponse>() {

    private var filter: List<ApiFilterParam>
    private var action: List<ApiActionParam>

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, OrderResponse> {
        return try {
            val currentPage = params.key ?: 1
            val response =
                getOrderResponse(
                    pagination = ApiPaginationParam(currentPage, settingsVm.defaultPageSize),
                    filter = ArrayList(filter),
                    action = ArrayList(action)
                )

            if (response.onEvent != null) {
                LoadResult.Error(ErrorApiResponseException(response.onEvent.text))
            } else if (response.response != null) {

                val data = response.response.items

                if (data.isEmpty()) {
                    return LoadResult.Error(EndOfPaginationException(context.getString(R.string.end_of_pagination)))
                } else {
                    val responseData = mutableListOf<OrderResponse>()
                    responseData.addAll(data)

                    LoadResult.Page(
                        data = responseData,
                        prevKey = if (currentPage == 1) null else -1,
                        nextKey = currentPage.plus(1)
                    )
                }
            } else {
                LoadResult.Error(InvalidApiResponseException(context.getString(R.string.invalid_response)))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, OrderResponse>): Int? {
        return null
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

    /**
     * Get a list of [OrderResponse]
     *
     * @param pagination Pagination
     * @param filter List of filters
     * @param action List of parameters
     * @return [APIResponse] of [ListResponse]<[OrderResponse]>
     */
    private suspend fun getOrderResponse(
        pagination: ApiPaginationParam,
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>
    ): APIResponse<ListResponse<OrderResponse>> {
        setProcessState(false)
        var response: APIResponse<ListResponse<OrderResponse>> = APIResponse()

        apiRequest.getListOf<OrderResponse>(
            objPath = ORDER_PATH,
            listName = OrderResponse.ORDER_RESPONSE_LIST_KEY,
            action = action,
            filter = filter,
            pagination = pagination,
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

    class InvalidApiResponseException(message: String) : Exception(message)
    class EndOfPaginationException(message: String) : Exception(message)
    class ErrorApiResponseException(message: String) : Exception(message)

    init {
        filter = builder.filter
        action = builder.action
    }

    class Builder {
        fun build(): OrderPagingSource {
            return OrderPagingSource(this)
        }

        internal var filter: List<ApiFilterParam> = listOf()
        internal var action: List<ApiActionParam> = listOf()

        fun filter(`val`: List<ApiFilterParam>): Builder {
            filter = `val`
            return this
        }

        fun action(`val`: List<ApiActionParam>): Builder {
            action = `val`
            return this
        }
    }
}
