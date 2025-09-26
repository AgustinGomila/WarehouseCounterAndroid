package com.example.warehouseCounter.data.ktor.v1.impl

import android.util.Log
import com.example.warehouseCounter.WarehouseCounterApp.Companion.httpClient
import com.example.warehouseCounter.WarehouseCounterApp.Companion.json
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.data.ktor.v1.dto.orderRequest.OrderRequest
import com.example.warehouseCounter.data.ktor.v1.dto.price.Price
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.ApiResponse
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.ApiResponse.Companion.RESULT_ERROR
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.LabelResponse
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.PickManualResponse
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlContentResponse
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlOrderBody
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlOrderResponse
import com.example.warehouseCounter.data.ktor.v1.dto.search.SearchObject
import com.example.warehouseCounter.data.ktor.v1.dto.token.TokenObject
import com.example.warehouseCounter.data.ktor.v1.dto.token.UserToken
import com.example.warehouseCounter.data.ktor.v1.dto.user.UserAuthData
import com.example.warehouseCounter.data.ktor.v1.service.APIService
import com.example.warehouseCounter.data.ktor.v2.dto.location.Warehouse
import io.ktor.client.call.*
import io.ktor.client.network.sockets.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL

class APIServiceImpl : APIService {
    companion object {
        /*
        ENDPOINTS:
        
        /api/collector-user/get-token
        /api/database/location-v2
        /api/item-code/send
        /api/item/get-prices
        /api/order/get
        /api/order/send
        /api/p-t-l/add-box-to-order
        /api/p-t-l/attach-order-to-warehouse-area
        /api/p-t-l/blink-all-order
        /api/p-t-l/blink-one-item
        /api/p-t-l/detach-order-from-warehouse-area
        /api/p-t-l/order-content
        /api/p-t-l/orders
        /api/p-t-l/orders-by-code
        /api/p-t-l/pick-manual
        /api/p-t-l/print-box
        /api/p-t-l/warehouses
         */

        private val apiUrl by lazy { settingsVm.urlPanel }

        fun validUrl(): Boolean {
            return try {
                val url = URL(apiUrl)
                url.protocol.isNotEmpty() && url.host.isNotEmpty()
            } catch (e: MalformedURLException) {
                Log.e(javaClass.simpleName, "Error", e)
                false
            }
        }
    }

    override suspend fun getToken(body: UserAuthData, callback: (TokenObject?) -> Unit) {
        val url = "$apiUrl/api/collector-user/get-token"
        var result: TokenObject? = null
        try {
            result = httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<TokenObject>()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Error", e)
        }
        callback(result)
    }

    override suspend fun getPtlOrder(body: UserToken, callback: (PtlOrderResponse) -> Unit) {
        val url = "$apiUrl/api/p-t-l/orders"
        var result = PtlOrderResponse(listOf())
        try {
            result = httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<PtlOrderResponse>()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Error", e)
        }
        callback(result)
    }

    override suspend fun getPrices(body: SearchObject, callback: (ArrayList<Price>) -> Unit) {
        val url = "$apiUrl/api/item/get-prices"
        var res: ArrayList<Price> = ArrayList()
        val result: String
        try {
            result = httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.bodyAsText()
            res = json.decodeFromString<ArrayList<Price>>(result)
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
        }
        callback(res)
    }

    override suspend fun getNewOrder(callback: (ArrayList<OrderRequest>) -> Unit) {
        val url = "$apiUrl/api/order/get"
        var res: ArrayList<OrderRequest> = ArrayList()
        val result: String
        try {
            result = httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.bodyAsText()
            res = json.decodeFromString<ArrayList<OrderRequest>>(result)
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
        }
        callback(res)
    }

    override suspend fun getWarehouse(body: UserToken, callback: (ArrayList<Warehouse>) -> Unit) {
        val url = "$apiUrl/api/p-t-l/warehouses"
        var res: ArrayList<Warehouse> = ArrayList()
        val result: String
        try {
            result = httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.bodyAsText()
            res = json.decodeFromString<ArrayList<Warehouse>>(result)
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
        }
        callback(res)
    }

    override suspend fun getPtlOrderByCode(body: PtlOrderBody, callback: (PtlOrderResponse) -> Unit) {
        val url = "$apiUrl/api/p-t-l/orders-by-code"
        var result = PtlOrderResponse(listOf())
        try {
            result = httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<PtlOrderResponse>()
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
        }
        callback(result)
    }

    override suspend fun sendItemCode(body: JSONObject, callback: () -> Unit) {
        val url = "$apiUrl/api/item-code/send"
        try {
            val result: String = httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }.bodyAsText()
            Log.i(javaClass.simpleName, result)
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
        }
        callback()
    }

    override suspend fun sendOrders(body: JSONObject, callback: () -> Unit) {
        val url = "$apiUrl/api/order/send"
        try {
            val result: String = httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }.bodyAsText()
            Log.i(javaClass.simpleName, result)
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
        }
        callback()
    }

    override suspend fun getDbLocation(
        version: String,
        callback: (ArrayList<com.example.warehouseCounter.data.ktor.v1.dto.database.DatabaseData>) -> Unit
    ) {
        val url = "$apiUrl/api/database/location$version"
        var res: ArrayList<com.example.warehouseCounter.data.ktor.v1.dto.database.DatabaseData> = ArrayList()
        val result: String
        try {
            result = httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.bodyAsText()
            val inter =
                json.decodeFromString<com.example.warehouseCounter.data.ktor.v1.dto.database.DatabaseDataIntermediate>(
                    result
                )
            res = arrayListOf(inter.databaseData)
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
        }
        callback(res)
    }

    override suspend fun attachPtlOrderToLocation(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    ) {
        val url = "$apiUrl/api/p-t-l/attach-order-to-warehouse-area"
        val result: ApiResponse = try {
            httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<ApiResponse>()
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
            ApiResponse(RESULT_ERROR, e.message.toString())
        } catch (e: SocketTimeoutException) {
            Log.e(javaClass.simpleName, "Error", e)
            ApiResponse(RESULT_ERROR, e.message.toString())
        }
        callback(result)
    }

    override suspend fun detachPtlOrderToLocation(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    ) {
        val url = "$apiUrl/api/p-t-l/detach-order-from-warehouse-area"
        val result: ApiResponse = try {
            httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<ApiResponse>()
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
            ApiResponse(RESULT_ERROR, e.message.toString())
        } catch (e: SocketTimeoutException) {
            Log.e(javaClass.simpleName, "Error", e)
            ApiResponse(RESULT_ERROR, e.message.toString())
        }
        callback(result)
    }

    override suspend fun addBoxToOrder(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    ) {
        val url = "$apiUrl/api/p-t-l/add-box-to-order"
        val result: ApiResponse = try {
            httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<ApiResponse>()
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
            ApiResponse(RESULT_ERROR, e.message.toString())
        } catch (e: SocketTimeoutException) {
            Log.e(javaClass.simpleName, "Error", e)
            ApiResponse(RESULT_ERROR, e.message.toString())
        }
        callback(result)
    }

    override suspend fun printBox(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (LabelResponse) -> Unit
    ) {
        val url = "$apiUrl/api/p-t-l/print-box"
        val result: LabelResponse = try {
            httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<LabelResponse>()
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
            LabelResponse(RESULT_ERROR, e.message.toString(), listOf())
        } catch (e: SocketTimeoutException) {
            Log.e(javaClass.simpleName, "Error", e)
            LabelResponse(RESULT_ERROR, e.message.toString(), listOf())
        }
        callback(result)
    }

    override suspend fun pickManual(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (PickManualResponse) -> Unit
    ) {
        val url = "$apiUrl/api/p-t-l/pick-manual"
        val result: PickManualResponse = try {
            httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<PickManualResponse>()
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
            PickManualResponse(RESULT_ERROR, e.message.toString(), listOf())
        } catch (e: SocketTimeoutException) {
            Log.e(javaClass.simpleName, "Error", e)
            PickManualResponse(RESULT_ERROR, e.message.toString(), listOf())
        }
        callback(result)
    }

    override suspend fun blinkOneItem(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    ) {
        val url = "$apiUrl/api/p-t-l/blink-one-item"
        val result: ApiResponse = try {
            httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<ApiResponse>()
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
            ApiResponse(RESULT_ERROR, e.message.toString())
        } catch (e: SocketTimeoutException) {
            Log.e(javaClass.simpleName, "Error", e)
            ApiResponse(RESULT_ERROR, e.message.toString())
        }
        callback(result)
    }

    override suspend fun blinkAllOrder(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (ApiResponse) -> Unit
    ) {
        val url = "$apiUrl/api/p-t-l/blink-all-order"
        val result: ApiResponse = try {
            httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<ApiResponse>()
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
            ApiResponse(RESULT_ERROR, e.message.toString())
        } catch (e: SocketTimeoutException) {
            Log.e(javaClass.simpleName, "Error", e)
            ApiResponse(RESULT_ERROR, e.message.toString())
        }
        callback(result)
    }

    override suspend fun getPtlOrderContent(
        body: com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam,
        callback: (PtlContentResponse) -> Unit
    ) {
        val url = "$apiUrl/api/p-t-l/order-content"
        val result: PtlContentResponse = try {
            httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<PtlContentResponse>()
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
            PtlContentResponse(RESULT_ERROR, e.message.toString(), listOf())
        } catch (e: SocketTimeoutException) {
            Log.e(javaClass.simpleName, "Error", e)
            PtlContentResponse(RESULT_ERROR, e.message.toString(), listOf())
        }
        callback(result)
    }
}
