package com.example.warehouseCounter.data.room.dao.client

import android.util.Log
import com.example.warehouseCounter.data.room.database.WcDatabase.Companion.database
import com.example.warehouseCounter.data.room.entity.client.Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

object ClientCoroutines {

    @Throws(Exception::class)
    fun count(
        onResult: (Int) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { database.clientDao().count() }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(0)
        }
    }

    @Throws(Exception::class)
    fun getById(
        clientId: Long,
        onResult: (Client?) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { database.clientDao().getById(clientId) }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(null)
        }
    }

    @Throws(Exception::class)
    fun get(
        onResult: (ArrayList<Client>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = async { ArrayList(database.clientDao().getAll()) }.await()
            onResult(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }
}
