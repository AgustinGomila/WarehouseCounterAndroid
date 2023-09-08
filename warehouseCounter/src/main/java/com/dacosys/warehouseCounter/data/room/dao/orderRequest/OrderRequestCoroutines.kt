package com.dacosys.warehouseCounter.data.room.dao.orderRequest

import android.util.Log
import com.dacosys.warehouseCounter.data.room.database.WcTempDatabase.Companion.database
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.OrderRequest
import kotlinx.coroutines.*
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest as OrderRequestKtor
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestContent as OrderRequestContentKtor

object OrderRequestCoroutines {
    @Throws(Exception::class)
    fun getOrderRequestById(
        id: Long,
        onResult: (OrderRequestKtor?) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { database.orderRequestDao().getById(id)?.toKtor }.await()
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
    fun update(
        orderRequest: OrderRequestKtor,
        contents: List<OrderRequestContentKtor>,
        onResult: (Boolean) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            async {
                val id = orderRequest.orderRequestId ?: 0
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
