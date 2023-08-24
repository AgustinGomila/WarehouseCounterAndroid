package com.dacosys.warehouseCounter.ktor.v2.functions.itemCode

import android.util.Log
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiServiceV2
import com.dacosys.warehouseCounter.ktor.v2.dto.item.ItemCodePayload
import com.dacosys.warehouseCounter.ktor.v2.dto.item.ItemCodeResponse
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*


class SendItemCode
/**
 * Get a [ItemCodePayload] by his ID
 *
 * @property id ID of the itemCodePayload.
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
        ktorApiServiceV2.sendItemCode(payload = payload, callback = {
            if (BuildConfig.DEBUG) Log.d(javaClass.simpleName, it.toString())
            r = it
            if (r != null) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
            else sendEvent(
                context.getString(R.string.an_error_occurred_while_trying_to_add_the_item_code),
                SnackBarType.INFO
            )
        })
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)

        if (event.snackBarType in getFinish() || event.snackBarType == SnackBarType.INFO) {
            onFinish.invoke(r)
        }
    }
}
