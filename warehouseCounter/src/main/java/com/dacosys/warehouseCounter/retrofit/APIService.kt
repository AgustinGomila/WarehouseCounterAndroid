package com.dacosys.warehouseCounter.retrofit

import com.dacosys.warehouseCounter.moshi.search.SearchPrice
import com.dacosys.warehouseCounter.moshi.user.UserAuthData
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface APIService {
    @POST("{api_url}api/collector-user/get-token")
    fun getToken(
        @Path(value = "api_url", encoded = true) apiUrl: String,
        @Body body: UserAuthData,
    ): Call<Any?>

    @POST("{api_url}api/item/get-prices")
    fun getPrices(
        @Path(value = "api_url", encoded = true) apiUrl: String,
        @Body body: SearchPrice,
    ): Call<Any?>

    @POST("{api_url}api/database/location")
    fun getDbLocation(
        @Path(value = "api_url", encoded = true) apiUrl: String,
    ): Call<Any?>
}

