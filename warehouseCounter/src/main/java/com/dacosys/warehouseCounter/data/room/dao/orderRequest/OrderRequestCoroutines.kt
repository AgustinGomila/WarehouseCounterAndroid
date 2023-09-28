package com.dacosys.warehouseCounter.data.room.dao.orderRequest

import android.util.Log
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.generateFilename
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.writeJsonToFile
import com.dacosys.warehouseCounter.data.room.database.WcTempDatabase.Companion.database
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import kotlinx.coroutines.*
import java.io.UnsupportedEncodingException
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest as OrderRequestKtor
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestContent as OrderRequestContentKtor

object OrderRequestCoroutines {

    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    @Throws(Exception::class)
    fun getByIdAsKtor(
        id: Long,
        onResult: (OrderRequestKtor?) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { database.orderRequestDao().getById(id)?.toKtor }.await()
            r?.roomId = id

            if (r != null) {
                val rc =
                    async { database.orderRequestContentDao().getByOrderId(id).map { it.toKtor } }.await()
                r.contents = rc
            }
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(null)
        }
    }

    @Throws(Exception::class)
    fun add(
        orderRequest: OrderRequest,
        onResult: (Long?) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val id = async { database.orderRequestDao().insert(orderRequest) }.await()
            onResult(id)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(null)
        }
    }

    @Throws(Exception::class)
    private fun update(
        orderRequest: OrderRequestKtor,
        contents: List<OrderRequestContentKtor>,
        onResult: (Boolean) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            async {
                val id = orderRequest.roomId
                val newContent = contents.map { it.toRoom(id) }.toList()
                val orRoom = orderRequest.toRoom

                database.orderRequestDao().update(
                    orderRequest = orRoom,
                    contents = ArrayList(newContent),
                )
            }.await()
            onResult(true)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(false)
        }
    }

    fun update(
        orderRequest: OrderRequestKtor,
        onEvent: (SnackBarEventData) -> Unit
    ): Boolean {
        var isDone = false
        var isOk = false

        try {
            var filename = orderRequest.filename.substringAfterLast("/")
            if (filename.isEmpty()) {
                filename = "${generateFilename()}.json"
            }

            val orJson = json.encodeToString(OrderRequestKtor.serializer(), orderRequest)
            Log.i(tag, orJson)

            update(orderRequest, orderRequest.contents) {
                isOk = if (it) {
                    writeJsonToFile(
                        filename = filename,
                        value = orJson,
                        completed = orderRequest.completed ?: false,
                        onEvent = onEvent,
                    )
                } else {
                    false
                }
                isDone = true
            }
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            Log.e(tag, e.message ?: "")
            isOk = false
            isDone = true
        }

        val startTime = System.currentTimeMillis()
        while (!isDone) {
            if (System.currentTimeMillis() - startTime == WarehouseCounterApp.settingsVm.connectionTimeout.toLong()) {
                isDone = true
            }
        }

        return isOk
    }

    @Throws(Exception::class)
    fun removeById(
        id: Long,
        onResult: () -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            async { database.orderRequestDao().deleteWithContent(id) }.await()
            onResult()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult()
        }
    }

    @Throws(Exception::class)
    fun removeById(
        idList: List<Long>,
        onResult: () -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            async { database.orderRequestDao().deleteWithContent(idList) }.await()
            onResult()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult()
        }
    }
}
