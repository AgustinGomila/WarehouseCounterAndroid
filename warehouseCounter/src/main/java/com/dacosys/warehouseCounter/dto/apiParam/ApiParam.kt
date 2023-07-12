package com.dacosys.warehouseCounter.dto.apiParam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class ApiParam() {
    @SerialName(userTokenTag)
    var userToken: String = ""

    @SerialName(ptlQueryTag)
    var ptlQuery: PtlQuery = PtlQuery()

    constructor(
        userToken: String, ptlQuery: PtlQuery,
    ) : this() {
        this.userToken = userToken
        this.ptlQuery = ptlQuery
    }

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val userTokenTag = "userToken"
        const val ptlQueryTag = "ptlQuery"
    }
}