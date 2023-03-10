package com.dacosys.warehouseCounter.dto.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Esta clase serializa y deserializa un Json con la estructura requerida por
 * por la interface de la API:
 * [com.dacosys.warehouseCounter.retrofit.APIService.getToken]
 */

@JsonClass(generateAdapter = true)
class UserAuthData {

    @Json(name = userAuthTag)
    var authData: AuthData = AuthData()

    companion object {
        const val userAuthTag = "userauthdata"
    }
}
