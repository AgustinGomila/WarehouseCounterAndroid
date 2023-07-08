package com.dacosys.warehouseCounter.dto.ptlOrder

import com.dacosys.warehouseCounter.dto.search.SearchItem
import com.dacosys.warehouseCounter.dto.token.UserToken
import kotlinx.serialization.Serializable

@Serializable
data class PtlOrderBody(val userToken: UserToken, val searchItem: SearchItem)