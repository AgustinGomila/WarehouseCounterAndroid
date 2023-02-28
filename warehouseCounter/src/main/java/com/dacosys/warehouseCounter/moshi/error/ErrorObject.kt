package com.dacosys.warehouseCounter.moshi.error

import android.util.Log
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException

/**
 * Esta clase serializa y deserializa un Json con la
 * siguiente estructura que utiliza la API cuando se produce un error
 * al procesar diferentes solicitudes.
 *
 * {
 *  'error': {
 *      'code': {'type':'string'},
 *      'name': {'type': 'string'},
 *      'description': {'type': 'string'}
 *  }
 * }
 */
@JsonClass(generateAdapter = true)
class ErrorObject {

    @Json(name = errorTag)
    var error: ErrorData = ErrorData()

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val errorTag = "error"

        /**
         * Determina si un JObject es un objeto de error
         */
        fun isError(jsonObj: Any): Boolean {
            try {
                val jsonError = moshi.adapter(ErrorObject::class.java).fromJsonValue(jsonObj)

                if (jsonError?.error != null && jsonError.error.code != "") {
                    val errorData = jsonError.error
                    println("JSON Error: ${errorData.code}, ${errorData.name}, ${errorData.description}")
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
