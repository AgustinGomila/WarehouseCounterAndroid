package com.dacosys.warehouseCounter.data.room.entity.orderRequest

import android.text.format.DateFormat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.room.dao.orderRequest.OrderRequestCoroutines
import com.dacosys.warehouseCounter.data.room.entity.client.Client
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestContent as OrderRequestContentKtor

class AddOrder {
    private val onEvent: (SnackBarEventData) -> Unit

    constructor(
        orderResponse: OrderResponse,
        onEvent: (SnackBarEventData) -> Unit,
        onNewId: (Long, String) -> Unit
    ) {
        val orderRequestId = orderResponse.id
        val clientId = orderResponse.clientId
        val clientName = ""
        val completed = orderResponse.completed.toBooleanStrict()
        val creationDate = orderResponse.rowCreationDate
        val description = orderResponse.description
        val externalId = orderResponse.externalId
        val finishDate = orderResponse.finishDate
        val orderRequestType = OrderRequestType.getById(orderResponse.orderTypeId)
        val resultAllowDiff = orderResponse.resultAllowDiff ?: true
        val resultAllowMod = orderResponse.resultAllowMod ?: true
        val resultDiffProduct = orderResponse.resultDiffProduct ?: true
        val resultDiffQty = orderResponse.resultDiffQty ?: true
        val startDate =
            if (!orderResponse.startDate.isNullOrEmpty()) orderResponse.startDate.toString()
            else DateFormat.format(Statics.DATE_FORMAT, System.currentTimeMillis()).toString()
        val userId = orderResponse.collectorUserId ?: Statics.currentUserId
        val zone = orderResponse.zone

        this.onEvent = onEvent
        sendEvent(
            String.format(
                context.getString(R.string.client_description),
                clientName,
                Statics.lineSeparator,
                description
            ), SnackBarType.INFO
        )

        val orRoom = OrderRequest(
            orderRequestId = orderRequestId,
            clientId = clientId ?: 0L,
            completed = if (completed) 1 else 0,
            creationDate = creationDate,
            description = description,
            externalId = externalId,
            finishDate = finishDate ?: "",
            orderTypeDescription = orderRequestType.description,
            orderTypeId = orderRequestType.id.toInt(),
            resultAllowDiff = if (resultAllowDiff) 1 else 0,
            resultAllowMod = if (resultAllowMod) 1 else 0,
            resultDiffProduct = if (resultDiffProduct) 1 else 0,
            resultDiffQty = if (resultDiffQty) 1 else 0,
            startDate = startDate,
            userId = userId,
            zone = zone
        )

        addOrder(
            orRoom = orRoom,
            contents = orderResponse.contentToKtor(),
            orderRequestId = orderRequestId,
            onNewId = onNewId
        )
    }

    constructor(
        client: Client?,
        description: String,
        orderRequestType: OrderRequestType,
        onEvent: (SnackBarEventData) -> Unit,
        onNewId: (Long, String) -> Unit
    ) : this(
        clientId = client?.clientId ?: 0L,
        clientName = client?.name ?: context.getString(R.string.no_client),
        description = description,
        orderRequestType = orderRequestType,
        onEvent = onEvent,
        onNewId = onNewId
    )

    constructor(
        clientId: Long,
        clientName: String,
        description: String,
        orderRequestType: OrderRequestType,
        onEvent: (SnackBarEventData) -> Unit,
        onNewId: (Long, String) -> Unit
    ) {
        this.onEvent = onEvent
        sendEvent(
            String.format(
                context.getString(R.string.client_description),
                clientName,
                Statics.lineSeparator,
                description
            ), SnackBarType.INFO
        )
        val orRoom = OrderRequest(
            clientId = clientId,
            creationDate = DateFormat.format(Statics.DATE_FORMAT, System.currentTimeMillis()).toString(),
            description = description,
            orderTypeDescription = orderRequestType.description,
            orderTypeId = orderRequestType.id.toInt(),
            resultAllowDiff = 1,
            resultAllowMod = 1,
            resultDiffProduct = 1,
            resultDiffQty = 1,
            startDate = DateFormat.format(Statics.DATE_FORMAT, System.currentTimeMillis()).toString(),
            userId = Statics.currentUserId
        )

        addOrder(
            orRoom = orRoom,
            contents = listOf(),
            orderRequestId = 0L,
            onNewId = onNewId
        )
    }

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

    private fun addOrder(
        orRoom: OrderRequest,
        contents: List<OrderRequestContentKtor>,
        orderRequestId: Long,
        onNewId: (Long, String) -> Unit
    ) {
        setProcessState(false)
        var newId = 0L
        var newFilename = ""

        OrderRequestCoroutines.add(
            orderRequest = orRoom,
            onResult = {
                if (it != null) {
                    newId = it
                    OrderRequestCoroutines.getByIdAsKtor(
                        id = newId,
                        filename = "",
                        onResult = { newOrder ->
                            if (newOrder != null) {

                                newOrder.orderRequestId = orderRequestId
                                newOrder.roomId = newId
                                newOrder.contents = contents

                                OrderRequestCoroutines.update(
                                    orderRequest = newOrder,
                                    onEvent = { },
                                    onFilename = { filename -> newFilename = filename })
                            }
                            setProcessState(true)
                        }
                    )
                } else {
                    sendEvent(context.getString(R.string.error_when_creating_the_order), SnackBarType.ERROR)
                    setProcessState(true)
                }
            })

        val startTime = System.currentTimeMillis()
        while (!getProcessState()) {
            if (System.currentTimeMillis() - startTime == (settingsVm.connectionTimeout * 1000).toLong()) {
                setProcessState(true)
            }
        }

        onNewId(newId, newFilename)
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        onEvent(event)
    }
}
