package com.dacosys.warehouseCounter.dto.clientPackage

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class AuthDataCont {

    @Json(name = userAuthTag)
    var authData: ClientAuthData = ClientAuthData()

    companion object {
        const val userAuthTag = "authdata"
    }
}

@JsonClass(generateAdapter = true)
class ClientAuthData {

    @Json(name = emailTag)
    var email: String = ""

    @Json(name = passTag)
    var password: String = ""

    @Json(name = versionTag)
    var version: String = "1"

    companion object {
        const val emailTag = "email"
        const val passTag = "password"
        const val versionTag = "version"
    }
}