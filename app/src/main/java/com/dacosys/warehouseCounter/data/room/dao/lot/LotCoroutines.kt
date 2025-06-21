package com.dacosys.warehouseCounter.data.room.dao.lot

import android.util.Log
import com.dacosys.warehouseCounter.data.room.database.WcDatabase.Companion.database
import com.dacosys.warehouseCounter.data.room.entity.lot.Lot
import kotlinx.coroutines.*

object LotCoroutines {
    @Throws(Exception::class)
    fun count(
        onResult: (Int) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { database.lotDao().count() }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(0)
        }
    }

    @Throws(Exception::class)
    fun get(
        onResult: (ArrayList<Lot>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { ArrayList(database.lotDao().getAll()) }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }
}
