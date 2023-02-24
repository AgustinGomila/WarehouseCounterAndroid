package com.dacosys.warehouseCounter.room.dao.itemRegex

import android.util.Log
import com.dacosys.warehouseCounter.room.database.WcDatabase
import com.dacosys.warehouseCounter.room.entity.itemRegex.ItemRegex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ItemRegexCoroutines {
    @Throws(Exception::class)
    fun get(
        onlyActive: Boolean = true,
        onResult: (ArrayList<ItemRegex>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = ArrayList(
                if (onlyActive) WcDatabase.getDatabase().itemRegexDao().getActive()
                else WcDatabase.getDatabase().itemRegexDao().getAll()
            )
            onResult.invoke(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }
}