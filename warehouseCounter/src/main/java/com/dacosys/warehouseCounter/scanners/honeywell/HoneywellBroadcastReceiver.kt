package com.dacosys.warehouseCounter.scanners.honeywell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dacosys.warehouseCounter.R

@Suppress("ConvertSecondaryConstructorToPrimary", "unused")
class HoneywellBroadcastReceiver : BroadcastReceiver {
    private lateinit var honeywell: Honeywell

    constructor()

    constructor(honeywellBroadcasts: Honeywell) : super() {
        this.honeywell = honeywellBroadcasts
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.w(this::class.java.simpleName, "Intent received {$intent} ({$context})")

        try {
            when (intent.action) {
                Honeywell.Constants.ACTION_BARCODE_DATA -> {
                    if (honeywell.lockScannerEvent) return

                    /*
                    These extras are available:
                        "version" (int) = Data Intent Api version
                        "aimId" (String) = The AIM Identifier
                        "charset" (String) = The charset used to convert "dataBytes" to "data" string "codeId" (String) = The Honeywell Symbology Identifier
                        "data" (String) = The barcode data as a string
                        "dataBytes" (byte[]) = The barcode data as a byte array
                        "timestamp" (String) = The barcode timestamp
                    */

                    val version = intent.getIntExtra("version", 0)
                    if (version >= 1) {
                        val data = intent.getStringExtra("data")
                        if (data != null) {
                            honeywell.sendScannedData(data)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(
                this::class.java.simpleName,
                context.getString(R.string.barcode_failure)
            )
        }
    }
}