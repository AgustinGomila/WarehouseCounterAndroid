package com.example.warehouseCounter.data.room.dao.orderRequest

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Update
import com.example.warehouseCounter.data.room.entity.orderRequest.OrderRequestContent
import com.example.warehouseCounter.data.room.entity.orderRequest.OrderRequestContentEntry as Entry

@Dao
interface OrderRequestContentDao {
    @Query("SELECT * FROM ${Entry.TABLE_NAME}")
    suspend fun getAll(): List<OrderRequestContent>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.ORDER_REQUEST_CONTENT_ID} = :itemId")
    suspend fun getById(itemId: Long): OrderRequestContent?

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.ORDER_REQUEST_ID} = :orderId")
    suspend fun getByOrderId(orderId: Long): List<OrderRequestContent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(content: OrderRequestContent): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contents: List<OrderRequestContent>)

    @Update
    suspend fun update(content: OrderRequestContent)

    @Update
    suspend fun update(contents: List<OrderRequestContent>): Int

    @Delete
    suspend fun delete(content: OrderRequestContent)

    @Query("DELETE FROM ${Entry.TABLE_NAME} WHERE ${Entry.ORDER_REQUEST_ID} = :itemId")
    suspend fun deleteByOrderId(itemId: Long)
}
