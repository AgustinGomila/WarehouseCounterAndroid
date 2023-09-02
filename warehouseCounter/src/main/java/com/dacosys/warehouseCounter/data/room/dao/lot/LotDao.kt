package com.dacosys.warehouseCounter.data.room.dao.lot

import androidx.room.*
import com.dacosys.warehouseCounter.data.room.entity.lot.Lot
import com.dacosys.warehouseCounter.data.room.entity.lot.LotEntry as Entry

@Dao
interface LotDao {
    @Query("SELECT * FROM ${Entry.TABLE_NAME}")
    suspend fun getAll(): List<Lot>

    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.LOT_ID} = :lotId")
    suspend fun getByLotId(lotId: Long): Lot?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lot: Lot)


    @Update
    suspend fun update(lot: Lot)


    @Delete
    suspend fun delete(lot: Lot)

    @Query("DELETE FROM ${Entry.TABLE_NAME} WHERE ${Entry.LOT_ID} = :lotId")
    suspend fun deleteById(lotId: Long): Int
}
