package com.example.warehouseCounter.scanners.jotter

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.example.warehouseCounter.scanners.Collector
import com.example.warehouseCounter.scanners.Collector.Companion.collectorType
import com.example.warehouseCounter.scanners.Collector.Companion.collectorTypeChanged
import com.example.warehouseCounter.scanners.Collector.Companion.isNfcRequired
import com.example.warehouseCounter.scanners.Scanner
import com.example.warehouseCounter.scanners.Scanner.ScannerListener
import com.example.warehouseCounter.scanners.collector.CollectorType
import com.example.warehouseCounter.scanners.collector.RfidType
import com.example.warehouseCounter.scanners.devices.floatingCamera.FloatingCameraBarcode
import com.example.warehouseCounter.scanners.devices.nfc.Nfc
import com.example.warehouseCounter.scanners.devices.nfc.Nfc.enableNfcForegroundDispatch
import com.example.warehouseCounter.scanners.devices.rfid.Rfid
import com.example.warehouseCounter.scanners.devices.rfid.Rfid.Companion.appHasBluetoothPermission
import com.example.warehouseCounter.scanners.devices.rfid.Rfid.Companion.isRfidRequired
import com.example.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.example.warehouseCounter.ui.snackBar.SnackBarType
import com.example.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.example.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.google.android.gms.common.api.CommonStatusCodes

/**
 * Esta clase se encarga de escuchar y administrar los eventos del ciclo de vida
 * de todas las actividades y fragmentos de la aplicación.
 *
 * En nuestro caso, la utilizamos para conectar y desconectar los
 * distintos medios de lectura de códigos a las actividades que lo necesitan.
 *
 * Medios administrados:
 * ---------------------
 * NFC
 * RFID
 *   - Vh75 (Bluetooth)
 * Imager
 *   - Honeywell
 *   - Zebra
 * Camara (ventana flotante)
 */

//------------------------------------------------------------------------------//
//                        Android Activity Lifecycle                            //
//                                                                              //
//                              +------------+                                  //
//                              |  Activity  |                                  //
//                              |  launched  |                                  //
//                              +-----+------+                                  //
//                                    |                                         //
//                             +------v-------+                                 //
//              +-------------->  onCreate()  |                                 //
//              |              +------+-------+                                 //
//              |                     |                                         //
//              |              +------v-------+             +---------------+   //
//              |              |  onStart()   <-------------+  onRestart()  |   //
//       User navigate to      +------+-------+             +-------^-------+   //
//         the activity               |                             |           //
//              |              +------v-------+                     |           //
//              |              |  onResume()  <------------+        |           //
//              |              +------+-------+            |        |           //
//              |                     |                    |        |           //
//      +-------+-------+   +---------+----------+         |        |           //
//      |  App process  |   |  Activity running  |  User return to  |           //
//      |     killed    |   +---------+----------+   the activity   |           //
//      +-------^-------+             |                    |        |           //
//              |           Another activity comes         |        |           //
//              |            into the foreground           |        |           //
//              |                     |                    |        |           //
//              |               +-----v-------+            |        |           //
//      Apps with higher -------+  onPause()  +------------+        |           //
//    priority need memory      +-----+-------+                     |           //
//              |                     |                     User navigate to    //
//              |               The activity is               the activity      //
//              |              no longer visible                    |           //
//              |                     |                             |           //
//              |               +-----v-------+                     |           //
//              +---------------+  onStop()   +---------------------+           //
//                              +-----+-------+                                 //
//                                    |                                         //
//                        The activity is finishing or                          //
//                        being destroyed by the system                         //
//                                    |                                         //
//                             +------v--------+                                //
//                             |  onDestroy()  |                                //
//                             +------+--------+                                //
//                                    |                                         //
//                              +-----v-------+                                 //
//                              |  Activity   |                                 //
//                              |  shut down  |                                 //
//                              +-------------+                                 //
//                                                                              //
// Usamos onResume()-onPause() para desconectar los lectores porque             //
// son los eventos en que se muestra y oculta la actividad al usuario.          //
//                                                                              //
// En la ventana flotante usamos onCreate()/onDestroy() ya que para registrar   //
// las actividades hay que hacerlo antes de onStart():                          //
// LifecycleOwners must call register before they are STARTED                   //
//                                                                              //
//------------------------------------------------------------------------------//

object ScannerManager : Jotter.Listener {
    private const val REQUEST_BLUETOOTH_CONNECT = 2001

    /**
     * Colección de ventanas flotantes y escáneres (Cada actividad tiene la suya)
     * Usamos colecciones porque simultáneamente pueden existir más de dos actividades
     * en diferentes momentos de su ciclo de vida.
     */
    private var floatingWindowList: ArrayList<FloatingCameraBarcode> = ArrayList()
    private var scannerList: ArrayList<Scanner> = ArrayList()

    override fun onReceiveActivityEvent(
        activity: Activity,
        event: String,
        bundle: Bundle?,
    ) {
        /**
         * const val CREATE = "CREATE"
         * const val START = "START"
         * const val RESUME = "RESUME"
         * const val PAUSE = "PAUSE"
         * const val STOP = "STOP"
         * const val SAVE_INSTANCE_STATE = "SAVE_INSTANCE_STATE"
         * const val DESTROY = "DESTROY"
         */

        if (activity !is ScannerListener) return
        val tActivity = activity as AppCompatActivity

        Log.v("LifeCycleCallback", "ACTIVITY IS ${tActivity.javaClass.simpleName} >>> $event")

        when (event) {
            "CREATE" -> onCreate(tActivity)
            "RESUME" -> onResume(tActivity)
            "PAUSE" -> onPause(tActivity)
            "DESTROY" -> onDestroy(tActivity)
        }
    }

    /**
     * Función pública llamada desde las actividades cuando reciben
     * la respuesta a la demanda de permisos de conexión Bluetooth
     */
    fun onRequestPermissionsResult(
        activity: AppCompatActivity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        if (!permissions.contains(Manifest.permission.BLUETOOTH_CONNECT)) return

        when (requestCode) {
            REQUEST_BLUETOOTH_CONNECT -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showMessage(
                        activity.window,
                        activity.getString(R.string.app_dont_have_necessary_permissions),
                        ERROR
                    )
                } else {
                    rfidSetListener(activity)
                }
            }
        }
    }

    fun autodetectDeviceModel(activity: AppCompatActivity) {
        var collectorType: CollectorType? = collectorType

        // Solo si no fue configurado o cambió la configuración
        if (collectorType == null || collectorType == CollectorType.none) {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL

            when {
                manufacturer.contains("Honeywell", true) ||
                        manufacturer.startsWith("Universal Global Scientific Industrial") ||
                        manufacturer.startsWith("Foxconn International Holdings Limited") ->
                    collectorType = CollectorType.honeywellNative

                manufacturer.contains("Motorola", true) ||
                        manufacturer.contains("Zebra", true) ||
                        manufacturer.contains("Symbol", true) ->
                    collectorType = CollectorType.zebra
            }

            Log.v(javaClass.simpleName, "Manufacturer: $manufacturer, Model: $model")

            if (collectorType != null) {
                Collector.Companion.collectorType = collectorType
                showMessage(activity.window, "${context.getString(R.string.device)}: $manufacturer $model", INFO)
                collectorTypeChanged = true
            }
        }

        if (collectorTypeChanged) {
            collectorTypeChanged = false

            // Restart Scanners
            destroyScanner(activity)
            createBarcodeReader(activity)
            resumeReaderDevices(activity)
        }
    }

    /**
     * Función CREATE
     */
    private fun onCreate(activity: AppCompatActivity) {
        create(activity)
    }

    /**
     * FloatingCameraBarcode llama a registerForActivityResult para solicitar
     * los permisos que necesita.
     *
     * Sólo se pueden registrar las actividades en onCreate().
     *
     * Si la actividad ya fue creada, y estamos recreándola para reconfigurar el
     * escáner luego de volver de Settings, estamos llegando aquí después de onResume(),
     * por lo tanto tenemos que evitar volver a registrar la actividad porque:
     *
     * java.lang.IllegalStateException:
     * LifecycleOwner com.example.warehousecounter.xxx.xxxActivity@cf72429
     * is attempting to register while current state is STARTED.
     * LifecycleOwners must call register before they are STARTED.
     */

    fun create(activity: AppCompatActivity) {
        if (activity is ScannerListener) {
            // Creamos y agregamos la ventana flotante de la actividad
            createFloatingWindow(activity)

            // Creamos y agregamos el escáner de la actividad
            createBarcodeReader(activity)

            if (isNfcRequired()) Nfc.setupNFCReader(activity)

            if (activity is Rfid.RfidDeviceListener && settingsVm.isRfidRequired) {
                rfidStart(activity)
                rfidSetup(activity)
            }
        }
    }

    fun rfidStart(activity: AppCompatActivity) {
        Rfid.destroy()
        if (!isRfidRequired(activity)) return

        Rfid.build(activity as Rfid.RfidDeviceListener, RfidType.vh75)

        rfidSetup(activity)
    }

    private fun rfidSetup(activity: AppCompatActivity) {
        val bluetoothManager =
            context.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
        val mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter == null) {
            showMessage(activity.window, activity.getString(R.string.there_are_no_bluetooth_devices), INFO)
        } else {
            if (!mBluetoothAdapter.isEnabled) {
                if (!appHasBluetoothPermission()) {
                    activity.requestPermissions(
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        REQUEST_BLUETOOTH_CONNECT
                    )
                    return
                }

                val resultForRfidConnect =
                    activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                        if (it.resultCode == CommonStatusCodes.SUCCESS || it.resultCode == CommonStatusCodes.SUCCESS_CACHE) {
                            rfidSetListener(activity)
                        }
                    }

                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBtIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                resultForRfidConnect.launch(enableBtIntent)
            } else {
                rfidSetListener(activity)
            }
        }
    }

    private fun createBarcodeReader(activity: AppCompatActivity) {
        try {
            scannerList.add(Scanner(activity))
        } catch (ex: Exception) {
            ex.printStackTrace()
            showMessage(activity.window, activity.getString(R.string.barcode_reader_not_initialized), ERROR)
            ErrorLog.writeLog(activity, javaClass.simpleName, ex)
        }
    }

    private fun rfidSetListener(activity: AppCompatActivity) {
        try {
            Rfid.setListener(activity as Rfid.RfidDeviceListener, RfidType.vh75)
        } catch (ex: Exception) {
            ex.printStackTrace()
            if (activity.isFinishing || activity.isDestroyed) return
            showMessage(activity.window, activity.getString(R.string.rfid_reader_not_initialized), ERROR)
        }
    }

    private fun showMessage(window: Window, msg: String, type: SnackBarType) {
        if (type == ERROR) logError(msg)
        makeText(window.decorView, msg, type)
    }

    private fun logError(message: String) = Log.e(this::class.java.simpleName, message)

    /**
     * Función RESUME
     */
    private fun onResume(activity: AppCompatActivity) {
        resumeReaderDevices(activity)
    }

    fun resumeReaderDevices(activity: AppCompatActivity) {
        if (isNfcRequired()) enableNfcForegroundDispatch(activity)

        if (activity is Rfid.RfidDeviceListener && settingsVm.isRfidRequired) Rfid.resume(activity)

        scannerList.firstOrNull { it.activityName() == activity.javaClass.simpleName }?.onResume()
        floatingWindowList.firstOrNull { it.activityName == activity.javaClass.simpleName }
            ?.onResume()
    }

    fun trigger(activity: AppCompatActivity) {
        scannerList.firstOrNull { it.activityName() == activity.javaClass.simpleName }?.trigger()
    }

    fun lockScanner(activity: AppCompatActivity, lock: Boolean) {
        scannerList.firstOrNull { it.activityName() == activity.javaClass.simpleName }
            ?.lockScanner(lock)
    }

    fun hideWindow(activity: AppCompatActivity) {
        floatingWindowList.firstOrNull { it.activityName == activity.javaClass.simpleName }
            ?.hideWindow()
    }

    fun toggleCameraFloatingWindowVisibility(activity: AppCompatActivity) {
        floatingWindowList.firstOrNull { it.activityName == activity.javaClass.simpleName }
            ?.toggleWindowVisibility()
    }

    private fun createFloatingWindow(activity: AppCompatActivity) {
        // Crear ventana y agregar a la colección
        floatingWindowList.add(FloatingCameraBarcode(activity))
    }

    /**
     * Función PAUSE
     */
    private fun onPause(activity: AppCompatActivity) {
        pauseReaderDevices(activity)
    }

    fun pauseReaderDevices(activity: AppCompatActivity) {
        if (activity is Rfid.RfidDeviceListener && settingsVm.isRfidRequired) Rfid.pause()

        if (isNfcRequired()) Nfc.disableNfcForegroundDispatch(activity)

        scannerList.firstOrNull { it.activityName() == activity.javaClass.simpleName }?.onPause()
        floatingWindowList.firstOrNull { it.activityName == activity.javaClass.simpleName }
            ?.onPause()
    }

    /**
     * Función DESTROY
     */
    private fun onDestroy(activity: AppCompatActivity) {
        activity.window?.decorView?.clearFocus()

        destroyScanner(activity)
        destroyFloatingWindow(activity)
    }

    private fun destroyScanner(activity: AppCompatActivity) {
        var idx = -1
        for ((index, f) in scannerList.withIndex()) {
            if (f.activityName() == activity.javaClass.simpleName) {
                f.onDestroy()
                idx = index
                break
            }
        }
        if (idx >= 0) scannerList.removeAt(idx)
    }

    private fun destroyFloatingWindow(activity: AppCompatActivity) {
        var idx = -1
        for ((index, f) in floatingWindowList.withIndex()) {
            if (f.activityName == activity.javaClass.simpleName) {
                f.onDestroy()
                idx = index
                break
            }
        }
        if (idx >= 0) floatingWindowList.removeAt(idx)
    }

    /**
     * No estamos usando Scanner en Fragmentos
     */
    override fun onReceiveFragmentEvent(
        fragment: Fragment,
        context: Context?,
        event: String,
        bundle: Bundle?,
    ) {
        if (fragment !is ScannerListener) return
        Log.v("LifeCycleCallback", "FRAGMENT IS $fragment >>> $event")
    }
}