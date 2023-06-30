package com.dacosys.warehouseCounter.ktor

import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.httpClient
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.dto.common.JsonUtils
import com.dacosys.warehouseCounter.dto.common.JsonUtils.Companion.fromJson
import com.dacosys.warehouseCounter.dto.ptlOrder.PtlOrder
import com.dacosys.warehouseCounter.dto.token.TokenObject
import com.dacosys.warehouseCounter.dto.user.UserAuthData
import com.google.gson.Gson
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.json.JSONObject

interface APIService {
    suspend fun getToken(body: UserAuthData, callback: (TokenObject?) -> Unit)
    suspend fun getPtlOrder(body: JSONObject, callback: (ArrayList<PtlOrder>) -> Unit)
}

class APIServiceImpl : APIService {
    companion object {
        private val apiUrl by lazy { settingRepository.urlPanel.value.toString() }
    }

    override suspend fun getToken(body: UserAuthData, callback: (TokenObject?) -> Unit) {
        val url = "${apiUrl}/api/collector-user/get-token"
        val result: String = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(JsonUtils.toJson(body))
        }.bodyAsText()

        val r = fromJson<TokenObject>(result)
        callback(r)
    }

    override suspend fun getPtlOrder(
        body: JSONObject,
        callback: (ArrayList<PtlOrder>) -> Unit
    ) {
        val url = "${apiUrl}/api/p-t-l/orders"
        val result: String = httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }.bodyAsText()

        val r: ArrayList<PtlOrder> = ArrayList()

        val jsonMap = moshi.adapter(Map::class.java).fromJson(result)
        for (orders in (jsonMap as Map<*, *>).entries) {
            val orderMap = orders.value as ArrayList<*>
            for (order in orderMap) {
                val json = Gson().toJsonTree(order).asJsonObject
                val o = fromJson<PtlOrder>(json.toString())
                    ?: continue
                r.add(o)
            }
        }
        callback(r)
    }
}