package com.dacosys.warehouseCounter.data.ktor.v1.functions

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV1
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v1.dto.ptlOrder.ApiResponse.Companion.RESULT_OK
import com.dacosys.warehouseCounter.data.ktor.v1.functions.GetToken.Companion.Token
import com.dacosys.warehouseCounter.data.ktor.v1.impl.APIServiceImpl.Companion.validUrl
import com.dacosys.warehouseCounter.data.ktor.v1.service.RequestResult
import com.dacosys.warehouseCounter.data.ktor.v1.service.ResultStatus
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*
import kotlin.concurrent.thread

class BlinkOneItem(
    private val itemId: Long,
    private val warehouseAreaId: Long,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (Boolean) -> Unit = { },
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
        val body = com.dacosys.warehouseCounter.data.ktor.v1.dto.apiParam.ApiParam(
            userToken = Token.token,
            ptlQuery = com.dacosys.warehouseCounter.data.ktor.v1.dto.apiParam.PtlQuery(
                itemId = itemId,
                warehouseAreaId = warehouseAreaId
            )
        )
        apiServiceV1.blinkOneItem(body = body, callback = {
            if (it.result == RESULT_OK) sendEvent(it.details, SnackBarType.SUCCESS)
            else sendEvent(context.getString(R.string.invalid_response), SnackBarType.ERROR)
        })
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)
        if (event.snackBarType in getFinish()) {
            onFinish.invoke(event.snackBarType == SnackBarType.SUCCESS)
        }
    }
}
