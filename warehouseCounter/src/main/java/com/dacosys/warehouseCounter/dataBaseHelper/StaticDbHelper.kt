package com.dacosys.warehouseCounter.dataBaseHelper

import android.database.sqlite.SQLiteDatabase

class StaticDbHelper {
    companion object {
        fun createDb() {
            DataBaseHelper.createDataBase()
        }

        fun getReadableDb(): SQLiteDatabase {
            return DataBaseHelper.getInstance()!!.readableDatabase
        }

        fun getWritableDb(): SQLiteDatabase {
            return DataBaseHelper.getInstance()!!.writableDatabase
        }
    }
}