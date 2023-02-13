package com.dacosys.warehouseCounter.retrofit.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.misc.Statics.Companion.Token
import com.dacosys.warehouseCounter.misc.Statics.Companion.cleanToken
import com.dacosys.warehouseCounter.model.error.ErrorObject
import com.dacosys.warehouseCounter.model.token.TokenObject
import com.dacosys.warehouseCounter.model.user.UserAuthData
import com.dacosys.warehouseCounter.retrofit.result.RequestResult
import com.dacosys.warehouseCounter.retrofit.result.ResultStatus
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * Obtiene un nuevo Token a partir de la configuración guardada
 */

class GetToken(private val onEvent: (RequestResult) -> Unit) {

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun execute(force: Boolean) {
        if (force || !preRequirements()) {
            scope.launch { doInBackground() }
        } else {
            onEvent(RequestResult(ResultStatus.SUCCESS, msg = context().getString(R.string.ok)))
        }
    }

    private fun preRequirements(): Boolean {
        val now = Calendar.getInstance().time
        val tokenDate = getTokenDate()

        if ((Token.token == "" || tokenDate < now).not()) {
            return Token.token != "" && tokenDate > now
        }
        return false
    }

    private var deferred: Deferred<Boolean>? = null
    private suspend fun doInBackground(): Boolean {
        var result = false
        coroutineScope {
            deferred = async { suspendFunction() }
            result = deferred?.await() ?: false
        }
        return result
    }

    private suspend fun suspendFunction(): Boolean = withContext(Dispatchers.IO) {
        val sv = settingViewModel()
        val baseUrl: String
        var apiUrl = ""

        try {
            val url = URL(sv.urlPanel)
            baseUrl = "${url.protocol}://${url.host}/"
            if (url.path.isNotEmpty()) apiUrl = "${url.path}/"
        } catch (e: MalformedURLException) {
            onEvent.invoke(
                RequestResult(
                    ResultStatus.ERROR,
                    context().getString(R.string.url_malformed)
                )
            )

            Log.e(this::class.java.simpleName, e.toString())
            return@withContext false
        }

        Log.d(this::class.java.simpleName,
            "Base URL: $baseUrl (Api URL: ${apiUrl.ifEmpty { "Vacío" }})"
        )

        /** Limpiamos el token actual antes de solicitar uno nuevo. **/
        cleanToken()

        val body = UserAuthData.getUserAuthData()

        val tokenInst = apiService().getToken(apiUrl = apiUrl, body = body)

        Log.i(this::class.java.simpleName, moshi().adapter(UserAuthData::class.java).toJson(body))

        tokenInst.enqueue(object : Callback<Any?> {
            override fun onResponse(call: Call<Any?>, response: Response<Any?>) {
                val resp = response.body()
                if (resp == null) {
                    Log.e(this.javaClass.simpleName, response.message())

                    onEvent.invoke(RequestResult(ResultStatus.ERROR, response.message()))
                    return
                }

                /**
                 * Comprobamos si es una respuesta de Error predefinida
                 */
                if (ErrorObject.isError(resp)) {
                    val errorObject = moshi().adapter(ErrorObject::class.java).fromJsonValue(resp)
                    Log.e(this.javaClass.simpleName, errorObject?.error.toString())
                    onEvent.invoke(RequestResult(ResultStatus.ERROR, errorObject?.error.toString()))
                    return
                }

                /**
                 * Comprobamos si es una respuesta del tipo Token
                 */
                if (TokenObject.isToken(resp)) {
                    val tokenObject = moshi().adapter(TokenObject::class.java).fromJsonValue(resp)

                    onEvent.invoke(
                        RequestResult(
                            ResultStatus.SUCCESS,
                            context().getString(R.string.successful_connection)
                        )
                    )

                    if (tokenObject != null) Token = tokenObject
                    return
                }

                Log.e(this.javaClass.simpleName, resp.toString())
                onEvent.invoke(RequestResult(ResultStatus.ERROR, resp.toString()))
            }

            override fun onFailure(call: Call<Any?>, t: Throwable) {
                Log.e(this.javaClass.simpleName, t.toString())

                onEvent.invoke(RequestResult(ResultStatus.ERROR, t.toString()))
            }
        })

        return@withContext true
    }

    private fun getTokenDate(): Date {
        // Formato que viene en el Json 2022-11-11 11:11:11
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val cal = try {
            sdf.parse(Token.expiration) ?: Date(Long.MIN_VALUE)
        } catch (e: java.lang.Exception) {
            Date(Long.MIN_VALUE)
        }
        return cal
    }
}