package com.dacosys.warehouseCounter.orderRequest.activities

import android.util.Log
import android.view.View
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.item.dbHelper.ItemDbHelper
import com.dacosys.warehouseCounter.itemCode.`object`.ItemCode
import com.dacosys.warehouseCounter.itemCode.dbHelper.ItemCodeDbHelper
import com.dacosys.warehouseCounter.misc.snackBar.MakeText
import com.dacosys.warehouseCounter.misc.snackBar.SnackBarType
import com.dacosys.warehouseCounter.orderRequest.`object`.Item
import com.dacosys.warehouseCounter.orderRequest.`object`.OrderRequestContent
import com.dacosys.warehouseCounter.orderRequest.`object`.Qty
import com.dacosys.warehouseCounter.orderRequest.dbHelper.OrcAdapter
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

class CheckCode {
    interface CheckCodeEnded {
        // Define data you like to return from AysncTask
        fun onCheckCodeEnded(orc: OrderRequestContent?, itemCode: ItemCode?)
    }

    private var scannedCode: String? = null
    private var itemCode: ItemCode? = null
    private var mCallback: CheckCodeEnded? = null

    private var orcAdapter: OrcAdapter? = null

    private var weakRefView: WeakReference<View>? = null
    private var parentView: View?
        get() {
            return weakRefView?.get()
        }
        set(value) {
            weakRefView = if (value != null) WeakReference(value) else null
        }

    fun addParams(
        parentView: View,
        callback: CheckCodeEnded,
        scannedCode: String,
        adapter: OrcAdapter,
    ) {
        this.parentView = parentView
        this.mCallback = callback
        this.scannedCode = scannedCode
        this.orcAdapter = adapter
    }

    private fun preExecute() {
        // TODO: JotterListener.lockScanner(this, true)
    }

    private fun postExecute(orc: OrderRequestContent?) {
        // TODO: JotterListener.lockScanner(this, false)
        mCallback?.onCheckCodeEnded(orc, itemCode)
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

    private var deferred: Deferred<OrderRequestContent?>? = null
    private suspend fun doInBackground(): OrderRequestContent? {
        var result: OrderRequestContent? = null
        coroutineScope {
            deferred = async { suspendFunction() }
            result = deferred?.await()
        }
        return result
    }

    private suspend fun suspendFunction(): OrderRequestContent? = withContext(Dispatchers.IO) {
        try {
            if (Statics.demoMode) {
                if (orcAdapter!!.count >= 5) {
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
                MakeText.makeText(parentView ?: return@withContext null, res, SnackBarType.ERROR)
                Log.e(this::class.java.simpleName, res)
                return@withContext null
            }

            if ((orcAdapter?.count() ?: 0) > 0) {
                // Buscar primero en el adaptador de la lista
                (0 until orcAdapter!!.count)
                    .map { orcAdapter!!.getItem(it) }
                    .filter { it != null && it.item!!.ean == scannedCode }
                    .forEach { return@withContext it }
            }

            // Si no está en el adaptador del control, buscar en la base de datos
            var searchItem: Item? = null

            val a = ItemDbHelper()
            var itemObj: com.dacosys.warehouseCounter.item.`object`.Item? = null
            val allByEan = a.selectByEan(scannedCode!!)
            if (allByEan.size > 0) {
                itemObj = allByEan[0]
            }

            if (itemObj != null) {
                searchItem = Item(itemObj, scannedCode!!)
            }

            // found it through item.ean
            if (searchItem != null) {
                // Agregar al adaptador con cantidades 0, después se definen y se agregan al Log
                return@withContext OrderRequestContent(
                    searchItem,
                    null,
                    Qty(0.toDouble(), 0.toDouble())
                )
            }

            // ¿No está? Buscar en la tabla item_code de la base de datos
            val icDbH = ItemCodeDbHelper()
            var itemCodeObj: ItemCode? = null
            val allByItemCode = icDbH.selectByCode(scannedCode!!)
            if (allByItemCode.size > 0) {
                itemCodeObj = allByItemCode[0]
            }

            // Encontrado! Cuidado, codeRead es diferente de eanCode
            if (itemCodeObj != null) {
                itemCode = itemCodeObj

                searchItem = Item(itemCodeObj.item!!, scannedCode!!)

                // Buscar de nuevo dentro del adaptador del control
                for (x in 0 until orcAdapter!!.count) {
                    val orc = orcAdapter!!.getItem(x)
                    if (orc != null && orc.item!!.ean == searchItem.ean) {
                        return@withContext orc
                    }
                }

                // Agregar al adaptador con cantidades 0, después se definen y se agregan al Log
                return@withContext OrderRequestContent(
                    searchItem,
                    null,
                    Qty(0.toDouble(), 0.toDouble())
                )
            }

            if (Statics.allowUnknownCodes()) {
                // Item desconocido, agregar al base de datos
                val item = addItemToDb(scannedCode!!)

                return@withContext if (item != null) {
                    // Agregar al adaptador con cantidades 0, después se definen y se agregan al Log
                    val finalOrc = OrderRequestContent(
                        item,
                        null,
                        Qty(0.toDouble(), 0.toDouble())
                    )
                    finalOrc
                } else {
                    val res = Statics.WarehouseCounter.getContext()
                        .getString(R.string.error_attempting_to_add_item_to_database)
                    MakeText.makeText(
                        parentView ?: return@withContext null,
                        res,
                        SnackBarType.ERROR
                    )
                    Log.e(this::class.java.simpleName, res)
                    null
                }
            } else {
                MakeText.makeText(
                    parentView ?: return@withContext null,
                    "${
                        Statics.WarehouseCounter.getContext()
                            .getString(R.string.unknown_item)
                    }: ${scannedCode ?: ""}", SnackBarType.INFO
                )
                return@withContext null
            }
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

    private fun addItemToDb(scannedCode: String): Item? {
        val itemDbHelper = ItemDbHelper()
        val itemId = itemDbHelper.insert(
            description = Statics.WarehouseCounter.getContext().getString(R.string.unknown_item),
            ean = scannedCode,
            price = 0.toDouble(),
            active = true,
            itemCategoryId = 1L,
            externalId = "",
            lotEnabled = false
        )

        return if (itemId!! < 0) {
            Item(
                itemDbHelper.selectByEan(scannedCode)[0],
                scannedCode
            )
        } else {
            null
        }
    }
}