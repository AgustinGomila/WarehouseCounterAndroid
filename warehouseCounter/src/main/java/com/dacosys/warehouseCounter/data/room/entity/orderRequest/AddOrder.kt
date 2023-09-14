package com.dacosys.warehouseCounter.data.room.entity.orderRequest

import android.text.format.DateFormat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.data.room.dao.orderRequest.OrderRequestCoroutines
import com.dacosys.warehouseCounter.data.room.entity.client.Client
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType

class AddOrder(
    client: Client?,
    description: String,
    orderRequestType: OrderRequestType,
    private val onEvent: (SnackBarEventData) -> Unit,
    onNewId: (Long) -> Unit
) {
    init {
        sendEvent(
            String.format(
                context.getString(R.string.client_description),
                client?.name ?: context.getString(R.string.no_client),
                Statics.lineSeparator,
                description
            ), SnackBarType.INFO
        )

        val orderRequest = OrderRequest(
            clientId = client?.clientId ?: 0,
            creationDate = DateFormat.format(Statics.DATE_FORMAT, System.currentTimeMillis()).toString(),
            description = description,
            orderTypeDescription = orderRequestType.description,
            orderTypeId = orderRequestType.id.toInt(),
            resultAllowDiff = 1,
            resultAllowMod = 1,
            resultDiffProduct = 1,
            resultDiffQty = 1,
            startDate = DateFormat.format(Statics.DATE_FORMAT, System.currentTimeMillis()).toString(),
            userId = Statics.currentUserId,
        )

        OrderRequestCoroutines.add(
            orderRequest = orderRequest,
            onResult = { newId ->
                if (newId != null) {
                    onNewId(newId)
                } else {
                    sendEvent(
                        context.getString(R.string.error_when_creating_the_order),
                        SnackBarType.ERROR
                    )
                }
            })
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        onEvent(event)
    }
}
