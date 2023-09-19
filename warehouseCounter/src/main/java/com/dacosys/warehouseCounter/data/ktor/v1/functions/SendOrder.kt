package com.dacosys.warehouseCounter.data.ktor.v1.functions

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV1
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.getCompletedPath
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.removeCountFiles
import com.dacosys.warehouseCounter.data.ktor.v1.dto.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.data.ktor.v1.dto.user.AuthData
import com.dacosys.warehouseCounter.data.ktor.v1.dto.user.UserAuthData.Companion.USER_AUTH_KEY
import com.dacosys.warehouseCounter.data.ktor.v1.impl.APIServiceImpl.Companion.validUrl
import com.dacosys.warehouseCounter.data.ktor.v1.service.RequestResult
import com.dacosys.warehouseCounter.data.ktor.v1.service.ResultStatus
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

class SendOrder(
    private val orderRequestArray: ArrayList<OrderRequest>,
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
        apiServiceV1.sendOrders(body = getBody(), callback = {
            removeCountFiles(
                path = getCompletedPath(),
                filesToRemove = successFiles,
                sendEvent = onEvent
            )
        })
    }

    private var successFiles: ArrayList<String> = ArrayList()

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

        // Todos los Pedidos //////////////////
        val orArrayJson = JSONObject()
        for ((index, orderRequest) in orderRequestArray.withIndex()) {
            orArrayJson.put("order$index", json.encodeToString(orderRequest))

            // Guardamos el archivo subido
            successFiles.add(orderRequest.filename)
        }
        jsonParam.put("orders", orArrayJson)
        // Fin Todos los Pedidos //////////////

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
