package com.example.warehouseCounter.scanners.scanCode

import android.util.Log
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.data.ktor.v2.dto.order.OrderRequestContent
import com.example.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.example.warehouseCounter.data.room.dao.itemCode.ItemCodeCoroutines
import com.example.warehouseCounter.data.room.entity.item.Item
import com.example.warehouseCounter.data.room.entity.itemRegex.ItemRegex
import com.example.warehouseCounter.misc.Statics
import com.example.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.FORMULA_ITEM
import com.example.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.PREFIX_ITEM
import com.example.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.PREFIX_ITEM_URL
import com.example.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.playSoundNotification
import com.example.warehouseCounter.scanners.scanCode.GetResultFromCode.Companion.searchString
import com.example.warehouseCounter.ui.snackBar.SnackBarEventData
import com.example.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.warehouseCounter.data.ktor.v2.dto.item.Item as ItemKtor

/**
 * Esta clase se encarga de analizar y buscar un código escaneado en una lista dada y en diferentes fuentes de códigos
 * de ítems.
 * Luego, convierte el resultado devuelto en un [OrderRequestContent] existente o crea uno nuevo si la configuración lo permite.
 *
 * @param code El código a buscar en la lista y en las fuentes de códigos de ítems.
 * @param list La lista de [OrderRequestContent] en la que se buscará el código.
 * @param onEvent Un callback opcional para manejar eventos y notificaciones a lo largo del proceso de búsqueda.
 * @param onFinish Un callback opcional que se llama cuando se completa la búsqueda y proporciona el resultado como [OrderRequestContentResult].
 */
class GetOrderRequestContentFromCode(
    private val code: String,
    private val list: ArrayList<OrderRequestContent>,
    private val onEvent: (SnackBarEventData) -> Unit = {},
    private val onFinish: (OrderRequestContentResult) -> Unit = {},
) {
    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    /**
     * Resultado de la búsqueda o nuevo [OrderRequestContent]
     */
    private var orc: OrderRequestContent? = null

    /** Un valor para qty positivo puede provenir de ItemCode o de RegexResult, si no está definido es NULL */
    private var qty: Double? = null

    /** Lot y ExternalID provienen del RegexResult si es positiva la búsqueda */
    private var lotCode: String? = null
    private var externalId: String? = null

    private var count: Int = 0

    data class OrderRequestContentResult(
        var orc: OrderRequestContent? = null,
        var qty: Double? = null,
        var lotCode: String? = null,
        var extId: String? = null,
    )

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

            GetResultFromCode.Builder()
                .withCode(code)
                .searchItemCode()
                .searchItemEan()
                .searchItemId()
                .searchItemRegex()
                .searchItemUrl()
                .onFinish { proceedByResult(it) }
                .build()
        } catch (ex: Exception) {
            sendEvent(ex.message.toString(), SnackBarType.ERROR)
            Log.e(tag, ex.message ?: "")

            playSoundNotification(false)
            sendFinish()
        }
    }

    private fun proceedByResult(it: GetResultFromCode.CodeResult) {
        if (it.item != null) {
            val item = it.item as ItemKtor
            qty = it.qty
            lotCode = it.lotCode
            externalId = it.externalId

            getItemFromListOrAdd(item)
        } else {
            // No existe el ítem, agregar como Desconocido si la configuración lo permite
            addUnknownItem()
        }
    }

    private fun isAlreadyInList(): Boolean {
        if (count == 0) return false

        // Reducir búsquedas innecesarias
        val searchItemId: Boolean
        var searchItemCode = true
        var searchItemEan = true
        val searchItemUrl: Boolean
        var searchItemRegex = true

        if (code.startsWith(PREFIX_ITEM)) {
            searchItemId = true
            searchItemCode = false
            searchItemEan = false
            searchItemUrl = false
            searchItemRegex = false
        } else if (code.contains(PREFIX_ITEM_URL)) {
            searchItemId = false
            searchItemCode = false
            searchItemEan = false
            searchItemUrl = true
            searchItemRegex = false
        } else {
            // Solo se usan con prefijo
            searchItemId = false
            searchItemUrl = false
        }

        if ((searchItemId || searchItemUrl) && isItemIdInTheList()) return true

        if (searchItemEan && isEanInTheList()) return true

        if (searchItemRegex && isItemRegexInTheList()) return true

        if (searchItemCode && isItemCodeInTheList()) return true

        return false
    }

    private fun isEanInTheList(): Boolean {
        (0 until count).map { list[it] }
            .filter { it.ean == code }.forEach { it2 ->
                orc = it2
                sendFinish()
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
                orc = it2
                sendFinish()
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

        var tempItemId: Long? = null
        var tempQty: Double? = null

        ItemCodeCoroutines.getByCode(
            code = code,
            onResult = {
                if (it.any()) {
                    tempItemId = it.first().itemId
                    tempQty = it.first().qty
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

        if (tempItemId == null) return false

        (0 until count).map { list[it] }
            .filter { it.itemId == tempItemId }.forEach { it2 ->
                orc = it2
                qty = tempQty
                sendFinish()
                return true
            }

        return false
    }

    private fun isItemRegexInTheList(): Boolean {
        var tempLotCode: String? = null
        var tempEan: String? = null
        var tempQty: Double? = null
        var tempExtId: String? = null

        ItemRegex.tryToRegex(code) {
            if (it.any()) {
                tempLotCode = it.first().lotCode
                tempEan = it.first().ean
                tempQty = it.first().qty?.toDouble()
                tempExtId = it.first().extId
            }
            setProcessState(true)
        }

        val startTime = System.currentTimeMillis()
        while (!getProcessState()) {
            if (System.currentTimeMillis() - startTime == (settingsVm.connectionTimeout * 1000).toLong()) {
                setProcessState(true)
            }
        }

        if (tempLotCode == null) return false

        (0 until count).map { list[it] }
            .filter { it.lotCode.isNotEmpty() && it.lotCode == tempLotCode }
            .forEach { it2 ->
                orc = it2
                qty = tempQty
                lotCode = tempLotCode
                externalId = tempEan
                sendFinish()
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

                sendFinish()
                return false
            }
        }

        // Nada que hacer, volver
        if (code.isEmpty()) {
            val res = context.getString(R.string.invalid_code)
            sendEvent(res, SnackBarType.ERROR)
            Log.e(tag, res)

            sendFinish()
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
                    orc = getOrderRequestContent(item = item, id = id)
                    sendFinish()
                } else {
                    playSoundNotification(false)
                    sendEvent(context.getString(R.string.error_attempting_to_add_item_to_database), SnackBarType.ERROR)
                    sendFinish()
                }
            }
        } else {
            playSoundNotification(false)
            sendEvent("${context.getString(R.string.unknown_item)}: $code", SnackBarType.INFO)
            sendFinish()
        }
    }

    private fun getItemFromListOrAdd(itemKtor: ItemKtor) {
        // Buscar de nuevo dentro del adaptador del control
        for (x in 0 until count) {
            val item = list[x]
            if (item.itemId == itemKtor.id)
                if (lotCode.isNullOrEmpty() || item.lotCode == lotCode) {
                    // Ya está en el adaptador, devolver el ítem
                    orc = item
                    sendFinish()
                    return
                }
        }

        // No está en el adaptador, devolver el ítem ya convertido
        orc = itemKtor.toOrderRequestContent(code, lotCode)
        sendFinish()
    }

    private fun getOrderRequestContent(item: Item, id: Long): OrderRequestContent {
        return OrderRequestContent().apply {
            codeRead = code
            itemId = id
            itemDescription = item.description
            ean = item.ean
            price = item.price?.toDouble()
            itemActive = item.active == 1
            externalId = item.externalId
            itemCategoryId = item.itemCategoryId
            lotEnabled = item.lotEnabled == 1
            lotCode = ""
            qtyCollected = 0.toDouble()
            qtyRequested = 0.toDouble()
        }
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent(event)
    }

    private fun sendFinish() {
        onFinish(OrderRequestContentResult(orc, qty, lotCode, externalId))
    }
}