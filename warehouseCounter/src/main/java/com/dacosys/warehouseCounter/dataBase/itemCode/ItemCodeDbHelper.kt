package com.dacosys.warehouseCounter.dataBase.itemCode

import android.database.Cursor
import android.database.SQLException
import android.util.Log
import com.dacosys.warehouseCounter.dataBase.dbHelper.StaticDbHelper
import com.dacosys.warehouseCounter.dataBase.itemCode.ItemCodeContract.ItemCodeEntry.Companion.CODE
import com.dacosys.warehouseCounter.dataBase.itemCode.ItemCodeContract.ItemCodeEntry.Companion.ITEM_ID
import com.dacosys.warehouseCounter.dataBase.itemCode.ItemCodeContract.ItemCodeEntry.Companion.QTY
import com.dacosys.warehouseCounter.dataBase.itemCode.ItemCodeContract.ItemCodeEntry.Companion.TABLE_NAME
import com.dacosys.warehouseCounter.dataBase.itemCode.ItemCodeContract.ItemCodeEntry.Companion.TO_UPLOAD
import com.dacosys.warehouseCounter.dataBase.itemCode.ItemCodeContract.getAllColumns
import com.dacosys.warehouseCounter.model.errorLog.ErrorLog
import com.dacosys.warehouseCounter.model.itemCode.ItemCode


/**
 * Created by Agustin on 28/12/2016.
 */

class ItemCodeDbHelper {

    fun insert(
        itemId: Long?,
        code: String,
        qty: Double?,
        toUpload: Boolean,
    ): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val newItemCode = ItemCode(
            itemId!!,
            code,
            qty!!,
            toUpload
        )

        return try {
            sqLiteDatabase.insert(
                TABLE_NAME,
                null,
                newItemCode.toContentValues()
            ) > 0
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            false
        }
    }

    fun insert(itemCode: ItemCode): Boolean {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        return try {
            sqLiteDatabase.insert(
                TABLE_NAME,
                null,
                itemCode.toContentValues()
            ) > 0
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            false
        }
    }

    fun unlinkCode(itemId: Long, code: String): Boolean {
        Log.i(this::class.java.simpleName, ": SQLite -> updateTransferred")

        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        /*
        UPDATE    item_code
        SET       qty = 0, to_upload = 1
        WHERE     (code = @code) AND (item_id = @item_id)
        */

        return try {
            val updateQ =
                "UPDATE $TABLE_NAME SET $TO_UPLOAD = 1, $QTY = 0 WHERE ($ITEM_ID = $itemId AND $CODE = '$code')"
            val c = sqLiteDatabase.rawQuery(updateQ, null)
            c.moveToFirst()
            c.close()

            getChangesCount() > 0
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            false
        }
    }

    fun deleteByItemId(itemId: Long?): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val selection = "$ITEM_ID = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(itemId!!.toString())

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

    fun deleteByItemIdCode(itemId: Long?, code: String): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val selection = "$ITEM_ID = ? AND $CODE = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(itemId!!.toString(), code)

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

    fun select(): ArrayList<ItemCode> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        val columns = getAllColumns()

        val order = CODE

        try {
            val c = sqLiteDatabase.query(
                TABLE_NAME, // Nombre de la tabla
                columns, // Lista de Columnas a consultar
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

    fun selectToUpload(): ArrayList<ItemCode> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()
        val columns = getAllColumns()

        val selection = "$TO_UPLOAD = 1" // WHERE id LIKE ?
        val order = CODE

        try {
            val c = sqLiteDatabase.query(
                TABLE_NAME, // Nombre de la tabla
                columns, // Lista de Columnas a consultar
                selection, // Columnas para la cláusula WHERE
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

    fun linkExists(itemId: Long, code: String): Boolean {
        Log.i(this::class.java.simpleName, ": SQLite -> linkExists")

        val db = StaticDbHelper.getReadableDb()

        /*
        SELECT   COUNT(*) AS Expr1
        FROM     item_code
        WHERE    (item_id = @item_id) AND (code = @code)
        */

        val countQuery =
            "SELECT * FROM $TABLE_NAME WHERE ($ITEM_ID = $itemId AND $CODE = '$code')"

        val cursor = db.rawQuery(countQuery, null)
        val count = cursor.count
        cursor.close()
        return count > 0
    }

    private fun getChangesCount(): Long {
        Log.i(this::class.java.simpleName, ": SQLite -> getChangesCount")

        val db = StaticDbHelper.getReadableDb()
        val statement = db.compileStatement("SELECT changes()")
        return statement.simpleQueryForLong()
    }


    fun updateTransferred(itemId: Long, code: String): Boolean {
        Log.i(this::class.java.simpleName, ": SQLite -> updateTransferred")

        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        return try {
            val updateQ =
                "UPDATE $TABLE_NAME SET $TO_UPLOAD = 0 WHERE ($ITEM_ID = $itemId AND $CODE = '$code')"
            val c = sqLiteDatabase.rawQuery(updateQ, null)
            c.moveToFirst()
            c.close()

            getChangesCount() > 0
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            false
        }
    }

    fun updateQty(
        itemId: Long?,
        code: String,
        qty: Double?,
    ): Boolean {
        Log.i(this::class.java.simpleName, ": SQLite -> updateQty")

        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        return try {
            val updateQ =
                "UPDATE $TABLE_NAME SET $QTY = $qty, $TO_UPLOAD = 1 WHERE ($ITEM_ID = $itemId AND $CODE = '$code')"
            val c = sqLiteDatabase.rawQuery(updateQ, null)
            c.moveToFirst()
            c.close()

            getChangesCount() > 0
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            false
        }
    }

    fun selectByItemId(itemId: Long?): ArrayList<ItemCode> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()
        val columns = getAllColumns()

        val selection = "$ITEM_ID = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(itemId!!.toString())
        val order = CODE

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

            // Eliminamos los que tienen cantidades <= 0
            val t = fromCursor(c)
            val r: ArrayList<ItemCode> = ArrayList()
            for (x in t) {
                if (x.qty <= 0) {
                    continue
                }
                r.add(x)
            }

            return r
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            return ArrayList()
        }
    }

    fun selectByCode(code: String): ArrayList<ItemCode> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        val columns = getAllColumns()

        val selection = "$CODE LIKE ?" // WHERE code LIKE ?
        val selectionArgs = arrayOf("%$code%")
        val order = CODE

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

            // Eliminamos los que tienen cantidades <= 0
            val t = fromCursor(c)
            val r: ArrayList<ItemCode> = ArrayList()
            for (x in t) {
                if (x.qty <= 0) {
                    continue
                }
                r.add(x)
            }

            return r
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            return ArrayList()
        }
    }

    private fun fromCursor(c: Cursor?): ArrayList<ItemCode> {
        val result = ArrayList<ItemCode>()
        c.use {
            if (it != null) {
                while (it.moveToNext()) {
                    val itId = it.getLong(it.getColumnIndexOrThrow(ITEM_ID))
                    val code = it.getString(it.getColumnIndexOrThrow(CODE))
                    val qty = it.getDouble(it.getColumnIndexOrThrow(QTY))
                    val toUpload = it.getInt(it.getColumnIndexOrThrow(TO_UPLOAD)) == 1

                    val temp = ItemCode(itId, code, qty, toUpload)
                    result.add(temp)
                }
            }
        }
        return result
    }

    companion object {

        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS [" + TABLE_NAME + "] " +
                "( [" + ITEM_ID + "] BIGINT NULL ," +
                " [" + CODE + "] NVARCHAR(45) NULL ," +
                " [" + QTY + "] FLOAT NULL ," +
                " [" + TO_UPLOAD + "] INT NOT NULL" +
                " )"

        val CREATE_INDEX: ArrayList<String> = arrayListOf(
            "DROP INDEX IF EXISTS [IDX_$ITEM_ID]",
            "DROP INDEX IF EXISTS [IDX_$CODE]",
            "CREATE INDEX [IDX_$ITEM_ID] ON [$TABLE_NAME] ([$ITEM_ID])",
            "CREATE INDEX [IDX_$CODE] ON [$TABLE_NAME] ([$CODE])"
        )
    }
}
