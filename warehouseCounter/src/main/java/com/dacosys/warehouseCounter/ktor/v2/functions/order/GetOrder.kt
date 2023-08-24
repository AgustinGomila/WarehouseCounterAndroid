package com.dacosys.warehouseCounter.ktor.v2.functions.order

import android.util.Log
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiServiceV2
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiActionParam
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*

class GetOrder(
    private val action: ArrayList<ApiActionParam>,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ArrayList<OrderResponse>) -> Unit,
) {
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val defaultAction: ArrayList<ApiActionParam>
            get() {
                return arrayListOf(
                    ApiActionParam(
                        action = ACTION_EXPAND,
                        extension = setOf(
                            EXTENSION_ORDER_CONTENT_LIST, EXTENSION_ORDER_SCAN_LOG, EXTENSION_LOCATION
                        )
                    )
                )
            }

        /** Valid extensions and actions for this function */
        const val ACTION_EXPAND = "expand"
        const val EXTENSION_ORDER_CONTENT_LIST = "orderContents"
        const val EXTENSION_ORDER_SCAN_LOG = "orderScanLogs"
        const val EXTENSION_LOCATION = "location"
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: ArrayList<OrderResponse> = ArrayList()

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
        ktorApiServiceV2.getOrder(
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
