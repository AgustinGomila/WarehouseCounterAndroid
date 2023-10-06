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
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.FORMULA_ITEM
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.PREFIX_ITEM
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.PREFIX_ITEM_URL
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.playSoundNotification
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.searchString
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.Item as ItemKtor
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.ItemCode as ItemCodeKtor

class GetOrderRequestContentFromCode(
    private val code: String,
    private val list: ArrayList<OrderRequestContent>,
    private val onEvent: (SnackBarEventData) -> Unit = {},
    private val onFinish: (OrderRequestContentResult) -> Unit = {},
) {
    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    data class OrderRequestContentResult(var orc: OrderRequestContent? = null, var itemCode: ItemCode? = null)

    private var itemCode: ItemCode? = null
    private var count: Int = 0

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        if (!preRequisites()) return

        scope.launch {
            coroutineScope {
                withContext(Dispatchers.IO) { suspendFunction() }
            }
        }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        try {
            if (isAlreadyInList()) {
                playSoundNotification(true)
                return@withContext
            }

            GetResultFromCode(
                code = code,
                searchItemId = true,
                searchItemCode = true,
                searchItemEan = true,
                searchItemUrl = true,
                useLike = false,
                onFinish = {
                    val itemResult = it.typedObject
                    if (itemResult is java.util.ArrayList<*>) {
                        val item = itemResult.firstOrNull() ?: return@GetResultFromCode
                        if (item is ItemKtor) {
                            getItemFromListOrAdd(item)
                        } else if (item is ItemCodeKtor) {
                            val itemKtor = item.item ?: return@GetResultFromCode
                            itemCode = item.toItemCodeRoom()
                            getItemFromListOrAdd(itemKtor)
                        }
                    } else {
                        // No existe el ítem, agregar como Desconocido si la configuración lo permite
                        addUnknownItem()
                        return@GetResultFromCode
                    }
                })
        } catch (ex: Exception) {
            sendEvent(ex.message.toString(), SnackBarType.ERROR)
            Log.e(tag, ex.message ?: "")

            playSoundNotification(false)
            onFinish(OrderRequestContentResult())
        }
    }

    private fun isAlreadyInList(): Boolean {
        if (count == 0) return false

        // Reducir búsquedas innecesarias
        val searchItemId: Boolean
        var searchItemCode = true
        var searchItemEan = true
        val searchItemUrl: Boolean

        if (code.startsWith(PREFIX_ITEM)) {
            searchItemId = true
            searchItemCode = false
            searchItemEan = false
            searchItemUrl = false
        } else if (code.contains(PREFIX_ITEM_URL)) {
            searchItemId = false
            searchItemCode = false
            searchItemEan = false
            searchItemUrl = true
        } else {
            // Solo se usan con prefijo
            searchItemId = false
            searchItemUrl = false
        }

        if ((searchItemId || searchItemUrl) && isItemIdInTheList()) return true

        if (searchItemEan && isEanInTheList()) return true

        if (searchItemCode && isItemCodeInTheList()) return true

        return false
    }

    private fun isEanInTheList(): Boolean {
        (0 until count).map { list[it] }
            .filter { it.ean == code }.forEach { it2 ->
                onFinish(OrderRequestContentResult(it2, itemCode))
                return true
            }

        return false
    }

    private fun isItemIdInTheList(): Boolean {
        var id: Long? = null

        if (code.contains(PREFIX_ITEM_URL)) {
            val idStr = code.substringAfterLast(PREFIX_ITEM_URL)
            try {
                id = idStr.toLong()
            } catch (ex: NumberFormatException) {
                Log.i(tag, "Could not parse: $ex")
                return false
            }
        } else {
            val match: String = searchString(code, FORMULA_ITEM, 1)
            if (match.isNotEmpty()) {
                try {
                    id = match.toLong()
                } catch (ex: NumberFormatException) {
                    Log.i(tag, "Could not parse: $ex")
                    return false
                }
            }
        }

        if (id == null) return false

        (0 until count).map { list[it] }
            .filter { it.itemId == id }.forEach { it2 ->
                onFinish(OrderRequestContentResult(it2, itemCode))
                return true
            }

        return false
    }

    @get:Synchronized
    private var isProcessDone = false

    @Synchronized
    private fun getProcessState(): Boolean {
        return isProcessDone
    }

    @Synchronized
    private fun setProcessState(state: Boolean) {
        isProcessDone = state
    }

    private fun isItemCodeInTheList(): Boolean {
        setProcessState(false)

        ItemCodeCoroutines.getByCode(
            code = code,
            onResult = {
                if (it.any()) {
                    itemCode = it.first()
                }
                setProcessState(true)
            }
        )

        val startTime = System.currentTimeMillis()
        while (!getProcessState()) {
            if (System.currentTimeMillis() - startTime == (settingsVm.connectionTimeout * 1000).toLong()) {
                setProcessState(true)
            }
        }

        val tempItemCode = itemCode ?: return false

        (0 until count).map { list[it] }
            .filter { it.itemId == tempItemCode.itemId }.forEach { it2 ->
                onFinish(OrderRequestContentResult(it2, itemCode))
                return true
            }

        return false
    }

    private fun preRequisites(): Boolean {
        count = list.count()

        if (Statics.DEMO_MODE) {
            if (count >= 5) {
                val res = context.getString(R.string.maximum_amount_of_demonstration_mode_reached)
                sendEvent(res, SnackBarType.ERROR)
                Log.e(tag, res)

                onFinish(OrderRequestContentResult())
                return false
            }
        }

        // Nada que hacer, volver
        if (code.isEmpty()) {
            val res = context.getString(R.string.invalid_code)
            sendEvent(res, SnackBarType.ERROR)
            Log.e(tag, res)

            onFinish(OrderRequestContentResult())
            return false
        }

        return true
    }

    private fun addUnknownItem() {
        if (settingsVm.allowUnknownCodes) {

            // Item desconocido, agregar al base de datos
            val item = Item(description = context.getString(R.string.unknown_item), ean = code)

            ItemCoroutines.add(item) { id ->
                if (id != null) {
                    playSoundNotification(true)
                    onFinish(getOrderRequestContentResult(item = item, id = id))
                } else {
                    playSoundNotification(false)
                    sendEvent(context.getString(R.string.error_attempting_to_add_item_to_database), SnackBarType.ERROR)
                    onFinish(OrderRequestContentResult())
                }
            }
        } else {
            playSoundNotification(false)
            sendEvent("${context.getString(R.string.unknown_item)}: $code", SnackBarType.INFO)
            onFinish(OrderRequestContentResult())
        }
    }

    private fun getItemFromListOrAdd(itemKtor: ItemKtor) {
        // Buscar de nuevo dentro del adaptador del control
        for (x in 0 until count) {
            val item = list[x]
            if (item.itemId == itemKtor.id) {
                // Ya está en el adaptador, devolver el ítem
                onFinish(OrderRequestContentResult(item, itemCode))
                return
            }
        }

        // No está en el adaptador, devolver el ítem ya convertido
        onFinish(OrderRequestContentResult(itemKtor.toOrderRequestContent(code), itemCode))
    }

    private fun getOrderRequestContentResult(item: Item, id: Long): OrderRequestContentResult {
        return OrderRequestContentResult(
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
