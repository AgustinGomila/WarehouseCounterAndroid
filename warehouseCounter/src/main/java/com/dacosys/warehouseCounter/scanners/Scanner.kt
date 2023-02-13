package com.dacosys.warehouseCounter.scanners

import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.model.collectorType.CollectorType
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
            val collectorType = CollectorType.getById(settingViewModel().collectorType)
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
        if (scannerDevice == null) return r
        try {
            when (scannerDevice!!.javaClass) {
                Honeywell::class.java -> {
                    r = (scannerDevice as Honeywell).activityName
                }
                HoneywellNative::class.java -> {
                    r = (scannerDevice as HoneywellNative).activityName
                }
                Zebra::class.java -> {
                    r = (scannerDevice as Zebra).activityName
                }
            }
        } catch (e: NoClassDefFoundError) {
            //handle carefully
        }
        return r
    }

    fun onResume() {
        if (scannerDevice != null) {
            try {
                when (scannerDevice!!.javaClass) {
                    Honeywell::class.java -> {
                        (scannerDevice as Honeywell).resume()
                    }
                    HoneywellNative::class.java -> {
                        (scannerDevice as HoneywellNative).resume()
                    }
                    Zebra::class.java -> {
                        (scannerDevice as Zebra).resume()
                    }
                }
            } catch (e: NoClassDefFoundError) {
                //handle carefully
            }
        }
    }

    fun onPause() {
        if (scannerDevice != null) {
            try {
                when (scannerDevice!!.javaClass) {
                    Honeywell::class.java -> {
                        (scannerDevice as Honeywell).pause()
                    }
                    HoneywellNative::class.java -> {
                        (scannerDevice as HoneywellNative).pause()
                    }
                    Zebra::class.java -> {
                        (scannerDevice as Zebra).pause()
                    }
                }
            } catch (e: NoClassDefFoundError) {
                //handle carefully
            }
        }
    }

    fun onDestroy() {
        if (scannerDevice != null) {
            try {
                when (scannerDevice!!.javaClass) {
                    Honeywell::class.java -> {
                        //(scannerDevice as Honeywell).destroy()
                    }
                    HoneywellNative::class.java -> {
                        (scannerDevice as HoneywellNative).destroy()
                    }
                    Zebra::class.java -> {
                        //(scannerDevice as Zebra).destroy()
                    }
                }
            } catch (e: NoClassDefFoundError) {
                //handle carefully
            }
        }
        // release resources!
        scannerDevice = null
    }

    fun trigger() {
        if (scannerDevice != null) {
            try {
                when (scannerDevice!!.javaClass) {
                    Honeywell::class.java -> {
                        (scannerDevice as Honeywell).triggerScanner()
                    }
                    HoneywellNative::class.java -> {
                        (scannerDevice as HoneywellNative).triggerScanner()
                    }
                    Zebra::class.java -> {
                        (scannerDevice as Zebra).triggerScanner()
                    }
                }
            } catch (e: NoClassDefFoundError) {
                //handle carefully
            }
        }
    }

    fun lockScanner(lock: Boolean) {
        if (scannerDevice != null) {
            try {
                when (scannerDevice!!.javaClass) {
                    Honeywell::class.java -> {
                        (scannerDevice as Honeywell).lockScannerEvent = lock
                    }
                    HoneywellNative::class.java -> {
                        (scannerDevice as HoneywellNative).lockScannerEvent = lock
                    }
                    Zebra::class.java -> {
                        (scannerDevice as Zebra).lockScannerEvent = lock
                    }
                }
            } catch (e: NoClassDefFoundError) {
                //handle carefully
            }
        }
    }
}