package com.dacosys.warehouseCounter.data.room.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.room.dao.orderRequest.LogDao
import com.dacosys.warehouseCounter.data.room.dao.orderRequest.OrderRequestContentDao
import com.dacosys.warehouseCounter.data.room.dao.orderRequest.OrderRequestDao
import com.dacosys.warehouseCounter.data.room.dao.pendingLabel.PendingLabelDao
import com.dacosys.warehouseCounter.data.room.database.WcTempDatabase.Companion.DATABASE_VERSION
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.Log
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.OrderRequestContent
import com.dacosys.warehouseCounter.data.room.entity.pendingLabel.PendingLabel
import com.dacosys.warehouseCounter.data.room.entity.pendingLabel.PendingLabelEntry

@Database(
    entities = [OrderRequest::class, OrderRequestContent::class, Log::class, PendingLabel::class],
    version = DATABASE_VERSION
)
abstract class WcTempDatabase : RoomDatabase() {
    abstract fun orderRequestDao(): OrderRequestDao
    abstract fun orderRequestContentDao(): OrderRequestContentDao
    abstract fun logDao(): LogDao
    abstract fun pendingLabelDao(): PendingLabelDao

    companion object {
        const val DATABASE_VERSION = 2
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
                        )
                            .addMigrations(MIGRATION_1_2)
                            .build()

                    INSTANCE = instance
                    instance
                }
            }

        /**
         * Migration 1 2
         * Se agregó una tabla "pending_label" donde se guardan los nuevos ID de los pedidos enviados
         * sin las etiquetas impresas, para que el operario pueda imprimirlas cuando lo necesite.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS ${PendingLabelEntry.TABLE_NAME} 
                        |(${PendingLabelEntry.ID} INTEGER NOT NULL PRIMARY KEY)""".trimMargin()
                )
            }
        }

        fun cleanInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
