package com.dacosys.warehouseCounter.moshi.error

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ErrorData

        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val codeTag = "code"
        const val nameTag = "name"
        const val descriptionTag = "description"
    }
}
