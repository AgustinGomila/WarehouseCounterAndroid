package com.dacosys.warehouseCounter.data.ktor.v1.functions

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV1
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.data.ktor.v1.dto.user.AuthData
import com.dacosys.warehouseCounter.data.ktor.v1.dto.user.UserAuthData.Companion.USER_AUTH_KEY
import com.dacosys.warehouseCounter.data.ktor.v1.impl.APIServiceImpl.Companion.validUrl
import com.dacosys.warehouseCounter.data.ktor.v1.service.RequestResult
import com.dacosys.warehouseCounter.data.ktor.v1.service.ResultStatus
import com.dacosys.warehouseCounter.data.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.data.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.data.room.entity.user.User
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import org.json.JSONObject
import kotlin.concurrent.thread

class SendItemCode(
    private val itemCodeArray: ArrayList<ItemCode>,
    private val onEvent: (SnackBarEventData) -> Unit = { },
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private lateinit var currentUser: User

    fun execute() {
        fun onGetTokenResult(it: RequestResult) {
            if (it.status == ResultStatus.SUCCESS) {
                scope.launch {
                    coroutineScope {
                        withContext(Dispatchers.IO) { suspendFunction() }
                    }
                }
            } else {
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
                thread { GetToken { onGetTokenResult(it) }.execute(false) }
            } else {
                sendEvent(context.getString(R.string.invalid_user), SnackBarType.ERROR)
            }
        }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        apiServiceV1.sendItemCode(body = getBody(), callback = {
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
            ItemCodeCoroutines.updateTransferred(
                itemId = itemCode.itemId ?: 0L, code = itemCode.code ?: ""
            )
        }
        sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
    }

    private fun getBody(): JSONObject {
        // BODY ////////////////////////////
        val jsonParam = JSONObject()

        // User Auth DATA //////////////////
        jsonParam.put(USER_AUTH_KEY, AuthData().apply {
            username = currentUser.name
            password = currentUser.password ?: ""
        })

        // Collector DATA //////////////////
        val collectorData = Statics.getDeviceData()
        jsonParam.put("collectorData", collectorData)

        // Todos los ItemCodes ////////////
        val icArrayJson = JSONObject()
        for ((index, itemCode) in itemCodeArray.withIndex()) {
            icArrayJson.put("itemCode$index", json.encodeToString(itemCode))

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
