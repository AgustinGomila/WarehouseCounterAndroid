package com.dacosys.warehouseCounter.data.room.database.helper

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.room.database.WcDatabase.Companion.DATABASE_NAME
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import com.dacosys.warehouseCounter.data.room.database.WcTempDatabase.Companion.DATABASE_NAME as TEMP_DATABASE_NAME

class FileHelper {
    companion object {
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

        fun copyDataBase(
            inputDbFile: File?,
            mCallback: DownloadDb.DownloadDbTask? = null,
        ): Boolean {
            if (inputDbFile == null) return false

            Log.d(
                this::class.java.simpleName, context.getString(R.string.copying_database)
            )

            //Open your local db as the input stream
            val myInput = FileInputStream(inputDbFile)

            // Path to the just created empty db
            val outFileName = context.getDatabasePath(DATABASE_NAME).toString()

            val file = File(outFileName)
            if (file.exists()) {
                Log.d(this::class.java.simpleName, "Eliminando base de datos antigua: $outFileName")
                file.delete()
            }

            Log.d(
                this::class.java.simpleName,
                "${context.getString(R.string.origin)}: ${inputDbFile.absolutePath}"
            )
            Log.d(
                this::class.java.simpleName,
                "${context.getString(R.string.destination)}: $outFileName"
            )

            try {
                //Open the empty db as the output stream
                val myOutput = FileOutputStream(outFileName)

                //transfer bytes from the inputfile to the outputfile
                val buffer = ByteArray(1024)
                var length: Int
                val totalSize: Long = inputDbFile.length()
                var total: Long = 0
                while (run {
                        length = myInput.read(buffer)
                        length
                    } > 0) {
                    total += length.toLong()
                    // only if total length is known
                    mCallback?.onDownloadFileTask(
                        msg = context.getString(R.string.copying_database),
                        fileType = FileType.DB_FILE,
                        downloadStatus = DownloadStatus.COPYING,
                        progress = (total * 100 / totalSize).toInt()
                    )
                    myOutput.write(buffer, 0, length)
                }

                //Close the streams
                myOutput.flush()
                myOutput.close()
                myInput.close()
            } catch (e: IOException) {
                ErrorLog.writeLog(
                    null, this::class.java.simpleName, "${
                        context.getString(R.string.exception_error)
                    } (Copy database): ${e.message}"
                )
                return false
            }

            Log.d(this::class.java.simpleName, context.getString(R.string.copy_ok))

            return true
        }
    }
}
