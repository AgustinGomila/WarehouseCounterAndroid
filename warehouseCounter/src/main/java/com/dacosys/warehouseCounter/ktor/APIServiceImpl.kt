package com.dacosys.warehouseCounter.ktor

import android.util.Log
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.httpClient
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.dto.apiParam.ApiParam
import com.dacosys.warehouseCounter.dto.database.DatabaseData
import com.dacosys.warehouseCounter.dto.database.DatabaseDataIntermediate
import com.dacosys.warehouseCounter.dto.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.dto.price.Price
import com.dacosys.warehouseCounter.dto.ptlOrder.*
import com.dacosys.warehouseCounter.dto.search.SearchObject
import com.dacosys.warehouseCounter.dto.token.TokenObject
import com.dacosys.warehouseCounter.dto.token.UserToken
import com.dacosys.warehouseCounter.dto.user.UserAuthData
import com.dacosys.warehouseCounter.dto.warehouse.Warehouse
import com.dacosys.warehouseCounter.misc.Statics
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.URL

class APIServiceImpl : APIService {
    companion object {
        private val apiUrl by lazy { settingViewModel.urlPanel }

        fun validUrl(): Boolean {
            val url = URL(apiUrl)
            return url.protocol.isNotEmpty() && url.host.isNotEmpty()
        }
    }

    override suspend fun getToken(body: UserAuthData, callback: (TokenObject?) -> Unit) {
        val url = "${apiUrl}/api/collector-user/get-token"
        val result = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<TokenObject>()
        callback(result)
    }

    override suspend fun getPtlOrder(body: UserToken, callback: (PtlOrderResponse) -> Unit) {
        val url = "${apiUrl}/api/p-t-l/orders"
        val res = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<PtlOrderResponse>()
        callback(res)
    }

    override suspend fun getPrices(body: SearchObject, callback: (ArrayList<Price>) -> Unit) {
        val url = "${apiUrl}/api/item/get-prices"
        val result: String = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()
        val res = Json.decodeFromString<ArrayList<Price>>(result)
        callback(res)
    }

    override suspend fun getNewOrder(callback: (ArrayList<OrderRequest>) -> Unit) {
        val url = "${apiUrl}/api/order/get"
        val result: String = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()
        try {
            val res = Json.decodeFromString<ArrayList<OrderRequest>>(result)
            callback(res)
        } catch (e: SerializationException) {
            Log.i(javaClass.simpleName, "Response: $result${Statics.lineSeparator}Exception: ${e.message.toString()}")
            callback(ArrayList())
        }
    }

    override suspend fun getWarehouse(body: UserToken, callback: (ArrayList<Warehouse>) -> Unit) {
        val url = "${apiUrl}/api/p-t-l/warehouses"
        val result: String = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()
        val res = Json.decodeFromString<ArrayList<Warehouse>>(result)
        callback(res)
    }

    override suspend fun getPtlOrderByCode(body: PtlOrderBody, callback: (PtlOrderResponse) -> Unit) {
        val url = "${apiUrl}/api/p-t-l/orders-by-code"
        val res = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<PtlOrderResponse>()
        callback(res)
    }

    override suspend fun sendItemCode(body: JSONObject, callback: () -> Unit) {
        val url = "${apiUrl}/api/item-code/send"
        val result: String = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }.bodyAsText()
        Log.i(javaClass.simpleName, result)
        callback()
    }

    override suspend fun sendOrders(body: JSONObject, callback: () -> Unit) {
        val url = "${apiUrl}/api/order/send"
        val result: String = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }.bodyAsText()
        Log.i(javaClass.simpleName, result)
        callback()
    }

    override suspend fun getDbLocation(version: String, callback: (ArrayList<DatabaseData>) -> Unit) {
        val url = "${apiUrl}/api/database/location$version"
        val result = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<DatabaseDataIntermediate>()
        callback(arrayListOf(result.databaseData))
    }

    override suspend fun attachPtlOrderToLocation(body: ApiParam, callback: (ApiResponse) -> Unit) {
        val url = "${apiUrl}/api/p-t-l/attach-order-to-warehouse-area"
        val result = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<ApiResponse>()
        callback(result)
    }

    override suspend fun detachPtlOrderToLocation(body: ApiParam, callback: (ApiResponse) -> Unit) {
        val url = "${apiUrl}/api/p-t-l/detach-order-from-warehouse-area"
        val result = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<ApiResponse>()
        callback(result)
    }

    override suspend fun addBoxToOrder(body: ApiParam, callback: (ApiResponse) -> Unit) {
        val url = "${apiUrl}/api/p-t-l/add-box-to-order"
        val result = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<ApiResponse>()
        callback(result)
    }

    override suspend fun printBox(body: ApiParam, callback: (LabelResponse) -> Unit) {
        val url = "${apiUrl}/api/p-t-l/print-box"
        val result = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<LabelResponse>()
        callback(result)
    }

    override suspend fun pickManual(body: ApiParam, callback: (PickManualResponse) -> Unit) {
        val url = "${apiUrl}/api/p-t-l/pick-manual"
        val result: String = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()
        Log.i(javaClass.simpleName, result)
        callback(Json.decodeFromString<PickManualResponse>(result))
    }

    override suspend fun blinkOneItem(body: ApiParam, callback: (ApiResponse) -> Unit) {
        val url = "${apiUrl}/api/p-t-l/blink-one-item"
        val result = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<ApiResponse>()
        callback(result)
    }

    override suspend fun blinkAllOrder(body: ApiParam, callback: (ApiResponse) -> Unit) {
        val url = "${apiUrl}/api/p-t-l/blink-all-order"
        val result = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<ApiResponse>()
        callback(result)
    }

    override suspend fun getPtlOrderContent(body: ApiParam, callback: (PtlContentResponse) -> Unit) {
        val url = "${apiUrl}/api/p-t-l/order-content"
        val result = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<PtlContentResponse>()
        callback(result)
    }
}