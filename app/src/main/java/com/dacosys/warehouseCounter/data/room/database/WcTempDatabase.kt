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
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.OrderRequestEntry
import com.dacosys.warehouseCounter.data.room.entity.pendingLabel.PendingLabel
import com.dacosys.warehouseCounter.data.room.entity.pendingLabel.PendingLabelEntry

@Database(
    entities = [OrderRequest::class, OrderRequestContent::class, Log::class, PendingLabel::class],
    version = DATABASE_VERSION,
    exportSchema = true
)
abstract class WcTempDatabase : RoomDatabase() {
    abstract fun orderRequestDao(): OrderRequestDao
    abstract fun orderRequestContentDao(): OrderRequestContentDao
    abstract fun logDao(): LogDao
    abstract fun pendingLabelDao(): PendingLabelDao

    companion object {
        private val TAG = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName
        const val DATABASE_VERSION = 3
        const val DATABASE_NAME = "wc_temp.sqlite"

        @Volatile
        private var INSTANCE: WcTempDatabase? = null

        val database: WcTempDatabase
            get() {
                synchronized(this) {
                    var instance = INSTANCE
                    if (instance == null) {
                        instance = Room.databaseBuilder(
                            context = context,
                            klass = WcTempDatabase::class.java,
                            name = DATABASE_NAME
                        )
                            .addMigrations(MIGRATION_1_2)
                            .addMigrations(MIGRATION_2_3)
                            .build()
                        INSTANCE = instance
                        android.util.Log.i(TAG, "NEW Instance: $INSTANCE")
                    }
                    return instance
                }
            }

        /**
         * Migration 1 2
         * Se agregó una tabla "pending_label" donde se guardan los nuevos ID de los pedidos enviados
         * sin las etiquetas impresas, para que el operario pueda imprimirlas cuando lo necesite.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS ${PendingLabelEntry.TABLE_NAME} 
                        |(${PendingLabelEntry.ID} INTEGER NOT NULL PRIMARY KEY)""".trimMargin()
                )
            }
        }

        /**
         * Migration 2 3
         *
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """ALTER TABLE ${OrderRequestEntry.TABLE_NAME} ADD ${OrderRequestEntry.ORDER_REQUEST_ID} INTEGER NOT NULL""".trimMargin()
                )
                db.execSQL(
                    """CREATE INDEX IDX_${OrderRequestEntry.TABLE_NAME}_${OrderRequestEntry.ORDER_REQUEST_ID} ON ${OrderRequestEntry.TABLE_NAME} (${OrderRequestEntry.ORDER_REQUEST_ID})""".trimIndent()
                )
            }
        }

        fun cleanInstance() {
            if (INSTANCE?.isOpen == true) {
                android.util.Log.i(TAG, "CLOSING Instance: $INSTANCE")
                INSTANCE?.close()
            }
            INSTANCE = null
        }
    }
}