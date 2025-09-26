package com.example.warehouseCounter.data.ktor.v1.service

import com.example.warehouseCounter.data.ktor.v1.dto.orderRequest.OrderRequest
import com.example.warehouseCounter.data.ktor.v1.dto.price.Price
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.ApiResponse
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.LabelResponse
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.PickManualResponse
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlContentResponse
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlOrderBody
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlOrderResponse
import com.example.warehouseCounter.data.ktor.v1.dto.search.SearchObject
import com.example.warehouseCounter.data.ktor.v1.dto.token.TokenObject
import com.example.warehouseCounter.data.ktor.v1.dto.token.UserToken
import com.example.warehouseCounter.data.ktor.v1.dto.user.UserAuthData
import com.example.warehouseCounter.data.ktor.v2.dto.location.Warehouse
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
        callback: (ArrayList<com.example.warehouseCounter.data.ktor.v1.dto.database.DatabaseData>) -> Unit
    )

    suspend fun attachPtlOrderToLocation(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    )

    suspend fun detachPtlOrderToLocation(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    )

    suspend fun addBoxToOrder(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    )

    suspend fun printBox(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (LabelResponse) -> Unit
    )

    suspend fun pickManual(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (PickManualResponse) -> Unit
    )

    suspend fun blinkOneItem(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    )

    suspend fun blinkAllOrder(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    )

    suspend fun getPtlOrderContent(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (PtlContentResponse) -> Unit
    )
}
