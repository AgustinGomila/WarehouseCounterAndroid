package com.dacosys.warehouseCounter.room.dao.itemCategory

import androidx.room.*
import com.dacosys.warehouseCounter.room.entity.itemCategory.ItemCategory
import com.dacosys.warehouseCounter.room.entity.itemCategory.ItemCategoryEntry as Entry

@Dao
interface ItemCategoryDao {
    @Query("SELECT * FROM ${Entry.TABLE_NAME}")
    suspend fun getAll(): List<ItemCategory>

    @Query("SELECT MAX(${Entry.ITEM_CATEGORY_ID}) FROM ${Entry.TABLE_NAME}")
    suspend fun getLastId(): Long

    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.ITEM_CATEGORY_ID} = :itemCategoryId")
    suspend fun getById(itemCategoryId: Long): ItemCategory?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(itemCategory: ItemCategory)


    @Update
    suspend fun update(itemCategory: ItemCategory)


    @Delete
    suspend fun delete(itemCategory: ItemCategory)
}