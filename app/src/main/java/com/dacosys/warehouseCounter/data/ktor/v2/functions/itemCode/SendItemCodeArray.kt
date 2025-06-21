package com.dacosys.warehouseCounter.data.ktor.v2.functions.itemCode

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.ItemCodePayload
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.ItemCodeResponse
import com.dacosys.warehouseCounter.data.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.data.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType

class SendItemCodeArray
/**
 * Send an [ItemCode] list
 *
 * @property payload [ItemCode] loading list.
 * @property onEvent Event to update the state of the UI according to the progress of the operation.
 * @property onFinish If the operation is successful it returns a list of [ItemCodeResponse] else null.
 */(
    private val payload: ArrayList<ItemCode>,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ArrayList<ItemCodeResponse>) -> Unit,
) {
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

    fun execute() {
        setProcessState(false)

        /** Converting the list of [ItemCode] to a list of [ItemCodePayload] */
        val icPayloadArray: ArrayList<ItemCodePayload> = ArrayList()
        for (ic in payload) {
            val qty = ic.qty ?: 0.0
            val itemId = ic.itemId ?: 0L
            val code = ic.code ?: ""
            icPayloadArray.add(ItemCodePayload(qty, itemId, code))
        }

        val allResp: ArrayList<ItemCodeResponse> = ArrayList()

        for ((index, icP) in icPayloadArray.withIndex()) {
            SendItemCode(
                payload = icP,
                onEvent = onEvent,
                onFinish = {
                    if (it != null) {
                        allResp.add(it)
                        /** Update transferred: Actualizar los ItemCode enviados en la base de datos local */
                        ItemCodeCoroutines.updateTransferred(itemId = it.itemId, code = it.code)
                    }
                    setProcessState(index == icPayloadArray.lastIndex)
                }
            ).execute()
        }

        val startTime = System.currentTimeMillis()
        while (!getProcessState()) {
            if (System.currentTimeMillis() - startTime == (settingsVm.connectionTimeout * 1000).toLong()) {
                sendEvent(context.getString(R.string.connection_timeout), SnackBarType.ERROR)
                setProcessState(true)
            }
        }

        onFinish(allResp)
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)
    }
}
