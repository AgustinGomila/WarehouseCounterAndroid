package com.dacosys.warehouseCounter.room.dao.item

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import com.dacosys.warehouseCounter.room.entity.item.Item
import com.dacosys.warehouseCounter.room.entity.item.ItemEntry as Entry
import com.dacosys.warehouseCounter.room.entity.itemCategory.ItemCategoryEntry as IcEntry

@Dao
interface ItemDao {
    @Query("SELECT * FROM ${Entry.TABLE_NAME}")
    suspend fun getAll(): List<Item>

    @Query("SELECT MAX(${Entry.ITEM_ID}) FROM ${Entry.TABLE_NAME}")
    suspend fun getLastId(): Long

    @Query("SELECT MIN(${Entry.ITEM_ID}) FROM ${Entry.TABLE_NAME}")
    suspend fun getMinorId(): Long

    @RewriteQueriesToDropUnusedColumns
    @Query("$basicSelect, $joinSelect $basicFrom $leftJoin WHERE ${Entry.TABLE_NAME}.${Entry.ITEM_ID} = :itemId $basicOrder")
    suspend fun getById(itemId: Long): Item?

    @RewriteQueriesToDropUnusedColumns
    @Query("$basicSelect, $joinSelect $basicFrom $leftJoin WHERE ${Entry.TABLE_NAME}.${Entry.ITEM_CATEGORY_ID} = :itemCatId $basicOrder")
    suspend fun getByItemCategoryId(itemCatId: Long): List<Item>

    @Query(getCodesQuery)
    suspend fun getCodes(onlyActive: Int = 1): List<String>

    /**
     * Get by formatted query
     *
     * @param query Ejemplo: [getEanDescCatQuery]
     * @return Una lista de [Item]
     */
    @RawQuery
    suspend fun getByQuery(query: SupportSQLiteQuery): List<Item>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item): Long?


    @Update
    suspend fun update(item: Item)

    @Query("UPDATE ${Entry.TABLE_NAME} SET ${Entry.DESCRIPTION} = :description WHERE ${Entry.ITEM_ID} = :itemId")
    suspend fun updateDescription(
        itemId: Long,
        description: String,
    )


    @Delete
    suspend fun delete(item: Item)

    companion object {
        const val basicFrom = "FROM ${Entry.TABLE_NAME}"
        const val basicOrder = "ORDER BY ${Entry.TABLE_NAME}.${Entry.DESCRIPTION}"

        const val basicSelect =
            "SELECT ${Entry.TABLE_NAME}.${Entry.ITEM_ID},${Entry.TABLE_NAME}.${Entry.DESCRIPTION},${Entry.TABLE_NAME}.${Entry.ACTIVE},${Entry.TABLE_NAME}.${Entry.PRICE},${Entry.TABLE_NAME}.${Entry.EAN},${Entry.TABLE_NAME}.${Entry.ITEM_CATEGORY_ID},${Entry.TABLE_NAME}.${Entry.EXTERNAL_ID},${Entry.TABLE_NAME}.${Entry.LOT_ENABLED}"

        const val joinSelect =
            "${IcEntry.TABLE_NAME}.${IcEntry.DESCRIPTION} AS ${Entry.ITEM_CATEGORY_STR}"

        const val leftJoin =
            "LEFT JOIN ${IcEntry.TABLE_NAME} ON ${IcEntry.TABLE_NAME}.${IcEntry.ITEM_CATEGORY_ID} = ${Entry.TABLE_NAME}.${Entry.ITEM_CATEGORY_ID}"

        const val getCodesQuery =
            "SELECT ${Entry.TABLE_NAME}.${Entry.EAN} FROM ${Entry.TABLE_NAME} WHERE ${Entry.TABLE_NAME}.${Entry.ACTIVE} = :onlyActive ORDER BY ${Entry.TABLE_NAME}.${Entry.EAN}"

        /**
         * Get a formatted query by ean, description or category
         *
         * @param ean             Ean del ítem. Opcional.
         * @param description     Descripción del ítem. Opcional.
         * @param itemCategoryId  Categoría. Opcional.
         * @return Query formateado
         */
        fun getEanDescCatQuery(
            ean: String = "",
            description: String = "",
            itemCategoryId: Long? = null,
        ): SimpleSQLiteQuery {
            var where = String()
            val args: MutableList<Any> = ArrayList()
            var condAdded = false

            if (ean.isNotEmpty()) {
                where += " WHERE "
                where += "${Entry.TABLE_NAME}.${Entry.EAN} LIKE '?%'"
                args.add(ean)
                condAdded = true
            }

            if (description.isNotEmpty()) {
                where += if (condAdded) " OR " else " WHERE "
                where += "${Entry.TABLE_NAME}.${Entry.DESCRIPTION} LIKE '?%'"
                args.add(description)
                condAdded = true
            }

            if (itemCategoryId != null) {
                where += if (condAdded) " AND " else " WHERE "
                where += "${Entry.TABLE_NAME}.${Entry.ITEM_CATEGORY_ID} = ?"
                args.add(itemCategoryId)
            }

            val query = "$basicSelect, $joinSelect $basicFrom $leftJoin $where $basicOrder"

            return SimpleSQLiteQuery(query, args.toTypedArray())
        }

        // Room uses an own database hash to uniquely identify the database
        // Since version 1 does not use Room, it doesn't have the database hash associated.
        // By implementing a Migration class, we're telling Room that it should use the data
        // from version 1 to version 2.
        // If no migration is provided, then the tables will be dropped and recreated.
        // Since we didn't alter the table, there's nothing else to do here.

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE item_new (" +
                            "_id INTEGER PRIMARY KEY NOT NULL," +
                            "description TEXT NOT NULL," +
                            "active INTEGER NOT NULL," +
                            "price REAL," +
                            "ean TEXT NOT NULL," +
                            "item_category_id INTEGER NOT NULL," +
                            "external_id TEXT," +
                            "lot_enabled INTEGER NOT NULL DEFAULT 0);"
                )

                database.execSQL(
                    "INSERT INTO item_new (" +
                            "ean, " +
                            "price, " +
                            "lot_enabled, " +
                            "description, " +
                            "active, " +
                            "external_id, " +
                            "item_id, " +
                            "item_category_id) " +
                            "SELECT " +
                            "ean, " +
                            "price, " +
                            "lot_enabled, " +
                            "description, " +
                            "active, " +
                            "external_id, " +
                            "item_id, " +
                            "item_category_id " +
                            "FROM item;"
                )

                // Los índices antiguos no incluían el nombre de la tabla.
                // Ahora incluyen el nombre de la tabla para evitar conflictos en Room.
                database.execSQL("DROP INDEX IF EXISTS IDX_item_ean;")
                database.execSQL("DROP INDEX IF EXISTS IDX_item_active;")
                database.execSQL("DROP INDEX IF EXISTS IDX_item_item_category_id;")
                database.execSQL("DROP INDEX IF EXISTS IDX_item_external_id;")

                database.execSQL("DROP TABLE item;")
                database.execSQL("ALTER TABLE item_new RENAME TO item;")

                database.execSQL("CREATE INDEX IDX_item_ea               ON item(ean);")
                database.execSQL("CREATE INDEX IDX_item_active           ON item(active);")
                database.execSQL("CREATE INDEX IDX_item_item_category_id ON item(item_category_id);")
                database.execSQL("CREATE INDEX IDX_item_external_id      ON item(external_id);")
            }
        }
    }
}