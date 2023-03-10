package com.dacosys.warehouseCounter.dto.error

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ErrorData {

    @Json(name = codeTag)
    var code: String = ""

    @Json(name = nameTag)
    var name: String = ""

    @Json(name = descriptionTag)
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
