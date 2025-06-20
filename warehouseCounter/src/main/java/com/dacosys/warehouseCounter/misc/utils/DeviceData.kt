package com.dacosys.warehouseCounter.misc.utils

import android.os.Build
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import org.json.JSONObject
import java.net.NetworkInterface
import java.util.*

class DeviceData {
    companion object {
        fun getDeviceData(): JSONObject {
            val ip = getIPAddress()
            val macAddress = "" // getMACAddress()
            val operatingSystem = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"

            val pm = WarehouseCounterApp.context.packageManager
            val pInfo = pm.getPackageInfo(WarehouseCounterApp.context.packageName, 0)
            var appName = "Unknown"
            if (pInfo != null) {
                appName = "${pm.getApplicationLabel(pInfo.applicationInfo)} ${
                    WarehouseCounterApp.context.getString(R.string.app_milestone)
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