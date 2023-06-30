package com.dacosys.warehouseCounter.retrofit.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.dto.warehouse.Warehouse
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
import com.dacosys.warehouseCounter.retrofit.functions.GetToken.Companion.Token
import com.dacosys.warehouseCounter.retrofit.result.RequestResult
import com.dacosys.warehouseCounter.retrofit.result.ResultStatus
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.SUCCESS
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

class GetWarehouse(
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ArrayList<Warehouse>) -> Unit,
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val r: ArrayList<Warehouse> = ArrayList()

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
                sendEvent(context.getString(R.string.invalid_or_expired_token), SnackBarType.ERROR)
            }
        }

        if (!DynamicRetrofit.prepare()) {
            sendEvent(context.getString(R.string.invalid_url), SnackBarType.ERROR)
            return
        }

        // Chequeamos que el Token sea válido
        thread { GetToken { onEvent(it) }.execute(false) }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        val body = getBody()

        val tempInst = apiService.getWarehouse(body = body)

        tempInst.enqueue(object : Callback<Any?> {
            override fun onResponse(call: Call<Any?>, response: Response<Any?>) {
                val resp = response.body()
                if (resp == null) {
                    Log.e(this.javaClass.simpleName, response.message())
                    sendEvent(response.message(), SnackBarType.ERROR)
                    return
                }

                /**
                 * Comprobamos si es una respuesta del tipo colección de Warehouse
                 */
                try {
                    val jsonArray = moshi.adapter(List::class.java).fromJsonValue(resp)
                    if (jsonArray?.any() == true) {
                        jsonArray.mapNotNullTo(r) {
                            moshi.adapter(Warehouse::class.java).fromJsonValue(it)
                        }
                    }

                    if (r.any()) sendEvent(context.getString(R.string.ok), SUCCESS)
                    else sendEvent(context.getString(R.string.no_results), SnackBarType.INFO)
                } catch (ex: Exception) {
                    sendEvent(context.getString(R.string.invalid_warehouses), SnackBarType.ERROR)
                    return
                }
            }

            override fun onFailure(call: Call<Any?>, t: Throwable) {
                Log.e(this.javaClass.simpleName, t.toString())
                sendEvent(t.toString(), SnackBarType.ERROR)
            }
        })
    }

    private fun getBody(): RequestBody {
        // Token DATA //////////////////
        val jsonParam = JSONObject()
        jsonParam.put("userToken", Token.token)

        return RequestBody.create("application/json".toMediaTypeOrNull(), jsonParam.toString())
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)

        if (event.snackBarType in getFinish() || event.snackBarType == SnackBarType.INFO) {
            onFinish.invoke(r)
        }
    }
}