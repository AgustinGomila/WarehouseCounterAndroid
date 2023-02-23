package com.dacosys.warehouseCounter.retrofit.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.moshi.error.ErrorObject
import com.dacosys.warehouseCounter.moshi.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.moshi.user.AuthData
import com.dacosys.warehouseCounter.moshi.user.UserAuthData.Companion.userAuthTag
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
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
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import kotlin.concurrent.thread

class SendOrder(
    private val orderRequestArray: ArrayList<OrderRequest>,
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
                        context().getString(R.string.invalid_or_expired_token), SnackBarType.ERROR
                    )
                )
            }
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
                        context().getString(R.string.invalid_user), SnackBarType.ERROR
                    )
                )
            }
        }
    }

    private suspend fun suspendFunction(): Boolean = withContext(Dispatchers.IO) {
        // Configuración de Retrofit
        val apiUrl = setupRetrofit()

        val body = getBody()

        val tempInst = apiService().sendOrders(apiUrl = apiUrl, body = body)

        tempInst.enqueue(object : Callback<Any?> {
            override fun onResponse(call: Call<Any?>, response: Response<Any?>) {
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

                // Eliminamos los archivos del dispositivo
                removeCountFiles()
            }

            override fun onFailure(call: Call<Any?>, t: Throwable) {
                Log.e(this.javaClass.simpleName, t.toString())
                onEvent.invoke(SnackBarEventData(t.toString(), SnackBarType.ERROR))
            }
        })

        return@withContext true
    }

    private var filesSuccess: ArrayList<String> = ArrayList()
    private fun removeCountFiles() {
        var isOk = true
        val currentDir = Statics.getCompletedPath()
        for (f in filesSuccess) {
            val filePath = currentDir.absolutePath + File.separator + f
            val fl = File(filePath)
            if (fl.exists()) {
                if (!fl.delete()) {
                    isOk = false
                    break
                }
            }
        }

        if (isOk) {
            onEvent.invoke(
                SnackBarEventData(
                    context().getString(R.string.ok), SnackBarType.SUCCESS
                )
            )
        } else {
            onEvent.invoke(
                SnackBarEventData(
                    context().getString(R.string.an_error_occurred_while_deleting_counts),
                    SnackBarType.ERROR
                )
            )
        }
    }

    private fun getBody(): RequestBody {
        // BODY ////////////////////////////
        val jsonParam = JSONObject()

        // User Auth DATA //////////////////
        jsonParam.put(userAuthTag, AuthData().apply {
            username = currentUser.name
            password = currentUser.password ?: ""
        })

        // Collector DATA //////////////////
        val collectorData = Statics.getDeviceData()
        jsonParam.put("collectorData", collectorData)

        // Todos los Pedidos //////////////////
        val orArrayJson = JSONObject()
        for ((index, orderRequest) in orderRequestArray.withIndex()) {
            orArrayJson.put("order$index", orderRequest)

            // Guardamos el archivo subido
            filesSuccess.add(orderRequest.filename)
        }
        jsonParam.put("orders", orArrayJson)
        // Fin Todos los Pedidos //////////////

        return RequestBody.create(MediaType.parse("application/json"), jsonParam.toString())
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