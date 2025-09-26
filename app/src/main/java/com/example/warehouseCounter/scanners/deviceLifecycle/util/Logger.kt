package com.example.warehouseCounter.scanners.deviceLifecycle.util

import android.util.Log

internal object Logger {

    internal var logEnabled: Boolean = true
    internal var tag = "DeviceLifecycle"

    internal fun debug(message: String) {
        if (logEnabled) {
            Log.d(tag, ">> $message")
        }
    }

    internal fun error(message: String, throwable: Throwable) {
        if (logEnabled) {
            Log.e(tag, ">> $message", throwable)
        }
    }
}