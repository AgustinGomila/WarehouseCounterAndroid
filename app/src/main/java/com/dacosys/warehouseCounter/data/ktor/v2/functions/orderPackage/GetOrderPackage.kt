package com.dacosys.warehouseCounter.data.ktor.v2.functions.orderPackage

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV2
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderPackage
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiActionParam
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiPaginationParam
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiPaginationParam.Companion.defaultPagination
import com.dacosys.warehouseCounter.misc.utils.NetworkState
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*

class GetOrderPackage(
    private val filter: ArrayList<ApiFilterParam> = arrayListOf(),
    private val action: ArrayList<ApiActionParam> = arrayListOf(),
    private val pagination: ApiPaginationParam = defaultPagination,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ArrayList<OrderPackage>) -> Unit,
) {
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val defaultAction: ArrayList<ApiActionParam>
            get() {
                return arrayListOf(
                    ApiActionParam(
                        action = ACTION_EXPAND, extension = setOf(
                            EXTENSION_STATUS, EXTENSION_ORDER_PACKAGE
                        )
                    )
                )
            }

        /** Valid extensions and actions for this function */
        const val ACTION_EXPAND = "expand"
        const val EXTENSION_STATUS = "status"
        const val EXTENSION_ORDER_PACKAGE = "order"
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: ArrayList<OrderPackage> = ArrayList()

    fun execute() {
        if (!NetworkState.isOnline()) {
            sendEvent(context.getString(R.string.connection_error), SnackBarType.ERROR)
            return
        }
        sendEvent(context.getString(R.string.searching_order_packages), SnackBarType.RUNNING)
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
        apiServiceV2.getOrderPackage(
            filter = filter,
            action = action,
            pagination = pagination,
            callback = {
                if (it.response != null) r = it.response.items
                if (it.onEvent != null) sendEvent(it.onEvent)
                else {
                    if (r.any()) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
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
        if (event.snackBarType in getFinish() || event.snackBarType == SnackBarType.INFO) {
            onFinish.invoke(r)
        }
    }
}
