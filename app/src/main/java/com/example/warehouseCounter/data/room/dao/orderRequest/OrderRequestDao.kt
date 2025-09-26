package com.example.warehouseCounter.data.room.dao.orderRequest

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import com.example.warehouseCounter.data.room.database.WcTempDatabase.Companion.database
import com.example.warehouseCounter.data.room.entity.orderRequest.OrderRequest
import com.example.warehouseCounter.data.room.entity.orderRequest.OrderRequestContent
import com.example.warehouseCounter.data.room.entity.orderRequest.OrderRequestEntry as Entry

@Dao
interface OrderRequestDao {

    @Query("SELECT * FROM ${Entry.TABLE_NAME}")
    suspend fun getAll(): List<OrderRequest>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.ID} = :itemId")
    suspend fun getById(itemId: Long): OrderRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(orderRequest: OrderRequest): Long

    @Update
    suspend fun update(orderRequest: OrderRequest)

    @Query("DELETE FROM ${Entry.TABLE_NAME} WHERE ${Entry.ID} = :itemId")
    suspend fun deleteById(itemId: Long)

    @Transaction
    suspend fun deleteWithContent(id: Long) {
        deleteById(id)
        database.orderRequestContentDao().deleteByOrderId(id)
        database.logDao().deleteByOrderId(id)
    }

    @Transaction
    suspend fun deleteWithContent(idList: List<Long>) {
        for (id in idList) {
            deleteById(id)
            database.orderRequestContentDao().deleteByOrderId(id)
            database.logDao().deleteByOrderId(id)
        }
    }

    @Transaction
    suspend fun update(orderRequest: OrderRequest, contents: List<OrderRequestContent>) {
        update(orderRequest)
        database.orderRequestContentDao().deleteByOrderId(orderRequest.id)
        database.orderRequestContentDao().insert(contents)
    }
}
