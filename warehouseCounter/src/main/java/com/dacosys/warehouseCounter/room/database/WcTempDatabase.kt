package com.dacosys.warehouseCounter.room.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.room.dao.orderRequest.LogDao
import com.dacosys.warehouseCounter.room.dao.orderRequest.OrderRequestContentDao
import com.dacosys.warehouseCounter.room.dao.orderRequest.OrderRequestDao
import com.dacosys.warehouseCounter.room.database.WcTempDatabase.Companion.DATABASE_VERSION
import com.dacosys.warehouseCounter.room.entity.orderRequest.Log
import com.dacosys.warehouseCounter.room.entity.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.room.entity.orderRequest.OrderRequestContent

@Database(
    entities = [OrderRequest::class, OrderRequestContent::class, Log::class],
    version = DATABASE_VERSION
)
abstract class WcTempDatabase : RoomDatabase() {
    abstract fun orderRequestDao(): OrderRequestDao
    abstract fun orderRequestContentDao(): OrderRequestContentDao
    abstract fun logDao(): LogDao

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "wc_temp.sqlite"

        @Volatile
        private var INSTANCE: WcTempDatabase? = null

        val database: WcTempDatabase
            get() {
                return INSTANCE ?: synchronized(this) {
                    val instance =
                        Room.databaseBuilder(
                            context = context,
                            klass = WcTempDatabase::class.java,
                            name = DATABASE_NAME
                        ).build()

                    INSTANCE = instance
                    instance
                }
            }

        fun cleanInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}