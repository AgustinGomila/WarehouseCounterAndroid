package com.dacosys.warehouseCounter.retrofit.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.Token
import com.dacosys.warehouseCounter.misc.Statics.Companion.cleanToken
import com.dacosys.warehouseCounter.moshi.error.ErrorObject
import com.dacosys.warehouseCounter.moshi.token.TokenObject
import com.dacosys.warehouseCounter.moshi.user.AuthData
import com.dacosys.warehouseCounter.moshi.user.UserAuthData
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
import com.dacosys.warehouseCounter.retrofit.result.RequestResult
import com.dacosys.warehouseCounter.retrofit.result.ResultStatus
import com.dacosys.warehouseCounter.room.entity.user.User
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

    private lateinit var currentUser: User

    fun execute(force: Boolean) {
        // Función que prosigue al resultado de getCurrentUser
        fun onEvent() {

            // ¿Es necesario pedir un Token nuevo?
            if (force || !preRequirements()) {

                // Pedimos el nuevo Token
                scope.launch {
                    coroutineScope {
                        withContext(Dispatchers.IO) { suspendFunction() }
                    }
                }
            } else {
                onEvent(
                    RequestResult(
                        ResultStatus.SUCCESS, msg = context().getString(R.string.ok)
                    )
                )
            }
        }

        Statics.getCurrentUser { user ->
            if (user != null) {
                currentUser = user

                // Continuamos con la ejecución...
                onEvent()
            } else {
                // Usuario inválido.
                onEvent.invoke(
                    RequestResult(
                        ResultStatus.ERROR, context().getString(R.string.invalid_user)
                    )
                )
            }
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

    private suspend fun suspendFunction(): Boolean = withContext(Dispatchers.IO) {
        // Configuración de Retrofit
        val apiUrl = setupRetrofit()

        /** Limpiamos el token actual antes de solicitar uno nuevo. **/
        cleanToken()

        val body = getBody()

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
            onEvent.invoke(
                RequestResult(
                    ResultStatus.ERROR, context().getString(R.string.url_malformed)
                )
            )
            Log.e(this::class.java.simpleName, e.toString())
            return ""
        }

        // Configuración y refresco de la conexión
        DynamicRetrofit.start(protocol = protocol, host = host)

        return apiUrl
    }

    /**
     * Devuelve las opciones guardadas en la configuración de la app en
     * forma de Json para enviar a la API
     */
    private fun getBody(): UserAuthData {
        return UserAuthData().apply {
            authData = AuthData().apply {
                username = currentUser.name
                password = currentUser.password ?: ""
            }
        }
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