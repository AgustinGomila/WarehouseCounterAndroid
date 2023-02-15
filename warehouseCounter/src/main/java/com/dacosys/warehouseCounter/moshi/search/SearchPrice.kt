package com.dacosys.warehouseCounter.moshi.search

import com.dacosys.warehouseCounter.misc.Statics.Companion.Token
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

    companion object {
        const val tokenTag = "userToken"
        const val searchItemTag = "searchItem"
    }
}
