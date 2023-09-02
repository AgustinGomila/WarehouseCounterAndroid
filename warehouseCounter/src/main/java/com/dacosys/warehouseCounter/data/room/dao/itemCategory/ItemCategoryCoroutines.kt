package com.dacosys.warehouseCounter.data.room.dao.itemCategory

import android.util.Log
import com.dacosys.warehouseCounter.data.room.database.WcDatabase.Companion.database
import com.dacosys.warehouseCounter.data.room.entity.itemCategory.ItemCategory
import kotlinx.coroutines.*

object ItemCategoryCoroutines {
    @Throws(Exception::class)
    fun getById(
        itemCategoryId: Long,
        onResult: (ItemCategory?) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { database.itemCategoryDao().getById(itemCategoryId) }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(null)
        }
    }

    @Throws(Exception::class)
    fun get(
        onResult: (ArrayList<ItemCategory>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { ArrayList(database.itemCategoryDao().getAll()) }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }
}
