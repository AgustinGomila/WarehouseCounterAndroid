package com.dacosys.warehouseCounter.scanners.zebra

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.ACTION_RESULT
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.ACTION_RESULT_NOTIFICATION
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.ACTIVITY_INTENT_FILTER_ACTION
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.DATA_WEDGE_INTENT_KEY_DATA
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.EXTRA_COMMAND
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.EXTRA_KEY_VALUE_CONFIGURATION_UPDATE
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.EXTRA_KEY_VALUE_NOTIFICATION_PROFILE_NAME
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.EXTRA_KEY_VALUE_NOTIFICATION_STATUS
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.EXTRA_KEY_VALUE_PROFILE_SWITCH
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.EXTRA_KEY_VALUE_SCANNER_STATUS
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.EXTRA_RESULT
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.EXTRA_RESULT_GET_VERSION_INFO
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.EXTRA_RESULT_INFO
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.EXTRA_RESULT_NOTIFICATION
import com.dacosys.warehouseCounter.scanners.zebra.Zebra.Constants.EXTRA_RESULT_NOTIFICATION_TYPE

@Suppress("ConvertSecondaryConstructorToPrimary", "unused")
class ZebraBroadcastReceiver : BroadcastReceiver {
    private lateinit var zebra: Zebra

    constructor()

    constructor(zebra: Zebra) : super() {
        this.zebra = zebra
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val b = intent.extras
        Log.d(this::class.java.simpleName, "DataWedge Action:$action")

        // Get DataWedge version info
        if (intent.hasExtra(EXTRA_RESULT_GET_VERSION_INFO)) {
            val versionInfo = intent.getBundleExtra(EXTRA_RESULT_GET_VERSION_INFO)
            val dwVersion = versionInfo!!.getString("DATAWEDGE")
            Log.i(this::class.java.simpleName, "DataWedge Version: $dwVersion")
        }

        if (action == ACTIVITY_INTENT_FILTER_ACTION) {
            //  Received a barcode scan
            try {
                displayScanResult(intent)
            } catch (e: Exception) {
                //  Catch error if the UI does not exist when we receive the broadcast...
            }
        } else if (action == ACTION_RESULT) {
            // Register to receive the result code
            if (intent.hasExtra(EXTRA_RESULT) && intent.hasExtra(EXTRA_COMMAND)) {
                val command = intent.getStringExtra(EXTRA_COMMAND)
                val result = intent.getStringExtra(EXTRA_RESULT)
                var info = ""
                if (intent.hasExtra(EXTRA_RESULT_INFO)) {
                    val resultInfo = intent.getBundleExtra(EXTRA_RESULT_INFO)
                    val keys = resultInfo!!.keySet()
                    for (key in keys) {
                        val `object` = resultInfo[key]
                        if (`object` is String) {
                            info += "$key: $`object`\n"
                        } else if (`object` is Array<*>) {
                            for (code in `object`) {
                                info += "$key: $code\n"
                            }
                        }
                    }
                    Log.d(
                        this::class.java.simpleName, """
     Command: $command
     Result: $result
     Result Info: $info""".trimIndent()
                    )
                    Log.d(
                        this::class.java.simpleName,
                        "Error Resulted. Command:$command\nResult: $result\nResult Info: $info"
                    )
                }
            }
        } else if (action == ACTION_RESULT_NOTIFICATION) {
            if (intent.hasExtra(EXTRA_RESULT_NOTIFICATION)) {
                val extras = intent.getBundleExtra(EXTRA_RESULT_NOTIFICATION)
                val notificationType = extras!!.getString(EXTRA_RESULT_NOTIFICATION_TYPE)
                if (notificationType != null) {
                    when (notificationType) {
                        EXTRA_KEY_VALUE_SCANNER_STATUS -> {
                            // Change in scanner status occurred
                            val displayScannerStatusText = extras.getString(
                                EXTRA_KEY_VALUE_NOTIFICATION_STATUS
                            ) +
                                    ", profile: " + extras.getString(
                                EXTRA_KEY_VALUE_NOTIFICATION_PROFILE_NAME
                            )
                            Log.i(
                                this::class.java.simpleName,
                                "Scanner status: $displayScannerStatusText"
                            )
                        }

                        EXTRA_KEY_VALUE_PROFILE_SWITCH -> {}
                        EXTRA_KEY_VALUE_CONFIGURATION_UPDATE -> {}
                    }
                }
            }
        }
    }

    private fun displayScanResult(initiatingIntent: Intent) {
        if (zebra.lockScannerEvent) return

        // store decoded data
        val codeRead = initiatingIntent.getStringExtra(DATA_WEDGE_INTENT_KEY_DATA)
        // store decoder type
        // val decodedLabelType = initiatingIntent.getStringExtra(datawedgeIntentKeyLabelType)

        if (codeRead != null) {
            zebra.sendScannedData(codeRead)
        }
    }
}