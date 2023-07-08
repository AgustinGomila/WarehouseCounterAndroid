package com.dacosys.warehouseCounter.dto.clientPackage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Ws {
    @SerialName(urlTag)
    var url: String = ""

    @SerialName(namespaceTag)
    var namespace: String = ""

    @SerialName(userTag)
    var user: String = ""

    @SerialName(passwordTag)
    var password: String = ""

    companion object {
        const val urlTag = "url"
        const val namespaceTag = "namespace"
        const val userTag = "ws_user"
        const val passwordTag = "ws_password"
    }
}