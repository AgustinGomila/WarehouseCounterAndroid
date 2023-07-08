package com.dacosys.warehouseCounter.dto.ptlOrder

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    @SerialName(RESULT_KEY) val result: String,
    @SerialName(DETAILS_KEY) val details: String,
) {
    companion object {
        const val RESULT_KEY = "result"
        const val DETAILS_KEY = "details"

        const val RESULT_OK = "ok"
        const val RESULT_ERROR = "error"
        const val RESULT_FAIL = "fail"
    }
}
