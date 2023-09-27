package com.dacosys.warehouseCounter.data.room.database.helper

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.room.database.WcDatabase.Companion.DATABASE_NAME
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import java.io.File
import java.io.IOException
import com.dacosys.warehouseCounter.data.room.database.WcTempDatabase.Companion.DATABASE_NAME as TEMP_DATABASE_NAME

class FileHelper {
    companion object {
        private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName
        private const val IMAGE_CONTROL_DATABASE_NAME = "imagecontrol.sqlite"

        fun removeDataBases() {
            removeImageControlDataBase()
            removeLocalDataBase()
            removeTempLocalDataBase()
        }

        private fun removeImageControlDataBase() {
            // Path to the just created empty db
            val outFileName = context.getDatabasePath(IMAGE_CONTROL_DATABASE_NAME).toString()

            try {
                Log.i("IC DataBase", "Eliminando: $outFileName")
                val f = File(outFileName)
                if (f.exists()) {
                    f.delete()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                ErrorLog.writeLog(null, "removeICDataBase", e)
            }
        }

        private fun removeLocalDataBase() {
            val outFileName = context.getDatabasePath(DATABASE_NAME).toString()
            try {
                Log.i("Local DataBase", "Eliminando: $outFileName")
                val f = File(outFileName)
                if (f.exists()) {
                    f.delete()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                ErrorLog.writeLog(null, "removeLocalDataBase", e)
            }
        }

        private fun removeTempLocalDataBase() {
            val outFileName = context.getDatabasePath(TEMP_DATABASE_NAME).toString()
            try {
                Log.i("Local DataBase", "Eliminando: $outFileName")
                val f = File(outFileName)
                if (f.exists()) {
                    f.delete()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                ErrorLog.writeLog(null, "removeTempLocalDataBase", e)
            }
        }

        fun copyDataBase(inputDbFile: File) {
            Log.d(tag, context.getString(R.string.copying_database))

            val outFile = context.getDatabasePath(DATABASE_NAME)

            Log.d(tag, "${context.getString(R.string.origin)}: ${inputDbFile.absolutePath}")
            Log.d(tag, "${context.getString(R.string.destination)}: $outFile")

            inputDbFile.copyTo(outFile, true)
            Log.d(tag, context.getString(R.string.copy_ok))
        }
    }
}
