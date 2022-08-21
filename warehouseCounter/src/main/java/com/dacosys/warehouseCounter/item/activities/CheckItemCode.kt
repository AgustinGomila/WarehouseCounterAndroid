package com.dacosys.warehouseCounter.item.activities

import android.util.Log
import android.view.View
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.item.`object`.Item
import com.dacosys.warehouseCounter.item.dbHelper.ItemAdapter
import com.dacosys.warehouseCounter.item.dbHelper.ItemDbHelper
import com.dacosys.warehouseCounter.itemCode.`object`.ItemCode
import com.dacosys.warehouseCounter.itemCode.dbHelper.ItemCodeDbHelper
import com.dacosys.warehouseCounter.misc.snackBar.MakeText
import com.dacosys.warehouseCounter.misc.snackBar.SnackBarType
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

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

    private var weakRefView: WeakReference<View>? = null
    private var parentView: View?
        get() {
            return weakRefView?.get()
        }
        set(value) {
            weakRefView = if (value != null) WeakReference(value) else null
        }

    fun addParams(
        parentView: View, callback: CheckCodeEnded, scannedCode: String, adapter: ItemAdapter,
    ) {
        this.parentView = parentView
        this.mCallback = callback
        this.scannedCode = scannedCode
        this.tempAdapter = adapter
    }

    private fun preExecute() {
        // TODO: JotterListener.lockScanner(this, true)
    }

    private fun postExecute(item: Item?) {
        // TODO: JotterListener.lockScanner(this, false)
        mCallback?.onCheckCodeEnded(scannedCode ?: "", item, itemCode)
    }

    fun execute() {
        preExecute()
        val it = doInBackground()
        postExecute(it)
    }

    private var deferred: Deferred<Item?>? = null
    private fun doInBackground(): Item? {
        var result: Item? = null
        runBlocking {
            deferred = async { suspendFunction() }
            result = deferred?.await()
        }
        return result
    }

    private suspend fun suspendFunction(): Item? = withContext(Dispatchers.IO) {
        try {
            if (Statics.demoMode) {
                if (tempAdapter!!.count >= 5) {
                    val res = Statics.WarehouseCounter.getContext()
                        .getString(R.string.maximum_amount_of_demonstration_mode_reached)
                    MakeText.makeText(
                        parentView ?: return@withContext null,
                        res,
                        SnackBarType.ERROR
                    )
                    Log.e(this::class.java.simpleName, res)
                    return@withContext null
                }
            }

            // Nada que hacer, volver
            if (scannedCode!!.isEmpty()) {
                val res = Statics.WarehouseCounter.getContext().getString(R.string.invalid_code)
                MakeText.makeText(
                    parentView ?: return@withContext null,
                    res,
                    SnackBarType.ERROR
                )
                Log.e(this::class.java.simpleName, res)
                return@withContext null
            }

            if (tempAdapter != null && tempAdapter!!.count() > 0) {
                // Buscar primero en el adaptador de la lista
                (0 until tempAdapter!!.count)
                    .map { tempAdapter!!.getItem(it) }
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
            MakeText.makeText(
                parentView ?: return@withContext null,
                ex.message.toString(),
                SnackBarType.ERROR
            )
            Log.e(this::class.java.simpleName, ex.message ?: "")
            return@withContext null
        }
    }
}