package com.dacosys.warehouseCounter.dto.error

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ErrorData {

    @SerialName(codeTag)
    var code: String = ""

    @SerialName(nameTag)
    var name: String = ""

    @SerialName(descriptionTag)
    var description: String = ""

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val codeTag = "code"
        const val nameTag = "name"
        const val descriptionTag = "description"
    }
}
