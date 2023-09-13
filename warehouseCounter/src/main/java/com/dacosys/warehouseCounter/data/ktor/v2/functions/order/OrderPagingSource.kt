package com.dacosys.warehouseCounter.data.ktor.v2.functions.order

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiServiceV2
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiActionParam
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam

class OrderPagingSource private constructor(builder: Builder) : PagingSource<Int, OrderResponse>() {

    private var filter: List<ApiFilterParam> = listOf()
    private var action: List<ApiActionParam> = listOf()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, OrderResponse> {
        return try {
            val currentPage = params.key ?: 1
            val response =
                ktorApiServiceV2.getOrderResponse(
                    page = currentPage,
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

        @Suppress("unused")
        fun filter(`val`: List<ApiFilterParam>): Builder {
            filter = `val`
            return this
        }

        @Suppress("unused")
        fun action(`val`: List<ApiActionParam>): Builder {
            action = `val`
            return this
        }
    }
}
