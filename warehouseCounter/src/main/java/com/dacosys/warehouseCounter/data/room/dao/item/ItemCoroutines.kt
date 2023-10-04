package com.dacosys.warehouseCounter.data.room.dao.item

import android.util.Log
import com.dacosys.warehouseCounter.data.room.database.WcDatabase.Companion.database
import com.dacosys.warehouseCounter.data.room.entity.item.Item
import kotlinx.coroutines.*

object ItemCoroutines {
    @Throws(Exception::class)
    fun getById(
        itemId: Long,
        onResult: (Item?) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { database.itemDao().getById(itemId) }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(null)
        }
    }

    @Throws(Exception::class)
    fun count(
        onResult: (Int) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { database.itemDao().count() }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(0)
        }
    }

    @Throws(Exception::class)
    fun get(
        onResult: (ArrayList<Item>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { ArrayList(database.itemDao().getAll()) }.await()
            onResult(r)
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
            val r = async { ArrayList(database.itemDao().getByItemCategoryId(itemCatId)) }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }

    @Throws(Exception::class)
    fun getByQuery(
        ean: String = "",
        externalId: String = "",
        description: String = "",
        itemCategoryId: Long? = null,
        onResult: (ArrayList<Item>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async {
                val query = ItemDao.getMultiQuery(
                    ean = ean,
                    externalId = externalId,
                    description = description,
                    itemCategoryId = itemCategoryId
                )
                ArrayList(database.itemDao().getByQuery(query))
            }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }

    @Throws(Exception::class)
    fun getEanCodes(
        onlyActive: Boolean = true,
        onResult: (ArrayList<String>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { ArrayList(database.itemDao().getEanCodes(if (onlyActive) 1 else 0)) }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }

    @Throws(Exception::class)
    fun getIds(
        onlyActive: Boolean = true,
        onResult: (ArrayList<Long>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { ArrayList(database.itemDao().getIds(if (onlyActive) 1 else 0)) }.await()
            onResult(r)
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
            val r = async { database.itemDao().insert(item) }.await()
            onResult(r)
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
            async {
                database.itemDao().updateDescription(
                    itemId = itemId,
                    description = description
                )
            }.await()
            onResult()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult()
        }
    }
}
