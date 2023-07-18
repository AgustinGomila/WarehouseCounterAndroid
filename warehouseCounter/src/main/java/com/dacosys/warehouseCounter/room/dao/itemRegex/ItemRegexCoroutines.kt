package com.dacosys.warehouseCounter.room.dao.itemRegex

import android.util.Log
import com.dacosys.warehouseCounter.room.database.WcDatabase.Companion.database
import com.dacosys.warehouseCounter.room.entity.itemRegex.ItemRegex
import kotlinx.coroutines.*

object ItemRegexCoroutines {
    @Throws(Exception::class)
    fun get(
        onlyActive: Boolean = true,
        onResult: (ArrayList<ItemRegex>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async {
                ArrayList(
                    if (onlyActive) database.itemRegexDao().getActive()
                    else database.itemRegexDao().getAll()
                )
            }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }
}