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
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest as OrderRequestKtor

class AddOrder {
    private val onEvent: (SnackBarEventData) -> Unit

    constructor(
        client: Client?,
        description: String,
        orderRequestType: OrderRequestType,
        onEvent: (SnackBarEventData) -> Unit,
        onNewId: (Long) -> Unit
    ) : this(
        clientId = client?.clientId,
        clientName = client?.name,
        description = description,
        orderRequestType = orderRequestType,
        onEvent = onEvent,
        onNewId = onNewId
    )

    constructor(
        clientId: Long?,
        clientName: String?,
        description: String,
        orderRequestType: OrderRequestType,
        onEvent: (SnackBarEventData) -> Unit,
        onNewId: (Long) -> Unit
    ) : this(
        clientId = clientId,
        clientName = clientName,
        creationDate = DateFormat.format(Statics.DATE_FORMAT, System.currentTimeMillis()).toString(),
        description = description,
        orderRequestType = orderRequestType,
        startDate = DateFormat.format(Statics.DATE_FORMAT, System.currentTimeMillis()).toString(),
        userId = Statics.currentUserId,
        onEvent = onEvent,
        onNewId = onNewId
    )

    constructor(
        order: OrderRequestKtor,
        onEvent: (SnackBarEventData) -> Unit,
        onNewId: (Long) -> Unit
    ) : this(
        orderRequestId = order.orderRequestId,
        clientId = order.clientId,
        completed = order.completed ?: false,
        creationDate = order.creationDate,
        description = order.description,
        externalId = order.externalId,
        finishDate = order.finishDate,
        orderRequestType = OrderRequestType.getById(order.orderTypeId),
        resultAllowDiff = order.resultAllowDiff ?: true,
        resultAllowMod = order.resultAllowMod ?: true,
        resultDiffProduct = order.resultDiffProduct ?: true,
        resultDiffQty = order.resultDiffQty ?: true,
        startDate = order.startDate,
        userId = order.userId ?: 0L,
        zone = order.zone,
        onEvent = onEvent,
        onNewId = onNewId
    )

    constructor(
        orderRequestId: Long? = null,
        clientId: Long? = null,
        clientName: String? = "",
        completed: Boolean = false,
        creationDate: String? = "",
        description: String,
        externalId: String = "",
        finishDate: String? = "",
        orderRequestType: OrderRequestType,
        resultAllowDiff: Boolean = true,
        resultAllowMod: Boolean = true,
        resultDiffProduct: Boolean = true,
        resultDiffQty: Boolean = true,
        startDate: String? = "",
        userId: Long,
        zone: String? = "",
        onEvent: (SnackBarEventData) -> Unit,
        onNewId: (Long) -> Unit
    ) {
        this.onEvent = onEvent
        sendEvent(
            String.format(
                context.getString(R.string.client_description),
                clientName ?: context.getString(R.string.no_client),
                Statics.lineSeparator,
                description
            ), SnackBarType.INFO
        )
        val orderRequest = OrderRequest(
            orderRequestId = orderRequestId ?: 0L,
            clientId = clientId ?: 0L,
            completed = if (completed) 1 else 0,
            creationDate = creationDate.toString(),
            description = description,
            externalId = externalId,
            finishDate = finishDate.toString(),
            orderTypeDescription = orderRequestType.description,
            orderTypeId = orderRequestType.id.toInt(),
            resultAllowDiff = if (resultAllowDiff) 1 else 0,
            resultAllowMod = if (resultAllowMod) 1 else 0,
            resultDiffProduct = if (resultDiffProduct) 1 else 0,
            resultDiffQty = if (resultDiffQty) 1 else 0,
            startDate = startDate.toString(),
            userId = userId,
            zone = zone.toString()
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
