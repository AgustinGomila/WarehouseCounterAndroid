package com.example.warehouseCounter.data.room.dao.itemRegex

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.warehouseCounter.data.room.entity.itemRegex.ItemRegex
import com.example.warehouseCounter.data.room.entity.itemRegex.ItemRegexEntry as Entry

@Dao
interface ItemRegexDao {
    @Query("SELECT COUNT(*) FROM ${Entry.TABLE_NAME}")
    suspend fun count(): Int

    @Query("SELECT * FROM ${Entry.TABLE_NAME}")
    suspend fun getAll(): List<ItemRegex>

    @Query("SELECT MAX(${Entry.ITEM_REGEX_ID}) FROM ${Entry.TABLE_NAME}")
    suspend fun getLastId(): Long

    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.ITEM_REGEX_ID} = :itemRegexId")
    suspend fun getByItemRegexId(itemRegexId: Long): ItemRegex?

    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.ACTIVE} = 1")
    suspend fun getActive(): List<ItemRegex>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(itemRegex: ItemRegex): Long?


    @Update
    suspend fun update(itemRegex: ItemRegex)


    @Delete
    suspend fun delete(itemRegex: ItemRegex)

    @Query("DELETE FROM ${Entry.TABLE_NAME} WHERE ${Entry.ITEM_REGEX_ID} = :itemRegexId")
    suspend fun deleteById(itemRegexId: Long): Int
}
