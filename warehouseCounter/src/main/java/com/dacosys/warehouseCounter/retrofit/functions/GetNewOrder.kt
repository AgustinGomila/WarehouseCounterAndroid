package com.dacosys.warehouseCounter.retrofit.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.dto.error.ErrorObject
import com.dacosys.warehouseCounter.dto.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.ktor.functions.GetToken
import com.dacosys.warehouseCounter.network.result.RequestResult
import com.dacosys.warehouseCounter.network.result.ResultStatus
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

interface NewOrderListener {
    fun onNewOrderEvent(itemArray: ArrayList<OrderRequest>)
}

class GetNewOrder(
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ArrayList<OrderRequest>) -> Unit,
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

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
                SnackBarEventData(context.getString(R.string.invalid_url), SnackBarType.ERROR)
            )
            return
        }

        // Chequeamos que el Token sea válido
        thread { GetToken { onEvent(it) }.execute(false) }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        val tempInst = apiService.getNewOrder()

        tempInst.enqueue(object : Callback<Any?> {
            override fun onResponse(call: Call<Any?>, response: Response<Any?>) {
                val r: ArrayList<OrderRequest> = ArrayList()
                val resp = response.body()
                if (resp == null) {
                    Log.e(this.javaClass.simpleName, response.message())
                    onEvent.invoke(SnackBarEventData(response.message(), SnackBarType.ERROR))
                    return
                }

                /**
                 * Comprobamos si es una respuesta de Error predefinida
                 */
                if (ErrorObject.isError(resp)) {
                    val errorObject = moshi.adapter(ErrorObject::class.java).fromJsonValue(resp)
                    onEvent.invoke(
                        SnackBarEventData(
                            errorObject?.error.toString(), SnackBarType.ERROR
                        )
                    )
                    return
                }

                /**
                 * Comprobamos si es una respuesta del tipo colección de OrderRequests
                 */
                try {
                    for (allOrderRequests in (resp as Map<*, *>).entries) {
                        val orderRequestMap = allOrderRequests.value as Map<*, *>
                        for (pack in orderRequestMap.values) {
                            val p = moshi.adapter(OrderRequest::class.java).fromJsonValue(pack)
                                ?: continue
                            r.add(p)
                        }
                    }
                } catch (ex: Exception) {
                    onEvent.invoke(
                        SnackBarEventData(
                            context.getString(R.string.invalid_orders), SnackBarType.ERROR
                        )
                    )
                    return
                }

                if (r.any()) onFinish.invoke(r)
                else {
                    onEvent.invoke(
                        SnackBarEventData(
                            context.getString(R.string.no_results), SnackBarType.INFO
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
}