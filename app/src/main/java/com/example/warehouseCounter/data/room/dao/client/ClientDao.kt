package com.example.warehouseCounter.data.room.dao.client

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.warehouseCounter.data.room.entity.client.Client
import com.example.warehouseCounter.data.room.entity.client.ClientEntry as Entry

@Dao
interface ClientDao {
    @Query("SELECT COUNT(*) FROM ${Entry.TABLE_NAME}")
    suspend fun count(): Int

    @Query("SELECT * FROM ${Entry.TABLE_NAME}")
    suspend fun getAll(): List<Client>

    @Query("SELECT MAX(${Entry.CLIENT_ID}) FROM ${Entry.TABLE_NAME}")
    suspend fun getLastId(): Long

    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.CLIENT_ID} = :clientId")
    suspend fun getById(clientId: Long): Client?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client): Long?


    @Update
    suspend fun update(client: Client)


    @Delete
    suspend fun delete(client: Client)
}

