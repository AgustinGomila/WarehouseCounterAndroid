package com.dacosys.warehouseCounter.data.ktor.v2.dto.apiParam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    @SerialName(NAME_KEY) val name: String,
    @SerialName(MESSAGE_KEY) val message: String = "",
    @SerialName(STATUS_KEY) val status: Int = 0,
    @SerialName(CODE_KEY) val code: Int = 0,
    @SerialName(TYPE_KEY) val type: String = "",
) {
    companion object {
        const val NAME_KEY = "name"
        const val MESSAGE_KEY = "message"
        const val STATUS_KEY = "status"
        const val CODE_KEY = "code"
        const val TYPE_KEY = "type"
    }
}
