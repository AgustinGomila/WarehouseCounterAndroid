package com.dacosys.warehouseCounter.data.token

import android.util.Log
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException

/**
 * Esta clase serializa y deserializa un Json con la siguiente estructura:
 * {
 *      'token': {'type':'string'},
 *      'expiration': {'type': 'string'}
 * }
 */
@JsonClass(generateAdapter = true)
class TokenObject() {

    @Json(name = tokenTag)
    var token: String = ""

    @Json(name = expirationTag)
    var expiration: String = ""

    constructor(
        token: String, expiration: String,
    ) : this() {
        this.token = token
        this.expiration = expiration
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TokenObject

        if (token != other.token) return false

        return true
    }

    override fun hashCode(): Int {
        var result = token.hashCode()
        result = 31 * result + expiration.hashCode()
        return result
    }

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val tokenTag = "token"
        const val expirationTag = "expiration"

        /**
         * Determina si un JObject es un objeto de token
         */
        fun isToken(jsonObj: Any): Boolean {
            try {
                val jsonToken = moshi().adapter(TokenObject::class.java).fromJsonValue(jsonObj)

                if (jsonToken?.token != null &&
                    jsonToken.token != "" &&
                    jsonToken.expiration != ""
                ) {
                    println("JSON Token: ${jsonToken.token}, ${jsonToken.expiration}")
                    return true
                }
            } catch (ignored: JsonDataException) {// No pasa nada...
            } catch (ignored: JsonEncodingException) {// No pasa nada...
            } catch (ex: Exception) {
                Log.e(this::class.java.simpleName, ex.toString())
                ex.printStackTrace()
            }

            return false
        }
    }
}
