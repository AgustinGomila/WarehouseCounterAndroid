package com.dacosys.warehouseCounter.dataBase.dbHelper

import android.database.sqlite.SQLiteDatabase

class StaticDbHelper {
    companion object {
        fun getReadableDb(): SQLiteDatabase {
            return DataBaseHelper.getReadableDb()
        }

        fun getWritableDb(): SQLiteDatabase {
            return DataBaseHelper.getWritableDb()
        }
    }
}