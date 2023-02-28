package com.dacosys.warehouseCounter.room.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.room.dao.client.ClientDao
import com.dacosys.warehouseCounter.room.dao.item.ItemDao
import com.dacosys.warehouseCounter.room.dao.itemCategory.ItemCategoryDao
import com.dacosys.warehouseCounter.room.dao.itemCode.ItemCodeDao
import com.dacosys.warehouseCounter.room.dao.itemRegex.ItemRegexDao
import com.dacosys.warehouseCounter.room.dao.lot.LotDao
import com.dacosys.warehouseCounter.room.dao.user.UserDao
import com.dacosys.warehouseCounter.room.database.WcDatabase.Companion.DATABASE_VERSION
import com.dacosys.warehouseCounter.room.entity.client.Client
import com.dacosys.warehouseCounter.room.entity.item.Item
import com.dacosys.warehouseCounter.room.entity.itemCategory.ItemCategory
import com.dacosys.warehouseCounter.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.room.entity.itemRegex.ItemRegex
import com.dacosys.warehouseCounter.room.entity.lot.Lot
import com.dacosys.warehouseCounter.room.entity.user.User


@Database(
    entities = [Item::class, Client::class, ItemCategory::class, ItemCode::class, ItemRegex::class, Lot::class, User::class],
    version = DATABASE_VERSION
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

        const val DATABASE_VERSION = 2
        const val DATABASE_NAME = "wc.sqlite"

        @Volatile
        private var INSTANCE: WcDatabase? = null

        fun getDatabase(): WcDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context,
                        WcDatabase::class.java,
                        DATABASE_NAME
                    )
                        .createFromAsset(DATABASE_NAME)
                        .addMigrations(MIGRATION_1_2)
                        .build()

                INSTANCE = instance
                instance
            }
        }

        fun cleanInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Since we didn't alter the table, there's nothing else to do here.
            }
        }
    }
}