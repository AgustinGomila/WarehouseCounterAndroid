package com.dacosys.warehouseCounter.room.dao.orderRequest

import com.dacosys.warehouseCounter.ktor.v2.dto.order.Log
import com.dacosys.warehouseCounter.room.database.WcTempDatabase.Companion.database
import kotlinx.coroutines.*

object LogCoroutines {
    @Throws(Exception::class)
    fun getByOrderId(
        orderId: Long,
        onResult: (ArrayList<Log>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { ArrayList(database.logDao().getByOrderId(orderId).map { it.toLogKtor }) }.await()
            onResult(r)
        } catch (e: Exception) {
            android.util.Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }

    @Throws(Exception::class)
    fun add(
        orderId: Long,
        log: Log,
        onResult: (Long?) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { database.logDao().insert(log.toRoom(orderId)) }.await()
            onResult(r)
        } catch (e: Exception) {
            android.util.Log.e(javaClass.simpleName, e.message.toString())
            onResult(null)
        }
    }
}