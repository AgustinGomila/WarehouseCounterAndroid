package com.dacosys.warehouseCounter.retrofit.search

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchItem

        if (id != other.id) return false
        if (extId != other.extId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (extId?.hashCode() ?: 0)
        return result
    }

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val idTag = "id"
        const val extIdTag = "extId"
    }
}
