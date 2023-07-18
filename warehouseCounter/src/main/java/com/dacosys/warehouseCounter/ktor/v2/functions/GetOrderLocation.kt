package com.dacosys.warehouseCounter.ktor.v2.functions

import android.util.Log
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiServiceV2
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderLocation
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiFilterParam
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*

class GetOrderLocation(
    private val filter: ArrayList<ApiFilterParam>,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (List<OrderLocation>) -> Unit,
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: List<OrderLocation> = listOf()

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
        ktorApiServiceV2.getOrderLocation(
            filter = filter,
            callback = {
                if (BuildConfig.DEBUG) Log.d(javaClass.simpleName, it.toString())
                r = it
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