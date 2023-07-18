package com.dacosys.warehouseCounter.ktor.v2.functions

import android.util.Log
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiServiceV2
import com.dacosys.warehouseCounter.ktor.v2.dto.location.Warehouse
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiActionParam
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
    private val action: ArrayList<ApiActionParam>,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ArrayList<Warehouse>) -> Unit,
) {
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
        ktorApiServiceV2.getWarehouse(action = action, callback = {
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