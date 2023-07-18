package com.dacosys.warehouseCounter.ktor.v1.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AuthData {

    @SerialName(USER_KEY)
    var username: String = ""

    @SerialName(PASS_KEY)
    var password: String = ""

    companion object {
        const val USER_KEY = "username"
        const val PASS_KEY = "password"
    }
}
