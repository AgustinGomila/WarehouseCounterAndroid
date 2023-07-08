package com.dacosys.warehouseCounter.dto.ptlOrder

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class LabelResponse(
    @SerialName(RESULT_KEY) val result: String,
    @SerialName(DETAILS_KEY) val details: String,
    @SerialName(LABELS_KEY) val labels: List<Label>,
) {
    companion object {
        const val RESULT_KEY = "result"
        const val DETAILS_KEY = "details"
        const val LABELS_KEY = "labels"
    }
}

