package com.dacosys.warehouseCounter.data.ktor.v2.functions.order

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.data.room.dao.orderRequest.OrderRequestCoroutines
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType

class SendOrder(
    private val orders: ArrayList<OrderRequest>,
    private val onEvent: (SnackBarEventData) -> Unit
) {
    init {

        CreateOrder(
            payload = orders,
            onEvent = { sendEvent(it.text, it.snackBarType) },
            onFinish = { successFiles ->
                if (successFiles.isNotEmpty()) {

                    /** We delete the files of the orders sent */
                    OrderRequest.removeCountFiles(
                        path = Statics.getCompletedPath(),
                        filesToRemove = successFiles,
                        sendEvent = { eventData ->

                            if (eventData.snackBarType == SnackBarType.SUCCESS) {

                                /** We remove the reference to the order in room database,
                                 * and we fill the list adapter at the end. */
                                OrderRequestCoroutines.removeById(
                                    idList = orders.mapNotNull { orderRequest -> orderRequest.orderRequestId },
                                    onResult = { sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS) })
                            } else {
                                sendEvent(eventData)
                            }
                        })
                }
            }
        ).execute()
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)
    }
}
