package com.dacosys.warehouseCounter.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AuthData {

    @SerialName(userTag)
    var username: String = ""

    @SerialName(passTag)
    var password: String = ""

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val userTag = "username"
        const val passTag = "password"
    }
}
