package com.dacosys.warehouseCounter.dto.ptlOrder

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PtlContentResponse(
    @Json(name = RESULT_KEY) val result: String,
    @Json(name = DETAILS_KEY) val details: String,
    @Json(name = CONTENTS_KEY) val contents: List<PtlContent>,
) {
    companion object {
        const val RESULT_KEY = "result"
        const val DETAILS_KEY = "details"
        const val CONTENTS_KEY = "contents"
    }
}