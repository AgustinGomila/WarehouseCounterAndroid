package com.dacosys.warehouseCounter.room.dao.itemCode

import android.util.Log
import com.dacosys.warehouseCounter.room.database.WcDatabase
import com.dacosys.warehouseCounter.room.entity.itemCode.ItemCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ItemCodeCoroutines {
    @Throws(Exception::class)
    fun getByItemId(
        itemId: Long,
        onResult: (ArrayList<ItemCode>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = ArrayList(WcDatabase.getDatabase().itemCodeDao().getByItemId(itemId))
            onResult.invoke(r)
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
            val r = ArrayList(WcDatabase.getDatabase().itemCodeDao().getByCode(code))
            onResult.invoke(r)
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
            val r = ArrayList(WcDatabase.getDatabase().itemCodeDao().getToUpload())
            onResult.invoke(r)
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
            WcDatabase.getDatabase().itemCodeDao().insert(itemCode)
            onResult.invoke()
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
            val r = WcDatabase.getDatabase().itemCodeDao().countLink(itemId, code)
            onResult.invoke(r)
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
            WcDatabase.getDatabase().itemCodeDao().unlinkCode(itemId, code)
            onResult.invoke()
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
            WcDatabase.getDatabase().itemCodeDao().updateQty(itemId, code, qty)
            onResult.invoke()
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
            WcDatabase.getDatabase().itemCodeDao().updateTransferred(itemId, code)
            onResult.invoke()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult()
        }
    }
}