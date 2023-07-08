package com.dacosys.warehouseCounter.dto.search

import com.dacosys.warehouseCounter.ktor.functions.GetToken.Companion.Token
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Esta clase serializa y deserializa un Json con la estructura requerida por
 * por la interface de la API:
 * [com.dacosys.warehouseCounter.retrofit.APIService.getPrices]
 */
@Serializable
class SearchObject() {

    @SerialName(userTokenTag)
    var userToken: String = ""

    @SerialName(searchItemTag)
    var searchItem: SearchItem = SearchItem()

    constructor(id: Long? = null, extId: Long? = null) : this() {
        userToken = Token.token
        searchItem = SearchItem(id, extId)
    }

    companion object {
        const val userTokenTag = "userToken"
        const val searchItemTag = "searchItem"
    }
}
