package com.dacosys.warehouseCounter.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Esta clase serializa y deserializa un Json con la estructura requerida por
 * por la interface de la API:
 * [com.dacosys.warehouseCounter.retrofit.APIService.getToken]
 */

@Serializable
class UserAuthData {

    @SerialName(userAuthTag)
    var authData: AuthData = AuthData()

    companion object {
        const val userAuthTag = "userauthdata"
    }
}
