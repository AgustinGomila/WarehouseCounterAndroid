package com.example.warehouseCounter.data.ktor.v2.functions.itemCode

import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV2
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.data.ktor.v2.dto.item.ItemCodePayload
import com.example.warehouseCounter.data.ktor.v2.dto.item.ItemCodeResponse
import com.example.warehouseCounter.misc.utils.NetworkState
import com.example.warehouseCounter.ui.snackBar.SnackBarEventData
import com.example.warehouseCounter.ui.snackBar.SnackBarType
import com.example.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SendItemCode
/**
 * Get a [ItemCodePayload] by his ID
 *
 * @property payload ItemCodePayload.
 * @property onEvent Event to update the state of the UI according to the progress of the operation.
 * @property onFinish If the operation is successful it returns a [ItemCodePayload] else null.
 */(
    private val payload: ItemCodePayload,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ItemCodeResponse?) -> Unit
) {

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: ItemCodeResponse? = null

    fun execute() {
        if (!NetworkState.isOnline()) {
            sendEvent(context.getString(R.string.connection_error), SnackBarType.ERROR)
            return
        }
        sendEvent(context.getString(R.string.sending_item_codes_), SnackBarType.RUNNING)
        scope.launch {
            coroutineScope {
                withContext(Dispatchers.IO) { suspendFunction() }
            }
        }
    }

    fun cancel() {
        if (scope.isActive) scope.cancel()
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        apiServiceV2.sendItemCode(payload = payload, callback = {
            if (it.response != null) r = it.response
            if (it.onEvent != null) sendEvent(it.onEvent)
            else {
                if (r != null) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
                else sendEvent(context.getString(R.string.no_results), SnackBarType.INFO)
            }
        })
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)
        if (event.snackBarType in getFinish() || SnackBarType.INFO.equals(event.snackBarType)) {
            onFinish.invoke(r)
        }
    }
}
