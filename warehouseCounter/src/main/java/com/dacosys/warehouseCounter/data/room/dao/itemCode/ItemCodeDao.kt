package com.dacosys.warehouseCounter.data.room.dao.itemCode

import androidx.room.*
import com.dacosys.warehouseCounter.data.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.data.room.entity.itemCode.ItemCodeEntry as Entry

@Dao
interface ItemCodeDao {
    @Query("SELECT * FROM ${Entry.TABLE_NAME}")
    suspend fun getAll(): List<ItemCode>

    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.CODE} LIKE :code AND ${Entry.QTY} > 0 ORDER BY ${Entry.CODE}")
    suspend fun getByCode(code: String): List<ItemCode>

    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.ITEM_ID} = :itemId AND ${Entry.QTY} > 0 ORDER BY ${Entry.CODE}")
    suspend fun getByItemId(itemId: Long?): List<ItemCode>

    @Query("SELECT COUNT(*) FROM ${Entry.TABLE_NAME} $BASIC_WHERE")
    suspend fun countLink(itemId: Long, code: String): Int

    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.TO_UPLOAD} = 1")
    suspend fun getToUpload(): List<ItemCode>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(itemCode: ItemCode): Long?


    @Update
    suspend fun update(itemCode: ItemCode)

    @Query(
        "UPDATE ${Entry.TABLE_NAME} SET ${Entry.QTY} = :qty, ${Entry.TO_UPLOAD} = 1 $BASIC_WHERE"
    )
    suspend fun updateQty(itemId: Long, code: String, qty: Double?)

    @Query(
        "UPDATE ${Entry.TABLE_NAME} SET ${Entry.TO_UPLOAD} = 0 $BASIC_WHERE"
    )
    suspend fun updateTransferred(itemId: Long, code: String)

    @Query(
        "UPDATE ${Entry.TABLE_NAME} SET ${Entry.QTY} = 0, ${Entry.TO_UPLOAD} = 1 $BASIC_WHERE"
    )
    suspend fun unlinkCode(itemId: Long, code: String)


    @Delete
    suspend fun delete(itemCode: ItemCode)

    companion object {
        const val BASIC_WHERE = "WHERE ${Entry.ITEM_ID} = :itemId AND ${Entry.CODE} = :code"
    }
}
