package com.dacosys.warehouseCounter.dto.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SearchItem() {

    @SerialName(idTag)
    var id: Long? = null

    @SerialName(extIdTag)
    var extId: Long? = null

    constructor(id: Long?, extId: Long?) : this() {
        this.id = id
        this.extId = extId
    }

    companion object {
        const val idTag = "id"
        const val extIdTag = "extId"
    }
}
