package com.dacosys.warehouseCounter.scanners.devices.zebra

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.scanners.Scanner
import java.util.concurrent.atomic.AtomicBoolean

// ****************************************************************************************
// *                                                                                      *
// *    Clase de control básico para scanners Zebra usando DataWedge                      *
// *                                                                                      *
// ****************************************************************************************
class Zebra(private val activity: AppCompatActivity) : Scanner() {
    // private variables
    private val bRequestSendResult = false

    private var ls = AtomicBoolean(false)
    var lockScannerEvent: Boolean
        get() = ls.get()
        set(value) = ls.set(value)

    private var broadcastReceiver: ZebraBroadcastReceiver? = null

    var activityName: String = ""

    init {
        createProfile()

        activityName = activity.javaClass.simpleName
        broadcastReceiver = ZebraBroadcastReceiver(this)

        initializeScanner()
    }

    fun sendScannedData(code: String) {
        if (lockScannerEvent) return
        (activity as ScannerListener?)?.scannerCompleted(code)
    }

    private fun initializeScanner() {
        // Use SET_CONFIG: http://techdocs.zebra.com/datawedge/latest/guide/api/setconfig/

        // Main bundle properties
        val profileConfig = Bundle()
        profileConfig.putString("PROFILE_NAME", Constants.EXTRA_PROFILENAME)
        profileConfig.putString("PROFILE_ENABLED", "true")
        profileConfig.putString("CONFIG_MODE", "UPDATE") // Update specified settings in profile

        // PLUGIN_CONFIG bundle properties
        val barcodeConfig = Bundle()
        barcodeConfig.putString("PLUGIN_NAME", "BARCODE")
        barcodeConfig.putString("RESET_CONFIG", "true")

        // PARAM_LIST bundle properties
        val barcodeProps = Bundle()
        barcodeProps.putString("scanner_selection", "auto")
        barcodeProps.putString("scanner_input_enabled", "true")

        // Set Symbologies
        barcodeProps.putString("decoder_pdf417", settingsVm.symbologyPDF417.toString())
        barcodeProps.putString("decoder_aztec", settingsVm.symbologyAztec.toString())
        barcodeProps.putString("decoder_qrcode", settingsVm.symbologyQRCode.toString())
        barcodeProps.putString("decoder_codabar", settingsVm.symbologyCODABAR.toString())
        barcodeProps.putString("decoder_code128", settingsVm.symbologyCode128.toString())
        barcodeProps.putString("decoder_code39", settingsVm.symbologyCode39.toString())
        barcodeProps.putString("decoder_code93", settingsVm.symbologyCode93.toString())
        barcodeProps.putString("decoder_datamatrix", settingsVm.symbologyDataMatrix.toString())
        barcodeProps.putString("decoder_ean13", settingsVm.symbologyEAN13.toString())
        barcodeProps.putString("decoder_ean8", settingsVm.symbologyEAN8.toString())
        barcodeProps.putString("decoder_maxicode", settingsVm.symbologyMaxiCode.toString())
        barcodeProps.putString("decoder_upca", settingsVm.symbologyUPCA.toString())
        barcodeProps.putString("decoder_upce0", settingsVm.symbologyUPCE.toString())

        // Bundle "barcodeProps" within bundle "barcodeConfig"
        barcodeConfig.putBundle("PARAM_LIST", barcodeProps)
        // Place "barcodeConfig" bundle within main "profileConfig" bundle
        profileConfig.putBundle("PLUGIN_CONFIG", barcodeConfig)

        // Create APP_LIST bundle to associate app with profile
        val appConfig = Bundle()
        appConfig.putString("PACKAGE_NAME", context.packageName)
        appConfig.putStringArray("ACTIVITY_LIST", arrayOf("*"))
        profileConfig.putParcelableArray("APP_LIST", arrayOf(appConfig))
        sendDataWedgeIntentWithExtra(Constants.EXTRA_SET_CONFIG, profileConfig)

        // Register for status change notification
        // Use REGISTER_FOR_NOTIFICATION: http://techdocs.zebra.com/datawedge/latest/guide/api/registerfornotification/
        val b = Bundle()
        b.putString(Constants.EXTRA_KEY_APPLICATION_NAME, context.packageName)
        b.putString(
            Constants.EXTRA_KEY_NOTIFICATION_TYPE,
            "SCANNER_STATUS"
        ) // register for changes in scanner status
        sendDataWedgeIntentWithExtra(Constants.EXTRA_REGISTER_NOTIFICATION, b)
        registerReceivers()

        // Get DataWedge version
        // Use GET_VERSION_INFO: http://techdocs.zebra.com/datawedge/latest/guide/api/getversioninfo/
        sendDataWedgeIntentWithExtra(
            Constants.EXTRA_GET_VERSION_INFO,
            Constants.EXTRA_EMPTY
        ) // must be called after registering BroadcastReceiver
    }

    // Create profile from UI onClick() event
    private fun createProfile() {
        val profileName = Constants.EXTRA_PROFILENAME

        // Send DataWedge intent with extra to create profile
        // Use CREATE_PROFILE: http://techdocs.zebra.com/datawedge/latest/guide/api/createprofile/
        sendDataWedgeIntentWithExtra(Constants.EXTRA_CREATE_PROFILE, profileName)

        // Configure created profile to apply to this app
        val profileConfig = Bundle()
        profileConfig.putString("PROFILE_NAME", Constants.EXTRA_PROFILENAME)
        profileConfig.putString("PROFILE_ENABLED", "true")
        profileConfig.putString(
            "CONFIG_MODE",
            "CREATE_IF_NOT_EXIST"
        ) // Create profile if it does not exist

        // Configure barcode input plugin
        val barcodeConfig = Bundle()
        barcodeConfig.putString("PLUGIN_NAME", "BARCODE")
        barcodeConfig.putString("RESET_CONFIG", "true") //  This is the default
        val barcodeProps = Bundle()
        barcodeConfig.putBundle("PARAM_LIST", barcodeProps)
        profileConfig.putBundle("PLUGIN_CONFIG", barcodeConfig)

        // Associate profile with this app
        val appConfig = Bundle()
        appConfig.putString("PACKAGE_NAME", context.packageName)
        appConfig.putStringArray("ACTIVITY_LIST", arrayOf("*"))
        profileConfig.putParcelableArray("APP_LIST", arrayOf(appConfig))
        profileConfig.remove("PLUGIN_CONFIG")

        // Apply configs
        // Use SET_CONFIG: http://techdocs.zebra.com/datawedge/latest/guide/api/setconfig/
        sendDataWedgeIntentWithExtra(Constants.EXTRA_SET_CONFIG, profileConfig)

        // Configure intent output for captured data to be sent to this app
        val intentConfig = Bundle()
        intentConfig.putString("PLUGIN_NAME", "INTENT")
        intentConfig.putString("RESET_CONFIG", "true")

        val intentProps = Bundle()
        intentProps.putString("intent_output_enabled", "true")
        intentProps.putString("intent_action", Constants.activityIntentFilterAction)
        intentProps.putString("intent_delivery", "2")
        intentConfig.putBundle("PARAM_LIST", intentProps)
        profileConfig.putBundle("PLUGIN_CONFIG", intentConfig)

        sendDataWedgeIntentWithExtra(Constants.EXTRA_SET_CONFIG, profileConfig)
        Log.v(javaClass.simpleName, "Created profile.  Check DataWedge app UI.")
    }

    // Toggle soft scan trigger from UI onClick() event
    // Use SOFT_SCAN_TRIGGER: http://techdocs.zebra.com/datawedge/latest/guide/api/softscantrigger/
    fun triggerScanner(view: View? = null) {
        sendDataWedgeIntentWithExtra(Constants.EXTRA_SOFT_SCAN_TRIGGER, "TOGGLE_SCANNING")
    }

    private fun unregisterReceivers() {
        Log.v(javaClass.simpleName, "Unregistering receiver (${activityName})")
        try {
            activity.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.message
        }
        unRegisterScannerStatus()
    }

    // Create filter for the broadcast intent
    private fun registerReceivers() {
        Log.v(javaClass.simpleName, "registerReceivers() on $activityName")
        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_RESULT_NOTIFICATION) // for notification result
        filter.addAction(Constants.ACTION_RESULT) // for error code result
        filter.addCategory(Intent.CATEGORY_DEFAULT) // needed to get version info

        // register to received broadcasts via DataWedge scanning
        filter.addAction(Constants.activityIntentFilterAction)
        filter.addAction(Constants.activityActionFromService)

        activity.registerReceiver(broadcastReceiver, filter)
    }

    // Unregister scanner status notification
    private fun unRegisterScannerStatus() {
        Log.v(javaClass.simpleName, "unRegisterScannerStatus() on $activityName")
        val b = Bundle()
        b.putString(Constants.EXTRA_KEY_APPLICATION_NAME, context.packageName)
        b.putString(Constants.EXTRA_KEY_NOTIFICATION_TYPE, Constants.EXTRA_KEY_VALUE_SCANNER_STATUS)
        val i = Intent()
        i.action = ContactsContract.Intents.Insert.ACTION
        i.putExtra(Constants.EXTRA_UNREGISTER_NOTIFICATION, b)

        activity.sendBroadcast(i)
    }

    private fun sendDataWedgeIntentWithExtra(extraKey: String, extras: Bundle) {
        val dwIntent = Intent()
        dwIntent.action = Constants.ACTION_DATAWEDGE
        dwIntent.putExtra(extraKey, extras)
        if (bRequestSendResult) dwIntent.putExtra(Constants.EXTRA_SEND_RESULT, "true")
        activity.sendBroadcast(dwIntent)
    }

    private fun sendDataWedgeIntentWithExtra(extraKey: String, extraValue: String) {
        val dwIntent = Intent()
        dwIntent.action = Constants.ACTION_DATAWEDGE
        dwIntent.putExtra(extraKey, extraValue)
        if (bRequestSendResult) dwIntent.putExtra(Constants.EXTRA_SEND_RESULT, "true")
        activity.sendBroadcast(dwIntent)
    }

    fun resume() {
        registerReceivers()
    }

    fun pause() {
        unregisterReceivers()
    }

    internal object Constants {
        const val activityIntentFilterAction = "com.zebra.datacapture1.ACTION"
        const val activityActionFromService = "com.zebra.datacapture1.service.ACTION"

        //private val datawedgeIntentKeySource = "com.symbol.datawedge.source"
        //private val datawedgeIntentKeyLabelType = "com.symbol.datawedge.label_type"
        const val datawedgeIntentKeyData = "com.symbol.datawedge.data_string"

        // DataWedge Sample supporting DataWedge APIs up to DW 7.0
        const val EXTRA_PROFILENAME = "DWDataCapture1"

        // DataWedge Extras
        const val EXTRA_GET_VERSION_INFO = "com.symbol.datawedge.api.GET_VERSION_INFO"
        const val EXTRA_CREATE_PROFILE = "com.symbol.datawedge.api.CREATE_PROFILE"
        const val EXTRA_KEY_APPLICATION_NAME = "com.symbol.datawedge.api.APPLICATION_NAME"
        const val EXTRA_KEY_NOTIFICATION_TYPE = "com.symbol.datawedge.api.NOTIFICATION_TYPE"
        const val EXTRA_SOFT_SCAN_TRIGGER = "com.symbol.datawedge.api.SOFT_SCAN_TRIGGER"
        const val EXTRA_RESULT_NOTIFICATION = "com.symbol.datawedge.api.NOTIFICATION"
        const val EXTRA_REGISTER_NOTIFICATION = "com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION"
        const val EXTRA_UNREGISTER_NOTIFICATION =
            "com.symbol.datawedge.api.UNREGISTER_FOR_NOTIFICATION"
        const val EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"
        const val EXTRA_RESULT_NOTIFICATION_TYPE = "NOTIFICATION_TYPE"
        const val EXTRA_KEY_VALUE_SCANNER_STATUS = "SCANNER_STATUS"
        const val EXTRA_KEY_VALUE_PROFILE_SWITCH = "PROFILE_SWITCH"
        const val EXTRA_KEY_VALUE_CONFIGURATION_UPDATE = "CONFIGURATION_UPDATE"
        const val EXTRA_KEY_VALUE_NOTIFICATION_STATUS = "STATUS"
        const val EXTRA_KEY_VALUE_NOTIFICATION_PROFILE_NAME = "PROFILE_NAME"
        const val EXTRA_SEND_RESULT = "SEND_RESULT"
        const val EXTRA_EMPTY = ""
        const val EXTRA_RESULT_GET_VERSION_INFO = "com.symbol.datawedge.api.RESULT_GET_VERSION_INFO"
        const val EXTRA_RESULT = "RESULT"
        const val EXTRA_RESULT_INFO = "RESULT_INFO"
        const val EXTRA_COMMAND = "COMMAND"

        // DataWedge Actions
        const val ACTION_DATAWEDGE = "com.symbol.datawedge.api.ACTION"
        const val ACTION_RESULT_NOTIFICATION = "com.symbol.datawedge.api.NOTIFICATION_ACTION"
        const val ACTION_RESULT = "com.symbol.datawedge.api.RESULT_ACTION"
    }
}