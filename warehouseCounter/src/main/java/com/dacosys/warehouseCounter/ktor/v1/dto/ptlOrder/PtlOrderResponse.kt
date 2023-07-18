package com.dacosys.warehouseCounter.ktor.v1.dto.ptlOrder

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PtlOrderResponse(
    @SerialName(ORDERS_KEY) val orders: List<PtlOrder>,
) {
    companion object {
        const val ORDERS_KEY = "orders"
    }
}

