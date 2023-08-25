package com.dacosys.warehouseCounter.ktor.v2.functions.location

import android.util.Log
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiServiceV2
import com.dacosys.warehouseCounter.ktor.v2.dto.location.Warehouse
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiActionParam
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiFilterParam
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*


class GetWarehouse
/**
 * Get a list of [Warehouse]
 *
 * @property action Actions and extensions for the query.
 * @property onEvent Event to update the state of the UI according to the progress of the operation.
 * @property onFinish If the operation is successful it returns a list of [Warehouse]
 */(
    private val filter: ArrayList<ApiFilterParam> = arrayListOf(),
    private val action: ArrayList<ApiActionParam> = arrayListOf(),
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ArrayList<Warehouse>) -> Unit,
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

    private var r: ArrayList<Warehouse> = ArrayList()

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
        ktorApiServiceV2.getWarehouse(
            filter = filter,
            action = action,
            callback = {
                if (BuildConfig.DEBUG) Log.d(javaClass.simpleName, it.toString())
                r = it.items
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
