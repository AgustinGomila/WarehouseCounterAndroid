package com.dacosys.warehouseCounter.dto.ptlOrder

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse(
    @Json(name = RESULT_KEY) val result: String,
    @Json(name = DETAILS_KEY) val details: String,
) {
    companion object {
        const val RESULT_KEY = "result"
        const val DETAILS_KEY = "details"

        const val RESULT_OK = "ok"
        const val RESULT_ERROR = "error"
        const val RESULT_FAIL = "fail"
    }
}
