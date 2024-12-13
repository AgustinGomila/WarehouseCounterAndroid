package com.dacosys.warehouseCounter.data.ktor.v2.functions.order

import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.getCompletedPath
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.removeOrdersFiles
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType

class SendOrder(
    private val orders: ArrayList<OrderRequest>,
    private val onEvent: (SnackBarEventData) -> Unit,
    private val onFinish: (ArrayList<Long>) -> Unit
) {
    init {
        // TODO: Revisar y probar envío de pedidos

        val newOrders = orders.mapNotNull { if (it.roomId == null) it else null }
        val oldOrders = orders.mapNotNull { if (it.roomId != null) it else null }

        CreateOrder(
            payload = ArrayList(newOrders),
            onEvent = { sendEvent(it.text, it.snackBarType) },
            onFinish = { ids, successFiles ->
                if (successFiles.isNotEmpty()) {
                    removeOrders(ids, successFiles)
                }
            }
        ).execute()

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

    private fun removeOrders(ids: ArrayList<Long>, filesToRemove: ArrayList<String>) {
        /** We delete the files of the orders sent */
        removeOrdersFiles(
            path = getCompletedPath(),
            filesToRemove = filesToRemove,
            sendEvent = { eventData ->

                if (eventData.snackBarType == SnackBarType.SUCCESS) {
                    // TODO: Ver esto, qué hacer con los pedidos enviados?

                    /** We remove the reference to the order in room database. */

                    // OrderRequestCoroutines.removeById(
                    //     idList = orders.mapNotNull { orderRequest -> orderRequest.roomId },
                    //     onResult = {
                    //         onFinish(ids)
                    //     })

                    onFinish(ids)
                } else {
                    sendEvent(eventData)
                }
            })
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)
    }
}
