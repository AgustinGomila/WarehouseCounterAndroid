package com.dacosys.warehouseCounter.scanners.scanCode

import android.media.MediaPlayer
import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.Item
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Rack
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.functions.item.GetItem
import com.dacosys.warehouseCounter.data.ktor.v2.functions.item.GetItem.Companion.defaultAction
import com.dacosys.warehouseCounter.data.ktor.v2.functions.item.ViewItem
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
    private var useLike: Boolean = true,
    private var onFinish: (GetFromCodeResult) -> Unit
) {
    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    data class GetFromCodeResult(var typedObject: Any?)

    private fun sendMessage(r: GetFromCodeResult) {
        onFinish(r)
    }

    private fun onItemIdResult(r: Item?) {
        searchItemId = false
        if (isFounded) {
            return
        }

        if (r != null || !taskPending()) {
            if (r != null) {
                isFounded = true
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendMessage(GetFromCodeResult(r?.let { arrayListOf(it) }))
        }
    }

    private fun onItemCodeResult(r: Item?) {
        searchItemCode = false
        if (isFounded) {
            return
        }

        if (r != null || !taskPending()) {
            if (r != null) {
                isFounded = true
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendMessage(GetFromCodeResult(r?.let { arrayListOf(it) }))
        }
    }

    private fun onItemEanResult(r: Item?) {
        searchItemEan = false
        if (isFounded) {
            return
        }

        if (r != null || !taskPending()) {
            if (r != null) {
                isFounded = true
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendMessage(GetFromCodeResult(r?.let { arrayListOf(it) }))
        }
    }

    private fun onItemByUrlResult(r: Item?) {
        searchItemUrl = false
        if (isFounded) {
            return
        }

        if (r != null || !taskPending()) {
            if (r != null) {
                isFounded = true
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendMessage(GetFromCodeResult(r?.let { arrayListOf(it) }))
        }
    }

    private fun onWarehouseAreaResult(r: WarehouseArea?) {
        searchWarehouseAreaId = false
        if (isFounded) {
            return
        }

        if (r != null || !taskPending()) {
            if (r != null) {
                isFounded = true
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendMessage(GetFromCodeResult(r?.let { arrayListOf(it) }))
        }
    }

    private fun onRackResult(r: Rack?) {
        searchRackId = false
        if (isFounded) {
            return
        }

        if (r != null || !taskPending()) {
            if (r != null) {
                isFounded = true
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendMessage(GetFromCodeResult(r?.let { arrayListOf(it) }))
        }
    }

    private fun onOrderResult(r: OrderResponse?) {
        searchOrder = false
        if (isFounded) {
            return
        }

        if (r != null || !taskPending()) {
            if (r != null) {
                isFounded = true
                playSoundNotification(true)
            } else {
                playSoundNotification(false)
            }
            sendMessage(GetFromCodeResult(r?.let { arrayListOf(it) }))
        }
    }

    private fun playSoundNotification(success: Boolean) {
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

    private fun searchString(origin: String, formula: String, position: Int): String {
        //  ex @"#WA#{0:00000}#"
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

    private val prefixRack = "#RK#"
    private val prefixWa = "#WA#"
    private val prefixItem = "#IT#"
    private val prefixOrder = "#ORD#"
    private val prefixItemUrl = "item/view?id="

    private val formulaRack = """${prefixRack}(\d+)#"""
    private val formulaWa = """${prefixWa}(\d+)#"""
    private val formulaItem = """${prefixItem}(\d+)#"""
    private val formulaOrder = """${prefixOrder}(\d+)#"""
    private val formulaItemUrl = """${prefixItemUrl}(\d+)"""

    private var isFounded = false
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
        if (code.startsWith(prefixRack) && searchRackId) {
            searchWarehouseAreaId = false
            searchRackId = true
            searchItemId = false
            searchItemCode = false
            searchItemEan = false
            searchItemUrl = false
            searchOrder = false
        } else if (code.startsWith(prefixWa) && searchWarehouseAreaId) {
            searchWarehouseAreaId = true
            searchRackId = false
            searchItemId = false
            searchItemCode = false
            searchItemEan = false
            searchItemUrl = false
            searchOrder = false
        } else if (code.startsWith(prefixOrder) && searchOrder) {
            searchWarehouseAreaId = false
            searchRackId = false
            searchItemId = false
            searchItemCode = false
            searchItemEan = false
            searchItemUrl = false
            searchOrder = true
        } else if (code.startsWith(prefixItem) && searchItemId) {
            searchWarehouseAreaId = false
            searchRackId = false
            searchItemId = true
            searchItemCode = false
            searchItemEan = false
            searchItemUrl = false
            searchOrder = false
        } else if (code.contains(prefixItemUrl) && searchItemUrl) {
            searchWarehouseAreaId = false
            searchRackId = false
            searchItemId = false
            searchItemCode = false
            searchItemEan = false
            searchItemUrl = true
            searchOrder = false
        } else {
            // Solo se usan con prefijo
            searchWarehouseAreaId = false
            searchRackId = false
            searchItemId = false
            searchItemUrl = false
            searchOrder = false
        }

        for (c in defaultPriority.sortedBy { it.pos }) {
            if (!c.active) continue

            if (c.codeType == CodeType.itemId && searchItemId && code.startsWith(prefixItem, true)) {
                getItem(code)
            } else if (c.codeType == CodeType.itemId && searchItemUrl && code.contains(prefixItemUrl, true)) {
                getItemByUrl(code)
            } else if (c.codeType == CodeType.ean && searchItemEan) {
                getItemByEan(code)
            }
            // } else if (c.codeType == CodeType.itemCode && searchItemCode) {
            //     getItemByItemCode(code)
        }

        if (searchOrder && code.startsWith(prefixOrder, true)) {
            getOrder(code)
        }

        if (searchRackId && code.startsWith(prefixRack, true)) {
            getRacks(code)
        }

        if (searchWarehouseAreaId && code.startsWith(prefixWa, true)) {
            getWarehouseAreas(code)
        }
    }

    private fun getItem(code: String) {
        val match: String = searchString(code, formulaItem, 1)
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
                    GetItem(
                        action = defaultAction,
                        filter = arrayListOf(
                            ApiFilterParam(
                                columnName = ApiFilterParam.EXTENSION_ITEM_CODE,
                                value = code,
                                conditional = if (useLike) ACTION_OPERATOR_LIKE else ""
                            )
                        ),
                        onEvent = { },
                        onFinish = {
                            if (it.any()) onItemCodeResult(it.first())
                            else getItemByCodeLocal(code)
                        }).execute()
                }
            }
        }
    }

    private fun getItemByCodeLocal(code: String) {
        ItemCoroutines.getByQuery(
            ean = code
        ) {
            if (it.any()) {
                val r = it.first().toOrderRequestContent(code)
                sendMessage(GetFromCodeResult(r))
            } else {
                sendMessage(GetFromCodeResult(null))
            }
        }
    }

    private fun getItemByEan(code: String) {
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
                    GetItem(
                        action = defaultAction,
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
                            else getItemByCodeLocal(code)
                        }).execute()
                }
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
        val match: String = searchString(code, formulaOrder, 1)
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
        val match: String = searchString(code, formulaWa, 1)
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
        val match: String = searchString(code, formulaRack, 1)
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
}
