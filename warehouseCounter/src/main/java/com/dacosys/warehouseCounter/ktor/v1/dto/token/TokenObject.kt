package com.dacosys.warehouseCounter.ktor.v1.dto.token

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Esta clase serializa y deserializa un Json con la siguiente estructura:
 * {
 *      'token': {'type':'string'},
 *      'expiration': {'type': 'string'}
 * }
 */
@Serializable
class TokenObject() {

    @SerialName(TOKEN_KEY)
    var token: String = ""

    @SerialName(EXPIRATION_KEY)
    var expiration: String = ""

    constructor(
        token: String, expiration: String,
    ) : this() {
        this.token = token
        this.expiration = expiration
    }

    companion object {
        const val TOKEN_KEY = "token"
        const val EXPIRATION_KEY = "expiration"
    }
}