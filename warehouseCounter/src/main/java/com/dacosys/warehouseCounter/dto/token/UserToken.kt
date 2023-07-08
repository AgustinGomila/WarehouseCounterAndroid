package com.dacosys.warehouseCounter.dto.token

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UserToken() {

    @SerialName(userTokenTag)
    var token: String = ""

    constructor(token: String) : this() {
        this.token = token
    }

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val userTokenTag = "userToken"
    }
}