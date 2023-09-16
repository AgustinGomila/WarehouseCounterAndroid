package com.dacosys.warehouseCounter.scanners.scanCode

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestContent
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.data.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.data.room.entity.item.Item
import com.dacosys.warehouseCounter.data.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GetOrderRequestContentFromCode(
    private var scannedCode: String,
    private var list: ArrayList<OrderRequestContent>,
    private var onEvent: (SnackBarEventData) -> Unit = {},
    private var onFinish: (GetFromCodeResult) -> Unit = {},
) {
    data class GetFromCodeResult(var orc: OrderRequestContent? = null, var itemCode: ItemCode? = null)

    private var itemCode: ItemCode? = null

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        scope.launch {
            coroutineScope {
                withContext(Dispatchers.IO) { suspendFunction() }
            }
        }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        try {
            val count = list.count()
            if (Statics.DEMO_MODE) {
                if (count >= 5) {
                    val res = context.getString(R.string.maximum_amount_of_demonstration_mode_reached)
                    sendEvent(res, SnackBarType.ERROR)
                    onFinish(GetFromCodeResult())
                    Log.e(this::class.java.simpleName, res)
                    return@withContext
                }
            }

            val code = scannedCode

            // Nada que hacer, volver
            if (code.isEmpty()) {
                val res = context.getString(R.string.invalid_code)
                sendEvent(res, SnackBarType.ERROR)
                onFinish(GetFromCodeResult())
                Log.e(this::class.java.simpleName, res)
                return@withContext
            }

            if (count > 0) {
                // Buscar primero en el adaptador de la lista
                (0 until count).map { list[it] }
                    .filter { it.ean == scannedCode }.forEach { it2 ->
                        onFinish(GetFromCodeResult(it2, itemCode))
                        return@withContext
                    }
            }

            ItemCoroutines.getByQuery(code) {
                val itemObj = it.firstOrNull()

                if (itemObj != null) {
                    onFinish(getCheckCodeResult(item = itemObj, id = itemObj.itemId, code = code))
                    return@getByQuery
                }

                ItemCodeCoroutines.getByCode(code) { icList ->
                    itemCode = icList.firstOrNull()

                    val tempItemId = itemCode?.itemId
                    if (tempItemId != null) {
                        // Buscar de nuevo dentro del adaptador del control
                        for (x in 0 until count) {
                            val item = list[x]
                            if (item.itemId == tempItemId) {
                                onFinish(GetFromCodeResult(item, itemCode))
                                return@getByCode
                            }
                        }
                    }

                    if (settingsVm.allowUnknownCodes) {
                        // Item desconocido, agregar al base de datos
                        val item = Item(
                            description = context.getString(R.string.unknown_item), ean = code
                        )

                        ItemCoroutines.add(item) { id ->
                            if (id != null) {
                                onFinish(getCheckCodeResult(item = item, id = id, code = code))
                            } else {
                                onFinish(GetFromCodeResult())
                                sendEvent(
                                    context.getString(R.string.error_attempting_to_add_item_to_database),
                                    SnackBarType.ERROR
                                )

                            }
                        }
                    } else {
                        onFinish(GetFromCodeResult())
                        sendEvent(
                            SnackBarEventData(
                                "${context.getString(R.string.unknown_item)}: $code",
                                SnackBarType.INFO
                            )
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            onFinish(GetFromCodeResult())
            sendEvent(ex.message.toString(), SnackBarType.ERROR)
            Log.e(this::class.java.simpleName, ex.message ?: "")
        }
    }

    private fun getCheckCodeResult(item: Item, id: Long, code: String): GetFromCodeResult {
        return GetFromCodeResult(
            orc = OrderRequestContent().apply {
                codeRead = code
                itemId = id
                itemDescription = item.description
                ean = item.ean
                price = item.price?.toDouble()
                itemActive = item.active == 1
                externalId = item.externalId
                itemCategoryId = item.itemCategoryId
                lotEnabled = item.lotEnabled == 1
                qtyCollected = 0.toDouble()
                qtyRequested = 0.toDouble()
            },
            itemCode = itemCode
        )
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent(event)
    }
}
