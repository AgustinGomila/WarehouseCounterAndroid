package com.example.warehouseCounter.data.ktor.v1.dto.warehouse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Warehouse(
    @SerialName(AREAS_KEY) var areas: List<WarehouseArea>,
) {
    companion object {
        const val AREAS_KEY = "warehouse_areas"
    }
}
