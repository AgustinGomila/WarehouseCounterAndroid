package com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder

import com.example.warehouseCounter.data.ktor.v1.dto.search.SearchItem
import com.example.warehouseCounter.data.ktor.v1.dto.token.UserToken
import kotlinx.serialization.Serializable

@Serializable
data class PtlOrderBody(val userToken: UserToken, val searchItem: SearchItem)
