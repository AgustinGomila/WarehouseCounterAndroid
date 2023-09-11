package com.dacosys.warehouseCounter.data.ktor.v2.functions.order

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiServiceV2
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.Barcode
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeParam
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*

class GetOrderBarcode(
    private val param: BarcodeParam,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ArrayList<Barcode>) -> Unit,
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: ArrayList<Barcode> = ArrayList()

    fun execute() {
        onEvent(
            SnackBarEventData(
                context.getString(R.string.searching_barcodes),
                SnackBarType.RUNNING
            )
        )
        scope.launch {
            coroutineScope {
                withContext(Dispatchers.IO) { suspendFunction() }
            }
        }
    }

    fun cancel() {
        if (scope.isActive) scope.cancel()
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        ktorApiServiceV2.getOrderBarcode(
            params = param,
            callback = {
                if (it.onEvent != null) sendEvent(it.onEvent)
                if (it.response != null) r = ArrayList(it.response)
                if (r.any()) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
                else sendEvent(context.getString(R.string.no_results), SnackBarType.INFO)
            })
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)
        if (event.snackBarType in getFinish() || event.snackBarType == SnackBarType.INFO) {
            onFinish.invoke(r)
        }
    }
}
