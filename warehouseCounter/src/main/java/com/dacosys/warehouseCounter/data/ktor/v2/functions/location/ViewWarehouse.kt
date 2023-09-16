package com.dacosys.warehouseCounter.data.ktor.v2.functions.location

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV2
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Warehouse
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiActionParam
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        if (!Statics.isOnline()) {
            sendEvent(context.getString(R.string.connection_error), SnackBarType.ERROR)
            return
        }
        sendEvent(context.getString(R.string.searching_warehouses), SnackBarType.RUNNING)
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
        apiServiceV2.viewWarehouse(
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
