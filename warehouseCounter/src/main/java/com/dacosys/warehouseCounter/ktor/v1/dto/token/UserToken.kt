package com.dacosys.warehouseCounter.ktor.v1.dto.token

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UserToken() {

    @SerialName(USER_TOKEN_KEY)
    var token: String = ""

    constructor(token: String) : this() {
        this.token = token
    }

    companion object {
        const val USER_TOKEN_KEY = "userToken"
    }
}