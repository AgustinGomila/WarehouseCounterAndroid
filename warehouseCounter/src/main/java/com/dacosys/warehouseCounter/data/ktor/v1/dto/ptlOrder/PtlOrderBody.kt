package com.dacosys.warehouseCounter.data.ktor.v1.dto.ptlOrder

import com.dacosys.warehouseCounter.data.ktor.v1.dto.search.SearchItem
import com.dacosys.warehouseCounter.data.ktor.v1.dto.token.UserToken
import kotlinx.serialization.Serializable

@Serializable
data class PtlOrderBody(val userToken: UserToken, val searchItem: SearchItem)
