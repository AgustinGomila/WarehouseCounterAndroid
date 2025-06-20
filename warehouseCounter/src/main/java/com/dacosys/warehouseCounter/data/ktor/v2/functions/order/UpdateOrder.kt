package com.dacosys.warehouseCounter.data.ktor.v2.functions.order

import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV2
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest.CREATOR.toUpdatePayload
import com.dacosys.warehouseCounter.misc.utils.NetworkState
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateOrder(
    private val payload: ArrayList<OrderRequest>,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (successIdList: ArrayList<Long>) -> Unit = { },
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun execute() {
        if (!NetworkState.isOnline()) {
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

    @get:Synchronized
    private var isProcessDone = false

    @Synchronized
    private fun getProcessState(): Boolean {
        return isProcessDone
    }

    @Synchronized
    private fun setProcessState(state: Boolean) {
        isProcessDone = state
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        setProcessState(false)

        for ((index, order) in payload.withIndex()) {

            val orderId = order.orderRequestId
            if (orderId != null) {

                if (BuildConfig.DEBUG) println(json.encodeToString(OrderRequest.serializer(), order))

                apiServiceV2.updateOrder(
                    id = orderId,
                    payload = toUpdatePayload(order),
                    callback = {
                        if (it.onEvent != null) sendEvent(it.onEvent)

                        var id: Long = 0
                        if (it.response != null) id = it.response.id

                        if (id > 0) successIdList.add(id)
                        setProcessState(index == payload.lastIndex)
                    })
            } else {
                sendEvent(context.getString(R.string.unknown_error), SnackBarType.ERROR)
                setProcessState(true)
            }
        }

        val startTime = System.currentTimeMillis()
        while (!getProcessState()) {
            if (System.currentTimeMillis() - startTime == (settingsVm.connectionTimeout * 1000).toLong()) {
                sendEvent(context.getString(R.string.connection_timeout), SnackBarType.ERROR)
                setProcessState(true)
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
