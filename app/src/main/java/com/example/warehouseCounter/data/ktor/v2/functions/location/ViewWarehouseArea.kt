package com.example.warehouseCounter.data.ktor.v2.functions.location

import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV2
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.example.warehouseCounter.data.ktor.v2.impl.ApiActionParam
import com.example.warehouseCounter.misc.utils.NetworkState
import com.example.warehouseCounter.ui.snackBar.SnackBarEventData
import com.example.warehouseCounter.ui.snackBar.SnackBarType
import com.example.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewWarehouseArea
/**
 * Get a [WarehouseArea] by his ID
 *
 * @property id ID of the warehouseArea.
 * @property action List of parameters.
 * @property onEvent Event to update the state of the UI according to the progress of the operation.
 * @property onFinish If the operation is successful it returns a [WarehouseArea] else null.
 */(
    private val id: Long,
    private val action: ArrayList<ApiActionParam> = arrayListOf(),
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (WarehouseArea?) -> Unit,
) {
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val defaultAction: ArrayList<ApiActionParam>
            get() {
                return arrayListOf(
                    ApiActionParam(
                        action = ACTION_EXPAND, extension = setOf(
                            EXTENSION_WAREHOUSE, EXTENSION_PTL_LIST, EXTENSION_STATUS
                        )
                    )
                )
            }

        /** Valid extensions and actions for this function */
        const val ACTION_EXPAND = "expand"
        const val EXTENSION_WAREHOUSE = "warehouse"
        const val EXTENSION_PTL_LIST = "ptls"
        const val EXTENSION_STATUS = "status"
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: WarehouseArea? = null

    fun execute() {
        if (!NetworkState.isOnline()) {
            sendEvent(context.getString(R.string.connection_error), SnackBarType.ERROR)
            return
        }
        sendEvent(context.getString(R.string.searching_areas), SnackBarType.RUNNING)
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
        apiServiceV2.viewWarehouseArea(
            id = id,
            action = action,
            callback = {
                if (it.response != null) r = it.response
                if (it.onEvent != null) sendEvent(it.onEvent)
                else {
                    if (r != null) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
                    else sendEvent(context.getString(R.string.no_results), SnackBarType.INFO)
                }
            })
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)
        if (event.snackBarType in getFinish() || SnackBarType.INFO.equals(event.snackBarType)) {
            onFinish.invoke(r)
        }
    }
}
