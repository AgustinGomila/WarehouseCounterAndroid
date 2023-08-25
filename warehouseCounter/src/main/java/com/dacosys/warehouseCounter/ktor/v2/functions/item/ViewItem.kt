package com.dacosys.warehouseCounter.ktor.v2.functions.item

import android.util.Log
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiServiceV2
import com.dacosys.warehouseCounter.ktor.v2.dto.item.Item
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiActionParam
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*

class ViewItem
/**
 * Get a [Item] by his ID
 *
 * @property id ID of the item.
 * @property action List of parameters.
 * @property onEvent Event to update the state of the UI according to the progress of the operation.
 * @property onFinish If the operation is successful it returns a [Item] else null.
 */(
    private val id: Long,
    private val action: ArrayList<ApiActionParam> = arrayListOf(),
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (Item?) -> Unit,
) {
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val defaultAction: ArrayList<ApiActionParam>
            get() {
                return arrayListOf(
                    ApiActionParam(
                        action = ACTION_EXPAND, extension = setOf(
                            EXTENSION_ITEM_CATEGORY, EXTENSION_ITEM_PRICE_LIST_CONTENTS
                        )
                    )
                )
            }

        /** Valid extensions and actions for this function */
        const val ACTION_EXPAND = "expand"
        const val EXTENSION_ITEM_CATEGORY = "itemCategory"
        const val EXTENSION_ITEM_PRICE_LIST_CONTENTS = "itemPriceListContents"
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: Item? = null

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
        ktorApiServiceV2.viewItem(
            id = id,
            action = action,
            callback = {
                if (BuildConfig.DEBUG) Log.d(javaClass.simpleName, it.toString())
                r = it
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
