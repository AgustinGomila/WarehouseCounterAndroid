package com.example.warehouseCounter.data.ktor.v1.dto.apiParam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class ApiParam() {
    @SerialName(USER_TOKEN_KEY)
    var userToken: String = ""

    @SerialName(PTL_QUERY_KEY)
    var ptlQuery: PtlQuery =
        PtlQuery()

    constructor(
        userToken: String, ptlQuery: PtlQuery,
    ) : this() {
        this.userToken = userToken
        this.ptlQuery = ptlQuery
    }

    companion object {
        const val USER_TOKEN_KEY = "userToken"
        const val PTL_QUERY_KEY = "ptlQuery"
    }
}
