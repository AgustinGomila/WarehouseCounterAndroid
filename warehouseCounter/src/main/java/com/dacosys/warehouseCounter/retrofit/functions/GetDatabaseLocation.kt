package com.dacosys.warehouseCounter.retrofit.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.moshi.database.DatabaseData
import com.dacosys.warehouseCounter.moshi.error.ErrorObject
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
import com.dacosys.warehouseCounter.retrofit.result.DbLocationResult
import com.dacosys.warehouseCounter.room.database.WcDatabase.Companion.DATABASE_VERSION
import com.dacosys.warehouseCounter.sync.ProgressStatus
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.MalformedURLException
import java.net.URL

class GetDatabaseLocation(private val onEvent: (DbLocationResult) -> Unit) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        scope.launch {
            coroutineScope {
                withContext(Dispatchers.IO) { suspendFunction() }
            }
        }
    }

    private suspend fun suspendFunction(): Boolean = withContext(Dispatchers.IO) {
        // Configuración de Retrofit
        val apiUrl = setupRetrofit()

        var version = ""
        if (DATABASE_VERSION > 1) {
            version = "-v$DATABASE_VERSION"
        }

        val dbLocInst = apiService().getDbLocation(apiUrl = apiUrl, version = version)

        dbLocInst.enqueue(object : Callback<Any?> {
            override fun onResponse(call: Call<Any?>, response: Response<Any?>) {
                val resp = response.body()
                if (resp == null) {
                    Log.e(this.javaClass.simpleName, response.raw().toString())
                    sendEvent(
                        status = ProgressStatus.crashed, msg = response.message()
                    )
                    return
                }

                /**
                 * Comprobamos si es una respuesta de Error predefinida
                 */
                if (ErrorObject.isError(resp)) {
                    val errorObject = moshi().adapter(ErrorObject::class.java).fromJsonValue(resp)
                    Log.e(this.javaClass.simpleName, errorObject?.error.toString())
                    sendEvent(
                        status = ProgressStatus.crashed, msg = errorObject?.error.toString()
                    )
                    return
                }

                /**
                 * Comprobamos si es una respuesta del tipo colección de DatabaseData
                 */
                var p = DatabaseData()
                for (dataCont in (resp as Map<*, *>).entries) {
                    p = moshi().adapter(DatabaseData::class.java).fromJsonValue(dataCont.value)
                        ?: continue
                    break
                }

                sendEvent(
                    status = ProgressStatus.finished,
                    result = p,
                    msg = if (p.dbDate.isNotEmpty() && p.dbFile.isNotEmpty()) {
                        context().getString(R.string.success_response)
                    } else {
                        context().getString(R.string.database_name_is_invalid)
                    }
                )
                return
            }

            override fun onFailure(call: Call<Any?>, t: Throwable) {
                Log.e(this.javaClass.simpleName, t.toString())
                sendEvent(
                    status = ProgressStatus.crashed, msg = t.toString()
                )
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
            sendEvent(
                status = ProgressStatus.crashed, msg = context().getString(R.string.url_malformed)
            )
            Log.e(this::class.java.simpleName, e.toString())
            return ""
        }

        // Configuración y refresco de la conexión
        DynamicRetrofit.start(protocol = protocol, host = host)

        return apiUrl
    }

    private fun sendEvent(
        status: ProgressStatus = ProgressStatus.unknown,
        result: DatabaseData = DatabaseData(),
        msg: String = "",
    ) {
        onEvent.invoke(
            DbLocationResult(
                status = status, result = result, msg = msg
            )
        )
    }
}