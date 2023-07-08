package com.dacosys.warehouseCounter.ktor.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiService
import com.dacosys.warehouseCounter.dto.token.TokenObject
import com.dacosys.warehouseCounter.dto.user.AuthData
import com.dacosys.warehouseCounter.dto.user.UserAuthData
import com.dacosys.warehouseCounter.ktor.APIServiceImpl.Companion.validUrl
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.network.RequestResult
import com.dacosys.warehouseCounter.network.ResultStatus
import com.dacosys.warehouseCounter.room.entity.user.User
import kotlinx.coroutines.*
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

    private lateinit var currentUser: User

    fun execute(force: Boolean) {
        // Función que prosigue al resultado de getCurrentUser
        fun onEvent() {

            // ¿Es necesario pedir un Token nuevo?
            if (force || !isTokenValid()) {

                // Pedimos el nuevo Token
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

        Statics.getCurrentUser { user ->
            if (user != null) {
                currentUser = user

                // Continuamos con la ejecución...
                onEvent()
            } else {
                // Usuario inválido.
                onEvent.invoke(RequestResult(ResultStatus.ERROR, context.getString(R.string.invalid_user)))
            }
        }
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

        ktorApiService.getToken(body = getBody(), callback = {
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