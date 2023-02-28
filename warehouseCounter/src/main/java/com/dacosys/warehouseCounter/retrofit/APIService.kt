package com.dacosys.warehouseCounter.retrofit

import com.dacosys.warehouseCounter.moshi.search.SearchPrice
import com.dacosys.warehouseCounter.moshi.user.UserAuthData
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface APIService {
    @POST("{api_url}api/collector-user/get-token")
    fun getToken(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: UserAuthData,
    ): Call<Any?>

    @POST("{api_url}api/item/get-prices")
    fun getPrices(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: SearchPrice,
    ): Call<Any?>

    @POST("{api_url}api/order/get")
    fun getNewOrder(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
    ): Call<Any?>

    @POST("{api_url}api/item-code/send")
    fun sendItemCode(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: RequestBody,
    ): Call<Any?>

    @POST("{api_url}api/order/send")
    fun sendOrders(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: RequestBody,
    ): Call<Any?>

    @POST("{api_url}api/database/location{version}")
    fun getDbLocation(
        @Path("api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Path("version", encoded = true) version: String,
    ): Call<Any?>
}