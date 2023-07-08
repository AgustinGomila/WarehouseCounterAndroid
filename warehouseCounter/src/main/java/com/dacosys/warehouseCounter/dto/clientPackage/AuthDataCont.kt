package com.dacosys.warehouseCounter.dto.clientPackage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AuthDataCont {

    @SerialName(userAuthTag)
    var authData: ClientAuthData = ClientAuthData()

    companion object {
        const val userAuthTag = "authdata"
    }
}