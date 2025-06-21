package com.dacosys.warehouseCounter.data.ktor.v1.dto.search

import com.dacosys.warehouseCounter.data.ktor.v1.functions.GetToken.Companion.Token
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Esta clase serializa y deserializa un Json con la estructura requerida
 * por la interface de la API: [com.dacosys.warehouseCounter.data.ktor.v1.impl.APIServiceImpl.getPrices]
 */
@Serializable
class SearchObject() {

    @SerialName(USER_TOKEN_KEY)
    var userToken: String = ""

    @SerialName(SEARCH_ITEM_KEY)
    var searchItem: SearchItem = SearchItem()

    constructor(id: Long? = null, extId: Long? = null) : this() {
        userToken = Token.token
        searchItem = SearchItem(id, extId)
    }

    companion object {
        const val USER_TOKEN_KEY = "userToken"
        const val SEARCH_ITEM_KEY = "searchItem"
    }
}
