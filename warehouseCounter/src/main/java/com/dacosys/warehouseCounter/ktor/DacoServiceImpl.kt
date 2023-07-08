package com.dacosys.warehouseCounter.ktor

import android.util.Log
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.dto.clientPackage.AuthDataCont
import com.dacosys.warehouseCounter.dto.clientPackage.PackageResponse
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

class DacoServiceImpl : DacoService {
    companion object {
        private val dacoUrl by lazy { "https://config.dacosys.com" }
    }

    override suspend fun getClientPackage(body: AuthDataCont, callback: (PackageResponse) -> Unit) {
        val url = "${dacoUrl}/configuration/retrieve"
        val result = WarehouseCounterApp.httpClient.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()
        Log.i(javaClass.simpleName, result)
        val json = Json { ignoreUnknownKeys = true }
        val res = json.decodeFromString<PackageResponse>(result)
        callback(res)
    }
}