package com.dacosys.warehouseCounter.data.ktor.v1.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV1
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v1.dto.token.TokenObject
import com.dacosys.warehouseCounter.data.ktor.v1.dto.user.AuthData
import com.dacosys.warehouseCounter.data.ktor.v1.dto.user.UserAuthData
import com.dacosys.warehouseCounter.data.ktor.v1.impl.APIServiceImpl.Companion.validUrl
import com.dacosys.warehouseCounter.data.ktor.v1.service.RequestResult
import com.dacosys.warehouseCounter.data.ktor.v1.service.ResultStatus
import com.dacosys.warehouseCounter.misc.CurrentUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Obtiene un nuevo Token a partir de la configuración guardada
 */

class GetToken(private val onEvent: (RequestResult) -> Unit) {

    companion object {
        var Token: TokenObject = TokenObject()

        fun cleanToken() {
            Token = TokenObject("", "")
        }
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun execute(force: Boolean) {
        fun onGetUserResult() {
            if (force || !isTokenValid()) {
                scope.launch {
                    coroutineScope {
                        withContext(Dispatchers.IO) { suspendFunction() }
                    }
                }
            } else {
                onEvent(RequestResult(ResultStatus.SUCCESS, msg = context.getString(R.string.ok)))
            }
        }

        if (!validUrl()) {
            onEvent.invoke(RequestResult(ResultStatus.ERROR, context.getString(R.string.invalid_url)))
            return
        }

        onGetUserResult()
    }

    private fun isTokenValid(): Boolean {
        val now = Calendar.getInstance().time
        val tokenDate = getTokenDate()

        if ((Token.token == "" || tokenDate < now).not()) {
            return Token.token != "" && tokenDate > now
        }
        return false
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        /** Limpiamos el token actual antes de solicitar uno nuevo. **/
        cleanToken()

        apiServiceV1.getToken(body = body, callback = {
            if (it == null) {
                Log.e(this.javaClass.simpleName, "error")
                onEvent.invoke(RequestResult(ResultStatus.ERROR, "error"))
                return@getToken
            }
            Token = it
            Log.i(this.javaClass.simpleName, it.toString())
            onEvent.invoke(RequestResult(ResultStatus.SUCCESS, it.toString()))
        })
    }

    /**
     * Devuelve las opciones guardadas en la configuración de la app en
     * forma de Json para enviar a la API
     */
    private val body by lazy {
        UserAuthData().apply {
            authData = AuthData().apply {
                username = CurrentUser.name
                password = CurrentUser.password
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
