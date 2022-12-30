package com.dacosys.warehouseCounter.data.token

import com.dacosys.warehouseCounter.Statics.Companion.Token
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Esta clase serializa y deserializa un Json con la estructura requerida por
 * por la interface de la API:
 * [com.dacosys.warehouseCounter.retrofit.APIService.getToken]
 * con la siguiente estructura:
 *
 * {
 *      'userToken': {'type':'string'}
 * }
 */
@JsonClass(generateAdapter = true)
class UserTokenObject() {

    @Json(name = userTokenTag)
    var token: String = ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserTokenObject

        if (token != other.token) return false

        return true
    }

    override fun hashCode(): Int {
        return token.hashCode()
    }

    constructor(token: String) : this() {
        this.token = token
    }

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val userTokenTag = "userToken"

        fun getUserToken(): UserTokenObject {
            return UserTokenObject(Token.token)
        }
    }
}