package com.dacosys.warehouseCounter.dto.ptlOrder

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class LabelResponse(
    @Json(name = RESULT_KEY) val result: String,
    @Json(name = DETAILS_KEY) val details: String,
    @Json(name = LABELS_KEY) val labels: List<Label>,
) {
    companion object {
        const val RESULT_KEY = "result"
        const val DETAILS_KEY = "details"
        const val LABELS_KEY = "labels"
    }
}

