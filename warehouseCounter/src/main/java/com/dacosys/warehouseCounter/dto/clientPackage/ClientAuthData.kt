package com.dacosys.warehouseCounter.dto.clientPackage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ClientAuthData {

    @SerialName(emailTag)
    var email: String = ""

    @SerialName(passTag)
    var password: String = ""

    @SerialName(versionTag)
    var version: String = "1"

    companion object {
        const val emailTag = "email"
        const val passTag = "password"
        const val versionTag = "version"
    }
}