package com.dacosys.warehouseCounter.data.ktor.v2.functions.order

import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV2
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.misc.utils.NetworkState
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.*

class CreateOrder(
    private val payload: ArrayList<OrderRequest>,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ids: ArrayList<Long>, successFiles: ArrayList<String>) -> Unit,
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun execute() {
        if (!NetworkState.isOnline()) {
            sendEvent(context.getString(R.string.connection_error), SnackBarType.ERROR)
            return
        }
        sendEvent(context.getString(R.string.creating_order_), SnackBarType.RUNNING)
        scope.launch {
            coroutineScope {
                withContext(Dispatchers.IO) { suspendFunction() }
            }
        }
    }

    fun cancel() {
        if (scope.isActive) scope.cancel()
    }

    /** Save the file names of the orders sent correctly */
    private val successFiles: ArrayList<String> = ArrayList()
    private val ids: ArrayList<Long> = ArrayList()

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
            if (BuildConfig.DEBUG) println(json.encodeToString(OrderRequest.serializer(), order))

            apiServiceV2.createOrder(payload = order, callback = {
                if (it.onEvent != null) sendEvent(it.onEvent)

                var id: Long = 0
                if (it.response != null) id = it.response.id

                if (id > 0) {
                    ids.add(id)
                    successFiles.add(order.filename)
                }
                setProcessState(index == payload.lastIndex)
            })
        }

        val startTime = System.currentTimeMillis()
        while (!getProcessState()) {
            if (System.currentTimeMillis() - startTime == (settingsVm.connectionTimeout * 1000).toLong()) {
                sendEvent(context.getString(R.string.connection_timeout), SnackBarType.ERROR)
                setProcessState(true)
            }
        }

        onFinish(ids, successFiles)
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)
    }
}
