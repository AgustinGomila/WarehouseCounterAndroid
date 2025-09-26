package com.example.warehouseCounter.data.ktor.v2.functions.order

import com.example.warehouseCounter.data.io.IOFunc.Companion.getCompletedPath
import com.example.warehouseCounter.data.io.IOFunc.Companion.removeOrdersFiles
import com.example.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.example.warehouseCounter.data.room.dao.orderRequest.OrderRequestCoroutines
import com.example.warehouseCounter.ui.snackBar.SnackBarEventData
import com.example.warehouseCounter.ui.snackBar.SnackBarType

class SendOrder(
    private val orders: ArrayList<OrderRequest>,
    private val onEvent: (SnackBarEventData) -> Unit,
    private val onFinish: (ArrayList<Long>) -> Unit
) {
    init {
        val newOrders = orders.mapNotNull { if (it.orderRequestId == null || it.orderRequestId == 0L) it else null }
        val oldOrders = orders.mapNotNull { if ((it.orderRequestId ?: 0L) > 0L) it else null }

        if (newOrders.isNotEmpty()) {
            CreateOrder(
                payload = ArrayList(newOrders),
                onEvent = { sendEvent(it.text, it.snackBarType) },
                onFinish = { ids, successFiles ->
                    if (successFiles.isNotEmpty()) {
                        removeOrders(ids, successFiles)
                    }
                }
            ).execute()
        }

        if (oldOrders.isNotEmpty()) {
            UpdateOrder(
                payload = ArrayList(oldOrders),
                onEvent = { sendEvent(it.text, it.snackBarType) },
                onFinish = { successFiles ->
                    if (successFiles.isNotEmpty()) {
                        removeOrders(
                            ids = ArrayList(oldOrders.mapNotNull { it.roomId }),
                            filesToRemove = ArrayList(successFiles.map { it.toString() })
                        )
                    }
                }
            ).execute()
        }
    }

    private fun removeOrders(ids: ArrayList<Long>, filesToRemove: ArrayList<String>) {
        /** We delete the files of the orders sent */
        removeOrdersFiles(
            path = getCompletedPath(),
            filesToRemove = filesToRemove,
            sendEvent = { eventData ->

                if (SnackBarType.SUCCESS.equals(eventData.snackBarType)) {
                    /** We remove the reference to the order in room database. */
                    OrderRequestCoroutines.removeById(
                        idList = orders.mapNotNull { orderRequest -> orderRequest.roomId },
                        onResult = {
                            onFinish(ids)
                        })

                    onFinish(ids)
                } else {
                    sendEvent(eventData)
                }
            })
    }

    private fun sendEvent(msg: String, type: Int) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)
    }
}
