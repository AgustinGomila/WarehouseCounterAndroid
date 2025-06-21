package com.dacosys.warehouseCounter.data.ktor.v2.dto.apiParam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Meta(
    @SerialName(TOTAL_COUNT_KEY) val totalCount: Int = 0,
    @SerialName(PAGE_COUNT_KEY) val pageCount: Int = 0,
    @SerialName(CURRENT_PAGE_KEY) val currentPage: Int = 0,
    @SerialName(PER_PAGE_KEY) val perPage: Int = 0
) {
    companion object {
        const val TOTAL_COUNT_KEY = "totalCount"
        const val PAGE_COUNT_KEY = "pageCount"
        const val CURRENT_PAGE_KEY = "currentPage"
        const val PER_PAGE_KEY = "perPage"
    }
}
