package com.dacosys.warehouseCounter.itemRegex.dbHelper

import android.database.Cursor
import android.database.SQLException
import com.dacosys.warehouseCounter.dataBaseHelper.StaticDbHelper
import com.dacosys.warehouseCounter.errorLog.ErrorLog
import com.dacosys.warehouseCounter.itemRegex.`object`.ItemRegex
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexContract.ItemRegexEntry.Companion.ACTIVE
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexContract.ItemRegexEntry.Companion.CODE_LENGTH
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexContract.ItemRegexEntry.Companion.DESCRIPTION
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexContract.ItemRegexEntry.Companion.ITEM_REGEX_ID
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexContract.ItemRegexEntry.Companion.JSON_CONFIG
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexContract.ItemRegexEntry.Companion.REGEX
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexContract.ItemRegexEntry.Companion.TABLE_NAME
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexContract.getAllColumns

/**
 * Created by Agustin on 28/12/2016.
 */

class ItemRegexDbHelper {

    private val lastId: Long
        get() {
            val sqLiteDatabase = StaticDbHelper.getReadableDb()

            return try {
                val mCount =
                    sqLiteDatabase.rawQuery("SELECT MAX($ITEM_REGEX_ID) FROM $TABLE_NAME", null)
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
        description: String,
        regex: String,
        jsonConfig: String,
        codeLength: Long,
        active: Boolean,
    ): Long {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val newId = lastId

        val newItemRegex = ItemRegex(
            newId,
            description,
            regex,
            jsonConfig,
            codeLength,
            active
        )

        return try {
            if (sqLiteDatabase.insert(
                    TABLE_NAME, null,
                    newItemRegex.toContentValues()
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

    fun insert(itemRegex: ItemRegex): Long {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        return try {
            sqLiteDatabase.insert(
                TABLE_NAME, null,
                itemRegex.toContentValues()
            )
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            0L
        }
    }

    fun update(itemRegex: ItemRegex): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val selection = "$ITEM_REGEX_ID = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(itemRegex.itemRegexId.toString())

        return try {
            sqLiteDatabase.update(
                TABLE_NAME,
                itemRegex.toContentValues(),
                selection,
                selectionArgs
            ) > 0
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            false
        }
    }

    fun delete(itemRegex: ItemRegex): Boolean {
        return deleteById(itemRegex.itemRegexId)
    }

    fun deleteById(id: Long): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val selection = "$ITEM_REGEX_ID = ?" // WHERE id LIKE ?
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

    fun select(): ArrayList<ItemRegex> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()
        val columns = getAllColumns()

        val order = DESCRIPTION

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

    fun select(active: Boolean): ArrayList<ItemRegex> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()
        val columns = getAllColumns()

        var selection: String? = null
        if (active) selection = "$ACTIVE = 1" // WHERE id LIKE ?
        val order = DESCRIPTION

        try {
            val c = sqLiteDatabase.query(
                TABLE_NAME, // Nombre de la tabla
                columns,// Lista de Columnas a consultar
                selection,// Columnas para la cláusula WHERE
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

    fun selectById(id: Long): ItemRegex? {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()
        val columns = getAllColumns()

        val selection = "$ITEM_REGEX_ID = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(id.toString())
        val order = DESCRIPTION

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

    fun selectByDescription(description: String): ArrayList<ItemRegex> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()
        val columns = getAllColumns()

        val selection = "$DESCRIPTION LIKE ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf("%$description%")
        val order = DESCRIPTION

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

    private fun fromCursor(c: Cursor?): ArrayList<ItemRegex> {
        val result = ArrayList<ItemRegex>()
        c.use {
            if (it != null) {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(ITEM_REGEX_ID))
                    val description = it.getString(it.getColumnIndexOrThrow(DESCRIPTION))
                    val regex = it.getString(it.getColumnIndexOrThrow(REGEX))
                    val jsonConfig = it.getString(it.getColumnIndexOrThrow(JSON_CONFIG))
                    val codeLength = it.getLong(it.getColumnIndexOrThrow(CODE_LENGTH))
                    val active = it.getInt(it.getColumnIndexOrThrow(ACTIVE)) == 1

                    val temp = ItemRegex(
                        id,
                        description,
                        regex,
                        jsonConfig,
                        codeLength,
                        active
                    )
                    result.add(temp)
                }
            }
        }
        return result
    }

    companion object {

        const val CREATE_TABLE = ("CREATE TABLE IF NOT EXISTS [" + TABLE_NAME + "] "
                + "( [" + ITEM_REGEX_ID + "] BIGINT NOT NULL ,"
                + " [" + DESCRIPTION + "] NVARCHAR(255) NOT NULL ,"
                + " [" + REGEX + "] TEXT NOT NULL ,"
                + " [" + JSON_CONFIG + "] TEXT NULL ,"
                + " [" + CODE_LENGTH + "] INT(11) NULL ,"
                + " [" + ACTIVE + "] INT NOT NULL ,"
                + " CONSTRAINT [PK_" + ITEM_REGEX_ID + "] PRIMARY KEY ([" + ITEM_REGEX_ID + "]) )")

        val CREATE_INDEX: ArrayList<String> = arrayListOf(
            "DROP INDEX IF EXISTS [IDX_$ITEM_REGEX_ID]",
            "CREATE UNIQUE INDEX [IDX_$ITEM_REGEX_ID] ON [$TABLE_NAME] ([$ITEM_REGEX_ID])"
        )
    }
}