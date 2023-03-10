package com.dacosys.warehouseCounter.retrofit.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.dto.ptlOrder.PtlContent
import com.dacosys.warehouseCounter.dto.ptlOrder.PtlContentResponse
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
import com.dacosys.warehouseCounter.retrofit.result.RequestResult
import com.dacosys.warehouseCounter.retrofit.result.ResultStatus
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

class GetPtlOrderContent(
    private val orderId: Long,
    private val warehouseAreaId: Long,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ArrayList<PtlContent>) -> Unit,
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val r: ArrayList<PtlContent> = ArrayList()

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

        val tempInst = apiService.getPtlOrderContent(body = body)

        tempInst.enqueue(object : Callback<Any?> {
            override fun onResponse(call: Call<Any?>, response: Response<Any?>) {
                val resp = response.body()
                if (resp == null) {
                    Log.e(this.javaClass.simpleName, response.message())
                    sendEvent(response.message(), SnackBarType.ERROR)
                    return
                }

                /**
                 * Comprobamos si es una respuesta del tipo colección de PtlOrderContents
                 */
                try {
                    val respObj = moshi.adapter(PtlContentResponse::class.java).fromJsonValue(resp)
                    if (respObj == null) {
                        sendEvent(context.getString(R.string.invalid_response), SnackBarType.ERROR)
                        return
                    }

                    r.addAll(respObj.contents)

                    if (r.any()) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
                    else sendEvent(context.getString(R.string.no_results), SnackBarType.INFO)
                } catch (ex: Exception) {
                    sendEvent(context.getString(R.string.invalid_contents), SnackBarType.ERROR)
                }
            }

            override fun onFailure(call: Call<Any?>, t: Throwable) {
                Log.e(this.javaClass.simpleName, t.toString())
                sendEvent(t.toString(), SnackBarType.ERROR)
            }
        })
    }

    private fun getBody(): RequestBody {
        // BODY ////////////////////////////
        val ptlQuery = JSONObject()
        ptlQuery.put("orderId", orderId).put("warehouseAreaId", warehouseAreaId)

        // Token DATA //////////////////
        val jsonParam = JSONObject()
        jsonParam.put("userToken", Statics.Token.token).put("ptlQuery", ptlQuery)

        return RequestBody.create(MediaType.parse("application/json"), jsonParam.toString())
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