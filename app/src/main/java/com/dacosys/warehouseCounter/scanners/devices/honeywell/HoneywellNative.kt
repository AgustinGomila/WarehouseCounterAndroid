package com.dacosys.warehouseCounter.scanners.devices.honeywell

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.scanners.Scanner
import com.honeywell.aidc.*
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by krrigan on 10/13/18.
 */
class HoneywellNative(private var weakRef: WeakReference<AppCompatActivity>) : Scanner(),
    BarcodeReader.BarcodeListener, BarcodeReader.TriggerListener {
    private var initialized = AtomicBoolean(false)
    private var initializing = AtomicBoolean(false)
    private var pendingResume = AtomicBoolean(false)

    private var autoMode = true

    private var ls = AtomicBoolean(false)
    var lockScannerEvent: Boolean
        get() = ls.get()
        set(value) = ls.set(value)

    var activityName: String = ""

    @Transient
    private var scannerManager: AidcManager? = null

    @Transient
    private var scanner: BarcodeReader? = null

    @Transient
    private var scannerListener: ScannerListener? = null

    @Transient
    private var properties: HashMap<String, Any> = HashMap()

    private fun init() {
        activityName = weakRef.get()?.javaClass?.simpleName.orEmpty()
        scannerListener = weakRef.get() as ScannerListener

        Log.v(javaClass.simpleName, "Initializing scanner on $activityName...")

        initializing.set(true)

        // Get bar code instance from Activity
        // create the AidcManager providing a Context and a
        // CreatedCallback implementation.
        AidcManager.create(weakRef.get()) { manager ->
            Log.v(javaClass.simpleName, "Manager created on $activityName...")
            scannerManager = manager
            scanner = scannerManager?.createBarcodeReader()

            /*
            If the barcode read event is not fired, ensure the scanner has been claimed.
            Although the scanner.claim() call is placed inside the Override for onResume(),
            it may no be called during the startup of the application.
            Put some extra scanner.claim() call in the onCreate() override function of the
            application to ensure the scanner is also claimed during the start of the application.
             */
            try {
                scanner?.claim()
            } catch (e: ScannerUnavailableException) {
                e.printStackTrace()
                Log.e(javaClass.simpleName, "Scanner unavailable")
            }

            loadProperties()
            setupScanner()

            initialized.set(true)
            initializing.set(false)
            if (pendingResume.get()) resumeScanner()
        }
    }

    private fun setupScanner() {
        // register bar code event listener
        scanner?.addBarcodeListener(this)

        // register trigger state change listener
        // When using Automatic Trigger control do not need to implement the onTriggerEvent
        if (!autoMode) scanner?.addTriggerListener(this)

        // set the trigger mode to auto control
        try {
            if (autoMode) {
                scanner?.setProperty(
                    BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                    BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL
                )
            } else {
                // set the trigger mode to client control
                scanner?.setProperty(
                    BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                    BarcodeReader.TRIGGER_CONTROL_MODE_CLIENT_CONTROL
                )
            }
            scanner?.setProperty(BarcodeReader.PROPERTY_DATA_PROCESSOR_LAUNCH_BROWSER, false)
            scanner?.setProperty(
                BarcodeReader.PROPERTY_TRIGGER_SCAN_MODE,
                BarcodeReader.TRIGGER_SCAN_MODE_ONESHOT
            )
        } catch (e: UnsupportedPropertyException) {
            e.printStackTrace()
        }

        scanner?.setProperties(properties)
    }

    override fun onBarcodeEvent(barcodeReadEvent: BarcodeReadEvent) {
        if (lockScannerEvent) return

        val codeRead = barcodeReadEvent.barcodeData
        if (codeRead != null) {
            scannerListener?.scannerCompleted(codeRead)
        }
    }

    override fun onFailureEvent(barcodeFailureEvent: BarcodeFailureEvent) {
        if (lockScannerEvent) return
        Log.v(javaClass.simpleName, context.getString(R.string.barcode_failure))
    }

    // When using Automatic Trigger control do not need to implement the
    // onTriggerEvent function
    override fun onTriggerEvent(triggerStateChangeEvent: TriggerStateChangeEvent) {
        if (lockScannerEvent) return

        try {
            // only handle trigger presses
            // turn on/off aimer, illumination and decoding
            scanner?.aim(triggerStateChangeEvent.state)
            scanner?.light(triggerStateChangeEvent.state)
            scanner?.decode(triggerStateChangeEvent.state)
        } catch (e: ScannerNotClaimedException) {
            e.printStackTrace()
            Log.e(javaClass.simpleName, "Scanner is not claimed")
        } catch (e: ScannerUnavailableException) {
            e.printStackTrace()
            Log.e(javaClass.simpleName, "Scanner unavailable")
        }
    }

    fun triggerScanner() {
        if (lockScannerEvent) return

        // Reconfigurar el escaner para que acepte el disparo manual
        autoMode = false
        removeListeners()
        setupScanner()
        autoMode = true

        // Iniciar el disparo, con una duración de 3 seg.
        startScanner()

        //software defined decode timeout!
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            stopScanner()

            // Volver el escaner a su estado normal
            removeListeners()
            setupScanner()
        }, 3000)
    }

    private fun startScanner() {
        scanner?.softwareTrigger(true)
    }

    private fun stopScanner() {
        scanner?.softwareTrigger(false)
    }

    private fun loadProperties() {
        properties = HashMap()

        properties[BarcodeReader.PROPERTY_PDF_417_ENABLED] = settingsVm.symbologyPDF417
        properties[BarcodeReader.PROPERTY_AZTEC_ENABLED] = settingsVm.symbologyAztec
        properties[BarcodeReader.PROPERTY_QR_CODE_ENABLED] = settingsVm.symbologyQRCode
        properties[BarcodeReader.PROPERTY_CODABAR_ENABLED] = settingsVm.symbologyCODABAR
        properties[BarcodeReader.PROPERTY_CODE_128_ENABLED] = settingsVm.symbologyCode128
        properties[BarcodeReader.PROPERTY_CODE_39_ENABLED] = settingsVm.symbologyCode39
        properties[BarcodeReader.PROPERTY_CODE_93_ENABLED] = settingsVm.symbologyCode93
        properties[BarcodeReader.PROPERTY_DATAMATRIX_ENABLED] = settingsVm.symbologyDataMatrix
        properties[BarcodeReader.PROPERTY_EAN_13_ENABLED] = settingsVm.symbologyEAN13
        properties[BarcodeReader.PROPERTY_EAN_8_ENABLED] = settingsVm.symbologyEAN8
        properties[BarcodeReader.PROPERTY_MAXICODE_ENABLED] = settingsVm.symbologyMaxiCode
        properties[BarcodeReader.PROPERTY_RSS_ENABLED] = settingsVm.symbologyRSS14
        properties[BarcodeReader.PROPERTY_RSS_EXPANDED_ENABLED] = settingsVm.symbologyRSSExpanded
        properties[BarcodeReader.PROPERTY_UPC_A_ENABLE] = settingsVm.symbologyUPCA
        properties[BarcodeReader.PROPERTY_UPC_E_ENABLED] = settingsVm.symbologyUPCE

        val sendDigit = settingsVm.sendBarcodeCheckDigit
        properties[BarcodeReader.PROPERTY_EAN_13_CHECK_DIGIT_TRANSMIT_ENABLED] = sendDigit
        properties[BarcodeReader.PROPERTY_EAN_8_CHECK_DIGIT_TRANSMIT_ENABLED] = sendDigit
        properties[BarcodeReader.PROPERTY_UPC_A_CHECK_DIGIT_TRANSMIT_ENABLED] = sendDigit

        properties[BarcodeReader.PROPERTY_CODE_39_CHECK_DIGIT_MODE] =
            if (sendDigit) BarcodeReader.CODE_39_CHECK_DIGIT_MODE_CHECK
            else BarcodeReader.CODE_39_CHECK_DIGIT_MODE_NO_CHECK

        // Set Max Code 39 barcode length
        properties[BarcodeReader.PROPERTY_CODE_39_MAXIMUM_LENGTH] = 10
        // Turn on center decoding
        properties[BarcodeReader.PROPERTY_CENTER_DECODE] = true
        // Enable bad read response
        properties[BarcodeReader.PROPERTY_NOTIFICATION_BAD_READ_ENABLED] = true
    }

    @Suppress("unused")
    fun setProperties(mapProperties: Map<String, Any>?) {
        if (mapProperties == null) return

        loadProperties()
        properties.putAll(mapProperties)

        if (scanner != null) scanner?.setProperties(properties)
    }

    fun resume(): Boolean {
        resumeScanner()
        return true
    }

    private fun resumeScanner() {
        /*
        If the barcode read event is not fired, ensure the scanner has been claimed.
        Although the scanner.claim() call is placed inside the Override for onResume(),
        it may no be called during the startup of the application.
        Put some extra scanner.claim() call in the onCreate() override function of the
        application to ensure the scanner is also claimed during the start of the application.
        */

        if (scanner == null) {
            pendingResume.set(true)
            if (!initialized.get() && !initializing.get()) init()
            return
        }

        Log.v(javaClass.simpleName, "Resuming scanner on $activityName...")

        try {
            scanner?.claim()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause(): Boolean {
        pauseScanner()
        return true
    }

    private fun pauseScanner() {
        Log.v(javaClass.simpleName, "Pausing scanner on $activityName...")
        // release the scanner claim so we don't get any scanner notifications while paused
        // and the scanner properties are restored to default.
        scanner?.release()
        pendingResume.set(false)
    }

    fun destroy() {
        Log.v(javaClass.simpleName, "Destroying scanner on $activityName...")
        try {
            removeListeners()

            // close BarcodeReader to clean up resources.
            scanner?.close()
            scanner = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            // close AidcManager to disconnect from the scanner service.
            // once closed, the object can no longer be used.
            scannerManager?.close()
            scannerManager = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // release listener
        scannerListener = null
        weakRef.clear()

        properties.clear()
        initialized.set(false)
    }

    private fun removeListeners() {
        // unregister barcode event listener
        scanner?.removeBarcodeListener(this)

        // unregister trigger state change listener
        // When using Automatic Trigger control do not need to implement the onTriggerEvent
        // function
        scanner?.removeTriggerListener(this)
    }

    init {
        init()
    }
}