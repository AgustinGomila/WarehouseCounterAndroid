package com.dacosys.warehouseCounter.data.room.dao.orderRequest

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import com.dacosys.warehouseCounter.data.room.entity.itemCategory.ItemCategoryEntry
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.Log
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.LogEntry as Entry

@Dao
interface LogDao {
    @Query("SELECT COUNT(*) FROM ${Entry.TABLE_NAME}")
    suspend fun count(): Int

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.ORDER_REQUEST_ID} = :orderId")
    suspend fun getByOrderId(orderId: Long): List<Log>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(content: Log): Long

    @Query("DELETE FROM ${Entry.TABLE_NAME} WHERE ${Entry.ORDER_REQUEST_ID} = :itemId")
    suspend fun deleteByOrderId(itemId: Long)
}
