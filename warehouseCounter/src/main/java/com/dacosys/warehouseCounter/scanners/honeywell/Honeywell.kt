package com.dacosys.warehouseCounter.scanners.honeywell

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.honeywell.Honeywell.Constants.PROPERTY_DATA_PROCESSOR_DATA_INTENT
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by luis901101 on 05/30/19.
 */
class Honeywell(private val activity: AppCompatActivity) : Scanner() {
    private var ls = AtomicBoolean(false)
    var lockScannerEvent: Boolean
        get() {
            return ls.get()
        }
        set(value) {
            ls.set(value)
        }

    var activityName: String = ""

    internal object Constants {
        /**
         * Get the result of decoded data
         */
        const val ACTION_BARCODE_DATA = "com.dacosys.warehouseCounter.intent.action.BARCODE"

        /**
         * Honeywell DataCollection Intent API
         * Claim scanner
         * Permissions:
         * "com.honeywell.decode.permission.DECODE"
         */
        const val ACTION_CLAIM_SCANNER = "com.honeywell.aidc.action.ACTION_CLAIM_SCANNER"

        /**
         * Honeywell DataCollection Intent API
         * Release scanner claim
         * Permissions:
         * "com.honeywell.decode.permission.DECODE"
         */
        const val ACTION_RELEASE_SCANNER = "com.honeywell.aidc.action.ACTION_RELEASE_SCANNER"

        /**
         * Honeywell DataCollection Intent API
         * Optional. Sets the scanner to claim. If scanner is not available or if extra is not used,
         * DataCollection will choose an available scanner. * Values : String
         * "dcs.scanner.imager" : Uses the internal scanner
         * "dcs.scanner.ring" : Uses the external ring scanner
         */
        const val EXTRA_SCANNER = "com.honeywell.aidc.extra.EXTRA_SCANNER"
        const val EXTRA_SCANNER_VALUE_IMAGER = "dcs.scanner.imager"

        /**
         * Honeywell DataCollection Intent API
         * Optional. Sets the profile to use. If profile is not available or if extra is not used, * the scanner will use factory default properties (not "DEFAULT" profile properties).
         * Values : String
         */
        const val EXTRA_PROFILE = "com.honeywell.aidc.extra.EXTRA_PROFILE"

        /**
         * Este es el nombre creado por defecto en los Honeywell
         * Settings / Scan Settings / Internal Scanner / + / Select an application / WarehouseCounter
         */
        const val appProfile = "WarehouseCounter"

        /**
         * Categoría por defecto. En otros ejemplos se envía vacía.
         */
        const val defaultCategory = "android.intent.category.DEFAULT"

        const val EXTRA_CONTROL = "com.honeywell.aidc.action.ACTION_CONTROL_SCANNER"

        /**
         * Set to true to start or continue scanning.
         * Set to false to stop scanning. Most scenarios only need this extra, however the scanner can be
         * put into other states by adding from the following extras.
         */
        const val EXTRA_SCAN = "com.honeywell.aidc.extra.EXTRA_SCAN"

        /**
         * Specify whether to turn the scanner aimer
         * on or off. This is optional; the default value is the value of EXTRA_SCAN.
         */
        const val EXTRA_AIM = "com.honeywell.aidc.extra.EXTRA_AIM"

        /**
         * Specify whether to turn the scanner
         * illumination on or off. This is optional; the default value is the value of EXTRA_SCAN.
         */
        const val EXTRA_LIGHT = "com.honeywell.aidc.extra.EXTRA_LIGHT"

        /**
         * Specify whether to turn the decoding
         * operation on or off. This is optional; the default value is the value of EXTRA_SCAN
         */
        const val EXTRA_DECODE = "com.honeywell.aidc.extra.EXTRA_DECODE"

        /**
         * Honeywell DataCollection Intent API
         * Optional. Overrides the profile properties (non-persistent) until the next scanner claim. * Values : Bundle
         */
        const val EXTRA_PROPERTIES = "com.honeywell.aidc.extra.EXTRA_PROPERTIES"
        const val SCANNER_PACKAGE = "com.intermec.datacollectionservice"

        const val PROPERTY_DATA_PROCESSOR_DATA_INTENT = "DPR_DATA_INTENT"
        const val PROPERTY_DATA_PROCESSOR_DATA_INTENT_ACTION = "DPR_DATA_INTENT_ACTION"
        const val PROPERTY_DATA_PROCESSOR_LAUNCH_BROWSER = "DPR_LAUNCH_BROWSER"

        const val PROPERTY_AZTEC_ENABLED = "DEC_AZTEC_ENABLED"
        const val PROPERTY_CODABAR_ENABLED = "DEC_CODABAR_ENABLED"
        const val PROPERTY_CODE_39_ENABLED = "DEC_CODE39_ENABLED"
        const val PROPERTY_CODE_93_ENABLED = "DEC_CODE93_ENABLED"
        const val PROPERTY_CODE_128_ENABLED = "DEC_CODE128_ENABLED"
        const val PROPERTY_DATAMATRIX_ENABLED = "DEC_DATAMATRIX_ENABLED"
        const val PROPERTY_EAN_8_ENABLED = "DEC_EAN8_ENABLED"
        const val PROPERTY_EAN_13_ENABLED = "DEC_EAN13_ENABLED"
        const val PROPERTY_MAXICODE_ENABLED = "DEC_MAXICODE_ENABLED"
        const val PROPERTY_PDF_417_ENABLED = "DEC_PDF417_ENABLED"
        const val PROPERTY_QR_CODE_ENABLED = "DEC_QR_ENABLED"
        const val PROPERTY_RSS_ENABLED = "DEC_RSS_14_ENABLED"
        const val PROPERTY_RSS_EXPANDED_ENABLED = "DEC_RSS_EXPANDED_ENABLED"
        const val PROPERTY_UPC_A_ENABLE = "DEC_UPCA_ENABLE"
        const val PROPERTY_UPC_E_ENABLED = "DEC_UPCE0_ENABLED"

        const val PROPERTY_INTERLEAVED_25_ENABLED = "DEC_I25_ENABLED"
        const val PROPERTY_GS1_128_ENABLED = "DEC_GS1_128_ENABLED"
        const val PROPERTY_EAN_13_CHECK_DIGIT_TRANSMIT_ENABLED = "DEC_EAN13_CHECK_DIGIT_TRANSMIT"
        const val PROPERTY_EAN_8_CHECK_DIGIT_TRANSMIT_ENABLED = "DEC_EAN8_CHECK_DIGIT_TRANSMIT"
        const val PROPERTY_UPC_A_CHECK_DIGIT_TRANSMIT_ENABLED = "DEC_UPCA_CHECK_DIGIT_TRANSMIT"
        const val PROPERTY_CODE_39_CHECK_DIGIT_MODE = "DEC_CODE39_CHECK_DIGIT_MODE"
        const val CODE_39_CHECK_DIGIT_MODE_CHECK = "check"
        const val CODE_39_CHECK_DIGIT_MODE_NO_CHECK = "noCheck"
        const val PROPERTY_CODE_39_MAXIMUM_LENGTH = "DEC_CODE39_MAX_LENGTH"
        const val PROPERTY_CENTER_DECODE = "DEC_WINDOW_MODE"
        const val PROPERTY_NOTIFICATION_BAD_READ_ENABLED = "NTF_BAD_READ_ENABLED"

        const val PROPERTY_TRIGGER_CONTROL_MODE = "TRIG_CONTROL_MODE"
        const val PROPERTY_TRIGGER_AUTO_MODE_TIMEOUT = "TRIG_AUTO_MODE_TIMEOUT"
        const val TRIGGER_CONTROL_MODE_DISABLE = "disable"
        const val TRIGGER_CONTROL_MODE_AUTO_CONTROL = "autoControl"
        const val TRIGGER_CONTROL_MODE_CLIENT_CONTROL = "clientControl"
        const val PROPERTY_TRIGGER_ENABLE = "TRIG_ENABLE"
        const val PROPERTY_TRIGGER_SCAN_DELAY = "TRIG_SCAN_DELAY"
        const val PROPERTY_TRIGGER_SCAN_MODE = "TRIG_SCAN_MODE"
        const val TRIGGER_SCAN_MODE_ONESHOT = "oneShot"
        const val TRIGGER_SCAN_MODE_CONTINUOUS = "continuous"
        const val TRIGGER_SCAN_MODE_READ_ON_RELEASE = "readOnRelease"
        const val TRIGGER_SCAN_MODE_READ_ON_SECOND_TRIGGER_PRESS = "readOnSecondTriggerPress"
    }

    private var broadcastReceiver: HoneywellBroadcastReceiver? = null
    private var intentFilter: IntentFilter? = null
    private var properties: Bundle? = null
    private var isOpened = false

    private fun init() {
        activityName = activity::class.java.simpleName
        Log.v(this::class.java.simpleName, "Initializing scanner on ${activityName}...")

        broadcastReceiver = HoneywellBroadcastReceiver(this)
        intentFilter = IntentFilter(Constants.ACTION_BARCODE_DATA)

        loadProperties()
    }

    private fun loadProperties() {
        val sv = settingViewModel()
        properties = Bundle()

        // Barcode camera scanner view
        properties?.putBoolean(Constants.PROPERTY_PDF_417_ENABLED, sv.symbologyPDF417)
        properties?.putBoolean(Constants.PROPERTY_AZTEC_ENABLED, sv.symbologyAztec)
        properties?.putBoolean(Constants.PROPERTY_QR_CODE_ENABLED, sv.symbologyQRCode)
        properties?.putBoolean(Constants.PROPERTY_CODABAR_ENABLED, sv.symbologyCODABAR)
        properties?.putBoolean(Constants.PROPERTY_CODE_128_ENABLED, sv.symbologyCode128)
        properties?.putBoolean(Constants.PROPERTY_CODE_39_ENABLED, sv.symbologyCode39)
        properties?.putBoolean(Constants.PROPERTY_CODE_93_ENABLED, sv.symbologyCode93)
        properties?.putBoolean(Constants.PROPERTY_DATAMATRIX_ENABLED, sv.symbologyDataMatrix)
        properties?.putBoolean(Constants.PROPERTY_EAN_13_ENABLED, sv.symbologyEAN13)
        properties?.putBoolean(Constants.PROPERTY_EAN_8_ENABLED, sv.symbologyEAN8)
        properties?.putBoolean(Constants.PROPERTY_MAXICODE_ENABLED, sv.symbologyMaxiCode)
        properties?.putBoolean(Constants.PROPERTY_RSS_ENABLED, sv.symbologyRSS14)
        properties?.putBoolean(Constants.PROPERTY_RSS_EXPANDED_ENABLED, sv.symbologyRSSExpanded)
        properties?.putBoolean(Constants.PROPERTY_UPC_A_ENABLE, sv.symbologyUPCA)
        properties?.putBoolean(Constants.PROPERTY_UPC_E_ENABLED, sv.symbologyUPCE)

        val sendDigit = sv.sendBarcodeCheckDigit

        properties?.putBoolean(Constants.PROPERTY_EAN_13_CHECK_DIGIT_TRANSMIT_ENABLED, sendDigit)
        properties?.putBoolean(Constants.PROPERTY_EAN_8_CHECK_DIGIT_TRANSMIT_ENABLED, sendDigit)
        properties?.putBoolean(Constants.PROPERTY_UPC_A_CHECK_DIGIT_TRANSMIT_ENABLED, sendDigit)

        properties?.putString(
            Constants.PROPERTY_CODE_39_CHECK_DIGIT_MODE,
            if (sendDigit) Constants.CODE_39_CHECK_DIGIT_MODE_CHECK
            else Constants.CODE_39_CHECK_DIGIT_MODE_NO_CHECK
        )

        // Set Max Code 39 barcode length
        properties?.putInt(Constants.PROPERTY_CODE_39_MAXIMUM_LENGTH, 10)
        // Turn on center decoding
        properties?.putBoolean(Constants.PROPERTY_CENTER_DECODE, true)
        // Enable bad read response
        properties?.putBoolean(Constants.PROPERTY_NOTIFICATION_BAD_READ_ENABLED, true)

        Log.v(this::class.java.simpleName, "Scanner minimal properties defined!")
    }

    fun setProperties(mapProperties: Map<String, Any>?) {
        if (mapProperties == null || mapProperties.isEmpty()) return
        loadProperties()
        for ((key, value1) in mapProperties) {
            if (value1 is String) properties!!.putString(key, value1.toString())
            if (value1 is Boolean) properties!!.putBoolean(
                key,
                java.lang.Boolean.valueOf(value1.toString())
            )
            if (value1 is Int) properties!!.putInt(key, Integer.valueOf(value1.toString()))
            if (value1 is Long) properties!!.putLong(key, java.lang.Long.valueOf(value1.toString()))
        }
    }

    fun sendScannedData(code: String) {
        if (lockScannerEvent) return
        (activity as ScannerListener?)?.scannerCompleted(code)
    }

    fun triggerScanner() {
        if (lockScannerEvent) return

        startScanner()

        //software defined decode timeout!
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ stopScanner() }, 3000)
    }

    private fun startScanner() {
        val intent = Intent(Constants.EXTRA_CONTROL).putExtra(Constants.EXTRA_SCAN, true)
        sendBroadcast(intent)
    }

    private fun stopScanner() {
        val intent = Intent(Constants.EXTRA_CONTROL).putExtra(Constants.EXTRA_SCAN, false)
        sendBroadcast(intent)
    }

    fun resume(): Boolean {
        resumeScanner()
        return true
    }

    fun pause(): Boolean {
        pauseScanner()
        return true
    }

    private fun resumeScanner(): Boolean {
        registerReceiver()
        try {
            if (properties == null) properties = Bundle()

            //When we press the scan button and read a barcode, a new Broadcast intent will be launched by the service
            properties?.putBoolean(PROPERTY_DATA_PROCESSOR_DATA_INTENT, true)

            //That intent will have the action "ACTION_BARCODE_DATA"
            // We will capture the intents with that action (every scan event while in the application)
            // in our BroadcastReceiver barcodeDataReceiver.
            properties?.putString(
                Constants.PROPERTY_DATA_PROCESSOR_DATA_INTENT_ACTION,
                Constants.ACTION_BARCODE_DATA
            )

            val intent = Intent(Constants.ACTION_CLAIM_SCANNER).putExtra(
                Constants.EXTRA_SCANNER,
                Constants.EXTRA_SCANNER_VALUE_IMAGER
            )
                .putExtra(Constants.EXTRA_PROPERTIES, properties)
                /*
                We are using "WarehouseCounter", so a profile with this name has to be created in Scanner settings:
                       Android Settings > Honeywell Settings > Scanning > Internal scanner > "+"
                - If we use "DEFAULT" it will apply the settings from the Default profile in Scanner settings
                - If not found, it will use Factory default settings.
                 */.putExtra(Constants.EXTRA_PROFILE, Constants.appProfile)

            sendBroadcast(intent)
            isOpened = true
        } catch (e: Exception) {
            e.message
        }

        return true
    }

    private fun sendBroadcast(intent: Intent) {
        val sdkVersion = android.os.Build.VERSION.SDK_INT
        intent
            /*
             * We use setPackage() in order to send an Explicit Broadcast Intent, since it is a requirement
             * after API Level 26+ (Android 8)
             */.setPackage(Constants.SCANNER_PACKAGE)

            /*
             * Always provide a non-empty category, for example "android.intent.category.DEFAULT"
             */.addCategory(Constants.defaultCategory)

        if (sdkVersion < 26) {
            Log.v(this::class.java.simpleName, "Send $intent (${activityName})")
            activity.sendBroadcast(intent)
        } else {
            //for Android O above "gives W/BroadcastQueue: Background execution not allowed: receiving Intent"
            //either set targetSDKversion to 25 or use implicit broadcast
            sendImplicitBroadcast(intent)
        }
    }

    private fun sendImplicitBroadcast(i: Intent) {
        val appContext = context()
        val pm: PackageManager = appContext.packageManager
        val matches = pm.queryBroadcastReceivers(i, 0)
        if (matches.size > 0) {
            for (resolveInfo in matches) {
                val explicit = Intent(i)
                val cn = ComponentName(
                    resolveInfo.activityInfo.applicationInfo.packageName,
                    resolveInfo.activityInfo.name
                )
                explicit.component = cn
                Log.v(this::class.java.simpleName, "Send $explicit (${activityName})")
                appContext.sendBroadcast(explicit)
            }
        } else {
            // to be compatible with Android 9 and later version for dynamic receiver
            Log.v(this::class.java.simpleName, "Send $i (${activityName})")
            appContext.sendBroadcast(i)
        }
    }

    private fun pauseScanner(): Boolean {
        unregisterReceiver()
        try {
            val intent = Intent(Constants.ACTION_RELEASE_SCANNER)
            sendBroadcast(intent)
            isOpened = false
        } catch (e: Exception) {
            e.message
        }

        return true
    }

    private fun registerReceiver() {
        Log.v(this::class.java.simpleName, "Registering receiver (${activityName})")
        try {
            activity.registerReceiver(broadcastReceiver, intentFilter)
        } catch (e: Exception) {
            e.message
        }
    }

    private fun unregisterReceiver() {
        Log.v(this::class.java.simpleName, "Unregistering receiver (${activityName})")
        try {
            activity.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.message
        }
    }

    init {
        init()
    }
}