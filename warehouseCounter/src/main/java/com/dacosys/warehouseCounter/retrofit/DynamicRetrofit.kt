package com.dacosys.warehouseCounter.retrofit

import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshiConverterFactory
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.okHttp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.retrofit
import retrofit2.Retrofit
import java.net.URL

/**
 * DynamicRetrofit permite actualizar la URL de Retrofit en la instancia de Koin
 */
class DynamicRetrofit {
    private fun buildRetrofit() = Retrofit.Builder().baseUrl(getUrl()).client(okHttp())
        .addConverterFactory(moshiConverterFactory()).build()

    var api: Retrofit = buildRetrofit()
        private set

    /**
     * Reconstruye la instancia de Retrofit.
     */
    fun refresh() {
        api = buildRetrofit()
    }

    companion object {
        var protocol: String = ""
        var host: String = ""

        private fun getUrl(): String {
            return "${protocol}://${host}/"
        }


        fun start(url: URL) {
            start(url.protocol, url.host)
        }

        fun start(protocol: String = "", host: String = "") {
            this.protocol = protocol
            this.host = host
            retrofit().refresh()
        }
    }
}