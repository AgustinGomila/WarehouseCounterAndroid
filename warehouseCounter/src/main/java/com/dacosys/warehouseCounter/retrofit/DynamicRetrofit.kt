package com.dacosys.warehouseCounter.retrofit

import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshiConverterFactory
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.okHttp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import retrofit2.Retrofit
import java.net.URL

/**
 * DynamicRetrofit permite actualizar la URL de Retrofit en la instancia de Koin
 */
class DynamicRetrofit {
    private fun baseUrl(): String {
        val url = URL(settingViewModel().urlPanel)
        return "${url.protocol}://${url.host}/"
    }

    private var baseUrl = baseUrl()

    private fun buildRetrofit() = Retrofit.Builder().baseUrl(baseUrl).client(okHttp())
        .addConverterFactory(moshiConverterFactory()).build()

    var api: Retrofit = buildRetrofit()
        private set

    fun refreshRetrofit() {
        api = buildRetrofit()
    }
}