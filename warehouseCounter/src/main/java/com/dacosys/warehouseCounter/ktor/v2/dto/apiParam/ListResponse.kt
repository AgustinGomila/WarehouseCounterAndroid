package com.dacosys.warehouseCounter.ktor.v2.dto.apiParam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListResponse<T>(
    var items: ArrayList<T> = ArrayList(),
    @SerialName(LINKS_KEY) var links: Links = Links(),
    @SerialName(META_KEY) var meta: Meta = Meta(),
) {
    companion object {
        const val LINKS_KEY = "_links"
        const val META_KEY = "_meta"
    }
}