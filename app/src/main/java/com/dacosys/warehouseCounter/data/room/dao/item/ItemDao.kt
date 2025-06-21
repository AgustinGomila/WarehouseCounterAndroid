package com.dacosys.warehouseCounter.data.room.dao.item

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.dacosys.warehouseCounter.data.room.dao.item.ItemDao.Companion.getMultiQuery
import com.dacosys.warehouseCounter.data.room.entity.item.Item
import com.dacosys.warehouseCounter.data.room.entity.item.ItemEntry as Entry
import com.dacosys.warehouseCounter.data.room.entity.itemCategory.ItemCategoryEntry as IcEntry

@Dao
interface ItemDao {
    @Query("SELECT COUNT(*) FROM ${Entry.TABLE_NAME}")
    suspend fun count(): Int

    @Query("SELECT * FROM ${Entry.TABLE_NAME}")
    suspend fun getAll(): List<Item>

    @Query("SELECT MAX(${Entry.ITEM_ID}) FROM ${Entry.TABLE_NAME}")
    suspend fun getLastId(): Long

    @Query("SELECT MIN(${Entry.ITEM_ID}) FROM ${Entry.TABLE_NAME}")
    suspend fun getMinorId(): Long

    @RewriteQueriesToDropUnusedColumns
    @Query("$BASIC_SELECT, $JOIN_SELECT $BASIC_FROM $LEFT_JOIN WHERE ${Entry.TABLE_NAME}.${Entry.ITEM_ID} = :itemId $BASIC_ORDER")
    suspend fun getById(itemId: Long): Item?

    @RewriteQueriesToDropUnusedColumns
    @Query("$BASIC_SELECT, $JOIN_SELECT $BASIC_FROM $LEFT_JOIN WHERE ${Entry.TABLE_NAME}.${Entry.ITEM_CATEGORY_ID} = :itemCatId $BASIC_ORDER")
    suspend fun getByItemCategoryId(itemCatId: Long): List<Item>

    @Query(GET_EAN_CODES_QUERY)
    suspend fun getEanCodes(onlyActive: Int = 1): List<String>

    @Query(GET_IDS_QUERY)
    suspend fun getIds(onlyActive: Int = 1): List<Long>

    /**
     * Get by formatted query
     *
     * @param query Ejemplo: [getMultiQuery]
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
        const val BASIC_FROM = "FROM ${Entry.TABLE_NAME}"
        const val BASIC_ORDER = "ORDER BY ${Entry.TABLE_NAME}.${Entry.DESCRIPTION}"

        const val BASIC_SELECT =
            "SELECT ${Entry.TABLE_NAME}.${Entry.ITEM_ID},${Entry.TABLE_NAME}.${Entry.DESCRIPTION},${Entry.TABLE_NAME}.${Entry.ACTIVE},${Entry.TABLE_NAME}.${Entry.PRICE},${Entry.TABLE_NAME}.${Entry.EAN},${Entry.TABLE_NAME}.${Entry.ITEM_CATEGORY_ID},${Entry.TABLE_NAME}.${Entry.EXTERNAL_ID},${Entry.TABLE_NAME}.${Entry.LOT_ENABLED}"

        const val JOIN_SELECT =
            "${IcEntry.TABLE_NAME}.${IcEntry.DESCRIPTION} AS ${Entry.ITEM_CATEGORY_STR}"

        const val LEFT_JOIN =
            "LEFT JOIN ${IcEntry.TABLE_NAME} ON ${IcEntry.TABLE_NAME}.${IcEntry.ITEM_CATEGORY_ID} = ${Entry.TABLE_NAME}.${Entry.ITEM_CATEGORY_ID}"

        const val GET_EAN_CODES_QUERY =
            "SELECT ${Entry.TABLE_NAME}.${Entry.EAN} FROM ${Entry.TABLE_NAME} WHERE ${Entry.TABLE_NAME}.${Entry.ACTIVE} = :onlyActive ORDER BY ${Entry.TABLE_NAME}.${Entry.EAN}"

        const val GET_IDS_QUERY =
            "SELECT ${Entry.TABLE_NAME}.${Entry.ITEM_ID} FROM ${Entry.TABLE_NAME} WHERE ${Entry.TABLE_NAME}.${Entry.ACTIVE} = :onlyActive ORDER BY ${Entry.TABLE_NAME}.${Entry.ITEM_ID}"

        /**
         * Get a formatted query by ean, description or category
         *
         * @param ean             Ean del ítem. Opcional.
         * @param description     Descripción del ítem. Opcional.
         * @param externalId      Id externo del ítem. Opcional.
         * @param itemCategoryId  Categoría. Opcional.
         * @return Query formateado
         */
        fun getMultiQuery(
            ean: String = "",
            description: String = "",
            externalId: String = "",
            itemCategoryId: Long? = null,
            useLike: Boolean = false
        ): SimpleSQLiteQuery {
            var where = String()
            val args: MutableList<Any> = ArrayList()
            var condAdded = false

            if (ean.isNotEmpty()) {
                where += "WHERE "
                where += "${Entry.TABLE_NAME}.${Entry.EAN} LIKE ?"
                args.add("${if (useLike) "%" else ""}$ean${if (useLike) "%" else ""}")
                condAdded = true
            }

            if (externalId.isNotEmpty()) {
                where += "WHERE "
                where += "${Entry.TABLE_NAME}.${Entry.EXTERNAL_ID} LIKE ?"
                args.add("${if (useLike) "%" else ""}$externalId${if (useLike) "%" else ""}")
                condAdded = true
            }

            if (description.isNotEmpty()) {
                where += if (condAdded) " OR " else "WHERE "
                where += "${Entry.TABLE_NAME}.${Entry.DESCRIPTION} LIKE ?"
                args.add("${if (useLike) "%" else ""}$description${if (useLike) "%" else ""}")
                condAdded = true
            }

            if (itemCategoryId != null) {
                where += if (condAdded) " AND " else "WHERE "
                where += "${Entry.TABLE_NAME}.${Entry.ITEM_CATEGORY_ID} = ?"
                args.add(itemCategoryId)
            }

            val query = "$BASIC_SELECT, $JOIN_SELECT $BASIC_FROM $LEFT_JOIN $where $BASIC_ORDER"

            return SimpleSQLiteQuery(query, args.toTypedArray())
        }
    }
}
