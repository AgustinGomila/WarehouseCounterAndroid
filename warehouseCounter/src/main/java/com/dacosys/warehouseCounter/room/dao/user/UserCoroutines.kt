package com.dacosys.warehouseCounter.room.dao.user

import android.util.Log
import com.dacosys.warehouseCounter.room.database.WcDatabase
import com.dacosys.warehouseCounter.room.entity.user.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class UserCoroutines {
    @Throws(Exception::class)
    fun getById(
        itemId: Long,
        onResult: (User?) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = WcDatabase.getDatabase().userDao().getById(itemId)
            onResult.invoke(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(null)
        }
    }

    @Throws(Exception::class)
    fun getByName(
        name: String,
        onResult: (User?) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = WcDatabase.getDatabase().userDao().getByName(name)
            onResult.invoke(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(null)
        }
    }

    @Throws(Exception::class)
    fun get(
        onResult: (ArrayList<User>) -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            val r = ArrayList(WcDatabase.getDatabase().userDao().getAll())
            onResult.invoke(r)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult(ArrayList())
        }
    }

    @Throws(Exception::class)
    fun add(
        user: User,
        onResult: () -> Unit = {},
    ) = CoroutineScope(Job() + Dispatchers.IO).launch {
        try {
            WcDatabase.getDatabase().userDao().insert(user)
            onResult.invoke()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
            onResult()
        }
    }
}