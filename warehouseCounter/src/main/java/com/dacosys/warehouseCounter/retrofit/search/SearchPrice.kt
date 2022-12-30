package com.dacosys.warehouseCounter.retrofit.search

import com.dacosys.warehouseCounter.Statics.Companion.Token
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Esta clase serializa y deserializa un Json con la estructura requerida por
 * por la interface de la API:
 * [com.dacosys.warehouseCounter.retrofit.APIService.getPrices]
 */
@JsonClass(generateAdapter = true)
class SearchPrice() {

    @Json(name = tokenTag)
    var userToken: String = ""

    @Json(name = searchItemTag)
    var searchItem: SearchItem = SearchItem()

    constructor(id: Long? = null, extId: Long? = null) : this() {
        userToken = Token.token
        searchItem = SearchItem(id, extId)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchPrice

        if (userToken != other.userToken) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userToken.hashCode()
        result = 31 * result + searchItem.hashCode()
        return result
    }

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val tokenTag = "userToken"
        const val searchItemTag = "searchItem"
    }
}
