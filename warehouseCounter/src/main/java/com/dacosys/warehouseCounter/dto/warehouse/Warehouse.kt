package com.dacosys.warehouseCounter.dto.warehouse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Warehouse(
    @SerialName(areasTag) var areas: List<WarehouseArea>,
) {
    companion object {
        const val areasTag = "warehouse_areas"
    }
}