package com.example.warehouseCounter.data.ktor.v2.dto.apiParam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Links(
    @SerialName(SELF_KEY) val self: Link = Link(),
    @SerialName(FIRST_KEY) val first: Link = Link(),
    @SerialName(LAST_KEY) val last: Link = Link(),
) {
    companion object {
        const val SELF_KEY = "self"
        const val FIRST_KEY = "first"
        const val LAST_KEY = "last"
    }
}


@Serializable
data class Link(
    @SerialName(HREF_KEY) val href: String = "",
) {
    companion object {
        const val HREF_KEY = "href"
    }
}
