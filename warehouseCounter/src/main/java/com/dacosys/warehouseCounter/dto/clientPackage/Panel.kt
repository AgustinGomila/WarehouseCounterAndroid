package com.dacosys.warehouseCounter.dto.clientPackage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Panel {
    @SerialName(urlTag)
    var url: String = ""

    companion object {
        const val urlTag = "url"
    }
}