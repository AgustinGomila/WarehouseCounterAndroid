package com.dacosys.warehouseCounter.retrofit.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.moshi.error.ErrorObject
import com.dacosys.warehouseCounter.moshi.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
import com.dacosys.warehouseCounter.retrofit.result.RequestResult
import com.dacosys.warehouseCounter.retrofit.result.ResultStatus
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.MalformedURLException
import java.net.URL
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
                        context().getString(R.string.invalid_or_expired_token), SnackBarType.ERROR
                    )
                )
            }
        }

        // Chequeamos que el Token sea válido
        thread { GetToken { onEvent(it) }.execute(false) }
    }

    private suspend fun suspendFunction(): Boolean = withContext(Dispatchers.IO) {
        // Configuración de Retrofit
        val apiUrl = setupRetrofit()

        val tempInst = apiService().getNewOrder(apiUrl = apiUrl)

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
                    val errorObject = moshi().adapter(ErrorObject::class.java).fromJsonValue(resp)
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
                            val p = moshi().adapter(OrderRequest::class.java).fromJsonValue(pack)
                                ?: continue
                            r.add(p)
                        }
                    }
                } catch (ex: Exception) {
                    onEvent.invoke(
                        SnackBarEventData(
                            context().getString(R.string.invalid_orders),
                            SnackBarType.ERROR
                        )
                    )
                    return
                }

                if (r.any()) onFinish.invoke(r)
                else {
                    onEvent.invoke(
                        SnackBarEventData(
                            context().getString(R.string.no_results), SnackBarType.INFO
                        )
                    )
                }
            }

            override fun onFailure(call: Call<Any?>, t: Throwable) {
                Log.e(this.javaClass.simpleName, t.toString())
                onEvent.invoke(SnackBarEventData(t.toString(), SnackBarType.ERROR))
            }
        })

        return@withContext true
    }

    private fun setupRetrofit(): String {
        val sv = settingViewModel()

        // URL específica del Cliente
        val protocol: String
        val host: String
        val apiUrl: String

        try {
            val url = URL(sv.urlPanel)
            protocol = url.protocol
            host = url.host
            apiUrl = if (url.path.isNotEmpty()) "${url.path}/" else ""

            Log.d(
                this::class.java.simpleName,
                "Base URL: ${protocol}://${host}/ (Api URL: ${apiUrl.ifEmpty { "Vacío" }})"
            )
        } catch (e: MalformedURLException) {
            Log.e(this::class.java.simpleName, e.toString())
            onEvent.invoke(
                SnackBarEventData(
                    context().getString(R.string.url_malformed), SnackBarType.ERROR
                )
            )
            return ""
        }

        // Configuración y refresco de la conexión
        DynamicRetrofit.start(protocol = protocol, host = host)

        return apiUrl
    }
}