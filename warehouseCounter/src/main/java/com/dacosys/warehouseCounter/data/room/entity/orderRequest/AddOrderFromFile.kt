package com.dacosys.warehouseCounter.data.room.entity.orderRequest

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.completePendingPath
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.data.room.dao.orderRequest.OrderRequestCoroutines
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlin.io.path.Path
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest as OrderRequestKtor

class AddOrderFromFile(
    filename: String,
    private val onEvent: (SnackBarEventData) -> Unit,
    onNewId: (Long) -> Unit
) {
    init {
        val order = OrderRequestKtor(
            Path(
                completePendingPath,
                filename
            ).toString()
        )
        val completeList = ArrayList(order.contents)

        val orderRequest = OrderRequest(
            clientId = order.clientId ?: 0,
            creationDate = order.creationDate.toString(),
            description = order.description,
            orderTypeDescription = OrderRequestType.packaging.description,
            orderTypeId = OrderRequestType.packaging.id.toInt(),
            resultAllowDiff = if (order.resultAllowDiff == true) 1 else 0,
            resultAllowMod = if (order.resultAllowMod == true) 1 else 0,
            resultDiffProduct = if (order.resultDiffProduct == true) 1 else 0,
            resultDiffQty = if (order.resultDiffQty == true) 1 else 0,
            startDate = order.startDate.toString(),
            userId = Statics.currentUserId,
        )

        OrderRequestCoroutines.add(
            orderRequest = orderRequest,
            onResult = { newId ->
                if (newId != null) {
                    orderRequest.orderRequestId = newId
                    OrderRequestCoroutines.update(
                        orderRequest = orderRequest.toKtor,
                        contents = completeList,
                        onResult = {
                            if (it) {
                                onNewId(newId)
                            } else {
                                sendEvent(
                                    context.getString(R.string.error_when_creating_the_order),
                                    SnackBarType.ERROR
                                )
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

