package com.dacosys.warehouseCounter.data.room.dao.pendingLabel

import androidx.room.*
import com.dacosys.warehouseCounter.data.room.entity.pendingLabel.PendingLabel
import com.dacosys.warehouseCounter.data.room.entity.pendingLabel.PendingLabelEntry

@Dao
interface PendingLabelDao {
    @Query("SELECT * FROM ${PendingLabelEntry.TABLE_NAME}")
    suspend fun getAll(): List<PendingLabel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingLabel: PendingLabel): Long

    @Query("DELETE FROM ${PendingLabelEntry.TABLE_NAME} WHERE ${PendingLabelEntry.ID} = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    suspend fun deleteList(idList: List<Long>) {
        for (id in idList) {
            deleteById(id)
        }
    }

    @Transaction
    suspend fun insertList(idList: List<Long>) {
        for (id in idList) {
            insert(PendingLabel(id))
        }
    }
}
