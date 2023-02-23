package com.dacosys.warehouseCounter.ui.activities.orderRequest

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.adapter.orderRequest.OrcAdapter
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.model.orderRequest.Item
import com.dacosys.warehouseCounter.model.orderRequest.OrderRequestContent
import com.dacosys.warehouseCounter.model.orderRequest.Qty
import com.dacosys.warehouseCounter.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.*

class CheckCode {
    interface CheckCodeEnded {
        // Define data you like to return from AysncTask
        fun onCheckCodeEnded(orc: OrderRequestContent?, itemCode: ItemCode?)
    }

    private var scannedCode: String? = null
    private var itemCode: ItemCode? = null
    private var mCallback: CheckCodeEnded? = null
    private var orcAdapter: OrcAdapter? = null
    private var onEventData: (SnackBarEventData) -> Unit = {}

    fun addParams(
        callback: CheckCodeEnded,
        scannedCode: String,
        adapter: OrcAdapter,
        onEventData: (SnackBarEventData) -> Unit = {},
    ) {
        this.mCallback = callback
        this.scannedCode = scannedCode
        this.orcAdapter = adapter
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
                if (orcAdapter!!.count >= 5) {
                    val res =
                        context().getString(R.string.maximum_amount_of_demonstration_mode_reached)
                    onEventData.invoke(SnackBarEventData(res, SnackBarType.ERROR))
                    mCallback?.onCheckCodeEnded(null, itemCode)
                    Log.e(this::class.java.simpleName, res)
                    return@withContext
                }
            }

            val code = scannedCode

            // Nada que hacer, volver
            if (code.isNullOrEmpty()) {
                val res = context().getString(R.string.invalid_code)
                onEventData.invoke(SnackBarEventData(res, SnackBarType.ERROR))
                mCallback?.onCheckCodeEnded(null, itemCode)
                Log.e(this::class.java.simpleName, res)
                return@withContext
            }

            if ((orcAdapter?.count() ?: 0) > 0) {
                // Buscar primero en el adaptador de la lista
                (0 until orcAdapter!!.count).map { orcAdapter!!.getItem(it) }
                    .filter { it != null && it.item!!.ean == code }.forEach { it2 ->
                        mCallback?.onCheckCodeEnded(it2, itemCode)
                        return@withContext
                    }
            }

            // Si no está en el adaptador del control, buscar en la base de datos
            ItemCoroutines().getByQuery(code) { it ->
                val itemObj = it.firstOrNull()

                if (itemObj != null) {
                    mCallback?.onCheckCodeEnded(
                        OrderRequestContent(
                            item = Item(itemObj, code),
                            lot = null,
                            qty = Qty(0.toDouble(), 0.toDouble())
                        ), itemCode
                    )
                    return@getByQuery
                }

                ItemCodeCoroutines().getByCode(code) { icList ->
                    itemCode = icList.firstOrNull()
                    val itemId = itemCode?.itemId
                    if (itemId == null) {
                        mCallback?.onCheckCodeEnded(null, null)
                        return@getByCode
                    }

                    // Buscar de nuevo dentro del adaptador del control
                    for (x in 0 until orcAdapter!!.count) {
                        val item = orcAdapter!!.getItem(x)
                        if (item != null && item.item!!.itemId == itemId) {
                            mCallback?.onCheckCodeEnded(item, itemCode)
                            return@getByCode
                        }
                    }

                    if (settingViewModel().allowUnknownCodes) {
                        // Item desconocido, agregar al base de datos
                        val item = com.dacosys.warehouseCounter.room.entity.item.Item(
                            description = context().getString(R.string.unknown_item), ean = code
                        )

                        ItemCoroutines().add(item) { id ->
                            if (id != null) {
                                mCallback?.onCheckCodeEnded(
                                    OrderRequestContent(
                                        item = Item(item, code),
                                        lot = null,
                                        qty = Qty(0.toDouble(), 0.toDouble())
                                    ), itemCode
                                )
                            } else {
                                mCallback?.onCheckCodeEnded(null, null)
                                onEventData.invoke(
                                    SnackBarEventData(
                                        context().getString(R.string.error_attempting_to_add_item_to_database),
                                        SnackBarType.ERROR
                                    )
                                )

                            }
                        }
                    } else {
                        mCallback?.onCheckCodeEnded(null, null)
                        onEventData.invoke(
                            SnackBarEventData(
                                "${context().getString(R.string.unknown_item)}: $code",
                                SnackBarType.INFO
                            )
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            mCallback?.onCheckCodeEnded(null, null)
            onEventData.invoke(SnackBarEventData(ex.message.toString(), SnackBarType.ERROR))
            Log.e(this::class.java.simpleName, ex.message ?: "")
        }
    }
}