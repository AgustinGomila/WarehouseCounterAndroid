package com.dacosys.warehouseCounter.data.room.dao.orderRequest

import androidx.room.*
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.Log
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.LogEntry as Entry

@Dao
interface LogDao {
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.ORDER_REQUEST_ID} = :orderId")
    suspend fun getByOrderId(orderId: Long): List<Log>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(content: Log): Long

    @Query("DELETE FROM ${Entry.TABLE_NAME} WHERE ${Entry.ORDER_REQUEST_ID} = :itemId")
    suspend fun deleteByOrderId(itemId: Long)
}
