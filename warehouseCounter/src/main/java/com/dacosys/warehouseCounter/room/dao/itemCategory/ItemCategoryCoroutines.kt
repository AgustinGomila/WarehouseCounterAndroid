package com.dacosys.warehouseCounter.room.dao.itemCategory

import android.util.Log
import com.dacosys.warehouseCounter.room.database.WcDatabase
import com.dacosys.warehouseCounter.room.entity.itemCategory.ItemCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ItemCategoryCoroutines {
    @Throws(Exception::class)
    fun getById(
        itemCategoryId: Long,
        onResult: (ItemCategory?) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = WcDatabase.getDatabase().itemCategoryDao().getById(itemCategoryId)
            onResult.invoke(r)
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
            val r = ArrayList(WcDatabase.getDatabase().itemCategoryDao().getAll())
            onResult.invoke(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }
}