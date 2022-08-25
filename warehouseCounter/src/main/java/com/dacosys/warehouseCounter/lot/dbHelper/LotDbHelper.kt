package com.dacosys.warehouseCounter.lot.dbHelper

import android.database.Cursor
import android.database.SQLException
import com.dacosys.warehouseCounter.dataBaseHelper.StaticDbHelper
import com.dacosys.warehouseCounter.errorLog.ErrorLog
import com.dacosys.warehouseCounter.lot.`object`.Lot
import com.dacosys.warehouseCounter.lot.dbHelper.LotContract.LotEntry.Companion.ACTIVE
import com.dacosys.warehouseCounter.lot.dbHelper.LotContract.LotEntry.Companion.CODE
import com.dacosys.warehouseCounter.lot.dbHelper.LotContract.LotEntry.Companion.LOT_ID
import com.dacosys.warehouseCounter.lot.dbHelper.LotContract.LotEntry.Companion.TABLE_NAME

/**
 * Created by Agustin on 28/12/2016.
 */

class LotDbHelper {

    private val lastId: Long
        get() {
            val sqLiteDatabase = StaticDbHelper.getReadableDb()

            return try {
                val mCount = sqLiteDatabase.rawQuery("SELECT MAX($LOT_ID) FROM $TABLE_NAME", null)
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
        code: String,
        active: Boolean,
    ): Long? {
        val newId = lastId

        val sqLiteDatabase = StaticDbHelper.getWritableDb()
        val newItem = Lot(
            newId,
            code,
            active
        )

        return try {
            if (sqLiteDatabase.insertOrThrow(
                    TABLE_NAME,
                    null,
                    newItem.toContentValues()
                ) > 0
            ) {
                newId
            } else {
                0L
            }
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            return 0L
        }
    }

    fun insert(lot: Lot): Long {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        return try {
            if (sqLiteDatabase.insert(
                    TABLE_NAME,
                    null,
                    lot.toContentValues()
                ) > 0
            ) {
                lot.lotId
            } else {
                0L
            }
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            0L
        }
    }

    fun update(lot: Lot): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val selection = "$LOT_ID = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(lot.lotId.toString())

        return try {
            sqLiteDatabase.update(
                TABLE_NAME,
                lot.toContentValues(),
                selection,
                selectionArgs
            ) > 0
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            false
        }
    }

    fun delete(lot: Lot): Boolean {
        return deleteById(lot.lotId)
    }

    fun deleteById(id: Long?): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val selection = "$LOT_ID = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(id!!.toString())

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

    fun select(): ArrayList<Lot> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        val rawQuery = "SELECT " +
                TABLE_NAME + "." + LOT_ID + "," +
                TABLE_NAME + "." + CODE + "," +
                TABLE_NAME + "." + ACTIVE + "," +
                " FROM " + TABLE_NAME +
                " ORDER BY " + TABLE_NAME + "." + CODE

        return try {
            val c = sqLiteDatabase.rawQuery(rawQuery, null)

            fromCursor(c)
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            ArrayList()
        }
    }

    fun selectById(id: Long?): Lot? {
        if (id == null || id <= 0) {
            return null
        }

        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        val where = " WHERE $TABLE_NAME.$LOT_ID = $id"

        val rawQuery = "SELECT " +
                TABLE_NAME + "." + LOT_ID + "," +
                TABLE_NAME + "." + CODE + "," +
                TABLE_NAME + "." + ACTIVE + ","
        " FROM " + TABLE_NAME +
                where +
                " ORDER BY " + TABLE_NAME + "." + CODE

        return try {
            val c = sqLiteDatabase.rawQuery(rawQuery, null)

            val result = fromCursor(c)
            when {
                result.isNotEmpty() -> result[0]
                else -> null
            }
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            null
        }
    }

    fun selectByLotId(lotId: String): ArrayList<Lot> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        var where = ""
        if (lotId.isNotEmpty()) {
            where =
                " WHERE $TABLE_NAME.$LOT_ID LIKE '$lotId%'"
        }

        val rawQuery = "SELECT " +
                TABLE_NAME + "." + LOT_ID + "," +
                TABLE_NAME + "." + CODE + "," +
                TABLE_NAME + "." + ACTIVE + ","
        " FROM " + TABLE_NAME +
                where +
                " ORDER BY " + TABLE_NAME + "." + CODE

        return try {
            val c = sqLiteDatabase.rawQuery(rawQuery, null)
            fromCursor(c)
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            ArrayList()
        }
    }

    fun selectByCode(code: String): ArrayList<Lot> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        var where = ""
        if (code.isNotEmpty()) {
            where =
                " WHERE $TABLE_NAME.$CODE LIKE '$code%'"
        }

        val rawQuery = "SELECT " +
                TABLE_NAME + "." + LOT_ID + "," +
                TABLE_NAME + "." + CODE + "," +
                TABLE_NAME + "." + ACTIVE + ","
        " FROM " + TABLE_NAME +
                where +
                " ORDER BY " + TABLE_NAME + "." + CODE

        return try {
            val c = sqLiteDatabase.rawQuery(rawQuery, null)
            fromCursor(c)
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            ArrayList()
        }
    }

    private fun fromCursor(c: Cursor?): ArrayList<Lot> {
        val result = ArrayList<Lot>()
        c.use {
            if (it != null) {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(LOT_ID))
                    val active = it.getInt(it.getColumnIndexOrThrow(ACTIVE)) == 1
                    val code = it.getString(it.getColumnIndexOrThrow(CODE))

                    val temp = Lot(
                        id,
                        code,
                        active
                    )

                    result.add(temp)
                }
            }
        }
        return result
    }

    companion object {

        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS [" + TABLE_NAME + "] " +
                "( [" + LOT_ID + "] BIGINT NOT NULL ," +
                " [" + CODE + "] NVARCHAR(255) NOT NULL ," +
                " [" + ACTIVE + "] INT NOT NULL ," +
                " CONSTRAINT [PK_" + LOT_ID + "] PRIMARY KEY ([" + LOT_ID + "]) )"

        val CREATE_INDEX: ArrayList<String> = arrayListOf(
            "DROP INDEX IF EXISTS [IDX_$CODE]",
            "CREATE INDEX [IDX_$CODE] ON [$TABLE_NAME] ([$CODE])"
        )
    }
}
