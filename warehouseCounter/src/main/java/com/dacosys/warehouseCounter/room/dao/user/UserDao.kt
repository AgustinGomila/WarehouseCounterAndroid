package com.dacosys.warehouseCounter.room.dao.user

import androidx.room.*
import com.dacosys.warehouseCounter.room.entity.user.User
import com.dacosys.warehouseCounter.room.entity.user.UserEntry as Entry

@Dao
interface UserDao {
    @Query("SELECT * FROM ${Entry.TABLE_NAME}")
    suspend fun getAll(): List<User>

    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.USER_ID} = :userId")
    suspend fun getById(userId: Long): User?

    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.NAME} = :name")
    suspend fun getByName(name: String): User?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)


    @Update
    suspend fun update(user: User)


    @Delete
    suspend fun delete(user: User)
}