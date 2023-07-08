package com.dacosys.warehouseCounter.dto.error

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

    @SerialName(errorTag)
    var error: ErrorData = ErrorData()

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val errorTag = "error"
    }
}
