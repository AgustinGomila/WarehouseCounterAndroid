package com.dacosys.warehouseCounter.data.ktor.v1.service

import com.dacosys.warehouseCounter.data.ktor.v1.dto.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.data.ktor.v1.dto.price.Price
import com.dacosys.warehouseCounter.data.ktor.v1.dto.ptlOrder.*
import com.dacosys.warehouseCounter.data.ktor.v1.dto.search.SearchObject
import com.dacosys.warehouseCounter.data.ktor.v1.dto.token.TokenObject
import com.dacosys.warehouseCounter.data.ktor.v1.dto.token.UserToken
import com.dacosys.warehouseCounter.data.ktor.v1.dto.user.UserAuthData
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Warehouse
import org.json.JSONObject

interface APIService {
    suspend fun getToken(body: UserAuthData, callback: (TokenObject?) -> Unit)
    suspend fun getPtlOrder(body: UserToken, callback: (PtlOrderResponse) -> Unit)
    suspend fun getPrices(body: SearchObject, callback: (ArrayList<Price>) -> Unit)
    suspend fun getNewOrder(callback: (ArrayList<OrderRequest>) -> Unit)
    suspend fun getWarehouse(body: UserToken, callback: (ArrayList<Warehouse>) -> Unit)
    suspend fun getPtlOrderByCode(body: PtlOrderBody, callback: (PtlOrderResponse) -> Unit)
    suspend fun sendItemCode(body: JSONObject, callback: () -> Unit)
    suspend fun sendOrders(body: JSONObject, callback: () -> Unit)
    suspend fun getDbLocation(
        version: String,
        callback: (ArrayList<com.dacosys.warehouseCounter.data.ktor.v1.dto.database.DatabaseData>) -> Unit
    )

    suspend fun attachPtlOrderToLocation(
        body: com.dacosys.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    )

    suspend fun detachPtlOrderToLocation(
        body: com.dacosys.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    )

    suspend fun addBoxToOrder(
        body: com.dacosys.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    )

    suspend fun printBox(
        body: com.dacosys.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (LabelResponse) -> Unit
    )

    suspend fun pickManual(
        body: com.dacosys.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (PickManualResponse) -> Unit
    )

    suspend fun blinkOneItem(
        body: com.dacosys.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    )

    suspend fun blinkAllOrder(
        body: com.dacosys.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    )

    suspend fun getPtlOrderContent(
        body: com.dacosys.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (PtlContentResponse) -> Unit
    )
}
