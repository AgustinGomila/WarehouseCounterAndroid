package com.dacosys.warehouseCounter.client.dbHelper

import android.database.Cursor
import android.database.SQLException
import com.dacosys.warehouseCounter.client.`object`.Client
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.ACTIVE
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.ADDRESS
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.CITY
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.CLIENT_ID
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.CONTACT_NAME
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.COUNTRY_ID
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.LATITUDE
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.LONGITUDE
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.NAME
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.PHONE
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.TABLE_NAME
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.TAX_NUMBER
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.USER_ID
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.getAllColumns
import com.dacosys.warehouseCounter.dataBaseHelper.StaticDbHelper
import com.dacosys.warehouseCounter.errorLog.ErrorLog

/**
 * Created by Agustin on 28/12/2016.
 */

class ClientDbHelper {

    private val lastId: Long
        get() {
            val sqLiteDatabase = StaticDbHelper.getReadableDb()

            return try {
                val mCount =
                    sqLiteDatabase.rawQuery("SELECT MAX($CLIENT_ID) FROM $TABLE_NAME", null)
                mCount.moveToFirst()
                val count = mCount.getInt(0).toLong()
                mCount.close()

                count + 1
            } catch (ex: SQLException) {
                ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
                0L
            }
        }

    fun insert(
        name: String,
        contact_name: String?,
        phone: String?,
        address: String?,
        city: String?,
        user_id: Long?,
        active: Boolean,
        latitude: Double?,
        longitude: Double?,
        country_id: Long?,
        tax_number: String?,
    ): Long {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val newId = lastId

        val newClient = Client(
            newId,
            name,
            contact_name,
            phone,
            address,
            city,
            user_id,
            active,
            latitude,
            longitude,
            country_id,
            tax_number
        )

        return try {
            if (sqLiteDatabase.insert(
                    TABLE_NAME,
                    null,
                    newClient.toContentValues()
                ) > 0
            ) {
                newId
            } else {
                0L
            }
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            0L
        }
    }

    fun insert(client: Client): Long {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        return try {
            sqLiteDatabase.insert(
                TABLE_NAME, null,
                client.toContentValues()
            )
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            0L
        }
    }

    fun update(client: Client): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val selection = "$CLIENT_ID = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(client.clientId.toString())

        return try {
            sqLiteDatabase.update(
                TABLE_NAME,
                client.toContentValues(),
                selection,
                selectionArgs
            ) > 0
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            false
        }
    }

    fun delete(client: Client): Boolean {
        return deleteById(client.clientId)
    }

    fun deleteById(id: Long): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val selection = "$CLIENT_ID = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(id.toString())

        return try {
            sqLiteDatabase.delete(
                TABLE_NAME,
                selection,
                selectionArgs
            ) > 0
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            false
        }
    }

    fun deleteAll(): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        return try {
            sqLiteDatabase.delete(
                TABLE_NAME,
                null,
                null
            ) > 0
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            false
        }
    }

    fun select(): ArrayList<Client> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()
        val columns = getAllColumns()

        val order = NAME

        try {
            val c = sqLiteDatabase.query(
                TABLE_NAME, // Nombre de la tabla
                columns,// Lista de Columnas a consultar
                null,// Columnas para la cláusula WHERE
                null,// Valores a comparar con las columnas del WHERE
                null,// Agrupar con GROUP BY
                null, // Condición HAVING para GROUP BY
                order  // Cláusula ORDER BY
            )

            return fromCursor(c)
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            return ArrayList()
        }
    }

    fun selectById(id: Long): Client? {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()
        val columns = getAllColumns()

        val selection = "$CLIENT_ID = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(id.toString())
        val order = NAME

        try {
            val c = sqLiteDatabase.query(
                TABLE_NAME, // Nombre de la tabla
                columns, // Lista de Columnas a consultar
                selection, // Columnas para la cláusula WHERE
                selectionArgs,// Valores a comparar con las columnas del WHERE
                null,// Agrupar con GROUP BY
                null, // Condición HAVING para GROUP BY
                order  // Cláusula ORDER BY
            )

            val result = fromCursor(c)
            return when {
                result.isNotEmpty() -> result[0]
                else -> null
            }
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            return null
        }
    }

    fun selectByDescription(description: String): ArrayList<Client> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()
        val columns = getAllColumns()

        val selection = "$NAME LIKE ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf("%$description%")
        val order = NAME

        try {
            val c = sqLiteDatabase.query(
                TABLE_NAME, // Nombre de la tabla
                columns, // Lista de Columnas a consultar
                selection, // Columnas para la cláusula WHERE
                selectionArgs,// Valores a comparar con las columnas del WHERE
                null,// Agrupar con GROUP BY
                null, // Condición HAVING para GROUP BY
                order  // Cláusula ORDER BY
            )

            return fromCursor(c)
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            return ArrayList()
        }
    }

    private fun fromCursor(c: Cursor?): ArrayList<Client> {
        val result = ArrayList<Client>()
        c.use {
            if (it != null) {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(CLIENT_ID))
                    val name = it.getString(it.getColumnIndexOrThrow(NAME))
                    val contactName = it.getString(it.getColumnIndexOrThrow(CONTACT_NAME))
                    val phone = it.getString(it.getColumnIndexOrThrow(PHONE))
                    val address = it.getString(it.getColumnIndexOrThrow(ADDRESS))
                    val city = it.getString(it.getColumnIndexOrThrow(CITY))
                    val userId = it.getLong(it.getColumnIndexOrThrow(USER_ID))
                    val active = it.getInt(it.getColumnIndexOrThrow(ACTIVE)) == 1
                    val latitude = it.getDouble(it.getColumnIndexOrThrow(LATITUDE))
                    val longitude = it.getDouble(it.getColumnIndexOrThrow(LONGITUDE))
                    val countryId = it.getLong(it.getColumnIndexOrThrow(COUNTRY_ID))
                    val taxNumber = it.getString(it.getColumnIndexOrThrow(TAX_NUMBER))

                    val temp = Client(
                        id,
                        name,
                        contactName,
                        phone,
                        address,
                        city,
                        userId,
                        active,
                        latitude,
                        longitude,
                        countryId,
                        taxNumber
                    )
                    result.add(temp)
                }
            }
        }
        return result
    }

    companion object {

        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS [" + TABLE_NAME + "] " +
                "( [" + CLIENT_ID + "] BIGINT NOT NULL , " +
                "[" + NAME + "] NVARCHAR(255) NOT NULL , " +
                "[" + CONTACT_NAME + "] NVARCHAR(255) NULL , " +
                "[" + PHONE + "] NVARCHAR(255) NULL , " +
                "[" + ADDRESS + "] NVARCHAR(255) NULL , " +
                "[" + CITY + "] NVARCHAR(255) NULL , " +
                "[" + USER_ID + "] INT NULL , " +
                "[" + ACTIVE + "] INT NOT NULL , " +
                "[" + LATITUDE + "] FLOAT NULL , " +
                "[" + LONGITUDE + "] FLOAT NULL , " +
                "[" + COUNTRY_ID + "] INT NULL , " +
                "[" + TAX_NUMBER + "] NVARCHAR(45) NULL , " +
                "CONSTRAINT [PK_" + CLIENT_ID + "] PRIMARY KEY ([" + CLIENT_ID + "]) )"

        val CREATE_INDEX: ArrayList<String> = arrayListOf(
            "DROP INDEX IF EXISTS [IDX_$NAME]",
            "DROP INDEX IF EXISTS [IDX_$CONTACT_NAME]",
            "CREATE INDEX [IDX_$NAME] ON [$TABLE_NAME] ([$NAME])",
            "CREATE INDEX [IDX_$CONTACT_NAME] ON [$TABLE_NAME] ([$CONTACT_NAME])"
        )
    }
}