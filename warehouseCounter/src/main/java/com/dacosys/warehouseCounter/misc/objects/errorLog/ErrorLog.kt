package com.dacosys.warehouseCounter.misc.objects.errorLog

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.UTCDataTime
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class ErrorLog {
    companion object : ActivityCompat.OnRequestPermissionsResultCallback {
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

        var errorLogPath = Statics.getLogPath()
        private const val REQUEST_EXTERNAL_STORAGE = 1777
        private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        private fun verifyPermissions(activity: AppCompatActivity) {
            // Check if we have write permission
            val storagePermission = ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            if (storagePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
                )
            } else {
                reallyWriteLog()
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
            if (activity == null) {
                return
            }

            tMsg = msg
            tClassName = className

            // Ver si la aplicación tiene permiso de escritura, sino pedir permiso.
            verifyPermissions(activity)
        }

        fun writeLog(activity: FragmentActivity? = null, className: String, ex: Exception) {
            writeLog(activity as AppCompatActivity, className, ex)
        }

        fun writeLog(activity: AppCompatActivity? = null, className: String, ex: Exception) {
            if (activity == null) {
                return
            }

            val errors = StringWriter()
            ex.printStackTrace(PrintWriter(errors))
            tMsg = errors.toString()
            tClassName = className

            // Ver si la aplicación tiene permiso de escritura, sino pedir permiso.
            verifyPermissions(activity)
        }

        private fun reallyWriteLog() {
            Log.e(tClassName, tMsg)

            val writeLog = settingViewModel().registryError
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