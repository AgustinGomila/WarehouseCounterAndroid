package com.dacosys.warehouseCounter.data.io

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
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
import java.text.SimpleDateFormat
import java.util.*

class IOFunc {
    companion object {
        private val tag = this::class.java.simpleName

        private const val PENDING_COUNT_PATH = "/pending_counts"
        private const val COMPLETED_COUNT_PATH = "/completed_counts"
        const val WC_ROOT_PATH = "/warehouse_counter"

        val completePendingPath: String =
            "${context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}${WC_ROOT_PATH}/${settingsVm.installationCode}$PENDING_COUNT_PATH/"

        fun getPendingPath(): File {
            return File(completePendingPath)
        }

        val completeCompletedPath: String =
            "${context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}${WC_ROOT_PATH}/${settingsVm.installationCode}$COMPLETED_COUNT_PATH/"

        fun getCompletedPath(): File {
            return File(completeCompletedPath)
        }

        fun getPendingOrders(): ArrayList<OrderRequest> {
            return getOrders(getPendingPath())
        }

        fun getCompletedOrders(): ArrayList<OrderRequest> {
            return getOrders(getCompletedPath())
        }

        private fun getOrders(path: File): ArrayList<OrderRequest> {
            val orArray: ArrayList<OrderRequest> = ArrayList()
            if (isExternalStorageReadable) {

                val filesInFolder = getFiles(path.absolutePath)
                if (!filesInFolder.isNullOrEmpty()) {

                    for (filename in filesInFolder) {
                        val filePath = path.absolutePath + File.separator + filename
                        val tempOr = OrderRequest(filePath)

                        if (tempOr.orderRequestId != null && !orArray.contains(tempOr)) {
                            tempOr.filename = filename
                            orArray.add(tempOr)
                        }
                    }

                }
            }
            return orArray
        }

        /** Checks if external storage is available to at least read */
        private val isExternalStorageReadable: Boolean
            get() {
                val state = Environment.getExternalStorageState()
                return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
            }

        fun removeCountFiles(
            path: File,
            filesToRemove: ArrayList<String>,
            sendEvent: (SnackBarEventData) -> Unit
        ) {
            sendEvent(removeOrders(path, filesToRemove))
        }

        fun countPending(): Int {
            val currentDir = getPendingPath()
            val files = currentDir.listFiles() ?: return 0
            return files.count { t -> t.extension == "json" }
        }

        private fun removeOrders(path: File, files: ArrayList<String>): SnackBarEventData {
            var result = SnackBarEventData(context.getString(R.string.ok), SnackBarType.SUCCESS)

            if (isExternalStorageWritable) {
                for (f in files) {
                    val filePath = "${path.absolutePath}${File.separator}$f"
                    val fl = File(filePath)
                    if (fl.exists()) {
                        if (!fl.delete()) {
                            result = SnackBarEventData(
                                context.getString(R.string.error_when_deleting_counts),
                                SnackBarType.ERROR
                            )
                            break
                        }
                    } else {
                        result = SnackBarEventData(
                            context.getString(R.string.error_when_deleting_counts_file_not_found),
                            SnackBarType.ERROR
                        )
                        break
                    }
                }
            } else {
                result = SnackBarEventData(context.getString(R.string.external_storage_unwritable), SnackBarType.ERROR)
            }

            return result
        }

        @get:Synchronized
        private var isReceiving = false
        fun saveNewOrders(
            activity: AppCompatActivity,
            requestCode: Int,
            itemArray: ArrayList<OrderRequest>,
            onEvent: (SnackBarEventData) -> Unit = { },
            onFinish: (Boolean) -> Unit
        ) {
            if (isReceiving) return

            if (itemArray.isEmpty()) {
                onFinish(true)
                return
            }

            isReceiving = true

            if (!isExternalStorageWritable) {
                isReceiving = false
                onEvent(
                    SnackBarEventData(
                        context.getString(R.string.error_external_storage_not_available_for_reading_or_writing),
                        SnackBarType.ERROR
                    )
                )
                onFinish(false)
            } else {
                verifyWritePermissions(
                    activity = activity,
                    requestCode = requestCode,
                    onGranted = {
                        writeNewOrderRequest(itemArray) {
                            if (it.snackBarType in SnackBarType.getFinish()) {
                                isReceiving = false

                                if (it.snackBarType == SnackBarType.SUCCESS) onFinish(true)
                                else {
                                    onEvent(SnackBarEventData(it.text, it.snackBarType))
                                    onFinish(false)
                                }
                            }
                        }
                    }
                )
            }
        }

        @Suppress("SpellCheckingInspection")
        private const val FILENAME_DATE_FORMAT: String = "yyyyMMddHHmmss"

        fun generateFilename(): String {
            val df = SimpleDateFormat(FILENAME_DATE_FORMAT, Locale.getDefault())
            return df.format(Date())
        }

        fun writeNewOrderRequest(newOrArray: ArrayList<OrderRequest>, onEvent: (SnackBarEventData) -> Unit) {
            val pendingOrderArray = getPendingOrders()

            var isOk = true
            for (newOrder in newOrArray) {
                val orJson = json.encodeToString(OrderRequest.serializer(), newOrder)

                // Acá se comprueba si el ID ya existe y actualizamos la orden.
                // Si no se agrega una orden nueva.
                var alreadyExists = false
                if (pendingOrderArray.any()) {
                    for (pendingOr in pendingOrderArray) {
                        if (pendingOr.orderRequestId == newOrder.orderRequestId) {
                            alreadyExists = true

                            isOk = if (newOrder.completed == true) {
                                // Está completada, eliminar localmente
                                val currentDir = getPendingPath()
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
                    val orFileName = "${generateFilename()}.json"
                    if (!writeToFile(
                            fileName = orFileName,
                            data = orJson,
                            directory = getPendingPath()
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
                Log.i(tag, orJson)
                val orFileName = origOrder.filename.substringAfterLast('/')

                error = !writeJsonToFile(
                    filename = orFileName,
                    value = orJson,
                    completed = newOrder.completed ?: false,
                    onEvent = onEvent,
                )
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                Log.e(tag, e.message ?: "")
                error = true
            }
            return !error
        }

        private val isExternalStorageWritable: Boolean
            get() {
                val state = Environment.getExternalStorageState()
                return Environment.MEDIA_MOUNTED == state
            }

        fun writeJsonToFile(
            filename: String,
            value: String,
            completed: Boolean,
            onEvent: (SnackBarEventData) -> Unit
        ): Boolean {
            if (!isExternalStorageWritable) {
                val res =
                    context.getString(R.string.error_external_storage_not_available_for_reading_or_writing)
                Log.e(tag, res)
                onEvent(SnackBarEventData(res, SnackBarType.ERROR))
                return false
            }

            var error = false

            val path = if (completed) getCompletedPath() else getPendingPath()
            if (writeToFile(fileName = filename, data = value, directory = path)) {
                if (completed) {
                    // Elimino la orden original
                    val file = File(getPendingPath(), filename)
                    if (file.exists()) file.delete()
                }
            } else {
                val res =
                    context.getString(R.string.an_error_occurred_while_trying_to_save_the_count)
                Log.e(tag, res)
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
                Log.e(tag, "File write failed: $e")
                return false
            }
        }

        private fun getFiles(directoryPath: String): ArrayList<String>? {
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
                Log.e(tag, "No existe el archivo: ${file.absolutePath}")
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

        private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        fun verifyWritePermissions(activity: AppCompatActivity, requestCode: Int, onGranted: () -> Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                onGranted()
                return
            }

            // Check if we have write permission
            val storagePermission = ActivityCompat.checkSelfPermission(
                activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                storagePermission != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(activity, PERMISSIONS_STORAGE, requestCode)
            } else {
                onGranted()
            }
        }
    }
}
