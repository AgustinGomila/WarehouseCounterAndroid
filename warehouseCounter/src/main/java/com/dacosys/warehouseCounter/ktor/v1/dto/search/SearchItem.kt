package com.dacosys.warehouseCounter.ktor.v1.dto.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SearchItem() {

    @SerialName(ID_KEY)
    var id: Long? = null

    @SerialName(EXT_ID_KEY)
    var extId: Long? = null

    constructor(id: Long?, extId: Long?) : this() {
        this.id = id
        this.extId = extId
    }

    companion object {
        const val ID_KEY = "id"
        const val EXT_ID_KEY = "extId"
    }
}
