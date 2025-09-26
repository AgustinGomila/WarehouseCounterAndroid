package com.example.warehouseCounter.data.ktor.v1.dto.clientPackage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Ws {
    @SerialName(URL_KEY)
    var url: String = ""

    @SerialName(NAMESPACE_KEY)
    var namespace: String = ""

    @SerialName(USER_KEY)
    var user: String = ""

    @SerialName(PASSWORD_KEY)
    var password: String = ""

    companion object {
        const val URL_KEY = "url"
        const val NAMESPACE_KEY = "namespace"
        const val USER_KEY = "ws_user"
        const val PASSWORD_KEY = "ws_password"
    }
}
