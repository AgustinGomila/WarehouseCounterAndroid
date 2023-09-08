package com.dacosys.warehouseCounter.data.io

import android.util.Log
import android.view.View
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import java.io.*
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset

class IOFunc {
    companion object {
        fun writeJsonToFile(v: View, filename: String, value: String, completed: Boolean): Boolean {

            fun showSnackBar(text: String, snackBarType: SnackBarType) {
                makeText(v, text, snackBarType)
            }

            if (!Statics.isExternalStorageWritable) {
                val res =
                    context.getString(R.string.error_external_storage_not_available_for_reading_or_writing)
                Log.e(this::class.java.simpleName, res)
                showSnackBar(res, SnackBarType.ERROR)
                return false
            }

            var error = false

            val path = if (completed) Statics.getCompletedPath() else Statics.getPendingPath()
            if (writeToFile(fileName = filename, data = value, directory = path)) {
                if (completed) {
                    // Elimino la orden original
                    val file = File(Statics.getPendingPath(), filename)
                    if (file.exists()) file.delete()
                }
            } else {
                val res =
                    context.getString(R.string.an_error_occurred_while_trying_to_save_the_count)
                Log.e(this::class.java.simpleName, res)
                showSnackBar(res, SnackBarType.ERROR)
                error = true
            }

            return !error
        }

        fun writeToFile(fileName: String, data: String, directory: File): Boolean {
            try {
                val file = File(directory, fileName)

                // Save your stream, don't forget to flush() it before closing it.
                file.parentFile?.mkdirs()
                file.createNewFile()

                val fOut = FileOutputStream(file)
                val outWriter = OutputStreamWriter(fOut)
                outWriter.append(data)

                outWriter.close()

                fOut.flush()
                fOut.close()

                return true
            } catch (e: IOException) {
                Log.e(this::class.java.simpleName, "File write failed: $e")
                return false
            }
        }

        fun getFiles(directoryPath: String): ArrayList<String>? {
            val filesArrayList = ArrayList<String>()
            val f = File(directoryPath)

            f.mkdirs()
            val files = f.listFiles()
            if (files == null || files.isEmpty()) return null
            else {
                files.filter { it.name.endsWith(".json") }.mapTo(filesArrayList) { it.name }
            }

            return filesArrayList
        }

        fun getJsonFromFile(filename: String): String {
            if (filename.isEmpty()) return ""

            val file = File(filename)
            return getJsonFromFile(file)
        }

        private fun getJsonFromFile(file: File): String {
            if (!file.exists()) {
                Log.e("WC", "No existe el archivo: ${file.absolutePath}")
                return ""
            }

            val stream = FileInputStream(file)
            val jsonStr: String
            try {
                val fc: FileChannel = stream.channel
                val bb: MappedByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())

                jsonStr = Charset.defaultCharset().decode(bb).toString()
            } catch (ex: Exception) {
                ex.printStackTrace()
                return ""
            } finally {
                stream.close()
            }

            return jsonStr
        }
    }
}
