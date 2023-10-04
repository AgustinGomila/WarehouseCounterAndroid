package com.dacosys.warehouseCounter.data.ktor.v2.functions.item

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV2
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.Item
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiActionParam
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiPaginationParam
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiPaginationParam.Companion.defaultPagination
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

class GetItem(
    private val filter: ArrayList<ApiFilterParam> = arrayListOf(),
    private val action: ArrayList<ApiActionParam> = arrayListOf(),
    private val pagination: ApiPaginationParam = defaultPagination,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ArrayList<Item>) -> Unit,
) {
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val defaultAction: ArrayList<ApiActionParam>
            get() {
                return arrayListOf(
                    ApiActionParam(
                        action = ACTION_EXPAND, extension = setOf(
                            EXTENSION_STATUS
                        )
                    )
                )
            }

        /** Valid extensions and actions for this function */
        const val ACTION_EXPAND = "expand"
        const val EXTENSION_STATUS = "status"
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: ArrayList<Item> = ArrayList()

    fun execute() {
        if (!Statics.isOnline()) {
            sendEvent(context.getString(R.string.connection_error), SnackBarType.ERROR)
            return
        }
        sendEvent(context.getString(R.string.searching_items), SnackBarType.RUNNING)
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
        apiServiceV2.getItem(
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
