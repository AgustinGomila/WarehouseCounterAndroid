package com.example.warehouseCounter.data.ktor.v1.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Esta clase serializa y deserializa un Json con la estructura requerida
 * por la interface de la API: [com.example.warehouseCounter.data.ktor.v1.impl.APIServiceImpl.getToken]
 */

@Serializable
class UserAuthData {

    @SerialName(USER_AUTH_KEY)
    var authData: AuthData = AuthData()

    companion object {
        const val USER_AUTH_KEY = "userauthdata"
    }
}
