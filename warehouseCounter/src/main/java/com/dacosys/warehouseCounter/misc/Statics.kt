package com.dacosys.warehouseCounter.misc

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.View
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.applicationName
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.room.dao.user.UserCoroutines
import com.dacosys.warehouseCounter.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.room.entity.user.User
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.scanners.vh75.Vh75Bt
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject
import java.io.*
import java.math.BigDecimal
import java.net.NetworkInterface
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.util.*

class Statics {
    companion object {
        var appName: String = "${applicationName()}M12"

        // Este flag es para reinicializar el colector después de cambiar en Settings.
        var collectorTypeChanged = false

        fun getScanOptions(): ScanOptions {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            options.setPrompt(context.getString(R.string.place_the_code_in_the_rectangle_of_the_viewer_to_capture_it))
            options.setBeepEnabled(true)
            options.setBarcodeImageEnabled(true)
            options.setOrientationLocked(false)
            //options.setTimeout(8000)

            return options
        }

        var downloadDbRequired = false

        val lineSeparator: String = System.getProperty("line.separator") ?: "\\r\\n"

        /**
         * Modo DEMO
         */
        const val demoMode = false
        const val superDemoMode = false
        const val downloadDbAlways = false
        const val testMode = false

        // Estos números se corresponden con package_id https://manager.dacosys.com/package/index
        const val APP_VERSION_ID: Int = 8 // WarehouseCounter Milestone12
        const val APP_VERSION_ID_IMAGECONTROL = 13 // ImageControl Milestone13

        // Este es el valor de program_id (Ver archivo Program.cs en el proyecto Identification)
        // Lo utiliza internamente ImageControl para identificar la aplicación que lo está usando.
        // Ver: https://source.cloud.google.com/assetcontrol/libs_windows/+/master:Collector/Identification/Program.cs
        const val INTERNAL_IMAGE_CONTROL_APP_ID: Int = 4

        const val WC_ROOT_PATH = "/warehouse_counter"
        private const val PENDING_COUNT_PATH = "/pending_counts"
        private const val COMPLETED_COUNT_PATH = "/completed_counts"

        fun getPendingPath(): File {
            return File("${context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}$WC_ROOT_PATH/${settingViewModel.installationCode}$PENDING_COUNT_PATH/")
        }

        fun getCompletedPath(): File {
            return File("${context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}$WC_ROOT_PATH/${settingViewModel.installationCode}$COMPLETED_COUNT_PATH/")
        }

        fun isOnline(): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                        return true
                    }

                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                        return true
                    }

                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                        return true
                    }
                }
            }
            return false
        }

        fun initRequired(): Boolean {
            val sv = settingViewModel
            return if (sv.useBtRfid) {
                if (Rfid.rfidDevice == null) {
                    true
                } else {
                    if ((Rfid.rfidDevice is Vh75Bt)) {
                        (Rfid.rfidDevice as Vh75Bt).mState == Vh75Bt.STATE_NONE
                    } else false
                }
            } else false
        }

        // region Variables con valores predefinidos para el selector de cantidades
        var decimalSeparator: Char = '.'
        var decimalPlaces: Int = 0
        // endregion

        // region Colección temporal de ItemCode
        // Reinsertar cuando se haya descargado la base de datos
        private var tempItemCodes: ArrayList<ItemCode> = ArrayList()

        fun insertItemCodes() {
            if (tempItemCodes.isEmpty()) return

            for (f in tempItemCodes) {
                if (f.code.isNullOrEmpty()) continue

                ItemCodeCoroutines().getByCode(f.code) {
                    if (!it.any()) {
                        f.toUpload = 0
                        ItemCodeCoroutines().add(f)
                    }
                }
            }

            tempItemCodes.clear()
        }
        // endregion

        fun generateTaskCode(): Int {
            val min = 10000
            val max = 99999
            return Random().nextInt(max - min + 1) + min
        }

        /* Checks if external storage is available to at least read */
        val isExternalStorageReadable: Boolean
            get() {
                val state = Environment.getExternalStorageState()
                return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
            }

        // region CURRENT USER
        var currentUserId: Long = -1L
        var currentUserName: String = ""
        var isLogged = false

        private var currentUser: User? = null
        fun getCurrentUser(onResult: (User?) -> Unit = {}) {
            if (currentUser == null) {
                UserCoroutines().getById(currentUserId) {
                    currentUser = it
                    onResult(currentUser)
                }
            } else onResult(currentUser)
        }

        fun cleanCurrentUser() {
            currentUser = null
        }
        // endregion CURRENT USER

        fun isDebuggable(): Boolean {
            return 0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
        }

        fun roundToString(d: Double, decimalPlaces: Int): String {
            val r = round(d, decimalPlaces).toString()
            return if (decimalPlaces == 0) {
                r.substring(0, r.indexOf('.'))
            } else {
                r
            }
        }

        fun roundToString(d: Float, decimalPlaces: Int): String {
            val r = round(d, decimalPlaces).toString()
            return if (decimalPlaces == 0) {
                r.substring(0, r.indexOf('.'))
            } else {
                r
            }
        }

        fun round(d: Double, decimalPlaces: Int): Double {
            var bd = BigDecimal(d.toString())
            bd = bd.setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP)
            return bd.toDouble()
        }

        fun round(d: Float, decimalPlaces: Int): Float {
            var bd = BigDecimal(d.toString())
            bd = bd.setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP)
            return bd.toFloat()
        }

        @Suppress("unused")
        inline fun <reified T> toArrayList(
            classToCastTo: Class<T>,
            values: Collection<Any>,
        ): ArrayList<T> {
            val collection = ArrayList<T>()
            for (value in values) {
                collection.add(classToCastTo.cast(value)!!)
            }
            return collection
        }

        val isExternalStorageWritable: Boolean
            get() {
                val state = Environment.getExternalStorageState()
                return Environment.MEDIA_MOUNTED == state
            }

        fun getDeviceData(): JSONObject {
            val ip = getIPAddress()
            val macAddress = "" // getMACAddress()
            val operatingSystem = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"

            val pm = context.packageManager
            val pInfo = pm.getPackageInfo(context.packageName, 0)
            var appName = "Unknown"
            if (pInfo != null) {
                appName = "${pm.getApplicationLabel(pInfo.applicationInfo)} ${
                    context.getString(R.string.app_milestone)
                } ${pInfo.versionName}"
            }

            // val processorId = Settings.Secure.getString(context().contentResolver, Settings.Secure.ANDROID_ID)
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

            val collectorData = JSONObject()
            collectorData.put("ip", ip).put("macAddress", macAddress)
                .put("operatingSystem", operatingSystem).put("appName", appName)
                .put("deviceName", deviceName)

            return collectorData
        }

        /**
         * Get IP address from first non-localhost interface
         * @param useIPv4   true=return ipv4, false=return ipv6
         * @return  address or empty string
         */
        private fun getIPAddress(useIPv4: Boolean = true): String {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intF in interfaces) {
                    val addressArray = Collections.list(intF.inetAddresses)
                    for (address in addressArray) {
                        if (!address.isLoopbackAddress) {
                            val hostAddress = address.hostAddress ?: continue
                            //boolean isIPv4 = InetAddressUtils.isIPv4Address(hostAddress);
                            val isIPv4 = hostAddress.indexOf(':') < 0

                            if (useIPv4) {
                                if (isIPv4) return hostAddress
                            } else {
                                if (!isIPv4) {
                                    val delimiter = hostAddress.indexOf('%') // drop ip6 zone suffix
                                    return if (delimiter < 0) hostAddress.uppercase(Locale.getDefault()) else hostAddress.substring(
                                        0, delimiter
                                    ).uppercase(Locale.getDefault())
                                }
                            }
                        }
                    }
                }
            } catch (ignored: Exception) {
            }
            // for now eat exceptions
            return ""
        }

        //private fun getMACAddress(interfaceName: String = "wlan0"): String {
        //    try {
        //        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        //        for (intF in interfaces) {
        //            if (!intF.name.equals(interfaceName, true)) continue
        //            val mac = intF.hardwareAddress ?: return ""
        //            val buf = StringBuilder()
        //            for (aMac in mac) buf.append(String.format("%02X:", aMac))
        //            if (buf.isNotEmpty()) buf.deleteCharAt(buf.length - 1)
        //            return buf.toString().replace(":", "")
        //        }
        //    } catch (ignored: Exception) {
        //    }
        //
        //    // for now eat exceptions
        //    return ""
        //}

        // region Operaciones de escritura en almacenamiento de Órdenes

        fun writeJsonToFile(v: View, filename: String, value: String, completed: Boolean): Boolean {
            if (!isExternalStorageWritable) {
                val res =
                    context.getString(R.string.error_external_storage_not_available_for_reading_or_writing)
                Log.e(this::class.java.simpleName, res)
                makeText(v, res, ERROR)
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
                Log.e(this::class.java.simpleName, res)
                makeText(v, res, ERROR)
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
            if (filename.isEmpty()) {
                return ""
            }

            val file = File(filename)
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

        // endregion
    }
}