package com.dacosys.warehouseCounter.scanners

import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.misc.objects.collectorType.CollectorType
import com.dacosys.warehouseCounter.scanners.honeywell.Honeywell
import com.dacosys.warehouseCounter.scanners.honeywell.HoneywellNative
import com.dacosys.warehouseCounter.scanners.zebra.Zebra
import java.lang.ref.WeakReference

/**
 * Created by Agustin on 19/10/2017.
 */

open class Scanner {
    interface ScannerListener {
        fun scannerCompleted(scanCode: String)
    }

    constructor()

    constructor(activity: AppCompatActivity) {
        build(activity)
    }

    private var scannerDevice: Scanner? = null
    private fun build(activity: AppCompatActivity): Scanner? {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        Log.i(
            this::class.java.simpleName,
            "SCANNER CONNECTED Manufacturer: $manufacturer, Model: $model"
        )

        try {
            val idStr = settingViewModel.collectorType
            var collectorType: CollectorType = CollectorType.none
            if (idStr.isDigitsOnly()) collectorType = CollectorType.getById(idStr.toInt())

            scannerDevice = when (collectorType) {
                CollectorType.honeywell -> Honeywell(activity)
                CollectorType.honeywellNative -> HoneywellNative(WeakReference(activity))
                CollectorType.zebra -> Zebra(activity)
                else -> null
            }
        } catch (e: NoClassDefFoundError) {
            //handle carefully
        }
        return scannerDevice
    }

    fun activityName(): String {
        var r = ""
        val scanner = scannerDevice ?: return r
        try {
            when (scanner) {
                is Honeywell -> r = scanner.activityName
                is HoneywellNative -> r = scanner.activityName
                is Zebra -> r = scanner.activityName
            }
        } catch (e: NoClassDefFoundError) {
            //handle carefully
        }
        return r
    }

    fun onResume() {
        val scanner = scannerDevice ?: return
        try {
            when (scanner) {
                is Honeywell -> scanner.resume()
                is HoneywellNative -> scanner.resume()
                is Zebra -> scanner.resume()
            }
        } catch (e: NoClassDefFoundError) {
            //handle carefully
        }
    }

    fun onPause() {
        val scanner = scannerDevice ?: return
        try {
            when (scanner) {
                is Honeywell -> scanner.pause()
                is HoneywellNative -> scanner.pause()
                is Zebra -> scanner.pause()
            }
        } catch (e: NoClassDefFoundError) {
            //handle carefully
        }
    }

    fun onDestroy() {
        val scanner = scannerDevice
        if (scanner != null) {
            try {
                when (scanner) {
                    is HoneywellNative -> scanner.destroy()
                }
            } catch (e: NoClassDefFoundError) {
                //handle carefully
            }
        }
        // release resources!
        scannerDevice = null
    }

    fun trigger() {
        val scanner = scannerDevice ?: return
        try {
            when (scanner) {
                is Honeywell -> scanner.triggerScanner()
                is HoneywellNative -> scanner.triggerScanner()
                is Zebra -> scanner.triggerScanner()
            }
        } catch (e: NoClassDefFoundError) {
            //handle carefully
        }
    }

    fun lockScanner(lock: Boolean) {
        val scanner = scannerDevice ?: return
        try {
            when (scanner) {
                is Honeywell -> scanner.lockScannerEvent = lock
                is HoneywellNative -> scanner.lockScannerEvent = lock
                is Zebra -> scanner.lockScannerEvent = lock
            }
        } catch (e: NoClassDefFoundError) {
            //handle carefully
        }
    }
}