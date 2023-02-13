package com.dacosys.warehouseCounter.dataBase.user

import android.database.Cursor
import android.database.SQLException
import com.dacosys.warehouseCounter.dataBase.dbHelper.StaticDbHelper
import com.dacosys.warehouseCounter.dataBase.user.UserContract.UserEntry.Companion.ACTIVE
import com.dacosys.warehouseCounter.dataBase.user.UserContract.UserEntry.Companion.NAME
import com.dacosys.warehouseCounter.dataBase.user.UserContract.UserEntry.Companion.PASSWORD
import com.dacosys.warehouseCounter.dataBase.user.UserContract.UserEntry.Companion.TABLE_NAME
import com.dacosys.warehouseCounter.dataBase.user.UserContract.UserEntry.Companion.USER_ID
import com.dacosys.warehouseCounter.dataBase.user.UserContract.getAllColumns
import com.dacosys.warehouseCounter.model.errorLog.ErrorLog
import com.dacosys.warehouseCounter.model.user.User

/**
 * Created by Agustin on 28/12/2016.
 */

class UserDbHelper {

    private val lastId: Long
        get() {
            val sqLiteDatabase = StaticDbHelper.getReadableDb()

            return try {
                val mCount = sqLiteDatabase.rawQuery("SELECT MAX($USER_ID) FROM $TABLE_NAME", null)
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
        active: Boolean,
        password: String?,
    ): Long {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val newId = lastId

        val newUser = User(
            newId,
            name,
            active,
            password
        )

        return try {
            if (sqLiteDatabase.insert(
                    TABLE_NAME,
                    null,
                    newUser.toContentValues()
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

    fun insert(user: User): Long {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        return try {
            if (sqLiteDatabase.insert(
                    TABLE_NAME,
                    null,
                    user.toContentValues()
                ) > 0
            ) {
                user.userId
            } else {
                0L
            }
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            0L
        }
    }

    fun update(user: User): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val selection = "$USER_ID = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(user.userId.toString())

        return try {
            sqLiteDatabase.update(
                TABLE_NAME,
                user.toContentValues(),
                selection,
                selectionArgs
            ) > 0
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            false
        }
    }

    fun delete(user: User): Boolean {
        return deleteById(user.userId)
    }

    fun deleteById(id: Long): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val selection = "$USER_ID = ?" // WHERE id LIKE ?
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

    fun select(): ArrayList<User> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        val columns = getAllColumns()

        val order = NAME

        try {
            val c = sqLiteDatabase.query(
                TABLE_NAME, // Nombre de la tabla
                columns,// Lista de Columnas a consultar
                null,// Columnas para la cláusula WHERE
                null, // Valores a comparar con las columnas del WHERE
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

    fun selectById(userId: Long): User? {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()
        val columns = getAllColumns()

        val selection = "$USER_ID = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(userId.toString())
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

    fun selectByName(description: String): ArrayList<User> {
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

    private fun fromCursor(c: Cursor?): ArrayList<User> {
        val result = ArrayList<User>()
        c.use {
            if (it != null) {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(USER_ID))
                    val name = it.getString(it.getColumnIndexOrThrow(NAME))
                    val password = it.getString(it.getColumnIndexOrThrow(PASSWORD))
                    val active = it.getInt(it.getColumnIndexOrThrow(ACTIVE)) == 1

                    val temp = User(id, name, active, password)
                    result.add(temp)
                }
            }
        }
        return result
    }

    companion object {

        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS [" + TABLE_NAME + "] " +
                "( [" + USER_ID + "] bigint NOT NULL , " +
                "[" + NAME + "] nvarchar(255) NOT NULL , " +
                "[" + ACTIVE + "] int NOT NULL , " +
                "[" + PASSWORD + "] nvarchar(100) NULL , " +
                "CONSTRAINT [PK_" + USER_ID + "] PRIMARY KEY ([" + USER_ID + "]) )"

        val CREATE_INDEX: ArrayList<String> = arrayListOf(
            "DROP INDEX IF EXISTS [IDX_$NAME]",
            "CREATE INDEX [IDX_$NAME] ON [$TABLE_NAME] ([$NAME])"
        )
    }
}
