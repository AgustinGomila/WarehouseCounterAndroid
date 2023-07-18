package com.dacosys.warehouseCounter.ktor.v1.dto.clientPackage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Panel {
    @SerialName(URL_KEY)
    var url: String = ""

    companion object {
        const val URL_KEY = "url"
    }
}