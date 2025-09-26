package com.example.warehouseCounter.data.ktor.v1.functions

import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV1
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.PickItem
import com.example.warehouseCounter.data.ktor.v1.functions.GetToken.Companion.Token
import com.example.warehouseCounter.data.ktor.v1.impl.APIServiceImpl.Companion.validUrl
import com.example.warehouseCounter.data.ktor.v1.service.RequestResult
import com.example.warehouseCounter.data.ktor.v1.service.ResultStatus
import com.example.warehouseCounter.ui.snackBar.SnackBarEventData
import com.example.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

class PickManual(
    private val orderId: Long,
    private val warehouseAreaId: Long,
    private val itemId: Long,
    private val qty: Int,
    private val onFinish: (ArrayList<PickItem>) -> Unit,
    private val onEvent: (SnackBarEventData) -> Unit = { },
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    var r: ArrayList<PickItem> = ArrayList()

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
        val body = com.example.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam(
            userToken = Token.token,
            ptlQuery = com.example.warehouseCounter.data.ktor.v1.dto.apiParam.PtlQuery(
                orderId = orderId,
                warehouseAreaId = warehouseAreaId,
                itemId = itemId,
                qty = qty
            )
        )
        apiServiceV1.pickManual(body = body, callback = {
            it.contents.forEach { s -> r.add(s) }
            if (r.any()) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
            else sendEvent(it.details, SnackBarType.INFO)
        })
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)
        if (event.snackBarType in SnackBarType.getFinish() || SnackBarType.INFO.equals(event.snackBarType)) {
            onFinish.invoke(r)
        }
    }
}
