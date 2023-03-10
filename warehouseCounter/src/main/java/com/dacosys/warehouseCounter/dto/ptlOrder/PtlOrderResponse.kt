package com.dacosys.warehouseCounter.dto.ptlOrder

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PtlOrderResponse(
    @Json(name = ORDERS_KEY) val orders: List<PtlOrder>,
) {
    companion object {
        const val ORDERS_KEY = "orders"
    }
}