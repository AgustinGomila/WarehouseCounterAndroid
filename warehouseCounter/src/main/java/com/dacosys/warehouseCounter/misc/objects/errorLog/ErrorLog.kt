package com.dacosys.warehouseCounter.misc.objects.errorLog

import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.WC_ROOT_PATH
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.verifyWritePermissions
import com.dacosys.warehouseCounter.misc.utils.UTCDataTime
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class ErrorLog {
    companion object : ActivityCompat.OnRequestPermissionsResultCallback {
        private const val ERROR_LOG_PATH = "/error_log"
        private const val REQUEST_EXTERNAL_STORAGE = 1777

        val errorLogPath: File
            get() {
                return File("${context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}${WC_ROOT_PATH}/${settingsVm.installationCode}${ERROR_LOG_PATH}/")
            }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray,
        ) {
            when (requestCode) {
                REQUEST_EXTERNAL_STORAGE -> {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        reallyWriteLog()
                    }
                }
            }
        }

        private fun getFileName(): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")

            val date = sdf.parse(sdf.format(Date())) ?: return "2001-01-01"

            sdf.timeZone = TimeZone.getDefault()
            return sdf.format(date).toString().replace("/", "-") + ".log"
        }

        private var tClassName: String = ""
        private var tMsg: String = ""

        fun writeLog(activity: FragmentActivity? = null, className: String, msg: String) {
            writeLog(activity as AppCompatActivity, className, msg)
        }

        fun writeLog(activity: AppCompatActivity? = null, className: String, msg: String) {
            if (activity == null) return

            tMsg = msg
            tClassName = className

            // Ver si la aplicación tiene permiso de escritura, si no pedir permiso.
            verifyWritePermissions(
                activity = activity,
                requestCode = REQUEST_EXTERNAL_STORAGE,
                onGranted = { reallyWriteLog() })
        }

        fun writeLog(activity: FragmentActivity? = null, className: String, ex: Exception) {
            writeLog(activity as AppCompatActivity, className, ex)
        }

        fun writeLog(activity: AppCompatActivity? = null, className: String, ex: Exception) {
            if (activity == null) return

            val errors = StringWriter()
            ex.printStackTrace(PrintWriter(errors))
            tMsg = errors.toString()
            tClassName = className

            // Ver si la aplicación tiene permiso de escritura, si no pedir permiso.
            verifyWritePermissions(
                activity = activity,
                requestCode = REQUEST_EXTERNAL_STORAGE,
                onGranted = { reallyWriteLog() },
            )
        }

        private fun reallyWriteLog() {
            Log.e(tClassName, tMsg)

            val writeLog = settingsVm.registryError
            if (writeLog) {
                val logFileName = getFileName()
                val logPath = errorLogPath

                val logFile = File("$logPath/$logFileName")
                val currentDate = UTCDataTime.getUTCDateTimeAsString()

                val parent = logFile.parentFile
                parent?.mkdirs()

                try {
                    val fOut = FileOutputStream(logFile, true)
                    val outWriter = OutputStreamWriter(fOut)
                    outWriter.append("\r$currentDate - $tClassName: $tMsg")

                    outWriter.close()

                    fOut.flush()
                    fOut.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
        }

        fun getLastErrorLog(): File? {
            val directory = errorLogPath
            val files = directory.listFiles()

            var newestFile: File? = null
            if (files != null && files.any()) {
                for (f in files) {
                    if (newestFile == null || f.lastModified() > (newestFile.lastModified())) {
                        newestFile = f
                    }
                }
            }

            return newestFile
        }
    }
}