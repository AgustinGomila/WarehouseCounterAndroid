package com.dacosys.warehouseCounter.ktor.functions

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiService
import com.dacosys.warehouseCounter.dto.user.AuthData
import com.dacosys.warehouseCounter.dto.user.UserAuthData.Companion.userAuthTag
import com.dacosys.warehouseCounter.ktor.APIServiceImpl.Companion.validUrl
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.network.RequestResult
import com.dacosys.warehouseCounter.network.ResultStatus
import com.dacosys.warehouseCounter.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.room.entity.user.User
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import kotlin.concurrent.thread

class SendItemCode(
    private val itemCodeArray: ArrayList<ItemCode>,
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
                sendEvent(context.getString(R.string.invalid_or_expired_token), SnackBarType.ERROR)
            }
        }

        if (!validUrl()) {
            sendEvent(context.getString(R.string.invalid_url), SnackBarType.ERROR)
            return
        }

        Statics.getCurrentUser { user ->
            if (user != null) {
                currentUser = user

                // Chequeamos que el Token sea válido
                thread { GetToken { onEvent(it) }.execute(false) }
            } else {
                // Token inválido.
                sendEvent(context.getString(R.string.invalid_user), SnackBarType.ERROR)
            }
        }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        ktorApiService.sendItemCode(body = getBody(), callback = {
            // Actualizamos los ItemCode enviados
            updateTransferred()
        })
    }

    private val itemCodeToUpdate: ArrayList<ItemCode> = ArrayList()

    /**
     * Update transferred
     * Actualizar los ItemCode enviados en la base de datos local
     */
    private fun updateTransferred() {
        for (itemCode in itemCodeToUpdate) {
            ItemCodeCoroutines().updateTransferred(
                itemId = itemCode.itemId ?: 0L, code = itemCode.code ?: ""
            )
        }
        onEvent.invoke(SnackBarEventData(context.getString(R.string.ok), SnackBarType.SUCCESS))
    }

    private fun getBody(): JSONObject {
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

        // Todos los ItemCodes ////////////
        val icArrayJson = JSONObject()
        for ((index, itemCode) in itemCodeArray.withIndex()) {
            icArrayJson.put("itemCode$index", Json.encodeToString(itemCode))

            // Actualizamos la lista de códigos a transferir
            itemCodeToUpdate.add(itemCode)
        }
        jsonParam.put("itemCodes", icArrayJson)
        // Fin Todos los ItemCodes ////////

        return jsonParam
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)
    }
}