package com.dacosys.warehouseCounter.model.user

import com.dacosys.warehouseCounter.misc.Statics.Companion.getCurrentUser
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.security.NoSuchAlgorithmException

/**
 * Esta clase serializa y deserializa un Json con la estructura requerida por
 * por la interface de la API:
 * [com.dacosys.warehouseCounter.retrofit.APIService.getToken]
 */

@JsonClass(generateAdapter = true)
class UserAuthData {

    @Json(name = userAuthTag)
    var authData: AuthData = AuthData()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserAuthData

        if (authData != other.authData) return false

        return true
    }

    override fun hashCode(): Int {
        return authData.hashCode()
    }

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val userAuthTag = "userauthdata"

        /**
         * Devuelve las opciones guardadas en la configuración de la app en
         * forma de Json para enviar a la API
         */
        fun getUserAuthData(): UserAuthData {
            val authData = AuthData()
            val userAuth = UserAuthData()

            val currentUser = getCurrentUser() ?: return UserAuthData()

            authData.username = currentUser.name
            authData.password = currentUser.password ?: ""

            userAuth.authData = authData
            return userAuth
        }

        private fun md5(s: String): String {
            try {
                // Create getMd5 Hash
                val digest = java.security.MessageDigest.getInstance("MD5")
                digest.update(s.toByteArray())
                val messageDigest = digest.digest()

                // Create Hex String
                val hexString = StringBuilder()
                for (aMessageDigest in messageDigest) {
                    var h = Integer.toHexString(0xFF and aMessageDigest.toInt())
                    while (h.length < 2) h = "0$h"
                    hexString.append(h)
                }
                return hexString.toString()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }

            return ""
        }
    }
}
