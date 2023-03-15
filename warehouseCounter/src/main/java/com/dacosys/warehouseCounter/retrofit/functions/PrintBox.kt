package com.dacosys.warehouseCounter.retrofit.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.dto.ptlOrder.Label
import com.dacosys.warehouseCounter.dto.ptlOrder.LabelResponse
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
import com.dacosys.warehouseCounter.retrofit.functions.GetToken.Companion.Token
import com.dacosys.warehouseCounter.retrofit.result.RequestResult
import com.dacosys.warehouseCounter.retrofit.result.ResultStatus
import com.dacosys.warehouseCounter.room.entity.user.User
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

class PrintBox(
    private val orderId: Long,
    private val onFinish: (ArrayList<Label>) -> Unit,
    private val onEvent: (SnackBarEventData) -> Unit = { },
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private lateinit var currentUser: User

    fun execute() {
        // Función que prosigue al resultado de GetToken
        fun onEvent(it: RequestResult) {
            if (it.status == ResultStatus.SUCCESS) {

                // Proseguimos con la búsqueda...
                scope.launch {
                    coroutineScope {
                        withContext(Dispatchers.IO) { suspendFunction() }
                    }
                }
            } else {
                // Token inválido.
                onEvent.invoke(
                    SnackBarEventData(
                        context.getString(R.string.invalid_or_expired_token), SnackBarType.ERROR
                    )
                )
            }
        }

        if (!DynamicRetrofit.prepare()) {
            onEvent.invoke(
                SnackBarEventData(
                    context.getString(R.string.invalid_url), SnackBarType.ERROR
                )
            )
            return
        }

        Statics.getCurrentUser { user ->
            if (user != null) {
                currentUser = user

                // Chequeamos que el Token sea válido
                thread { GetToken { onEvent(it) }.execute(false) }
            } else {
                // Token inválido.
                onEvent.invoke(
                    SnackBarEventData(
                        context.getString(R.string.invalid_user), SnackBarType.ERROR
                    )
                )
            }
        }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        val body = getBody()

        val tempInst = apiService.printBox(body = body)

        tempInst.enqueue(object : Callback<Any?> {
            override fun onResponse(call: Call<Any?>, response: Response<Any?>) {
                val resp = response.body()
                if (resp == null) {
                    Log.e(this.javaClass.simpleName, response.message())
                    onEvent.invoke(SnackBarEventData(response.message(), SnackBarType.ERROR))
                    return
                }

                try {
                    val respObj = moshi.adapter(LabelResponse::class.java).fromJsonValue(resp)
                    if (respObj == null) {
                        onEvent.invoke(
                            SnackBarEventData(
                                context.getString(R.string.invalid_response), SnackBarType.ERROR
                            )
                        )
                        return
                    }

                    val r: ArrayList<Label> = ArrayList(respObj.labels)

                    if (r.any()) {
                        onFinish.invoke(r)
                    } else {
                        onEvent.invoke(SnackBarEventData(respObj.details, SnackBarType.INFO))
                    }
                } catch (ex: Exception) {
                    onEvent.invoke(
                        SnackBarEventData(
                            context.getString(R.string.invalid_response), SnackBarType.ERROR
                        )
                    )
                }
            }

            override fun onFailure(call: Call<Any?>, t: Throwable) {
                Log.e(this.javaClass.simpleName, t.toString())
                onEvent.invoke(SnackBarEventData(t.toString(), SnackBarType.ERROR))
            }
        })
    }

    private fun getBody(): RequestBody {
        // BODY ////////////////////////////
        val ptlQuery = JSONObject()
        ptlQuery.put("orderId", orderId)

        // Token DATA //////////////////
        val jsonParam = JSONObject()
        jsonParam.put("userToken", Token.token).put("ptlQuery", ptlQuery)

        return RequestBody.create(MediaType.parse("application/json"), jsonParam.toString())
    }
}