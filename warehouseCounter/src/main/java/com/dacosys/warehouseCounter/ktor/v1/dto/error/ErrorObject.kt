package com.dacosys.warehouseCounter.ktor.v1.dto.error

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
@Serializable
class ErrorObject {

    @SerialName(ERROR_KEY)
    var error: ErrorData = ErrorData()

    companion object {
        const val ERROR_KEY = "error"
    }
}
