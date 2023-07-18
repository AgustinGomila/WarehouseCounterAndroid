package com.dacosys.warehouseCounter.ktor.v1.dto.clientPackage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ClientAuthData {

    @SerialName(EMAIL_KEY)
    var email: String = ""

    @SerialName(PASS_KEY)
    var password: String = ""

    @SerialName(VERSION_KEY)
    var version: String = "1"

    companion object {
        const val EMAIL_KEY = "email"
        const val PASS_KEY = "password"
        const val VERSION_KEY = "version"
    }
}