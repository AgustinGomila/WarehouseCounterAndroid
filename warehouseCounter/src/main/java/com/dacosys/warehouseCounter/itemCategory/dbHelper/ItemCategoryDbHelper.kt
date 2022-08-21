package com.dacosys.warehouseCounter.itemCategory.dbHelper

import android.database.Cursor
import android.database.SQLException
import com.dacosys.warehouseCounter.dataBaseHelper.StaticDbHelper
import com.dacosys.warehouseCounter.errorLog.ErrorLog
import com.dacosys.warehouseCounter.itemCategory.`object`.ItemCategory
import com.dacosys.warehouseCounter.itemCategory.dbHelper.ItemCategoryContract.ItemCategoryEntry.Companion.ACTIVE
import com.dacosys.warehouseCounter.itemCategory.dbHelper.ItemCategoryContract.ItemCategoryEntry.Companion.DESCRIPTION
import com.dacosys.warehouseCounter.itemCategory.dbHelper.ItemCategoryContract.ItemCategoryEntry.Companion.ITEM_CATEGORY_ID
import com.dacosys.warehouseCounter.itemCategory.dbHelper.ItemCategoryContract.ItemCategoryEntry.Companion.PARENT_ID
import com.dacosys.warehouseCounter.itemCategory.dbHelper.ItemCategoryContract.ItemCategoryEntry.Companion.TABLE_NAME
import com.dacosys.warehouseCounter.itemCategory.dbHelper.ItemCategoryContract.getAllColumns

/**
 * Created by Agustin on 28/12/2016.
 */

class ItemCategoryDbHelper {

    private val lastId: Long
        get() {
            val sqLiteDatabase = StaticDbHelper.getReadableDb()

            return try {
                val mCount =
                    sqLiteDatabase.rawQuery("SELECT MAX($ITEM_CATEGORY_ID) FROM $TABLE_NAME", null)
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
        active: Boolean,
        parentId: Long,
    ): Long {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val newId = lastId

        val newItemCategory = ItemCategory(
            newId,
            description,
            active,
            parentId
        )

        return try {
            if (sqLiteDatabase.insert(
                    TABLE_NAME, null,
                    newItemCategory.toContentValues()
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

    fun insert(itemCategory: ItemCategory): Long {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        return try {
            sqLiteDatabase.insert(
                TABLE_NAME, null,
                itemCategory.toContentValues()
            )
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            0L
        }
    }

    fun update(itemCategory: ItemCategory): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val selection = "$ITEM_CATEGORY_ID = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(itemCategory.itemCategoryId.toString())

        return try {
            sqLiteDatabase.update(
                TABLE_NAME,
                itemCategory.toContentValues(),
                selection,
                selectionArgs
            ) > 0
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            false
        }
    }

    fun delete(itemCategory: ItemCategory): Boolean {
        return deleteById(itemCategory.itemCategoryId)
    }

    fun deleteById(id: Long): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val selection = "$ITEM_CATEGORY_ID = ?" // WHERE id LIKE ?
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

    fun select(): ArrayList<ItemCategory> {
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

    fun selectById(id: Long): ItemCategory? {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()
        val columns = getAllColumns()

        val selection = "$ITEM_CATEGORY_ID = ?" // WHERE id LIKE ?
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

    fun selectByDescription(description: String): ArrayList<ItemCategory> {
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

    private fun fromCursor(c: Cursor?): ArrayList<ItemCategory> {
        val result = ArrayList<ItemCategory>()
        c.use {
            if (it != null) {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(ITEM_CATEGORY_ID))
                    val active = it.getInt(it.getColumnIndexOrThrow(ACTIVE)) == 1
                    val description = it.getString(it.getColumnIndexOrThrow(DESCRIPTION))
                    val parentId = it.getLong(it.getColumnIndexOrThrow(PARENT_ID))

                    val temp = ItemCategory(
                        id,
                        description,
                        active,
                        parentId
                    )
                    result.add(temp)
                }
            }
        }
        return result
    }

    companion object {

        const val CREATE_TABLE = ("CREATE TABLE IF NOT EXISTS [" + TABLE_NAME + "] "
                + "( [" + ITEM_CATEGORY_ID + "] BIGINT NOT NULL ,"
                + " [" + DESCRIPTION + "] NVARCHAR(255) NOT NULL ,"
                + " [" + ACTIVE + "] INT NOT NULL ,"
                + " [" + PARENT_ID + "] BIGINT NOT NULL ,"
                + " CONSTRAINT [PK_" + ITEM_CATEGORY_ID + "] PRIMARY KEY ([" + ITEM_CATEGORY_ID + "]) )")

        val CREATE_INDEX: ArrayList<String> = arrayListOf(
            "DROP INDEX IF EXISTS [IDX_$PARENT_ID]",
            "DROP INDEX IF EXISTS [IDX_$DESCRIPTION]",
            "CREATE INDEX [IDX_$PARENT_ID] ON [$TABLE_NAME] ([$PARENT_ID])",
            "CREATE INDEX [IDX_$DESCRIPTION] ON [$TABLE_NAME] ([$DESCRIPTION])"
        )
    }
}