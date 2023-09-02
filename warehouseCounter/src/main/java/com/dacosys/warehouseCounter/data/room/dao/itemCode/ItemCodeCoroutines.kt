package com.dacosys.warehouseCounter.data.room.dao.itemCode

import android.util.Log
import com.dacosys.warehouseCounter.data.room.database.WcDatabase.Companion.database
import com.dacosys.warehouseCounter.data.room.entity.itemCode.ItemCode
import kotlinx.coroutines.*

object ItemCodeCoroutines {
    @Throws(Exception::class)
    fun getByItemId(
        itemId: Long,
        onResult: (ArrayList<ItemCode>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { ArrayList(database.itemCodeDao().getByItemId(itemId)) }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }

    @Throws(Exception::class)
    fun getByCode(
        code: String,
        onResult: (ArrayList<ItemCode>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { ArrayList(database.itemCodeDao().getByCode(code)) }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }

    @Throws(Exception::class)
    fun getToUpload(
        onResult: (ArrayList<ItemCode>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { ArrayList(database.itemCodeDao().getToUpload()) }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }


    @Throws(Exception::class)
    fun add(
        itemCode: ItemCode,
        onResult: () -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            async { database.itemCodeDao().insert(itemCode) }.await()
            onResult()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult()
        }
    }

    @Throws(Exception::class)
    fun countLink(
        itemId: Long,
        code: String = "",
        onResult: (Int) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async {
                database.itemCodeDao().countLink(
                    itemId = itemId,
                    code = code
                )
            }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(0)
        }
    }

    @Throws(Exception::class)
    fun unlinkCode(
        itemId: Long,
        code: String = "",
        onResult: () -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            async {
                database.itemCodeDao().unlinkCode(
                    itemId = itemId,
                    code = code
                )
            }.await()
            onResult()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult()
        }
    }

    @Throws(Exception::class)
    fun updateQty(
        itemId: Long,
        code: String = "",
        qty: Double?,
        onResult: () -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            async {
                database.itemCodeDao().updateQty(
                    itemId = itemId,
                    code = code,
                    qty = qty
                )
            }.await()
            onResult()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult()
        }
    }

    @Throws(Exception::class)
    fun updateTransferred(
        itemId: Long,
        code: String = "",
        onResult: () -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            async {
                database.itemCodeDao().updateTransferred(
                    itemId = itemId,
                    code = code
                )
            }.await()
            onResult()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult()
        }
    }
}
