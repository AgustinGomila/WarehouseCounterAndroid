package com.dacosys.warehouseCounter.room.dao.item

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
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
                where += "WHERE "
                where += "${Entry.TABLE_NAME}.${Entry.EAN} LIKE ?"
                args.add("${ean}%")
                condAdded = true
            }

            if (description.isNotEmpty()) {
                where += if (condAdded) " OR " else "WHERE "
                where += "${Entry.TABLE_NAME}.${Entry.DESCRIPTION} LIKE ?"
                args.add("${description}%")
                condAdded = true
            }

            if (itemCategoryId != null) {
                where += if (condAdded) " AND " else "WHERE "
                where += "${Entry.TABLE_NAME}.${Entry.ITEM_CATEGORY_ID} = ?"
                args.add(itemCategoryId)
            }

            val query = "$basicSelect, $joinSelect $basicFrom $leftJoin $where $basicOrder"

            return SimpleSQLiteQuery(query, args.toTypedArray())
        }
    }
}