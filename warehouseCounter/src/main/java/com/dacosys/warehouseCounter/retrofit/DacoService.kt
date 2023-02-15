package com.dacosys.warehouseCounter.retrofit

import com.dacosys.warehouseCounter.moshi.clientPackage.AuthDataCont
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface DacoService {
    @POST("configuration/retrieve")
    fun getClientPackage(
        @Body body: AuthDataCont,
    ): Call<Any?>
}