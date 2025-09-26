package com.example.warehouseCounter.data.room.dao.user

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.warehouseCounter.data.room.entity.user.User
import com.example.warehouseCounter.data.room.entity.user.UserEntry as Entry

@Dao
interface UserDao {
    @Query("SELECT COUNT(*) FROM ${Entry.TABLE_NAME}")
    suspend fun count(): Int

    @Query("SELECT * FROM ${Entry.TABLE_NAME}")
    suspend fun getAll(): List<User>

    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.USER_ID} = :userId")
    suspend fun getById(userId: Long): User?

    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.NAME} = :name")
    suspend fun getByName(name: String): User?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User): Long?


    @Update
    suspend fun update(user: User)


    @Delete
    suspend fun delete(user: User)
}
