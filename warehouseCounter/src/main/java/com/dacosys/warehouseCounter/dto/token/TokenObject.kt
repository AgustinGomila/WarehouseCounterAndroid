package com.dacosys.warehouseCounter.dto.token

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

    @SerialName(tokenTag)
    var token: String = ""

    @SerialName(expirationTag)
    var expiration: String = ""

    constructor(
        token: String, expiration: String,
    ) : this() {
        this.token = token
        this.expiration = expiration
    }

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val tokenTag = "token"
        const val expirationTag = "expiration"
    }
}