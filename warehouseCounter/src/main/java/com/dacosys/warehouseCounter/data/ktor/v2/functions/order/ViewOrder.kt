package com.dacosys.warehouseCounter.data.ktor.v2.functions.order

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiServiceV2
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiActionParam
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*

class ViewOrder
/**
 * Get a [OrderResponse] by his ID
 *
 * @property id ID of the rack.
 * @property action List of parameters.
 * @property onEvent Event to update the state of the UI according to the progress of the operation.
 * @property onFinish If the operation is successful it returns a [OrderResponse] else null.
 */(
    private val id: Long,
    private val action: ArrayList<ApiActionParam> = arrayListOf(),
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (OrderResponse?) -> Unit,
) {
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val defaultAction: ArrayList<ApiActionParam>
            get() {
                return arrayListOf(
                    ApiActionParam(
                        action = ACTION_EXPAND,
                        extension = setOf(
                            EXTENSION_ORDER_CONTENT_LIST, EXTENSION_ORDER_SCAN_LOG, EXTENSION_LOCATION
                        )
                    )
                )
            }

        /** Valid extensions and actions for this function */
        const val ACTION_EXPAND = "expand"
        const val EXTENSION_ORDER_CONTENT_LIST = "orderContents"
        const val EXTENSION_ORDER_SCAN_LOG = "orderScanLogs"
        const val EXTENSION_LOCATION = "location"
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: OrderResponse? = null

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
        ktorApiServiceV2.viewOrder(
            id = id,
            action = action,
            callback = {
                if (it.onEvent != null) sendEvent(it.onEvent)
                if (it.response != null) r = it.response
                if (r != null) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
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
