package com.example.warehouseCounter.data.room.database.helper

import android.util.Log
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.data.room.database.WcDatabase.Companion.DATABASE_NAME
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import com.example.warehouseCounter.data.room.database.WcTempDatabase.Companion.DATABASE_NAME as TEMP_DATABASE_NAME

class FileHelper {
    companion object {
        private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName
        private const val IMAGE_CONTROL_DATABASE_NAME = "imagecontrol.sqlite"

        fun removeDataBases() {
            Log.i(tag, "Eliminando bases de datos...")
            context.deleteDatabase(IMAGE_CONTROL_DATABASE_NAME)
            context.deleteDatabase(DATABASE_NAME)
            context.deleteDatabase(TEMP_DATABASE_NAME)
        }

        fun moveDatabaseFrom(inputDbFile: File) {
            Log.d(tag, context.getString(R.string.copying_database))

            val outFile = context.getDatabasePath(DATABASE_NAME)

            Log.d(tag, "${context.getString(R.string.origin)}: ${inputDbFile.absolutePath}")
            Log.d(tag, "${context.getString(R.string.destination)}: $outFile")

            try {
                FileInputStream(inputDbFile).use { inputStream ->
                    FileOutputStream(outFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d(tag, context.getString(R.string.copy_ok))
            } catch (e: IOException) {
                Log.e(tag, "${context.getString(R.string.error_copying_database)}: ${e.message}")
            }
        }
    }
}
