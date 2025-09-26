package com.example.warehouseCounter.data.ktor.v1.functions

import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV1
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.WarehouseCounterApp.Companion.json
import com.example.warehouseCounter.data.io.IOFunc.Companion.getCompletedPath
import com.example.warehouseCounter.data.io.IOFunc.Companion.removeOrdersFiles
import com.example.warehouseCounter.data.ktor.v1.dto.orderRequest.OrderRequest
import com.example.warehouseCounter.data.ktor.v1.dto.user.AuthData
import com.example.warehouseCounter.data.ktor.v1.dto.user.UserAuthData.Companion.USER_AUTH_KEY
import com.example.warehouseCounter.data.ktor.v1.impl.APIServiceImpl.Companion.validUrl
import com.example.warehouseCounter.data.ktor.v1.service.RequestResult
import com.example.warehouseCounter.data.ktor.v1.service.ResultStatus
import com.example.warehouseCounter.misc.CurrentUser
import com.example.warehouseCounter.misc.utils.DeviceData
import com.example.warehouseCounter.ui.snackBar.SnackBarEventData
import com.example.warehouseCounter.ui.snackBar.SnackBarType
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

        thread { GetToken { onGetTokenResult(it) }.execute(false) }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        apiServiceV1.sendOrders(body = getBody(), callback = {
            removeOrdersFiles(
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
            username = CurrentUser.name
            password = CurrentUser.password
        })

        // Collector DATA //////////////////
        val collectorData = DeviceData.getDeviceData()
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
