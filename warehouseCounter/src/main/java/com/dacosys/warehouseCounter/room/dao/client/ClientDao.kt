package com.dacosys.warehouseCounter.room.dao.client

import androidx.room.*
import com.dacosys.warehouseCounter.room.entity.client.Client
import com.dacosys.warehouseCounter.room.entity.client.ClientEntry as Entry

@Dao
interface ClientDao {
    @Query("SELECT * FROM ${Entry.TABLE_NAME}")
    suspend fun getAll(): List<Client>

    @Query("SELECT MAX(${Entry.CLIENT_ID}) FROM ${Entry.TABLE_NAME}")
    suspend fun getLastId(): Long

    @Query("SELECT * FROM ${Entry.TABLE_NAME} WHERE ${Entry.CLIENT_ID} = :clientId")
    suspend fun getById(clientId: Long): Client?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client)


    @Update
    suspend fun update(client: Client)


    @Delete
    suspend fun delete(client: Client)
}

