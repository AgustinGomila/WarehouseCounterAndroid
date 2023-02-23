package com.dacosys.warehouseCounter.moshi.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class AuthData {

    @Json(name = userTag)
    var username: String = ""

    @Json(name = passTag)
    var password: String = ""

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val userTag = "username"
        const val passTag = "password"
    }
}
