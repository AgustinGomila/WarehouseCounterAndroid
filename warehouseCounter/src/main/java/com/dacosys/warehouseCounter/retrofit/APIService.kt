package com.dacosys.warehouseCounter.retrofit

import com.dacosys.warehouseCounter.dto.search.SearchObject
import com.dacosys.warehouseCounter.dto.user.UserAuthData
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
        @Body body: SearchObject,
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

    @POST("{api_url}api/p-t-l/warehouses")
    fun getWarehouse(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: RequestBody,
    ): Call<Any?>

    @POST("{api_url}api/p-t-l/orders")
    fun getPtlOrder(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: RequestBody,
    ): Call<Any?>

    @POST("{api_url}api/p-t-l/orders-by-code")
    fun getPtlOrderByCode(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: RequestBody,
    ): Call<Any?>

    @POST("{api_url}api/p-t-l/attach-order-to-warehouse-area")
    fun attachPtlOrderToLocation(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: RequestBody,
    ): Call<Any?>

    @POST("{api_url}api/p-t-l/detach-order-from-warehouse-area")
    fun detachPtlOrderToLocation(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: RequestBody,
    ): Call<Any?>

    @POST("{api_url}api/p-t-l/order-content")
    fun getPtlOrderContent(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: RequestBody,
    ): Call<Any?>

    @POST("{api_url}api/p-t-l/blink-all-order")
    fun blinkAllOrder(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: RequestBody,
    ): Call<Any?>

    @POST("{api_url}api/p-t-l/blink-one-item")
    fun blinkOneItem(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: RequestBody,
    ): Call<Any?>

    @POST("{api_url}api/p-t-l/pick-manual")
    fun pickManual(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: RequestBody,
    ): Call<Any?>

    @POST("{api_url}api/p-t-l/print-box")
    fun printBox(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: RequestBody,
    ): Call<Any?>

    @POST("{api_url}api/p-t-l/add-box-to-order")
    fun addBoxToOrder(
        @Path(value = "api_url", encoded = true) apiUrl: String = DynamicRetrofit.apiUrl,
        @Body body: RequestBody,
    ): Call<Any?>
}