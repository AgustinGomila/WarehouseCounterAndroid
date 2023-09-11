package com.dacosys.warehouseCounter.data.ktor.v2.functions.template

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiServiceV2
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelTemplate
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiActionParam
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*

class ViewBarcodeLabelTemplate
/**
 * Get a [BarcodeLabelTemplate] by his ID
 *
 * @property id ID of the rack.
 * @property action List of parameters.
 * @property onEvent Event to update the state of the UI according to the progress of the operation.
 * @property onFinish If the operation is successful it returns a [BarcodeLabelTemplate] else null.
 */(
    private val id: Long,
    private val action: ArrayList<ApiActionParam>,
    private val onFinish: (BarcodeLabelTemplate?) -> Unit,
    private val onEvent: (SnackBarEventData) -> Unit = { },
) {

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: BarcodeLabelTemplate? = null

    fun execute() {
        onEvent(
            SnackBarEventData(
                context.getString(R.string.searching_templates),
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
        ktorApiServiceV2.viewBarcodeLabelTemplate(
            id = id,
            action = action,
            callback = {
                if (it.onEvent != null) sendEvent(it.onEvent)
                if (it.response != null) r = it.response
                if (r != null) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
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
