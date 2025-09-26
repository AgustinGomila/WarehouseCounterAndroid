package com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PtlContentResponse(
    @SerialName(RESULT_KEY) val result: String,
    @SerialName(DETAILS_KEY) val details: String,
    @SerialName(CONTENTS_KEY) val contents: List<PtlContent>,
) {
    companion object {
        const val RESULT_KEY = "result"
        const val DETAILS_KEY = "details"
        const val CONTENTS_KEY = "contents"
    }
}
