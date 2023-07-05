package com.dacosys.warehouseCounter.scanners

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.collectorType.CollectorType
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.scanners.Scanner.ScannerListener
import com.dacosys.warehouseCounter.scanners.floatingCamera.FloatingCameraBarcode
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.nfc.Nfc.enableNfcForegroundDispatch
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.scanners.rfid.RfidType
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.google.android.gms.common.api.CommonStatusCodes
import id.pahlevikun.jotter.Jotter

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

object JotterListener : Jotter.Listener {
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

        Log.v("LifeCycleCallback", "ACTIVITY IS ${tActivity::class.java.simpleName} >>> $event")

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
                    makeText(
                        activity.window.decorView,
                        activity.getString(R.string.app_dont_have_necessary_permissions),
                        SnackBarType.ERROR
                    )
                } else {
                    rfidSetListener(activity)
                }
            }
        }
    }

    fun autodetectDeviceModel(activity: AppCompatActivity) {
        val idStr = settingViewModel.collectorType
        var collectorType: CollectorType = CollectorType.none
        if (idStr.isDigitsOnly()) collectorType = CollectorType.getById(idStr.toInt())

        // Solo si no fue configurado o cambió la configuración
        if (collectorType == CollectorType.none) {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL

            when {
                manufacturer.contains("Honeywell", true)
                        || manufacturer.startsWith("Universal Global Scientific Industrial")
                        || manufacturer.startsWith("Foxconn International Holdings Limited") -> {
                    collectorType = CollectorType.honeywellNative
                }

                manufacturer.contains("Motorola", true)
                        || manufacturer.contains("Zebra", true)
                        || manufacturer.contains("Symbol", true) -> {
                    collectorType = CollectorType.zebra
                }
                /*
                manufacturer.contains("Janam", true)-> when (model) {
                    "XG3" -> collectorType = CollectorType.janamXG3
                    "XM5" -> collectorType = CollectorType.janamXM5
                    "XT2" -> collectorType = CollectorType.janamXT2
                }
                */
            }

            Log.v(this::class.java.simpleName, "Manufacturer: $manufacturer, Model: $model")

            settingViewModel.collectorType = collectorType.id.toString()
            makeText(
                activity.window.decorView,
                "${context.getString(R.string.device)}: $manufacturer $model",
                SnackBarType.INFO
            )
            Statics.collectorTypeChanged = true
        }

        if (Statics.collectorTypeChanged) {
            Statics.collectorTypeChanged = false

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
     * LifecycleOwner com.dacosys.warehouseCounter.xxx.xxxActivity@cf72429
     * is attempting to register while current state is STARTED.
     * LifecycleOwners must call register before they are STARTED.
     */

    fun create(activity: AppCompatActivity) {
        if (activity is ScannerListener) {
            // Creamos y agregamos la ventana flotante de la actividad
            createFloatingWindow(activity)

            // Creamos y agregamos el escáner de la actividad
            createBarcodeReader(activity)

            if (settingViewModel.useNfc) Nfc.setupNFCReader(activity)

            if (activity is Rfid.RfidDeviceListener && settingViewModel.useBtRfid) {
                rfidStart(activity)
                rfidSetup(activity)
            }
        }
    }

    fun rfidStart(activity: AppCompatActivity) {
        Rfid.destroy()
        Rfid.build(null, activity, RfidType.vh75)
    }

    private fun rfidSetup(activity: AppCompatActivity) {
        val bluetoothManager =
            context.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
        val mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter == null) {
            makeText(
                activity.window.decorView,
                activity.getString(R.string.there_are_no_bluetooth_devices),
                SnackBarType.INFO
            )
        } else {
            if (!mBluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBtIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        activity.requestPermissions(
                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                            REQUEST_BLUETOOTH_CONNECT
                        )
                    }
                    return
                }
                val resultForRfidConnect =
                    activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                        if (it?.resultCode == CommonStatusCodes.SUCCESS || it?.resultCode == CommonStatusCodes.SUCCESS_CACHE) {
                            rfidSetListener(activity)
                        }
                    }

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
            makeText(
                activity.window.decorView,
                activity.getString(R.string.barcode_reader_not_initialized),
                SnackBarType.ERROR
            )
            ErrorLog.writeLog(activity, this::class.java.simpleName, ex)
        }
    }

    private fun rfidSetListener(activity: AppCompatActivity) {
        try {
            Rfid.setListener(activity as Rfid.RfidDeviceListener, RfidType.vh75)
        } catch (ex: Exception) {
            ex.printStackTrace()
            makeText(
                activity.window.decorView,
                activity.getString(R.string.rfid_reader_not_initialized),
                SnackBarType.ERROR
            )
            ErrorLog.writeLog(activity, this::class.java.simpleName, ex)
        }
    }

    /**
     * Función RESUME
     */
    private fun onResume(activity: AppCompatActivity) {
        resumeReaderDevices(activity)
    }

    fun resumeReaderDevices(activity: AppCompatActivity) {
        if (settingViewModel.useNfc) enableNfcForegroundDispatch(activity)

        if (activity is Rfid.RfidDeviceListener && settingViewModel.useBtRfid) Rfid.resume(
            activity
        )

        scannerList.firstOrNull { it.activityName() == activity::class.java.simpleName }?.onResume()
        floatingWindowList.firstOrNull { it.activityName == activity::class.java.simpleName }
            ?.onResume()
    }

    fun trigger(activity: AppCompatActivity) {
        scannerList.firstOrNull { it.activityName() == activity::class.java.simpleName }?.trigger()
    }

    fun lockScanner(activity: AppCompatActivity, lock: Boolean) {
        scannerList.firstOrNull { it.activityName() == activity::class.java.simpleName }
            ?.lockScanner(lock)
    }

    fun hideWindow(activity: AppCompatActivity) {
        floatingWindowList.firstOrNull { it.activityName == activity.javaClass.simpleName }
            ?.hideWindow()
    }

    fun toggleCameraFloatingWindowVisibility(activity: AppCompatActivity) {
        floatingWindowList.firstOrNull { it.activityName == activity::class.java.simpleName }
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
        if (activity is Rfid.RfidDeviceListener && settingViewModel.useBtRfid) Rfid.pause()

        if (settingViewModel.useNfc) Nfc.disableNfcForegroundDispatch(activity)

        scannerList.firstOrNull { it.activityName() == activity::class.java.simpleName }?.onPause()
        floatingWindowList.firstOrNull { it.activityName == activity::class.java.simpleName }
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
            if (f.activityName() == activity::class.java.simpleName) {
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
            if (f.activityName == activity::class.java.simpleName) {
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