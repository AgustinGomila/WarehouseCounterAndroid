package com.example.warehouseCounter.data.room.dao.itemCategory

import android.util.Log
import com.example.warehouseCounter.data.room.database.WcDatabase.Companion.database
import com.example.warehouseCounter.data.room.entity.itemCategory.ItemCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

object ItemCategoryCoroutines {

    @Throws(Exception::class)
    fun count(
        onResult: (Int) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { database.itemCategoryDao().count() }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(0)
        }
    }

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
