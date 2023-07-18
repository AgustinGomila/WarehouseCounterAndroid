package com.dacosys.warehouseCounter.ui.activities.orderRequest

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequestContent
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.room.entity.item.Item
import com.dacosys.warehouseCounter.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.*

class CheckCode(
    private var callback: (CheckCodeEnded) -> Unit = {},
    private var scannedCode: String,
    private var list: ArrayList<OrderRequestContent>,
    private var onEventData: (SnackBarEventData) -> Unit = {},
) {
    data class CheckCodeEnded(var orc: OrderRequestContent?, var itemCode: ItemCode?)

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
                    onEventData.invoke(SnackBarEventData(res, SnackBarType.ERROR))
                    callback.invoke(CheckCodeEnded(null, itemCode))
                    Log.e(this::class.java.simpleName, res)
                    return@withContext
                }
            }

            val code = scannedCode

            // Nada que hacer, volver
            if (code.isEmpty()) {
                val res = context.getString(R.string.invalid_code)
                onEventData.invoke(SnackBarEventData(res, SnackBarType.ERROR))
                callback.invoke(CheckCodeEnded(null, itemCode))
                Log.e(this::class.java.simpleName, res)
                return@withContext
            }

            if (count > 0) {
                // Buscar primero en el adaptador de la lista
                (0 until count).map { list[it] }
                    .filter { it.ean == scannedCode }.forEach { it2 ->
                        callback.invoke(CheckCodeEnded(it2, itemCode))
                        return@withContext
                    }
            }

            // Si no está en el adaptador del control, buscar en la base de datos
            ItemCoroutines.getByQuery(code) {
                val itemObj = it.firstOrNull()

                if (itemObj != null) {
                    callback.invoke(
                        CheckCodeEnded(
                            orc = OrderRequestContent().apply {
                                codeRead = code // TODO: Ver esto...
                                itemId = itemObj.itemId
                                itemDescription = itemObj.description
                                ean = itemObj.ean
                                price = itemObj.price?.toDouble()
                                itemActive = itemObj.active == 1
                                externalId = itemObj.externalId
                                itemCategoryId = itemObj.itemCategoryId
                                lotEnabled = itemObj.lotEnabled == 1
                                qtyCollected = 0.toDouble()
                                qtyRequested = 0.toDouble()
                            },
                            itemCode = itemCode
                        )
                    )
                    return@getByQuery
                }

                ItemCodeCoroutines.getByCode(code) { icList ->
                    itemCode = icList.firstOrNull()
                    val itemId = itemCode?.itemId
                    if (itemId == null) {
                        callback.invoke(CheckCodeEnded(null, null))
                        return@getByCode
                    }

                    // Buscar de nuevo dentro del adaptador del control
                    for (x in 0 until count) {
                        val item = list[x]
                        if (item.itemId == itemId) {
                            callback.invoke(CheckCodeEnded(item, itemCode))
                            return@getByCode
                        }
                    }

                    if (settingViewModel.allowUnknownCodes) {
                        // Item desconocido, agregar al base de datos
                        val item = Item(
                            description = context.getString(R.string.unknown_item), ean = code
                        )

                        ItemCoroutines.add(item) { id ->
                            if (id != null) {
                                callback.invoke(
                                    CheckCodeEnded(
                                        OrderRequestContent().apply {
                                            this.itemId = item.itemId
                                            itemDescription = item.description
                                            ean = item.ean
                                            price = item.price?.toDouble()
                                            itemActive = item.active == 1
                                            externalId = item.externalId
                                            itemCategoryId = item.itemCategoryId
                                            lotEnabled = item.lotEnabled == 1
                                            qtyCollected = 0.toDouble()
                                            qtyRequested = 0.toDouble()
                                        }, itemCode
                                    )
                                )
                            } else {
                                callback.invoke(CheckCodeEnded(null, null))
                                onEventData.invoke(
                                    SnackBarEventData(
                                        context.getString(R.string.error_attempting_to_add_item_to_database),
                                        SnackBarType.ERROR
                                    )
                                )

                            }
                        }
                    } else {
                        callback.invoke(CheckCodeEnded(null, null))
                        onEventData.invoke(
                            SnackBarEventData(
                                "${context.getString(R.string.unknown_item)}: $code",
                                SnackBarType.INFO
                            )
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            callback.invoke(CheckCodeEnded(null, null))
            onEventData.invoke(SnackBarEventData(ex.message.toString(), SnackBarType.ERROR))
            Log.e(this::class.java.simpleName, ex.message ?: "")
        }
    }
}