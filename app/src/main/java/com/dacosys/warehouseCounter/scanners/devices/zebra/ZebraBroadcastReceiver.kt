package com.dacosys.warehouseCounter.scanners.devices.zebra

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dacosys.warehouseCounter.scanners.devices.zebra.Zebra.Constants.EXTRA_RESULT_INFO

@Suppress("unused")
class ZebraBroadcastReceiver : BroadcastReceiver {
    private lateinit var zebra: Zebra

    constructor()

    constructor(zebra: Zebra) : super() {
        this.zebra = zebra
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        intent.extras
        Log.d(javaClass.simpleName, "DataWedge Action:$action")

        // Get DataWedge version info
        if (intent.hasExtra(Zebra.Constants.EXTRA_RESULT_GET_VERSION_INFO)) {
            val versionInfo = intent.getBundleExtra(Zebra.Constants.EXTRA_RESULT_GET_VERSION_INFO)
            val dwVersion = versionInfo!!.getString("DATAWEDGE")
            Log.i(javaClass.simpleName, "DataWedge Version: $dwVersion")
        }

        if (action == Zebra.Constants.activityIntentFilterAction) {
            //  Received a barcode scan
            try {
                displayScanResult(intent)
            } catch (e: Exception) {
                //  Catch error if the UI does not exist when we receive the broadcast...
            }
        } else if (action == Zebra.Constants.ACTION_RESULT) {
            // Register to receive the result code
            if (intent.hasExtra(Zebra.Constants.EXTRA_RESULT) && intent.hasExtra(Zebra.Constants.EXTRA_COMMAND)) {
                val command = intent.getStringExtra(Zebra.Constants.EXTRA_COMMAND)
                val result = intent.getStringExtra(Zebra.Constants.EXTRA_RESULT)
                var info = ""
                if (intent.hasExtra(EXTRA_RESULT_INFO)) {
                    val resultInfo = intent.getBundleExtra(EXTRA_RESULT_INFO)
                    if (resultInfo != null) {
                        val keys = resultInfo.keySet()
                        for (key in keys) {
                            // First try to get as String
                            val value = resultInfo.getString(key)
                            if (value != null) {
                                info += "$key: $value\n"
                            } else {
                                // Try to get as Array<String>
                                val arrayValue = resultInfo.getStringArray(key)
                                if (arrayValue != null) {
                                    arrayValue.forEach { code ->
                                        info += "$key: $code\n"
                                    }
                                } else {
                                    // Not valid
                                    info += "$key: Not valid\n"
                                }
                            }
                        }
                    }
                    Log.d(
                        javaClass.simpleName, """
     Command: $command
     Result: $result
     Result Info: $info""".trimIndent()
                    )
                    Log.d(
                        javaClass.simpleName,
                        "Error Resulted. Command:$command\nResult: $result\nResult Info: $info"
                    )
                }
            }
        } else if (action == Zebra.Constants.ACTION_RESULT_NOTIFICATION) {
            if (intent.hasExtra(Zebra.Constants.EXTRA_RESULT_NOTIFICATION)) {
                val extras = intent.getBundleExtra(Zebra.Constants.EXTRA_RESULT_NOTIFICATION)
                val notificationType = extras!!.getString(Zebra.Constants.EXTRA_RESULT_NOTIFICATION_TYPE)
                if (notificationType != null) {
                    when (notificationType) {
                        Zebra.Constants.EXTRA_KEY_VALUE_SCANNER_STATUS -> {
                            // Change in scanner status occurred
                            val displayScannerStatusText =
                                extras.getString(Zebra.Constants.EXTRA_KEY_VALUE_NOTIFICATION_STATUS) + ", profile: " + extras.getString(
                                    Zebra.Constants.EXTRA_KEY_VALUE_NOTIFICATION_PROFILE_NAME
                                )
                            Log.i(javaClass.simpleName, "Scanner status: $displayScannerStatusText")
                        }

                        Zebra.Constants.EXTRA_KEY_VALUE_PROFILE_SWITCH -> {}
                        Zebra.Constants.EXTRA_KEY_VALUE_CONFIGURATION_UPDATE -> {}
                    }
                }
            }
        }
    }

    private fun displayScanResult(initiatingIntent: Intent) {
        if (zebra.lockScannerEvent) return

        // store decoded data
        val codeRead = initiatingIntent.getStringExtra(Zebra.Constants.datawedgeIntentKeyData)
        // store decoder type
        // val decodedLabelType = initiatingIntent.getStringExtra(datawedgeIntentKeyLabelType)

        if (codeRead != null) {
            zebra.sendScannedData(codeRead)
        }
    }
}