package com.dacosys.warehouseCounter.data.room.entity.orderRequest

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.room.dao.orderRequest.OrderRequestCoroutines
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType

class RepackOrder(order: OrderResponse, private val onEvent: (SnackBarEventData) -> Unit, onNewId: (Long) -> Unit) {
    init {
        sendEvent(
            String.format(
                context.getString(R.string.client_description),
                order.clientId ?: context.getString(R.string.no_client),
                Statics.lineSeparator,
                order.description
            ), SnackBarType.INFO
        )

        val orderRequest = OrderRequest(
            clientId = order.clientId ?: 0,
            creationDate = order.rowCreationDate, // TODO: Fecha actual
            description = order.description,
            orderTypeDescription = OrderRequestType.packaging.description,
            orderTypeId = OrderRequestType.packaging.id.toInt(),
            resultAllowDiff = if (order.resultAllowDiff == true) 1 else 0,
            resultAllowMod = if (order.resultAllowMod == true) 1 else 0,
            resultDiffProduct = if (order.resultDiffProduct == true) 1 else 0,
            resultDiffQty = if (order.resultDiffQty == true) 1 else 0,
            startDate = order.startDate,
            userId = Statics.currentUserId,
        )

        OrderRequestCoroutines.add(
            orderRequest = orderRequest,
            onResult = { newId ->
                if (newId != null) {
                    orderRequest.orderRequestId = newId
                    OrderRequestCoroutines.update(
                        orderRequest = orderRequest.toKtor,
                        contents = order.contentToKtor(),
                        onResult = {
                            if (it) {
                                onNewId(newId)
                            } else {
                                sendEvent(context.getString(R.string.error_when_updating_the_order), SnackBarType.ERROR)
                            }
                        })
                } else {
                    sendEvent(context.getString(R.string.error_when_creating_the_order), SnackBarType.ERROR)
                }
            })
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        onEvent(event)
    }
}
