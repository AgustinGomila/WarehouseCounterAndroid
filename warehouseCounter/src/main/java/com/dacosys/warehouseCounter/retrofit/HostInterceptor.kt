package com.dacosys.warehouseCounter.retrofit

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.net.MalformedURLException
import java.net.URL

/**
 * Host interceptor
 * Intercepta la demanda del Host por la clase OkHttp para actualizarla al vuelo. Lo utilizamos para
 * cambiar dinámicamente la conexión de Retrofit.
 * @constructor Create empty Host interceptor
 */
class HostInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val url: URL = try {
            URL(DynamicRetrofit.getUrl())
        } catch (e: MalformedURLException) {
            Log.e(this::class.java.simpleName, e.toString())
            return dummyResponse
        }

        var request = chain.request()
        val newUrl = request.url.newBuilder().host(url.host).build()

        request = request.newBuilder().url(newUrl).build()

        return chain.proceed(request)
    }

    private val dummyResponse: Response = Response.Builder()
        .request(
            Request.Builder().url(DynamicRetrofit.getUrl()).build()
        )
        .protocol(Protocol.HTTP_2)
        .code(404)
        .message(WarehouseCounterApp.context.getString(R.string.invalid_url))
        .body(ResponseBody.create("application/json".toMediaTypeOrNull(), ""))
        .build()
}