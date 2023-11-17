package com.dacosys.warehouseCounter.scanners.scanCode

import android.media.MediaPlayer
import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.Item
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.ItemCode
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Rack
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.functions.item.GetItem
import com.dacosys.warehouseCounter.data.ktor.v2.functions.item.ViewItem
import com.dacosys.warehouseCounter.data.ktor.v2.functions.itemCode.GetItemCode
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.ViewRack
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.ViewWarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.GetOrder
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.ViewOrder
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam.Companion.ACTION_OPERATOR_LIKE
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.data.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.data.room.entity.itemRegex.ItemRegex
import com.dacosys.warehouseCounter.data.room.entity.itemRegex.ItemRegex.Companion.RegexResult
import com.dacosys.warehouseCounter.scanners.scanCode.GetResultFromCode.Builder
import kotlin.concurrent.thread

/**
 * Clase que se encarga de analizar y buscar un código en fuentes locales y proporcionadas por la API.
 * Puedes utilizar el Builder para configurar las opciones de búsqueda antes de realizar la búsqueda.
 *
 * @param builder Un objeto [Builder] que te permite configurar las opciones de búsqueda.
 */
class GetResultFromCode private constructor(builder: Builder) {

    private var code: String
    private var searchItemCode: Boolean
    private var searchItemEan: Boolean
    private var searchItemId: Boolean
    private var searchItemRegex: Boolean
    private var searchItemUrl: Boolean
    private var searchOrder: Boolean
    private var searchOrderExternalId: Boolean
    private var searchRackId: Boolean
    private var searchWarehouseAreaId: Boolean
    private var useLike: Boolean
    private var onFinish: (CodeResult) -> Unit

    class Builder {
        /**
         * Construye la instancia de [GetResultFromCode] con la configuración actual.
         */
        fun build(): GetResultFromCode {
            return GetResultFromCode(this)
        }

        internal var code: String = ""
        internal var searchItemCode: Boolean = false
        internal var searchItemEan: Boolean = false
        internal var searchItemId: Boolean = false
        internal var searchItemRegex: Boolean = false
        internal var searchItemUrl: Boolean = false
        internal var searchOrder: Boolean = false
        internal var searchOrderExternalId: Boolean = false
        internal var searchRackId: Boolean = false
        internal var searchWarehouseAreaId: Boolean = false
        internal var useLike: Boolean = false
        internal var onFinish: (CodeResult) -> Unit = {}

        /**
         * Configura el código a buscar.
         */
        @Suppress("unused")
        fun withCode(code: String) = apply { this.code = code }

        /**
         * Activa la búsqueda en el campo de código de ítem.
         */
        @Suppress("unused")
        fun searchItemCode() = apply { searchItemCode = true }

        /**
         * Activa la búsqueda en el campo de código EAN de ítem.
         */
        @Suppress("unused")
        fun searchItemEan() = apply { searchItemEan = true }

        /**
         * Activa la búsqueda de etiquetas con prefijo para ítems.
         */
        @Suppress("unused")
        fun searchItemId() = apply { searchItemId = true }

        /**
         * Activa la búsqueda de etiquetas con Regex válidos para ítems.
         */
        @Suppress("unused")
        fun searchItemRegex() = apply { searchItemRegex = true }

        /**
         * Activa la búsqueda de códigos con URL a la vista de ítems de la API.
         */
        @Suppress("unused")
        fun searchItemUrl() = apply { searchItemUrl = true }

        /**
         * Activa la búsqueda de etiquetas con prefijo para pedidos.
         */
        @Suppress("unused")
        fun searchOrder() = apply { searchOrder = true }

        /**
         * Activa la búsqueda de códigos externos de pedidos.
         */
        @Suppress("unused")
        fun searchOrderExternalId() = apply { searchOrderExternalId = true }

        /**
         * Activa la búsqueda de etiquetas con prefijo para racks.
         */
        @Suppress("unused")
        fun searchRackId() = apply { searchRackId = true }

        /**
         * Activa la búsqueda de etiquetas con prefijo para áreas.
         */
        @Suppress("unused")
        fun searchWarehouseAreaId() = apply { searchWarehouseAreaId = true }

        /**
         * Activa la búsqueda utilizando operador LIKE (similitud).
         */
        @Suppress("unused")
        fun useLike() = apply { useLike = true }

        /**
         * Configura un callback que se llama cuando se completa la búsqueda.
         */
        @Suppress("unused")
        fun onFinish(callback: (CodeResult) -> Unit) = apply { onFinish = callback }
    }


    companion object {
        private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

        const val PREFIX_RACK = "#RK#"
        const val PREFIX_WA = "#WA#"
        const val PREFIX_ITEM = "#IT#"
        const val PREFIX_ORDER = "#ORD#"
        const val PREFIX_ITEM_URL = "item/view?id="

        const val FORMULA_RACK = """${PREFIX_RACK}(\d+)#"""
        const val FORMULA_WA = """${PREFIX_WA}(\d+)#"""
        const val FORMULA_ITEM = """${PREFIX_ITEM}(\d+)#"""
        const val FORMULA_ORDER = """${PREFIX_ORDER}(\d+)#"""

        fun playSoundNotification(success: Boolean) {
            try {
                val resId = if (success) R.raw.scan_success else R.raw.scan_fail
                val mp = MediaPlayer.create(context, resId)
                mp.setOnCompletionListener { mp.release() }
                mp.start()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        fun searchString(origin: String, formula: String, position: Int): String {
            val rx = Regex(formula)
            val matches = rx.matchEntire(origin)

            if (matches != null) {
                if (matches.groups.size >= position && matches.groups[position]?.value.toString()
                        .isNotEmpty()
                ) {
                    try {
                        return matches.groupValues[position]
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        val res =
                            "Error doing regex.\r\n formula $formula\r\n string $origin\r\n${ex.message}"
                        Log.e(tag, res)
                    }
                }
            }
            return ""
        }
    }

    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    data class CodeResult(var item: Any? = null, var qty: Double? = null, var lot: String? = null)

    @get:Synchronized
    private var isFounded: Boolean = false

    @Synchronized
    private fun getIsFounded(): Boolean {
        return isFounded
    }

    @Synchronized
    private fun setIsFounded() {
        isFounded = true
    }


    private fun taskPending(): Boolean {
        return searchItemCode ||
                searchItemEan ||
                searchItemId ||
                searchItemRegex ||
                searchItemUrl ||
                searchOrder ||
                searchOrderExternalId ||
                searchRackId ||
                searchWarehouseAreaId
    }

    data class CodePriority(
        val codeType: CodeType,
        val active: Boolean,
        val pos: Int,
    )

    private val defaultPriority: ArrayList<CodePriority>
        get() {
            val t: ArrayList<CodePriority> = ArrayList()
            t.add(CodePriority(CodeType.itemId, true, 1))
            t.add(CodePriority(CodeType.ean, true, 2))
            t.add(CodePriority(CodeType.itemCode, true, 3))
            t.add(CodePriority(CodeType.itemRegex, true, 4))
            return t
        }

    init {
        this.code = builder.code
        this.searchItemCode = builder.searchItemCode
        this.searchItemEan = builder.searchItemEan
        this.searchItemId = builder.searchItemId
        this.searchItemRegex = builder.searchItemRegex
        this.searchItemUrl = builder.searchItemUrl
        this.searchOrder = builder.searchOrder
        this.searchOrderExternalId = builder.searchOrderExternalId
        this.searchRackId = builder.searchRackId
        this.searchWarehouseAreaId = builder.searchWarehouseAreaId
        this.useLike = builder.useLike
        this.onFinish = builder.onFinish

        execute()
    }

    private fun execute() {
        // Reducir búsquedas innecesarias
        if (code.startsWith(PREFIX_RACK) && searchRackId) {
            searchRackId = true
            searchItemCode = false
            searchItemEan = false
            searchItemId = false
            searchItemRegex = false
            searchItemUrl = false
            searchOrder = false
            searchOrderExternalId = false
            searchWarehouseAreaId = false
        } else if (code.startsWith(PREFIX_WA) && searchWarehouseAreaId) {
            searchWarehouseAreaId = true
            searchItemCode = false
            searchItemEan = false
            searchItemId = false
            searchItemRegex = false
            searchItemUrl = false
            searchOrder = false
            searchOrderExternalId = false
            searchRackId = false
        } else if (code.startsWith(PREFIX_ORDER) && searchOrder) {
            searchOrder = true
            searchItemCode = false
            searchItemEan = false
            searchItemId = false
            searchItemRegex = false
            searchItemUrl = false
            searchOrderExternalId = false
            searchRackId = false
            searchWarehouseAreaId = false
        } else if (code.startsWith(PREFIX_ITEM) && searchItemId) {
            searchItemId = true
            searchItemCode = false
            searchItemEan = false
            searchItemRegex = false
            searchItemUrl = false
            searchOrder = false
            searchOrderExternalId = false
            searchRackId = false
            searchWarehouseAreaId = false
        } else if (code.contains(PREFIX_ITEM_URL) && searchItemUrl) {
            searchItemUrl = true
            searchItemCode = false
            searchItemEan = false
            searchItemId = false
            searchItemRegex = false
            searchOrder = false
            searchOrderExternalId = false
            searchRackId = false
            searchWarehouseAreaId = false
        } else {
            // Solo se usan con prefijo
            searchItemId = false
            searchItemUrl = false
            searchOrder = false
            searchRackId = false
            searchWarehouseAreaId = false
        }

        for (c in defaultPriority.sortedBy { it.pos }) {
            if (!c.active) continue

            if (c.codeType == CodeType.itemId && searchItemId && code.startsWith(PREFIX_ITEM, true)) {
                tryToGetItemById(code)
            } else if (c.codeType == CodeType.itemId && searchItemUrl && code.contains(PREFIX_ITEM_URL, true)) {
                tryToGetItemByUrl(code)
            } else if (c.codeType == CodeType.ean && searchItemEan) {
                tryToGetItemByEan(code)
            } else if (c.codeType == CodeType.itemCode && searchItemCode) {
                tryToGetItemCode(code)
            } else if (c.codeType == CodeType.itemRegex && searchItemRegex) {
                tryToGetItemRegex(code)
            }
        }

        if (searchOrder && code.startsWith(PREFIX_ORDER, true)) {
            getOrder(code)
        }

        if (searchOrderExternalId) {
            getOrderByExternalId(code)
        }

        if (searchRackId && code.startsWith(PREFIX_RACK, true)) {
            getRacks(code)
        }

        if (searchWarehouseAreaId && code.startsWith(PREFIX_WA, true)) {
            getWarehouseAreas(code)
        }

        val startTime = System.currentTimeMillis()
        while (taskPending()) {
            if (System.currentTimeMillis() - startTime == (WarehouseCounterApp.settingsVm.connectionTimeout * 1000).toLong()) {
                sendFinish(CodeResult())
                break
            }
        }
    }

    private fun tryToGetItemById(code: String) {
        thread {
            val match: String = searchString(code, FORMULA_ITEM, 1)
            if (match.isNotEmpty()) {
                val id: Long
                try {
                    id = match.toLong()
                } catch (ex: NumberFormatException) {
                    Log.i(tag, "Could not parse: $ex")
                    searchItemId = false
                    return@thread
                }

                if (id > 0) {
                    getItemById(id) {
                        sendFinish(it)
                    }
                } else searchItemId = false
            } else searchItemId = false
        }
    }

    private fun getItemById(id: Long, onFinish: (CodeResult) -> Unit) {
        ItemCoroutines.getById(
            itemId = id
        ) {
            if (it != null) {
                onItemIdResult(it.toKtor(), onFinish)
            } else {
                getItemByIdFromApi(id.toString(), onFinish)
            }
        }
    }

    private fun getItemByIdFromApi(id: String, onFinish: (CodeResult) -> Unit) {
        ViewItem(
            id = id,
            onFinish = { onItemIdResult(it, onFinish) }
        ).execute()
    }

    private fun tryToGetItemCode(code: String) {
        if (code.isNotEmpty()) {
            thread {
                getItemCode(code) {
                    sendFinish(it)
                }
            }
        } else searchItemCode = false
    }

    private fun getItemCode(code: String, onFinish: (CodeResult) -> Unit) {
        ItemCodeCoroutines.getByCode(
            code = code
        ) { codes ->
            if (codes.any()) {
                val itemCode = codes.first()
                val itemId = itemCode.itemId
                if (itemId != null) {
                    ItemCoroutines.getById(
                        itemId = itemId
                    ) { item ->
                        if (item != null) {
                            val itemCodeKtor = itemCode.toKtor()
                            itemCodeKtor.item = item.toKtor()
                            onItemCodeResult(itemCodeKtor, onFinish)
                        } else searchItemCode = false
                    }
                } else searchItemCode = false
            } else {
                getItemCodeFromApi(code, onFinish)
            }
        }
    }

    private fun getItemCodeFromApi(code: String, onFinish: (CodeResult) -> Unit) {
        GetItemCode(
            action = GetItemCode.defaultAction,
            filter = arrayListOf(
                ApiFilterParam(
                    columnName = ApiFilterParam.EXTENSION_ITEM_CODE_CODE,
                    value = code,
                    conditional = if (useLike) ACTION_OPERATOR_LIKE else ""
                )
            ),
            onEvent = { },
            onFinish = { itemCodes ->
                onItemCodeResult(itemCodes.firstOrNull(), onFinish)
            }
        ).execute()
    }

    private fun tryToGetItemRegex(code: String) {
        thread {
            ItemRegex.tryToRegex(code) {
                if (it.any()) {
                    if (it.count() > 1) Log.i(tag, "Existen múltiples coincidencias Regex")
                    val regexRes = it.firstOrNull()
                    if (regexRes == null) {
                        searchItemRegex = false
                        return@tryToRegex
                    }
                    if (regexRes.qty != null) {
                        getItemByItemRegex(regexRes) { codeResult ->
                            sendFinish(codeResult)
                        }
                    } else {
                        searchItemRegex = false
                        Log.e(tag, "Cantidad nula en Regex")
                    }
                } else searchItemRegex = false
            }
        }
    }

    private fun getItemByItemRegex(regexRes: RegexResult, onFinish: (CodeResult) -> Unit) {
        ItemCoroutines.getByQuery(
            ean = regexRes.ean
        ) {
            if (it.any()) {
                onItemRegexResult(it.first().toKtor(), regexRes, onFinish)
            } else {
                GetItem(
                    filter = arrayListOf(
                        ApiFilterParam(
                            columnName = ApiFilterParam.EXTENSION_ITEM_EAN,
                            value = regexRes.ean,
                            conditional = if (useLike) ACTION_OPERATOR_LIKE else ""
                        )
                    ),
                    onEvent = { },
                    onFinish = { items ->
                        if (items.any()) onItemRegexResult(items.first(), regexRes, onFinish)
                        else searchItemRegex = false
                    }).execute()
            }
        }
    }

    private fun tryToGetItemByEan(code: String) {
        if (code.isNotEmpty()) {
            thread {
                getItemByEan(code) {
                    sendFinish(it)
                }
            }
        } else searchItemEan = false
    }

    private fun getItemByEan(code: String, onFinish: (CodeResult) -> Unit) {
        ItemCoroutines.getByQuery(
            ean = code
        ) {
            if (it.any()) {
                onItemEanResult(it.first().toKtor(), onFinish)
            } else {
                getItemByEanFromApi(code, onFinish)
            }
        }
    }

    private fun getItemByEanFromApi(code: String, onFinish: (CodeResult) -> Unit) {
        GetItem(
            filter = arrayListOf(
                ApiFilterParam(
                    columnName = ApiFilterParam.EXTENSION_ITEM_EAN,
                    value = code,
                    conditional = if (useLike) ACTION_OPERATOR_LIKE else ""
                )
            ),
            onEvent = { },
            onFinish = { items ->
                onItemEanResult(items.firstOrNull(), onFinish)
            }).execute()
    }

    private fun tryToGetItemByUrl(code: String) {
        thread {
            val match: String = code.substringAfterLast("?id=")
            if (match.isNotEmpty()) {
                val id: String
                try {
                    id = match
                } catch (ex: NumberFormatException) {
                    Log.i(tag, "Could not parse: $ex")
                    searchItemUrl = false
                    return@thread
                }

                val longId: Long = id.toLongOrNull() ?: id.toLongOrNull() ?: 0
                if (longId > 0) {
                    getItemById(longId) {
                        sendFinish(it)
                    }
                } else if (id.isNotEmpty()) {
                    getItemByIdFromApi(id, onFinish)
                    searchItemUrl = false
                }
            } else searchItemUrl = false
        }
    }

    private fun getOrder(code: String) {
        thread {
            val match: String = searchString(code, FORMULA_ORDER, 1)
            if (match.isNotEmpty()) {
                val id: Long
                try {
                    id = match.toLong()
                } catch (ex: NumberFormatException) {
                    Log.i(tag, "Could not parse: $ex")
                    searchOrder = false
                    return@thread
                }

                if (id > 0) {
                    ViewOrder(
                        id = id,
                        onFinish = {
                            onOrderResult(it)
                        }).execute()
                } else searchOrder = false
            } else searchOrder = false
        }
    }

    private fun getOrderByExternalId(code: String) {
        thread {
            if (code.isNotEmpty()) {
                GetOrder(
                    filter = arrayListOf(
                        ApiFilterParam(
                            columnName = ApiFilterParam.EXTENSION_ORDER_EXTERNAL_ID,
                            value = code,
                            conditional = ACTION_OPERATOR_LIKE
                        )
                    ),
                    onFinish = {
                        onOrderResult(it.firstOrNull())
                    }).execute()
            } else searchOrderExternalId = false
        }
    }

    private fun getWarehouseAreas(code: String) {
        thread {
            val match: String = searchString(code, FORMULA_WA, 1)
            if (match.isNotEmpty()) {
                val id: Long
                try {
                    id = match.toLong()
                } catch (ex: NumberFormatException) {
                    Log.i(tag, "Could not parse: $ex")
                    searchWarehouseAreaId = false
                    return@thread
                }

                if (id > 0) {
                    ViewWarehouseArea(
                        id = id,
                        onFinish = {
                            onWarehouseAreaResult(it)
                        }).execute()
                } else searchWarehouseAreaId = false
            } else searchWarehouseAreaId = false
        }
    }

    private fun getRacks(code: String) {
        thread {
            val match: String = searchString(code, FORMULA_RACK, 1)
            if (match.isNotEmpty()) {
                val id: Long
                try {
                    id = match.toLong()
                } catch (ex: NumberFormatException) {
                    Log.i(tag, "Could not parse: $ex")
                    searchRackId = false
                    return@thread
                }

                if (id > 0) {
                    ViewRack(
                        id = id,
                        onFinish = {
                            onRackResult(it)
                        }).execute()
                } else searchRackId = false
            } else searchRackId = false
        }
    }

    private fun sendFinish(r: CodeResult) {
        onFinish(r)
    }

    private fun onItemIdResult(r: Item?, onFinish: (CodeResult) -> Unit) {
        searchItemId = false
        searchItemUrl = false
        if (getIsFounded()) return

        if (r != null || !taskPending()) {
            if (r != null) {
                setIsFounded()
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            onFinish(CodeResult(item = r))
        }
    }

    private fun onItemCodeResult(r: ItemCode?, onFinish: (CodeResult) -> Unit) {
        searchItemCode = false
        if (getIsFounded()) return

        if (r != null || !taskPending()) {
            var item: Item? = null
            var qty: Double? = null

            if (r != null) {
                item = r.item
                qty = r.qty

                setIsFounded()
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            onFinish(CodeResult(item = item, qty = qty))
        }
    }

    private fun onItemEanResult(r: Item?, onFinish: (CodeResult) -> Unit) {
        searchItemEan = false
        if (getIsFounded()) return

        if (r != null || !taskPending()) {
            if (r != null) {
                setIsFounded()
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            onFinish(CodeResult(item = r))
        }
    }

    private fun onItemRegexResult(r: Item?, regexRes: RegexResult, onFinish: (CodeResult) -> Unit) {
        searchItemRegex = false
        if (getIsFounded()) return

        if (r != null || !taskPending()) {
            if (r != null) {
                setIsFounded()
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            onFinish(CodeResult(item = r, qty = regexRes.qty?.toDouble(), lot = regexRes.lot))
        }
    }

    private fun onWarehouseAreaResult(r: WarehouseArea?) {
        searchWarehouseAreaId = false
        if (getIsFounded()) return

        if (r != null || !taskPending()) {
            if (r != null) {
                setIsFounded()
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendFinish(CodeResult(r))
        }
    }

    private fun onRackResult(r: Rack?) {
        searchRackId = false
        if (getIsFounded()) return

        if (r != null || !taskPending()) {
            if (r != null) {
                setIsFounded()
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendFinish(CodeResult(r))
        }
    }

    private fun onOrderResult(r: OrderResponse?) {
        searchOrderExternalId = false
        searchOrder = false
        if (getIsFounded()) return

        if (r != null || !taskPending()) {
            if (r != null) {
                setIsFounded()
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendFinish(CodeResult(r))
        }
    }
}
