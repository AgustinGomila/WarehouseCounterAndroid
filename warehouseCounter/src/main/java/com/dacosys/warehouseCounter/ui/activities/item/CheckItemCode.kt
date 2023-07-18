package com.dacosys.warehouseCounter.ui.activities.item

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.room.entity.item.Item
import com.dacosys.warehouseCounter.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.*

class CheckItemCode(
    private var callback: (CheckCodeEnded) -> Unit = {},
    private var scannedCode: String,
    private var list: ArrayList<Item>,
    private var onEventData: (SnackBarEventData) -> Unit = {},
) {
    data class CheckCodeEnded(
        var scannedCode: String,
        var item: Item?,
        var itemCode: ItemCode?,
    )

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
                    onEventData(SnackBarEventData(res, SnackBarType.ERROR))
                    callback.invoke(CheckCodeEnded(scannedCode = "", item = null, itemCode = null))
                    Log.e(this::class.java.simpleName, res)
                    return@withContext
                }
            }

            val code = scannedCode

            // Nada que hacer, volver
            if (code.isEmpty()) {
                val res = context.getString(R.string.invalid_code)
                onEventData(SnackBarEventData(res, SnackBarType.ERROR))
                callback.invoke(CheckCodeEnded(scannedCode = "", item = null, itemCode = null))
                Log.e(this::class.java.simpleName, res)
                return@withContext
            }

            if (count > 0) {
                // Buscar primero en el adaptador de la lista
                (0 until count).map { list[it] }
                    .filter { it.ean == scannedCode }.forEach { it2 ->
                        callback.invoke(
                            CheckCodeEnded(
                                scannedCode = code, item = it2, itemCode = itemCode
                            )
                        )
                        return@withContext
                    }
            }

            // Si no está en el adaptador del control, buscar en la base de datos
            ItemCoroutines.getByQuery(code) {
                val itemObj = it.firstOrNull()

                if (itemObj != null) {
                    callback.invoke(
                        CheckCodeEnded(
                            scannedCode = code, item = itemObj, itemCode = null
                        )
                    )
                    return@getByQuery
                }

                ItemCodeCoroutines.getByCode(code) { icList ->
                    itemCode = icList.firstOrNull()
                    val itemId = itemCode?.itemId
                    if (itemId == null) {
                        callback.invoke(
                            CheckCodeEnded(
                                scannedCode = code, item = null, itemCode = null
                            )
                        )
                        return@getByCode
                    }

                    // Buscar de nuevo dentro del adaptador del control
                    for (x in 0 until count) {
                        val item = list[x]
                        if (item.itemId == itemId) {
                            callback.invoke(
                                CheckCodeEnded(
                                    scannedCode = code, item = item, itemCode = itemCode
                                )
                            )
                            return@getByCode
                        }
                    }

                    callback.invoke(
                        CheckCodeEnded(
                            scannedCode = code, item = null, itemCode = null
                        )
                    )
                }
            }
        } catch (ex: Exception) {
            onEventData(SnackBarEventData(ex.message.toString(), SnackBarType.ERROR))
            callback.invoke(CheckCodeEnded(scannedCode = "", item = null, itemCode = null))
            Log.e(this::class.java.simpleName, ex.message ?: "")
            return@withContext
        }
    }
}