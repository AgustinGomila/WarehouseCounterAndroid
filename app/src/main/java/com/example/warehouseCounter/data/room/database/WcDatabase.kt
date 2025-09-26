package com.example.warehouseCounter.data.room.database

import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.data.room.dao.client.ClientDao
import com.example.warehouseCounter.data.room.dao.item.ItemDao
import com.example.warehouseCounter.data.room.dao.itemCategory.ItemCategoryDao
import com.example.warehouseCounter.data.room.dao.itemCode.ItemCodeDao
import com.example.warehouseCounter.data.room.dao.itemRegex.ItemRegexDao
import com.example.warehouseCounter.data.room.dao.lot.LotDao
import com.example.warehouseCounter.data.room.dao.user.UserDao
import com.example.warehouseCounter.data.room.database.WcDatabase.Companion.DATABASE_VERSION
import com.example.warehouseCounter.data.room.entity.client.Client
import com.example.warehouseCounter.data.room.entity.item.Item
import com.example.warehouseCounter.data.room.entity.itemCategory.ItemCategory
import com.example.warehouseCounter.data.room.entity.itemCode.ItemCode
import com.example.warehouseCounter.data.room.entity.itemRegex.ItemRegex
import com.example.warehouseCounter.data.room.entity.lot.Lot
import com.example.warehouseCounter.data.room.entity.user.User

@Database(
    entities = [Item::class, Client::class, ItemCategory::class, ItemCode::class, ItemRegex::class, Lot::class, User::class],
    version = DATABASE_VERSION,
    exportSchema = true
)
abstract class WcDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun clientDao(): ClientDao
    abstract fun itemCategoryDao(): ItemCategoryDao
    abstract fun itemCodeDao(): ItemCodeDao
    abstract fun itemRegexDao(): ItemRegexDao
    abstract fun lotDao(): LotDao
    abstract fun userDao(): UserDao

    companion object {
        private val TAG = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName
        const val DATABASE_VERSION = 2
        const val DATABASE_NAME = "wc.sqlite"

        @Volatile
        private var INSTANCE: WcDatabase? = null

        val database: WcDatabase
            get() {
                synchronized(this) {
                    var instance = INSTANCE
                    if (instance == null) {
                        instance = Room.databaseBuilder(
                            context = context,
                            klass = WcDatabase::class.java,
                            name = DATABASE_NAME
                        )
                            .createFromAsset(DATABASE_NAME)
                            .addMigrations(MIGRATION_1_2)
                            .build()
                        INSTANCE = instance
                        Log.i(TAG, "NEW Instance: $INSTANCE")
                    }
                    return instance
                }
            }

        fun cleanInstance() {
            if (INSTANCE?.isOpen == true) {
                Log.i(TAG, "CLOSING Instance: $INSTANCE")
                INSTANCE?.close()
            }
            INSTANCE = null
        }

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Since we didn't alter the table, there's nothing else to do here.
            }
        }
    }
}
