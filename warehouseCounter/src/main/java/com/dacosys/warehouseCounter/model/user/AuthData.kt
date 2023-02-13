package com.dacosys.warehouseCounter.model.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class AuthData {

    @Json(name = userTag)
    var username: String = ""

    @Json(name = passTag)
    var password: String = ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AuthData

        if (username != other.username) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + password.hashCode()
        return result
    }

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val userTag = "username"
        const val passTag = "password"
    }
}
