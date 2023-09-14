package com.dacosys.warehouseCounter.data.ktor.v2.functions.order

import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiServiceV2
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest.CREATOR.toUpdatePayload
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.*

class UpdateOrder(
    private val payload: ArrayList<OrderRequest>,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (successIdList: ArrayList<Long>) -> Unit = { },
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun execute() {
        if (!Statics.isOnline()) {
            sendEvent(context.getString(R.string.connection_error), SnackBarType.ERROR)
            return
        }
        sendEvent(context.getString(R.string.updating_order_), SnackBarType.RUNNING)
        scope.launch {
            coroutineScope {
                withContext(Dispatchers.IO) { suspendFunction() }
            }
        }
    }

    fun cancel() {
        if (scope.isActive) scope.cancel()
    }

    /** Save the Ids of the orders updated correctly */
    private val successIdList: ArrayList<Long> = ArrayList()

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        var isDone = false
        for ((index, order) in payload.withIndex()) {
            if (BuildConfig.DEBUG) println(json.encodeToString(OrderRequest.serializer(), order))

            ktorApiServiceV2.updateOrder(payload = toUpdatePayload(order), callback = {
                if (it.onEvent != null) sendEvent(it.onEvent)

                var id: Long = 0
                if (it.response != null) id = it.response.id

                if (id > 0) successIdList.add(id)
                isDone = index == payload.lastIndex
            })
        }

        val startTime = System.currentTimeMillis()
        while (!isDone) {
            if (System.currentTimeMillis() - startTime == settingViewModel.connectionTimeout.toLong()) {
                sendEvent(context.getString(R.string.connection_timeout), SnackBarType.ERROR)
                isDone = true
            }
        }

        onFinish(successIdList)
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)
    }
}
