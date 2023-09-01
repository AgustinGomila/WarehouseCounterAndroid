package com.dacosys.warehouseCounter.ktor.v2.functions.order

import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiServiceV2
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderMovePayload
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import kotlinx.coroutines.*

class MoveOrder(
    private val order: OrderMovePayload,
    private val onFinish: (OrderResponse?) -> Unit = { },
    private val onEvent: (SnackBarEventData) -> Unit = { },
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

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
        ktorApiServiceV2.moveOrder(
            payload = order,
            callback = {
                if (it.onEvent != null) onEvent(it.onEvent)
                if (it.response != null) onFinish(it.response)
            })
    }
}
