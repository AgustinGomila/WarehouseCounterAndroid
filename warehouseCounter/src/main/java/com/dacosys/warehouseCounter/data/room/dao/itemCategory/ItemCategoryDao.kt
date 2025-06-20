package com.dacosys.warehouseCounter.data.room.dao.itemCategory

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dacosys.warehouseCounter.data.room.entity.itemCategory.ItemCategory
import com.dacosys.warehouseCounter.data.room.entity.itemCategory.ItemCategoryEntry as Entry

@Dao
interface ItemCategoryDao {
    @Query("SELECT COUNT(*) FROM ${Entry.TABLE_NAME}")
    suspend fun count(): Int

    @Query("SELECT * FROM ${Entry.TABLE_NAME}")
    suspend fun getAll(): List<ItemCategory>

    @Query("SELECT MAX(${Entry.ITEM_CATEGORY_ID}) FROM ${Entry.TABLE_NAME}")
    suspend fun getLastId(): Long

    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.ITEM_CATEGORY_ID} = :itemCategoryId")
    suspend fun getById(itemCategoryId: Long): ItemCategory?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(itemCategory: ItemCategory): Long?


    @Update
    suspend fun update(itemCategory: ItemCategory)


    @Delete
    suspend fun delete(itemCategory: ItemCategory)
}
