package com.dacosys.warehouseCounter.moshi.search

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class SearchItem() {

    @Json(name = idTag)
    var id: Long? = null

    @Json(name = extIdTag)
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
