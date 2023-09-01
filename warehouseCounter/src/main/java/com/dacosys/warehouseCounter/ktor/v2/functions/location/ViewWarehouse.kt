package com.dacosys.warehouseCounter.ktor.v2.functions.location

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiServiceV2
import com.dacosys.warehouseCounter.ktor.v2.dto.location.Warehouse
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiActionParam
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*

class ViewWarehouse
/**
 * Get a [Warehouse] by his ID
 *
 * @property id ID of the warehouse.
 * @property action List of parameters.
 * @property onEvent Event to update the state of the UI according to the progress of the operation.
 * @property onFinish If the operation is successful it returns a [Warehouse] else null.
 */(
    private val id: Long,
    private val action: ArrayList<ApiActionParam>,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (Warehouse?) -> Unit,
) {
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val defaultAction: ArrayList<ApiActionParam>
            get() {
                return arrayListOf(
                    ApiActionParam(
                        action = ACTION_EXPAND, extension = setOf(
                            EXTENSION_WAREHOUSE_AREA_LIST, EXTENSION_STATUS
                        )
                    )
                )
            }

        /** Valid extensions and actions for this function */
        const val ACTION_EXPAND = "expand"
        const val EXTENSION_WAREHOUSE_AREA_LIST = "warehouseAreas"
        const val EXTENSION_STATUS = "status"
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: Warehouse? = null

    fun execute() {
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
        ktorApiServiceV2.viewWarehouse(
            id = id,
            action = action,
            callback = {
                if (it.onEvent != null) sendEvent(it.onEvent)
                if (it.response != null) r = it.response
                if (r != null) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
                else sendEvent(context.getString(R.string.item_not_exists), SnackBarType.INFO)
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
