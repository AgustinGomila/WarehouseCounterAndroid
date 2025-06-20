package com.dacosys.warehouseCounter.data.ktor.v1.dto.clientPackage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AuthDataCont {

    @SerialName(USER_AUTH_KEY)
    var authData: ClientAuthData =
        ClientAuthData()

    companion object {
        const val USER_AUTH_KEY = "authdata"
    }
}
