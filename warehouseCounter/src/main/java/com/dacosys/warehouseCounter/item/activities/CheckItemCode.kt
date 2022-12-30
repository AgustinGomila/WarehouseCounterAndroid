package com.dacosys.warehouseCounter.item.activities

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.item.`object`.Item
import com.dacosys.warehouseCounter.item.dbHelper.ItemAdapter
import com.dacosys.warehouseCounter.item.dbHelper.ItemDbHelper
import com.dacosys.warehouseCounter.itemCode.`object`.ItemCode
import com.dacosys.warehouseCounter.itemCode.dbHelper.ItemCodeDbHelper
import com.dacosys.warehouseCounter.misc.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.misc.snackBar.SnackBarType
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

    private fun preExecute() {
        // TODO: JotterListener.lockScanner(this, true)
    }

    private fun postExecute(item: Item?) {
        // TODO: JotterListener.lockScanner(this, false)
        mCallback?.onCheckCodeEnded(scannedCode ?: "", item, itemCode)
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        preExecute()
        scope.launch {
            val it = doInBackground()
            postExecute(it)
        }
    }

    private var deferred: Deferred<Item?>? = null
    private suspend fun doInBackground(): Item? {
        var result: Item? = null
        coroutineScope {
            deferred = async { suspendFunction() }
            result = deferred?.await()
        }
        return result
    }

    private suspend fun suspendFunction(): Item? = withContext(Dispatchers.IO) {
        try {
            if (Statics.demoMode) {
                if (tempAdapter!!.count >= 5) {
                    val res =
                        context().getString(R.string.maximum_amount_of_demonstration_mode_reached)
                    onEventData(SnackBarEventData(res, SnackBarType.ERROR))
                    Log.e(this::class.java.simpleName, res)
                    return@withContext null
                }
            }

            // Nada que hacer, volver
            if (scannedCode!!.isEmpty()) {
                val res = context().getString(R.string.invalid_code)
                onEventData(SnackBarEventData(res, SnackBarType.ERROR))
                Log.e(this::class.java.simpleName, res)
                return@withContext null
            }

            if (tempAdapter != null && tempAdapter!!.count() > 0) {
                // Buscar primero en el adaptador de la lista
                (0 until tempAdapter!!.count).map { tempAdapter!!.getItem(it) }
                    .filter { it != null && it.ean == scannedCode }
                    .forEach { return@withContext it }
            }

            // Si no está en el adaptador del control, buscar en la base de datos
            val a = ItemDbHelper()
            val itemObj = a.selectByEan(scannedCode!!).firstOrNull()

            if (itemObj != null) {
                return@withContext itemObj
            }

            // ¿No está? Buscar en la tabla item_code de la base de datos
            val icDbH = ItemCodeDbHelper()
            val itemCodeObj: ItemCode? = icDbH.selectByCode(scannedCode!!).firstOrNull()
            // Encontrado! Cuidado, codeRead es diferente de eanCode
            if (itemCodeObj != null) {
                itemCode = itemCodeObj

                // Buscar de nuevo dentro del adaptador del control
                for (x in 0 until tempAdapter!!.count) {
                    val item = tempAdapter!!.getItem(x)
                    if (item != null && item.itemId == itemCodeObj.itemId) {
                        return@withContext item
                    }
                }
            }

            // Item desconocido, agregar al base de datos
            return@withContext null
        } catch (ex: Exception) {
            onEventData(SnackBarEventData(ex.message.toString(), SnackBarType.ERROR))
            Log.e(this::class.java.simpleName, ex.message ?: "")
            return@withContext null
        }
    }
}