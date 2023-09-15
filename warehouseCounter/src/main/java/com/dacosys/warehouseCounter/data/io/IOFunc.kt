package com.dacosys.warehouseCounter.data.io

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.UnsupportedEncodingException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset

class IOFunc {
    companion object {
        fun writeNewOrderRequest(newOrArray: java.util.ArrayList<OrderRequest>, onEvent: (SnackBarEventData) -> Unit) {
            val pendingOrderArray = OrderRequest.getPendingOrders()

            var isOk = true
            for (newOrder in newOrArray) {
                val orJson = WarehouseCounterApp.json.encodeToString(OrderRequest.serializer(), newOrder)

                // Acá se comprueba si el ID ya existe y actualizamos la orden.
                // Si no se agrega una orden nueva.
                var alreadyExists = false
                if (pendingOrderArray.any()) {
                    for (pendingOr in pendingOrderArray) {
                        if (pendingOr.orderRequestId == newOrder.orderRequestId) {
                            alreadyExists = true

                            isOk = if (newOrder.completed == true) {
                                // Está completada, eliminar localmente
                                val currentDir = Statics.getPendingPath()
                                val filePath = "${currentDir.absolutePath}${File.separator}${pendingOr.filename}"
                                val fl = File(filePath)
                                fl.delete()
                            } else {
                                // Actualizar contenido local
                                updateOrder(origOrder = pendingOr, newOrder = newOrder, onEvent = onEvent)
                            }

                            break
                        }
                    }
                }

                if (!alreadyExists) {
                    val orFileName = "${OrderRequest.generateFilename()}.json"
                    if (!writeToFile(
                            fileName = orFileName,
                            data = orJson,
                            directory = Statics.getPendingPath()
                        )
                    ) {
                        isOk = false
                        break
                    }
                }
            }

            newOrArray.clear()

            if (isOk) {
                val res = context.getString(R.string.new_counts_saved)
                onEvent(SnackBarEventData(res, SnackBarType.SUCCESS))
            } else {
                val res = context.getString(R.string.an_error_occurred_while_trying_to_save_the_count)
                onEvent(SnackBarEventData(res, SnackBarType.ERROR))
            }
        }

        private fun updateOrder(
            origOrder: OrderRequest,
            newOrder: OrderRequest,
            onEvent: (SnackBarEventData) -> Unit
        ): Boolean {
            var error: Boolean
            try {
                val orJson = json.encodeToString(OrderRequest.serializer(), newOrder)
                Log.i(this::class.java.simpleName, orJson)
                val orFileName = origOrder.filename.substringAfterLast('/')

                error = !writeJsonToFile(
                    filename = orFileName,
                    value = orJson,
                    completed = newOrder.completed ?: false,
                    onEvent = onEvent,
                )
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                Log.e(this::class.java.simpleName, e.message ?: "")
                error = true
            }
            return !error
        }

        fun writeJsonToFile(
            filename: String,
            value: String,
            completed: Boolean,
            onEvent: (SnackBarEventData) -> Unit
        ): Boolean {
            if (!Statics.isExternalStorageWritable) {
                val res =
                    context.getString(R.string.error_external_storage_not_available_for_reading_or_writing)
                Log.e(this::class.java.simpleName, res)
                onEvent(SnackBarEventData(res, SnackBarType.ERROR))
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
                onEvent(SnackBarEventData(res, SnackBarType.ERROR))
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
