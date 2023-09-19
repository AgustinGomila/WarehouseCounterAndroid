package com.dacosys.warehouseCounter.misc

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.applicationName
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.data.room.dao.user.UserCoroutines
import com.dacosys.warehouseCounter.data.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.data.room.entity.user.User
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.NetworkInterface
import java.util.*

class Statics {
    companion object {
        var appName: String = "${applicationName}M12"

        // Este flag es para reinicializar el colector después de cambiar en Settings.
        var collectorTypeChanged = false

        var downloadDbRequired = false

        val lineSeparator: String = System.getProperty("line.separator") ?: "\\r\\n"
        const val DATE_FORMAT: String = "yyyy-MM-dd hh:mm:ss"

        /**
         * Modo DEMO
         */
        const val DEMO_MODE = false
        const val SUPER_DEMO_MODE = false
        const val DOWNLOAD_DB_ALWAYS = false
        const val TEST_MODE = false

        // Estos números se corresponden con package_id https://manager.dacosys.com/package/index
        const val APP_VERSION_ID: Int = 8 // WarehouseCounter Milestone12
        const val APP_VERSION_ID_IMAGECONTROL = 13 // ImageControl Milestone13

        // Este es el valor de program_id (Ver archivo Program.cs en el proyecto Identification)
        // Lo utiliza internamente ImageControl para identificar la aplicación que lo está usando.
        // Ver: https://source.cloud.google.com/assetcontrol/libs_windows/+/master:Collector/Identification/Program.cs
        const val INTERNAL_IMAGE_CONTROL_APP_ID: Int = 4

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

        // region Colección temporal de ItemCode
        // Reinsertar cuando se haya descargado la base de datos
        private var tempItemCodes: ArrayList<ItemCode> = ArrayList()

        fun insertItemCodes() {
            if (tempItemCodes.isEmpty()) return

            for (f in tempItemCodes) {
                if (f.code.isNullOrEmpty()) continue

                ItemCodeCoroutines.getByCode(f.code) {
                    if (!it.any()) {
                        f.toUpload = 0
                        ItemCodeCoroutines.add(f)
                    }
                }
            }

            tempItemCodes.clear()
        }
        // endregion

        // region CURRENT USER
        var currentUserId: Long = -1L
        var currentPass: String = ""
        var currentUserName: String = ""
        var isLogged = false

        private var currentUser: User? = null
        fun getCurrentUser(onResult: (User?) -> Unit = {}) {
            if (currentUser == null) {
                UserCoroutines.getById(currentUserId) {
                    currentUser = it
                    onResult(currentUser)
                }
            } else onResult(currentUser)
        }

        fun cleanCurrentUser() {
            currentUser = null

            currentUserId = -1L
            currentPass = ""
            currentUserName = ""
            isLogged = false
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
            bd = bd.setScale(decimalPlaces, RoundingMode.HALF_UP)
            return bd.toDouble()
        }

        fun round(d: Float, decimalPlaces: Int): Float {
            var bd = BigDecimal(d.toString())
            bd = bd.setScale(decimalPlaces, RoundingMode.HALF_UP)
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
    }
}
