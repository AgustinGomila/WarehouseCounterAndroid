package com.dacosys.warehouseCounter.scanners.scanCode

import android.media.MediaPlayer
import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.Item
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.ItemCode
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Rack
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.functions.item.GetItem
import com.dacosys.warehouseCounter.data.ktor.v2.functions.item.ViewItem
import com.dacosys.warehouseCounter.data.ktor.v2.functions.itemCode.ViewItemCode
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.ViewRack
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.ViewWarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.ViewOrder
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam.Companion.ACTION_OPERATOR_LIKE
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import kotlin.concurrent.thread

class GetResultFromCode(
    code: String,
    private var searchItemId: Boolean = false,
    private var searchItemCode: Boolean = false,
    private var searchItemEan: Boolean = false,
    private var searchItemUrl: Boolean = false,
    private var searchWarehouseAreaId: Boolean = false,
    private var searchRackId: Boolean = false,
    private var searchOrder: Boolean = false,
    private var useLike: Boolean = false,
    private var onFinish: (GetFromCodeResult) -> Unit
) {
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
                val mp = MediaPlayer.create(
                    context,
                    when {
                        success -> R.raw.scan_success
                        else -> R.raw.scan_fail
                    }
                )
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

    data class GetFromCodeResult(var typedObject: Any?)

    @get:Synchronized
    private var isFounded2: Boolean = false

    @Synchronized
    private fun getIsFounded(): Boolean {
        return isFounded2
    }

    @Synchronized
    private fun setIsFounded() {
        isFounded2 = true
    }


    private fun taskPending(): Boolean {
        return searchWarehouseAreaId || searchRackId || searchItemId || searchItemCode || searchItemEan || searchItemUrl || searchOrder
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
            return t
        }

    init {
        // Reducir búsquedas innecesarias
        if (code.startsWith(PREFIX_RACK) && searchRackId) {
            searchRackId = true
            searchItemCode = false
            searchItemEan = false
            searchItemId = false
            searchItemUrl = false
            searchOrder = false
            searchWarehouseAreaId = false
        } else if (code.startsWith(PREFIX_WA) && searchWarehouseAreaId) {
            searchWarehouseAreaId = true
            searchItemCode = false
            searchItemEan = false
            searchItemId = false
            searchItemUrl = false
            searchOrder = false
            searchRackId = false
        } else if (code.startsWith(PREFIX_ORDER) && searchOrder) {
            searchOrder = true
            searchItemCode = false
            searchItemEan = false
            searchItemId = false
            searchItemUrl = false
            searchRackId = false
            searchWarehouseAreaId = false
        } else if (code.startsWith(PREFIX_ITEM) && searchItemId) {
            searchItemId = true
            searchItemCode = false
            searchItemEan = false
            searchItemUrl = false
            searchOrder = false
            searchRackId = false
            searchWarehouseAreaId = false
        } else if (code.contains(PREFIX_ITEM_URL) && searchItemUrl) {
            searchItemUrl = true
            searchItemCode = false
            searchItemEan = false
            searchItemId = false
            searchOrder = false
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
                getItem(code)
            } else if (c.codeType == CodeType.itemId && searchItemUrl && code.contains(PREFIX_ITEM_URL, true)) {
                getItemByUrl(code)
            } else if (c.codeType == CodeType.ean && searchItemEan) {
                getItemByEan(code)
            } else if (c.codeType == CodeType.itemCode && searchItemCode) {
                getItemByItemCode(code)
            }
        }

        if (searchOrder && code.startsWith(PREFIX_ORDER, true)) {
            getOrder(code)
        }

        if (searchRackId && code.startsWith(PREFIX_RACK, true)) {
            getRacks(code)
        }

        if (searchWarehouseAreaId && code.startsWith(PREFIX_WA, true)) {
            getWarehouseAreas(code)
        }
    }

    private fun getItem(code: String) {
        val match: String = searchString(code, FORMULA_ITEM, 1)
        if (match.isNotEmpty()) {
            val id: Long
            try {
                id = match.toLong()
            } catch (ex: NumberFormatException) {
                Log.i(tag, "Could not parse: $ex")
                return
            }

            if (id > 0) {
                thread {
                    ViewItem(
                        id = id,
                        onFinish = {
                            onItemIdResult(it)
                        }).execute()
                }
            }
        }
    }

    private fun getItemByItemCode(code: String) {
        if (code.isNotEmpty()) {
            val id: Long
            try {
                id = code.toLong()
            } catch (ex: NumberFormatException) {
                Log.i(tag, "Could not parse: $ex")
                return
            }

            if (id > 0) {
                thread {
                    ViewItemCode(
                        id = id,
                        action = ViewItemCode.defaultAction,
                        onFinish = {
                            onItemCodeResult(it)
                        }).execute()
                }
            }
        }
    }

    private fun getItemByEanLocal(code: String) {
        ItemCoroutines.getByQuery(
            ean = code
        ) {
            if (it.any()) {
                val r = it.first().toKtor()
                sendMessage(GetFromCodeResult(r))
            } else {
                sendMessage(GetFromCodeResult(null))
            }
        }
    }

    private fun getItemByEan(code: String) {
        if (code.isNotEmpty()) {
            thread {
                GetItem(
                    filter = arrayListOf(
                        ApiFilterParam(
                            columnName = ApiFilterParam.EXTENSION_ITEM_EAN,
                            value = code,
                            conditional = if (useLike) ACTION_OPERATOR_LIKE else ""
                        )
                    ),
                    onEvent = { },
                    onFinish = {
                        if (it.any()) onItemEanResult(it.first())
                        else getItemByEanLocal(code)
                    }).execute()
            }
        }
    }

    private fun getItemByUrl(code: String) {
        val match: String = code.substringAfterLast("?id=")
        if (match.isNotEmpty()) {
            val id: Long
            try {
                id = match.toLong()
            } catch (ex: NumberFormatException) {
                Log.i(tag, "Could not parse: $ex")
                return
            }

            if (id > 0) {
                thread {
                    ViewItem(
                        id = id,
                        onFinish = {
                            onItemByUrlResult(it)
                        }).execute()
                }
            }
        }
    }

    private fun getOrder(code: String) {
        val match: String = searchString(code, FORMULA_ORDER, 1)
        if (match.isNotEmpty()) {
            val id: Long
            try {
                id = match.toLong()
            } catch (ex: NumberFormatException) {
                Log.i(tag, "Could not parse: $ex")
                return
            }

            if (id > 0) {
                thread {
                    ViewOrder(
                        id = id,
                        onFinish = {
                            onOrderResult(it)
                        }).execute()
                }
            }
        }
    }

    private fun getWarehouseAreas(code: String) {
        val match: String = searchString(code, FORMULA_WA, 1)
        if (match.isNotEmpty()) {
            val id: Long
            try {
                id = match.toLong()
            } catch (ex: NumberFormatException) {
                Log.i(tag, "Could not parse: $ex")
                return
            }

            if (id > 0) {
                thread {
                    ViewWarehouseArea(
                        id = id,
                        onFinish = {
                            onWarehouseAreaResult(it)
                        }).execute()
                }
            }
        }
    }

    private fun getRacks(code: String) {
        val match: String = searchString(code, FORMULA_RACK, 1)
        if (match.isNotEmpty()) {
            val id: Long
            try {
                id = match.toLong()
            } catch (ex: NumberFormatException) {
                Log.i(tag, "Could not parse: $ex")
                return
            }

            if (id > 0) {
                thread {
                    ViewRack(
                        id = id,
                        onFinish = {
                            onRackResult(it)
                        }).execute()
                }
            }
        }
    }

    private fun sendMessage(r: GetFromCodeResult) {
        onFinish(r)
    }

    private fun onItemIdResult(r: Item?) {
        searchItemId = false
        if (getIsFounded()) return

        if (r != null || !taskPending()) {
            if (r != null) {
                setIsFounded()
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendMessage(GetFromCodeResult(r?.let { arrayListOf(it) }))
        }
    }

    private fun onItemCodeResult(r: ItemCode?) {
        searchItemCode = false
        if (getIsFounded()) return

        if (r != null || !taskPending()) {
            if (r != null) {
                setIsFounded()
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendMessage(GetFromCodeResult(r?.let { arrayListOf(it) }))
        }
    }

    private fun onItemEanResult(r: Item?) {
        searchItemEan = false
        if (getIsFounded()) return

        if (r != null || !taskPending()) {
            if (r != null) {
                setIsFounded()
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendMessage(GetFromCodeResult(r?.let { arrayListOf(it) }))
        }
    }

    private fun onItemByUrlResult(r: Item?) {
        searchItemUrl = false
        if (getIsFounded()) return

        if (r != null || !taskPending()) {
            if (r != null) {
                setIsFounded()
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendMessage(GetFromCodeResult(r?.let { arrayListOf(it) }))
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
            sendMessage(GetFromCodeResult(r?.let { arrayListOf(it) }))
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
            sendMessage(GetFromCodeResult(r?.let { arrayListOf(it) }))
        }
    }

    private fun onOrderResult(r: OrderResponse?) {
        searchOrder = false
        if (getIsFounded()) return

        if (r != null || !taskPending()) {
            if (r != null) {
                setIsFounded()
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendMessage(GetFromCodeResult(r?.let { arrayListOf(it) }))
        }
    }
}
