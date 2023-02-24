package com.dacosys.warehouseCounter.room.dao.item

import android.util.Log
import com.dacosys.warehouseCounter.room.database.WcDatabase
import com.dacosys.warehouseCounter.room.entity.item.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ItemCoroutines {
    @Throws(Exception::class)
    fun getById(
        itemId: Long,
        onResult: (Item?) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = WcDatabase.getDatabase().itemDao().getById(itemId)
            onResult.invoke(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(null)
        }
    }

    @Throws(Exception::class)
    fun get(
        onResult: (ArrayList<Item>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = ArrayList(WcDatabase.getDatabase().itemDao().getAll())
            onResult.invoke(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }

    @Throws(Exception::class)
    fun getByItemCategoryId(
        itemCatId: Long,
        onResult: (ArrayList<Item>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = ArrayList(WcDatabase.getDatabase().itemDao().getByItemCategoryId(itemCatId))
            onResult.invoke(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }

    @Throws(Exception::class)
    fun getByQuery(
        ean: String = "",
        description: String = "",
        itemCategoryId: Long? = null,
        onResult: (ArrayList<Item>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val query = ItemDao.getEanDescCatQuery(ean, description, itemCategoryId)
            val r = ArrayList(WcDatabase.getDatabase().itemDao().getByQuery(query))
            onResult.invoke(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }

    @Throws(Exception::class)
    fun getCodes(
        onlyActive: Boolean = true,
        onResult: (ArrayList<String>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = ArrayList(WcDatabase.getDatabase().itemDao().getCodes(if (onlyActive) 1 else 0))
            onResult.invoke(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }

    @Throws(Exception::class)
    fun add(
        item: Item,
        onResult: (Long?) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = WcDatabase.getDatabase().itemDao().insert(item)
            onResult.invoke(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(null)
        }
    }

    @Throws(Exception::class)
    fun updateDescription(
        itemId: Long,
        description: String = "",
        onResult: () -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            WcDatabase.getDatabase().itemDao().updateDescription(itemId, description)
            onResult.invoke()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult()
        }
    }
}