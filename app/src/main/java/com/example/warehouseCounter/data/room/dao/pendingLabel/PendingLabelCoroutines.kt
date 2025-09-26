package com.example.warehouseCounter.data.room.dao.pendingLabel


import android.util.Log
import com.example.warehouseCounter.data.room.database.WcTempDatabase.Companion.database
import com.example.warehouseCounter.data.room.entity.pendingLabel.PendingLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

object PendingLabelCoroutines {
    @Throws(Exception::class)
    fun add(
        pendingIdList: List<Long>,
        onResult: () -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            async { database.pendingLabelDao().insertList(pendingIdList) }.await()
            onResult()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult()
        }
    }

    @Throws(Exception::class)
    fun remove(
        pendingIdList: List<Long>,
        onResult: () -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            async { database.pendingLabelDao().deleteList(pendingIdList) }.await()
            onResult()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult()
        }
    }

    @Throws(Exception::class)
    fun get(
        onResult: (ArrayList<PendingLabel>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { ArrayList(database.pendingLabelDao().getAll()) }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }
}
