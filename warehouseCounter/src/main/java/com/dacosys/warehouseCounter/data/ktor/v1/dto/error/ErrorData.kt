package com.dacosys.warehouseCounter.data.ktor.v1.dto.error

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ErrorData {

    @SerialName(CODE_KEY)
    var code: String = ""

    @SerialName(NAME_KEY)
    var name: String = ""

    @SerialName(DESCRIPTION_KEY)
    var description: String = ""

    companion object {
        const val CODE_KEY = "code"
        const val NAME_KEY = "name"
        const val DESCRIPTION_KEY = "description"
    }
}
