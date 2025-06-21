package com.dacosys.warehouseCounter.data.ktor.v1.impl

import android.util.Log
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.httpClient
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.data.ktor.v1.dto.clientPackage.AuthDataCont
import com.dacosys.warehouseCounter.data.ktor.v1.dto.clientPackage.PackageResponse
import com.dacosys.warehouseCounter.data.ktor.v1.service.DacoService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException
import javax.net.ssl.SSLHandshakeException

class DacoServiceImpl : DacoService {
    companion object {
        private val dacoUrl by lazy { "https://config.dacosys.com" }
    }

    override suspend fun getClientPackage(
        body: AuthDataCont,
        callback: (PackageResponse) -> Unit
    ) {
        val url = "$dacoUrl/configuration/retrieve"
        var res = PackageResponse(mapOf())
        try {
            val result = httpClient.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.bodyAsText()
            Log.i(javaClass.simpleName, result)
            res = json.decodeFromString<PackageResponse>(
                result
            )
        } catch (e: SSLHandshakeException) {
            Log.e(javaClass.simpleName, "Error", e)
        } catch (e: SerializationException) {
            Log.e(javaClass.simpleName, "Error", e)
        }
        callback(res)
    }
}

