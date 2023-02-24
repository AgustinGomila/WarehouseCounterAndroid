package com.dacosys.warehouseCounter.ui.activities.item

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.adapter.item.ItemAdapter
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.room.entity.item.Item
import com.dacosys.warehouseCounter.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.*

class CheckItemCode {
    interface CheckCodeEnded {
        // Define data you like to return from AysncTask
        fun onCheckCodeEnded(
            scannedCode: String,
            item: Item?,
            itemCode: ItemCode?,
        )
    }

    private var scannedCode: String? = null
    private var itemCode: ItemCode? = null
    private var mCallback: CheckCodeEnded? = null
    private var tempAdapter: ItemAdapter? = null
    private var onEventData: (SnackBarEventData) -> Unit = {}

    fun addParams(
        callback: CheckCodeEnded,
        scannedCode: String,
        adapter: ItemAdapter,
        onEventData: (SnackBarEventData) -> Unit = {},
    ) {
        this.mCallback = callback
        this.scannedCode = scannedCode
        this.tempAdapter = adapter
        this.onEventData = onEventData
    }

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
            if (Statics.demoMode) {
                if (tempAdapter!!.count >= 5) {
                    val res =
                        context().getString(R.string.maximum_amount_of_demonstration_mode_reached)
                    onEventData(SnackBarEventData(res, SnackBarType.ERROR))
                    mCallback?.onCheckCodeEnded(
                        scannedCode = "", item = null, itemCode = null
                    )
                    Log.e(this::class.java.simpleName, res)
                    return@withContext
                }
            }

            val code = scannedCode

            // Nada que hacer, volver
            if (code.isNullOrEmpty()) {
                val res = context().getString(R.string.invalid_code)
                onEventData(SnackBarEventData(res, SnackBarType.ERROR))
                mCallback?.onCheckCodeEnded(
                    scannedCode = "", item = null, itemCode = null
                )
                Log.e(this::class.java.simpleName, res)
                return@withContext
            }

            if (tempAdapter != null && tempAdapter!!.count() > 0) {
                // Buscar primero en el adaptador de la lista
                (0 until tempAdapter!!.count).map { tempAdapter!!.getItem(it) }
                    .filter { it != null && it.ean == scannedCode }.forEach { it2 ->
                        mCallback?.onCheckCodeEnded(
                            scannedCode = code, item = it2, itemCode = itemCode
                        )
                        return@withContext
                    }
            }

            // Si no está en el adaptador del control, buscar en la base de datos
            ItemCoroutines().getByQuery(code) {
                val itemObj = it.firstOrNull()

                if (itemObj != null) {
                    mCallback?.onCheckCodeEnded(
                        scannedCode = code, item = itemObj, itemCode = null
                    )
                    return@getByQuery
                }

                ItemCodeCoroutines().getByCode(code) { icList ->
                    itemCode = icList.firstOrNull()
                    val itemId = itemCode?.itemId
                    if (itemId == null) {
                        mCallback?.onCheckCodeEnded(
                            scannedCode = code, item = null, itemCode = null
                        )
                        return@getByCode
                    }

                    // Buscar de nuevo dentro del adaptador del control
                    for (x in 0 until tempAdapter!!.count) {
                        val item = tempAdapter!!.getItem(x)
                        if (item != null && item.itemId == itemId) {
                            mCallback?.onCheckCodeEnded(
                                scannedCode = code, item = item, itemCode = itemCode
                            )
                            return@getByCode
                        }
                    }

                    mCallback?.onCheckCodeEnded(
                        scannedCode = code, item = null, itemCode = null
                    )
                }
            }
        } catch (ex: Exception) {
            onEventData(SnackBarEventData(ex.message.toString(), SnackBarType.ERROR))
            mCallback?.onCheckCodeEnded(
                scannedCode = "", item = null, itemCode = null
            )
            Log.e(this::class.java.simpleName, ex.message ?: "")
            return@withContext
        }
    }
}