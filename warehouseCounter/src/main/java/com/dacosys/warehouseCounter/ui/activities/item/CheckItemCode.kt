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
    private var scannedCode: String,
    private var list: ArrayList<Item>,
    private var onEventData: (SnackBarEventData) -> Unit = {},
    private var onFinish: (CheckCodeEnded) -> Unit = {},
) {
    data class CheckCodeEnded(
        var scannedCode: String,
        var item: Item? = null,
        var itemCode: ItemCode? = null,
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
                    sendEvent(res, SnackBarType.ERROR)
                    onFinish.invoke(CheckCodeEnded(scannedCode = ""))
                    Log.e(this::class.java.simpleName, res)
                    return@withContext
                }
            }

            // Nada que hacer, volver
            if (scannedCode.isEmpty()) {
                val res = context.getString(R.string.invalid_code)
                sendEvent(res, SnackBarType.ERROR)
                onFinish.invoke(CheckCodeEnded(scannedCode = ""))
                Log.e(this::class.java.simpleName, res)
                return@withContext
            }

            if (count > 0) {
                // Buscar primero en el adaptador de la lista
                (0 until count).map { list[it] }
                    .filter { it.ean == scannedCode }.forEach { it2 ->
                        onFinish.invoke(CheckCodeEnded(scannedCode = scannedCode, item = it2, itemCode = itemCode))
                        return@withContext
                    }
            }

            // Si no está en el adaptador del control, buscar en la base de datos
            ItemCoroutines.getByQuery(scannedCode) {
                val itemObj = it.firstOrNull()

                if (itemObj != null) {
                    onFinish.invoke(CheckCodeEnded(scannedCode = scannedCode, item = itemObj))
                    return@getByQuery
                }

                ItemCodeCoroutines.getByCode(scannedCode) { icList ->
                    itemCode = icList.firstOrNull()
                    val itemId = itemCode?.itemId
                    if (itemId == null) {
                        onFinish.invoke(CheckCodeEnded(scannedCode = scannedCode))
                        return@getByCode
                    }

                    // Buscar de nuevo dentro del adaptador del control
                    for (x in 0 until count) {
                        val item = list[x]
                        if (item.itemId == itemId) {
                            onFinish.invoke(CheckCodeEnded(scannedCode = scannedCode, item = item, itemCode = itemCode))
                            return@getByCode
                        }
                    }

                    onFinish.invoke(CheckCodeEnded(scannedCode = scannedCode))
                }
            }
        } catch (ex: Exception) {
            sendEvent(ex.message.toString(), SnackBarType.ERROR)
            onFinish.invoke(CheckCodeEnded(scannedCode = ""))
            Log.e(this::class.java.simpleName, ex.message ?: "")
            return@withContext
        }
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEventData.invoke(event)
    }
}
