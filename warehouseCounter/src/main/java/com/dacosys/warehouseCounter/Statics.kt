package com.dacosys.warehouseCounter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Insets
import android.graphics.Typeface
import android.graphics.drawable.InsetDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.dacosys.warehouseCounter.Statics.WarehouseCounter.Companion.getContext
import com.dacosys.warehouseCounter.collectorType.`object`.CollectorType
import com.dacosys.warehouseCounter.configuration.QRConfigType
import com.dacosys.warehouseCounter.configuration.QRConfigType.CREATOR.QRConfigApp
import com.dacosys.warehouseCounter.configuration.QRConfigType.CREATOR.QRConfigClientAccount
import com.dacosys.warehouseCounter.configuration.QRConfigType.CREATOR.QRConfigImageControl
import com.dacosys.warehouseCounter.configuration.QRConfigType.CREATOR.QRConfigWebservice
import com.dacosys.warehouseCounter.errorLog.ErrorLog
import com.dacosys.warehouseCounter.itemCode.`object`.ItemCode
import com.dacosys.warehouseCounter.itemCode.dbHelper.ItemCodeDbHelper
import com.dacosys.warehouseCounter.misc.Preference
import com.dacosys.warehouseCounter.misc.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.misc.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.scanners.vh75.Vh75Bt
import com.dacosys.warehouseCounter.sync.GetClientPackages
import com.dacosys.warehouseCounter.sync.ProgressStatus
import com.dacosys.warehouseCounter.user.`object`.User
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanOptions
import id.pahlevikun.jotter.Jotter
import id.pahlevikun.jotter.event.ActivityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.json.JSONObject
import java.io.*
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.net.NetworkInterface
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Created by Agustin on 24/01/2017.
 */

class Statics {
    class WarehouseCounter : Application() {
        override fun onCreate() {
            super.onCreate()
            sApplication = this

            // Setup ImageControl context
            com.dacosys.imageControl.Statics.ImageControl().setAppContext(this)

            // Eventos del ciclo de vida de las actividades
            // que nos interesa interceptar para conectar y
            // desconectar los medios de lectura de códigos.
            Jotter
                .Builder(this)
                .setLogEnable(true)
                .setActivityEventFilter(
                    listOf(
                        ActivityEvent.CREATE,
                        ActivityEvent.RESUME,
                        ActivityEvent.PAUSE,
                        ActivityEvent.DESTROY
                    )
                )
                //.setFragmentEventFilter(listOf(FragmentEvent.VIEW_CREATE, FragmentEvent.PAUSE))
                .setJotterListener(JotterListener)
                .build()
                .startListening()
        }

        companion object {
            fun getContext(): Context {
                return getApplication()!!.applicationContext
            }

            private fun getApplication(): Application? {
                return sApplication
            }

            private var sApplication: Application? = null
        }
    }

    companion object : DialogInterface.OnMultiChoiceClickListener {
        const val testMode = false

        var appName: String = "${getApplicationName()}M12"

        // Este flag es para reinicializar el colector después de cambiar en Settings.
        var collectorTypeChanged = false

        // region Operaciones de escritura en almacenamiento de Ordenes

        fun writeJsonToFile(v: View, filename: String, value: String, completed: Boolean): Boolean {
            if (!isExternalStorageWritable) {
                val res = getContext()
                    .getString(R.string.error_external_storage_not_available_for_reading_or_writing)
                Log.e(this::class.java.simpleName, res)
                makeText(v, res, ERROR)
                return false
            }

            var error = false

            val path = if (completed) getCompletedPath() else getPendingPath()
            if (writeToFile(fileName = filename, data = value, directory = path)) {
                if (completed) {
                    // Elimino la orden original
                    val file = File(getPendingPath(), filename)
                    if (file.exists()) file.delete()
                }
            } else {
                val res = getContext()
                    .getString(R.string.an_error_occurred_while_trying_to_save_the_count)
                Log.e(this::class.java.simpleName, res)
                makeText(v, res, ERROR)
                error = true
            }

            return !error
        }

        fun writeToFile(fileName: String, data: String, directory: File): Boolean {
            try {
                val file = File(directory, fileName)

                // Save your stream, don't forget to flush() it before closing it.
                file.parentFile?.mkdirs()
                file.createNewFile()

                val fOut = FileOutputStream(file)
                val outWriter = OutputStreamWriter(fOut)
                outWriter.append(data)

                outWriter.close()

                fOut.flush()
                fOut.close()

                return true
            } catch (e: IOException) {
                Log.e(this::class.java.simpleName, "File write failed: $e")
                return false
            }
        }

        // endregion

        fun closeImageControl() {
            com.dacosys.imageControl.Statics.cleanInstance()
        }

        fun setupImageControl() {
            // Setup ImageControl
            com.dacosys.imageControl.Statics.appAllowScreenRotation =
                prefsGetBoolean(Preference.allowScreenRotation)

            com.dacosys.imageControl.Statics.currentUserId = currentUserId
            com.dacosys.imageControl.Statics.currentUserName = currentUserName
            com.dacosys.imageControl.Statics.newInstance()

            com.dacosys.imageControl.Statics.useImageControl = useImageControl
            com.dacosys.imageControl.Statics.wsIcUrl = wsIcUrl
            com.dacosys.imageControl.Statics.wsIcNamespace = wsIcNamespace
            com.dacosys.imageControl.Statics.wsIcProxy = wsIcProxy
            com.dacosys.imageControl.Statics.wsIcProxyPort = wsIcProxyPort
            com.dacosys.imageControl.Statics.wsIcUseProxy = wsIcUseProxy
            com.dacosys.imageControl.Statics.wsIcProxyUser = wsIcProxyUser
            com.dacosys.imageControl.Statics.wsIcProxyPass = wsIcProxyPass
            com.dacosys.imageControl.Statics.icUser = icUser
            com.dacosys.imageControl.Statics.icPass = icPass
            com.dacosys.imageControl.Statics.wsIcUser = wsIcUser
            com.dacosys.imageControl.Statics.wsIcPass = wsIcPass
            com.dacosys.imageControl.Statics.maxHeightOrWidth = maxHeightOrWidth
        }

        fun getScanOptions(): ScanOptions {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            options.setPrompt(getContext().getString(R.string.place_the_code_in_the_rectangle_of_the_viewer_to_capture_it))
            options.setBeepEnabled(true)
            options.setBarcodeImageEnabled(true)
            options.setOrientationLocked(false)
            //options.setTimeout(8000)

            return options
        }

        var downloadDbRequired = false

        val lineSeparator: String = System.getProperty("line.separator") ?: "\\r\\n"

        /**
         * Modo DEMO
         */
        var demoMode = false
        var superDemoMode = false // BuildConfig.DEBUG
        var demoQrConfigCode = """
{"config":{"client_email":"super-clin.com","client_password":"123456"}}
                    """.trimIndent()

        /**
         * Base de datos SQLite local
         */
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "wc.sqlite"
        const val APP_VERSION_ID: Int = 8

        private const val IMAGE_CONTROL_PATH = "/image_control"
        private const val IMAGE_CONTROL_DATABASE_NAME = "imagecontrol.sqlite"
        const val APP_VERSION_ID_IMAGECONTROL = 7
        private const val INTERNAL_IMAGE_CONTROL_APP_ID: Int = 1

        private const val WC_ROOT_PATH = "/warehouse_counter"
        private const val PENDING_COUNT_PATH = "/pending_counts"
        private const val COMPLETED_COUNT_PATH = "/completed_counts"
        private const val ERROR_LOG_PATH = "/error_log"

        fun getPendingPath(): File {
            return File("${getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}$WC_ROOT_PATH/$installationCode$PENDING_COUNT_PATH/")
        }

        fun getCompletedPath(): File {
            return File("${getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}$WC_ROOT_PATH/$installationCode$COMPLETED_COUNT_PATH/")
        }

        fun getLogPath(): File {
            return File("${getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}$WC_ROOT_PATH/$installationCode$ERROR_LOG_PATH/")
        }

        /**
         * Estados de las tareas asincrónicas
         */

        var STARTING = 0
        var FINISHED = 1
        var CANCELED = 2
        var RUNNING = 3
        var CRASHED = 4

        fun isOnline(): Boolean {
            val connectivityManager =
                getContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                        return true
                    }
                }
            }
            return false
        }

        fun removeDataBases() {
            removeImageControlDataBase()
            removeLocalDataBase()
        }

        private fun removeImageControlDataBase() {
            // Path to the just created empty db
            val outFileName =
                getContext().getDatabasePath(IMAGE_CONTROL_DATABASE_NAME)
                    .toString()

            try {
                Log.i("IC DataBase", "Eliminando: $outFileName")
                val f = File(outFileName)
                if (f.exists()) {
                    f.delete()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                ErrorLog.writeLog(null, "removeICDataBase", e)
            }
        }

        private fun removeLocalDataBase() {
            // Path to the just created empty db
            val outFileName =
                getContext().getDatabasePath(DATABASE_NAME).toString()

            try {
                Log.i("Local DataBase", "Eliminando: $outFileName")
                val f = File(outFileName)
                if (f.exists()) {
                    f.delete()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                ErrorLog.writeLog(null, "removeLocalDataBase", e)
            }
        }

        fun initRequired(): Boolean {
            return if (isRfidRequired()) {
                if (Rfid.rfidDevice == null) {
                    true
                } else {
                    if ((Rfid.rfidDevice is Vh75Bt)) {
                        (Rfid.rfidDevice as Vh75Bt).mState == Vh75Bt.STATE_NONE
                    } else false
                }
            } else false
        }

        fun isRfidRequired(): Boolean {
            if (!prefsGetBoolean(Preference.useBtRfid)) {
                return false
            }

            val btAddress = prefsGetString(Preference.rfidBtAddress)
            return btAddress.isNotEmpty()
        }

        fun isNfcRequired(): Boolean {
            return prefsGetBoolean(Preference.useNfc)
        }

        fun allowUnknownCodes(): Boolean {
            return prefsGetBoolean(Preference.allowUnknownCodes)
        }

        // region Variables con valores predefinidos para el selector de cantidades
        var decimalSeparator: Char = '.'
        var decimalPlaces: Int = 0
        // endregion

        // region Colección temporal de ItemCode
        // Reinsertar cuando se haya descargado la base de datos
        private var tempItemCodes: ArrayList<ItemCode> = ArrayList()

        fun insertItemCodes() {
            if (tempItemCodes.isNotEmpty()) {
                val icDbHelper = ItemCodeDbHelper()

                for (f in tempItemCodes) {
                    val codeCount = icDbHelper.selectByCode(f.code).count()
                    if (codeCount == 0) {
                        f.toUpload = false
                        icDbHelper.insert(f)
                    }
                }

                tempItemCodes.clear()
            }
        }
        // endregion

        fun generateTaskCode(): Int {
            val min = 10000
            val max = 99999
            return Random().nextInt(max - min + 1) + min
        }

        /* Checks if external storage is available to at least read */
        val isExternalStorageReadable: Boolean
            get() {
                val state = Environment.getExternalStorageState()
                return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
            }

        // region CURRENT USER
        var currentUserId: Long = -1L
        var currentUserName: String = ""

        fun getCurrentUser(): User? {
            return User.selectById(currentUserId)
        }
        // endregion CURRENT USER

        fun divisorChar(): String {
            return prefsGetString(Preference.divisionChar)
        }

        // region COLLECTOR TYPE THINGS

        val collectorType: CollectorType
            get() {
                return CollectorType.getById(prefsGetInt(Preference.collectorType))
            }

        // endregion COLLECTOR TYPE THINGS

        // region BLUETOOTH PRINTER THINGS
        var printerBluetoothDevice: BluetoothDevice? = null
            get() {
                if (field == null) {
                    refreshBluetoothPrinter()
                }
                return field
            }

        private fun refreshBluetoothPrinter() {
            if (prefsGetBoolean(Preference.useBtPrinter)) {
                val printerMacAddress = prefsGetString(Preference.printerBtAddress)
                if (printerMacAddress.isEmpty()) {
                    return
                }

                val bluetoothManager = getContext()
                    .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val mBluetoothAdapter = bluetoothManager.adapter

                if (ActivityCompat.checkSelfPermission(
                        getContext(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }

                val mPairedDevices = mBluetoothAdapter!!.bondedDevices

                if (mPairedDevices.size > 0) {
                    for (mDevice in mPairedDevices) {
                        if (mDevice.address == printerMacAddress) {
                            printerBluetoothDevice = mDevice
                            return
                        }
                    }
                }
            }
        }
        // endregion BLUETOOTH PRINTER THINGS

        // region SOME CONFIG VALUES AND PREFERENCES FUNCTIONS
        fun getConfigFromScannedCode(
            callback: GetClientPackages.TaskGetPackagesEnded,
            scanCode: String,
            mode: QRConfigType,
        ) {
            if (prefs == null) {
                callback.onTaskGetPackagesEnded(
                    status = ProgressStatus.crashed,
                    result = ArrayList(),
                    clientEmail = "",
                    clientPassword = "",
                    msg = getContext()
                        .getString(R.string.configuration_not_loaded)
                )
                return
            }

            val mainJson = JSONObject(scanCode)
            val mainTag =
                when {
                    mainJson.has("config") && mode == QRConfigClientAccount -> "config"
                    mainJson.has(appName) && mode != QRConfigClientAccount -> appName
                    else -> ""
                }

            if (mainTag.isEmpty()) {
                callback.onTaskGetPackagesEnded(
                    status = ProgressStatus.crashed,
                    result = ArrayList(),
                    clientEmail = "",
                    clientPassword = "",
                    msg = getContext()
                        .getString(R.string.invalid_code)
                )
                return
            }

            val confJson = mainJson.getJSONObject(mainTag)

            when (mode) {
                QRConfigClientAccount -> {
                    // Package Client Setup
                    val installationCode =
                        if (confJson.has(Preference.installationCode.key)) confJson.getString(
                            Preference.installationCode.key
                        ) else ""
                    val email =
                        if (confJson.has(Preference.clientEmail.key)) confJson.getString(Preference.clientEmail.key) else ""
                    val password =
                        if (confJson.has(Preference.clientPassword.key)) confJson.getString(
                            Preference.clientPassword.key
                        ) else ""

                    if (email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                        getConfig(
                            callback = callback,
                            email = email,
                            password = password,
                            installationCode = installationCode
                        )
                    } else {
                        callback.onTaskGetPackagesEnded(
                            status = ProgressStatus.crashed,
                            result = ArrayList(),
                            clientEmail = email,
                            clientPassword = password,
                            msg = getContext().getString(R.string.invalid_code)
                        )
                    }
                }
                QRConfigWebservice, QRConfigApp, QRConfigImageControl -> {
                    tryToLoadConfig(confJson)
                    callback.onTaskGetPackagesEnded(
                        status = ProgressStatus.success,
                        result = ArrayList(),
                        clientEmail = "",
                        clientPassword = "",
                        msg =
                        when (mode) {
                            QRConfigImageControl -> getContext().getString(R.string.imagecontrol_configured)
                            QRConfigWebservice -> getContext().getString(R.string.server_configured)
                            else -> getContext().getString(R.string.configuration_applied)
                        }
                    )
                }
                else -> {
                    callback.onTaskGetPackagesEnded(
                        status = ProgressStatus.crashed,
                        result = ArrayList(),
                        clientEmail = "",
                        clientPassword = "",
                        msg = getContext().getString(R.string.invalid_code)
                    )
                }
            }
        }

        fun generateQrCode(weakAct: WeakReference<FragmentActivity>, data: String) {
            val activity = weakAct.get() ?: return
            if (activity.isFinishing) return

            val writer = QRCodeWriter()
            try {
                var w: Int = getScreenWidth(activity)
                val h: Int = getScreenHeight(activity)
                if (h < w) {
                    w = h
                }

                // CREAR LA IMAGEN
                val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, w, w)
                val width = bitMatrix.width
                val height = bitMatrix.height

                val pixels = IntArray(width * height)
                for (y in 0 until height) {
                    val offset = y * width
                    for (x in 0 until width) {
                        val color: Int = if (bitMatrix.get(x, y)) {
                            Color.BLACK
                        } else {
                            Color.WHITE
                        }

                        pixels[offset + x] = color
                    }
                }

                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bmp.setPixels(pixels, 0, width, 0, 0, width, height)

                val imageView = ImageView(activity)
                imageView.setImageBitmap(bmp)
                val builder = AlertDialog.Builder(activity)
                    .setTitle(R.string.configuration_qr_code)
                    .setMessage(R.string.scan_the_code_below_with_another_device_to_copy_the_configuration)
                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .setView(imageView)

                builder.create().show()
            } catch (e: WriterException) {
                e.printStackTrace()
            }
        }

        fun getBarcodeForConfig(ps: ArrayList<Preference>, mainTag: String): String {
            val jsonObject = JSONObject()

            for (p in ps) {
                if (p.defaultValue is Int) {
                    val value = prefsGetInt(p)
                    if (value != p.defaultValue) {
                        jsonObject.put(p.key, value)
                    }
                } else if (p.defaultValue is Boolean) {
                    val value = prefsGetBoolean(p)
                    if (value != p.defaultValue) {
                        jsonObject.put(p.key, value)
                    }
                } else if (p.defaultValue is String) {
                    val value = prefsGetString(p)
                    if (value != p.defaultValue && value.isNotEmpty()) {
                        jsonObject.put(p.key, value)
                    }
                } else if (p.defaultValue is Long) {
                    val value = prefsGetLong(p)
                    if (value != p.defaultValue) {
                        jsonObject.put(p.key, value)
                    }
                } else if (p.defaultValue is Float) {
                    val value = prefsGetFloat(p)
                    if (value != p.defaultValue) {
                        jsonObject.put(p.key, value)
                    }
                }
            }

            val jsonRes = JSONObject()
            jsonRes.put(mainTag, jsonObject)

            Log.d(this::class.java.simpleName, jsonRes.toString())
            return jsonRes.toString()
        }

        private fun getApplicationName(): String {
            val applicationInfo = getContext().applicationInfo
            return when (val stringId = applicationInfo.labelRes) {
                0 -> applicationInfo.nonLocalizedLabel.toString()
                else -> getContext().getString(stringId)
            }
        }

        private fun tryToLoadConfig(conf: JSONObject) {
            val availablePref = Preference.getConfigPreferences()
            for (prefName in conf.keys()) {

                // No está permitido cargar configuraciones de cliente por esta vía.
                if (!availablePref.any { it.key == prefName }) {
                    continue
                }

                val p = prefs!!.edit()
                val tempPref = prefs!!.all[prefName]
                if (tempPref != null) {
                    try {
                        when (tempPref) {
                            is String -> p.putString(prefName, conf.getString(prefName))
                            is Boolean -> p.putBoolean(prefName, conf.getBoolean(prefName))
                            is Int -> p.putInt(prefName, conf.getInt(prefName))
                            is Float -> p.putFloat(prefName, conf.getDouble(prefName).toFloat())
                            is Long -> p.putLong(prefName, conf.getLong(prefName))
                            else -> p.putString(prefName, conf.getString(prefName))
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        Log.e(
                            this::class.java.simpleName,
                            "Imposible convertir valor de configuración: $prefName"
                        )
                        ErrorLog.writeLog(null, "tryToLoadConfig", ex)
                    }
                } else {
                    val pref = Preference.getByKey(prefName)
                    if (pref != null) {
                        try {
                            when (pref.defaultValue) {
                                is String -> p.putString(prefName, conf.getString(prefName))
                                is Boolean -> p.putBoolean(prefName, conf.getBoolean(prefName))
                                is Int -> p.putInt(prefName, conf.getInt(prefName))
                                is Float -> p.putFloat(prefName, conf.getDouble(prefName).toFloat())
                                is Long -> p.putLong(prefName, conf.getLong(prefName))
                                else -> p.putString(prefName, conf.getString(prefName))
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            Log.e(
                                this::class.java.simpleName,
                                "Imposible convertir valor de configuración: $prefName"
                            )
                            ErrorLog.writeLog(null, "tryToLoadConfig", ex)
                        }
                    }
                }
                p.apply()
            }
        }

        fun setDebugConfigValues() {
            // VALORES POR DEFECTO, SÓLO PARA DEBUG
            if (prefs != null) {
                if (isDebuggable() || BuildConfig.DEBUG) {
                    val x = prefs!!.edit()

                    // region WAREHOUSE COUNTER FTP
                    if (urlPanel.isEmpty()) {
                        x.putString(
                            Preference.urlPanel.key,
                            Preference.urlPanel.debugValue as String?
                        )
                    }
                    // endregion

                    // region IMAGE CONTROL WEBSERVICE
                    if (wsIcUrl.isEmpty()) {
                        x.putString(
                            Preference.icWsServer.key,
                            Preference.icWsServer.debugValue as String?
                        )
                    }

                    if (wsIcNamespace.isEmpty()) {
                        x.putString(
                            Preference.icWsNamespace.key,
                            Preference.icWsNamespace.debugValue as String?
                        )
                    }

                    if (wsIcUser.isEmpty()) {
                        x.putString(
                            Preference.icWsUser.key,
                            Preference.icWsUser.debugValue as String?
                        )
                    }

                    if (wsIcPass.isEmpty()) {
                        x.putString(
                            Preference.icWsPass.key,
                            Preference.icWsPass.debugValue as String?
                        )
                    }

                    if (prefsGetString(Preference.icUser).isEmpty()
                    ) {
                        x.putString(
                            Preference.icUser.key,
                            Preference.icUser.debugValue as String?
                        )
                    }

                    if (prefsGetString(Preference.icPass).isEmpty()
                    ) {
                        x.putString(Preference.icPass.key, Preference.icPass.debugValue as String?)
                    }
                    // endregion

                    run {
                        x.apply()
                    }
                }
            }
        }

        // endregion SOME CONFIG VALUES AND PREFERENCES FUNCTIONS

        var installationCode: String = ""
            get() {
                return field.ifEmpty {
                    return prefsGetString(Preference.installationCode)
                }
            }

        var urlPanel: String = "" //"http://client.dacosys.com:80/7RDRHHAH/panel"
            get() {
                return field.ifEmpty {
                    return prefsGetString(Preference.urlPanel)
                }
            }

        private const val api: String = "api"

        val apiUrl = "$urlPanel/$api"

        val proxy: String
            get() {
                return prefsGetString(Preference.proxy)
            }

        val proxyPort: Int
            get() {
                return prefsGetInt(Preference.proxyPort)
            }

        val useProxy: Boolean
            get() {
                return prefsGetBoolean(Preference.useProxy)
            }

        val proxyUser: String
            get() {
                return prefsGetString(Preference.proxyUser)
            }

        val proxyPass: String
            get() {
                return prefsGetString(Preference.proxyPass)
            }

        // region PROXY THINGS
        private var avoidSetupProxyDialog = false

        interface TaskSetupProxyEnded {
            fun onTaskSetupProxyEnded(
                status: ProgressStatus,
                email: String,
                password: String,
                installationCode: String,
            )
        }

        fun setupProxy(
            callback: TaskSetupProxyEnded,
            weakAct: WeakReference<FragmentActivity>,
            email: String,
            password: String,
            installationCode: String = "",
        ) {
            val activity = weakAct.get() ?: return
            if (activity.isFinishing) return

            if (avoidSetupProxyDialog) {
                return
            }

            avoidSetupProxyDialog = true

            val alert: AlertDialog.Builder = AlertDialog.Builder(activity)
            alert.setTitle(
                getContext().getString(R.string.configure_proxy_question)
            )

            val proxyEditText = EditText(activity)
            proxyEditText.hint = getContext().getString(R.string.proxy)
            proxyEditText.isFocusable = true
            proxyEditText.isFocusableInTouchMode = true

            val proxyPortEditText = EditText(activity)
            proxyPortEditText.inputType = InputType.TYPE_CLASS_NUMBER
            proxyPortEditText.hint = getContext().getString(R.string.port)
            proxyPortEditText.isFocusable = true
            proxyPortEditText.isFocusableInTouchMode = true

            val proxyUserEditText = EditText(activity)
            proxyUserEditText.inputType = InputType.TYPE_CLASS_TEXT
            proxyUserEditText.hint = getContext().getString(R.string.user)
            proxyUserEditText.isFocusable = true
            proxyUserEditText.isFocusableInTouchMode = true

            val proxyPassEditText = TextInputEditText(activity)
            proxyPassEditText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            proxyPassEditText.hint = getContext().getString(R.string.password)
            proxyPassEditText.isFocusable = true
            proxyPassEditText.isFocusableInTouchMode = true
            proxyPassEditText.typeface = Typeface.DEFAULT
            proxyPassEditText.transformationMethod = PasswordTransformationMethod()

            val inputLayout = TextInputLayout(getContext())
            inputLayout.endIconMode = END_ICON_PASSWORD_TOGGLE
            inputLayout.addView(proxyPassEditText)

            val layout = LinearLayout(getContext())
            layout.orientation = LinearLayout.VERTICAL

            layout.addView(proxyEditText)
            layout.addView(proxyPortEditText)
            layout.addView(proxyUserEditText)
            layout.addView(inputLayout)

            alert.setView(layout)
            alert.setNegativeButton(R.string.no) { _, _ ->
                if (prefs == null) {
                    return@setNegativeButton
                }
                val p = (prefs ?: return@setNegativeButton).edit()
                p.putBoolean(Preference.useProxy.key, false)
                p.apply()
            }
            alert.setPositiveButton(R.string.yes) { _, _ ->
                if (prefs == null) {
                    return@setPositiveButton
                }
                val proxy = proxyEditText.text
                val port = proxyPortEditText.text
                val user = proxyUserEditText.text
                val pass = proxyPassEditText.text

                val p = (prefs ?: return@setPositiveButton).edit()
                if (proxy != null) {
                    p.putBoolean(Preference.useProxy.key, true)
                    p.putString(Preference.proxy.key, proxy.toString())
                }

                if (port != null) {
                    p.putInt(Preference.proxyPort.key, Integer.parseInt(port.toString()))
                }

                if (user.isNotEmpty()) {
                    p.putString(Preference.proxyUser.key, user.toString())
                }

                if (pass != null && pass.isNotEmpty()) {
                    p.putString(Preference.proxyPass.key, pass.toString())
                }

                p.apply()
            }
            alert.setOnDismissListener {
                callback.onTaskSetupProxyEnded(
                    status = ProgressStatus.finished,
                    email = email,
                    password = password,
                    installationCode = installationCode
                )
                avoidSetupProxyDialog = false
            }

            val dialog = alert.create()
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

            dialog.show()
            proxyEditText.requestFocus()
        }
        // endregion PROXY THINGS

        // region Selección automática de paquetes del cliente
        private var allProductsArray: ArrayList<JSONObject> = ArrayList()
        private var validProductsArray: ArrayList<JSONObject> = ArrayList()
        private var selected: BooleanArray? = null

        override fun onClick(dialog: DialogInterface?, which: Int, isChecked: Boolean) {
            if (isChecked) {
                val tempProdVersionId = validProductsArray[which].getString("product_version_id")

                for (i in 0 until validProductsArray.size) {
                    if ((selected ?: return)[i]) {
                        val prodVerId = validProductsArray[i].getString("product_version_id")
                        if (prodVerId == tempProdVersionId) {
                            (selected ?: return)[i] = false
                            (dialog as AlertDialog).listView.setItemChecked(i, false)
                        }
                    }
                }
            }

            (selected ?: return)[which] = isChecked
        }

        private var ValidProducts = getValidProducts()
        private fun getValidProducts(): ArrayList<String> {
            val r: ArrayList<String> = ArrayList()
            r.add(APP_VERSION_ID.toString())
            r.add(APP_VERSION_ID_IMAGECONTROL.toString())
            return r
        }

        fun selectClientPackage(
            parentView: View,
            callback: TaskConfigPanelEnded,
            weakAct: WeakReference<FragmentActivity>,
            allPackage: ArrayList<JSONObject>,
            email: String,
            password: String,
        ) {
            val activity = weakAct.get() ?: return
            if (activity.isFinishing) return

            allProductsArray.clear()
            for (pack in allPackage) {
                val pvId = pack.getString("product_version_id")
                if (ValidProducts.contains(pvId) && !allProductsArray.contains(pack)) {
                    allProductsArray.add(pack)
                }
            }

            if (!allProductsArray.any()) {
                makeText(
                    parentView,
                    getContext()
                        .getString(R.string.there_are_no_valid_products_for_the_selected_client),
                    ERROR
                )
                return
            }

            if (allProductsArray.size == 1) {
                val productVersionId = allProductsArray[0].getString("product_version_id")
                if (
                    productVersionId == APP_VERSION_ID.toString() ||
                    productVersionId == APP_VERSION_ID_IMAGECONTROL.toString()
                ) {
                    setConfigPanel(
                        parentView = parentView,
                        callback = callback,
                        packArray = arrayListOf(allProductsArray[0]),
                        email = email,
                        password = password
                    )
                    return
                } else {
                    makeText(
                        parentView,
                        getContext()
                            .getString(R.string.there_are_no_valid_products_for_the_selected_client),
                        ERROR
                    )
                    return
                }
            }

            var validProducts = false
            validProductsArray.clear()
            val client = allProductsArray[0].getString("client")
            val listItems: ArrayList<String> = ArrayList()

            for (pack in allProductsArray) {
                val productVersionId = pack.getString("product_version_id")

                // WarehouseCounter M12 o ImageControl M11
                if (
                    productVersionId == APP_VERSION_ID.toString() ||
                    productVersionId == APP_VERSION_ID_IMAGECONTROL.toString()
                ) {
                    validProducts = true
                    val clientPackage = pack.getString("client_package_content_description")

                    listItems.add(clientPackage)
                    validProductsArray.add(pack)
                }
            }

            if (!validProducts) {
                makeText(
                    parentView,
                    getContext()
                        .getString(R.string.there_are_no_valid_products_for_the_selected_client),
                    ERROR
                )
                return
            }

            selected = BooleanArray(validProductsArray.size)

            val cw = ContextThemeWrapper(activity, R.style.AlertDialogTheme)
            val builder = AlertDialog.Builder(cw)

            val title = TextView(activity)
            title.text =
                String.format(
                    "%s - %s",
                    client,
                    getContext().getString(R.string.select_package)
                )
            title.textSize = 16F
            title.gravity = Gravity.CENTER_HORIZONTAL
            builder.setCustomTitle(title)

            builder.setMultiChoiceItems(
                listItems.toTypedArray(),
                selected,
                this
            )

            builder.setPositiveButton(R.string.accept) { dialog, _ ->
                val selectedPacks: ArrayList<JSONObject> = ArrayList()
                for ((i, prod) in validProductsArray.withIndex()) {
                    if ((selected ?: return@setPositiveButton)[i]) {
                        selectedPacks.add(prod)
                    }
                }

                if (selectedPacks.size > 0) {
                    setConfigPanel(
                        parentView = parentView,
                        callback = callback,
                        packArray = selectedPacks,
                        email = email,
                        password = password
                    )
                }
                dialog.dismiss()
            }

            val layoutDefault = ResourcesCompat.getDrawable(
                getContext().resources,
                R.drawable.layout_thin_border,
                null
            )
            val inset = InsetDrawable(layoutDefault, 20)

            val dialog = builder.create()
            dialog.window?.setBackgroundDrawable(inset)
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            dialog.show()
        }

        interface TaskConfigPanelEnded {
            fun onTaskConfigPanelEnded(status: ProgressStatus)
        }

        private fun setConfigPanel(
            parentView: View,
            callback: TaskConfigPanelEnded,
            packArray: ArrayList<JSONObject>,
            email: String,
            password: String,
        ) {
            for (pack in packArray) {
                val active = pack.getInt("active")
                if (active == 0) {
                    makeText(
                        parentView,
                        getContext().getString(R.string.inactive_installation),
                        ERROR
                    )
                    continue
                }

                // PANEL DE CONFIGURACIÓN
                val productId = pack.getString("product_version_id")
                val panelJsonObj = pack.getJSONObject("panel")
                val appUrl =
                    when {
                        panelJsonObj.has("url")
                        -> panelJsonObj.getString("url") ?: ""
                        else -> ""
                    }

                if (appUrl.isEmpty()) {
                    makeText(
                        parentView,
                        getContext().getString(R.string.app_panel_url_can_not_be_obtained),
                        ERROR
                    )
                    return
                }

                val clientPackage =
                    when {
                        pack.has("client_package_content_description")
                        -> pack.getString("client_package_content_description") ?: ""
                        else -> ""
                    }

                val installationCode =
                    when {
                        pack.has("installation_code")
                        -> pack.getString("installation_code") ?: ""
                        else -> ""
                    }

                var url: String
                var namespace: String
                var user: String
                var pass: String
                var icUser: String
                var icPass: String

                val wsJsonObj = pack.getJSONObject("ws")
                url = if (wsJsonObj.has("url")) wsJsonObj.getString("url") else ""
                namespace = if (wsJsonObj.has("namespace")) wsJsonObj.getString("namespace") else ""
                user = if (wsJsonObj.has("ws_user")) wsJsonObj.getString("ws_user") else ""
                pass = if (wsJsonObj.has("ws_password")) wsJsonObj.getString("ws_password") else ""

                val customOptJsonObj = pack.getJSONObject("custom_options")
                icUser =
                    if (customOptJsonObj.has("ic_user")) customOptJsonObj.getString("ic_user") else ""
                icPass =
                    if (customOptJsonObj.has("ic_password")) customOptJsonObj.getString("ic_password") else ""

                if (prefs == null) {
                    return
                }
                val x = (prefs ?: return).edit()
                if (productId == APP_VERSION_ID.toString()) {
                    x.putString(Preference.urlPanel.key, appUrl)
                    x.putString(Preference.installationCode.key, installationCode)
                    x.putString(Preference.clientPackage.key, clientPackage)
                    x.putString(Preference.clientEmail.key, email)
                    x.putString(Preference.clientPassword.key, password)
                } else if (productId == APP_VERSION_ID_IMAGECONTROL.toString()) {
                    x.putBoolean(Preference.useImageControl.key, true)

                    x.putString(Preference.icWsServer.key, url)
                    x.putString(Preference.icWsNamespace.key, namespace)
                    x.putString(Preference.icWsUser.key, user)
                    x.putString(Preference.icWsPass.key, pass)
                    x.putString(Preference.icUser.key, icUser)
                    x.putString(Preference.icPass.key, icPass)
                }
                run { x.apply() }
            }

            downloadDbRequired = true
            callback.onTaskConfigPanelEnded(ProgressStatus.finished)
        }
        // endregion

        fun getConfig(
            callback: GetClientPackages.TaskGetPackagesEnded,
            email: String,
            password: String,
            installationCode: String,
        ) {
            if (email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                thread {
                    val get = GetClientPackages()
                    get.addParams(
                        callback = callback,
                        email = email,
                        password = password,
                        installationCode = installationCode
                    )
                    get.execute()
                }
            }
        }

        private fun isDebuggable(): Boolean {
            return 0 != getContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
        }

        fun roundToString(d: Double, decimalPlaces: Int): String {
            val r = round(d, decimalPlaces).toString()
            return if (decimalPlaces == 0) {
                r.substring(0, r.indexOf('.'))
            } else {
                r
            }
        }

        fun roundToString(d: Float, decimalPlaces: Int): String {
            val r = round(d, decimalPlaces).toString()
            return if (decimalPlaces == 0) {
                r.substring(0, r.indexOf('.'))
            } else {
                r
            }
        }

        fun round(d: Double, decimalPlaces: Int): Double {
            var bd = BigDecimal(d.toString())
            bd = bd.setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP)
            return bd.toDouble()
        }

        fun round(d: Float, decimalPlaces: Int): Float {
            var bd = BigDecimal(d.toString())
            bd = bd.setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP)
            return bd.toFloat()
        }

        @Suppress("unused")
        inline fun <reified T> toArrayList(
            classToCastTo: Class<T>,
            values: Collection<Any>,
        ): ArrayList<T> {
            val collection = ArrayList<T>()
            for (value in values) {
                collection.add(classToCastTo.cast(value)!!)
            }
            return collection
        }

        val isExternalStorageWritable: Boolean
            get() {
                val state = Environment.getExternalStorageState()
                return Environment.MEDIA_MOUNTED == state
            }

        @SuppressLint("HardwareIds")
        fun getDeviceData(): JSONObject {
            val ip = getIPAddress()
            val macAddress = getMACAddress()
            val operatingSystem =
                "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"

            val pm = getContext().packageManager
            val pInfo = pm.getPackageInfo(getContext().packageName, 0)
            var appName = "Unknown"
            if (pInfo != null) {
                appName =
                    "${pm.getApplicationLabel(pInfo.applicationInfo)} ${
                        getContext().getString(R.string.app_milestone)
                    } ${pInfo.versionName}"
            }

            val processorId =
                Settings.Secure.getString(
                    getContext().contentResolver,
                    Settings.Secure.ANDROID_ID
                )
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

            val collectorData = JSONObject()
            collectorData
                .put("ip", ip)
                .put("macAddress", macAddress)
                .put("operatingSystem", operatingSystem)
                .put("appName", appName)
                .put("processorId", processorId)
                .put("deviceName", deviceName)

            return collectorData
        }

        /**
         * Get IP address from first non-localhost interface
         * @param useIPv4   true=return ipv4, false=return ipv6
         * @return  address or empty string
         */
        private fun getIPAddress(useIPv4: Boolean = true): String {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intF in interfaces) {
                    val addressArray = Collections.list(intF.inetAddresses)
                    for (address in addressArray) {
                        if (!address.isLoopbackAddress) {
                            val hostAddress = address.hostAddress ?: continue
                            //boolean isIPv4 = InetAddressUtils.isIPv4Address(hostAddress);
                            val isIPv4 = hostAddress.indexOf(':') < 0

                            if (useIPv4) {
                                if (isIPv4)
                                    return hostAddress
                            } else {
                                if (!isIPv4) {
                                    val delimiter = hostAddress.indexOf('%') // drop ip6 zone suffix
                                    return if (delimiter < 0) hostAddress.uppercase(Locale.getDefault()) else hostAddress.substring(
                                        0,
                                        delimiter
                                    ).uppercase(Locale.getDefault())
                                }
                            }
                        }
                    }
                }
            } catch (ignored: Exception) {
            }
            // for now eat exceptions
            return ""
        }

        private fun getMACAddress(interfaceName: String = "wlan0"): String {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intF in interfaces) {
                    if (!intF.name.equals(interfaceName, true)) continue
                    val mac = intF.hardwareAddress ?: return ""
                    val buf = StringBuilder()
                    for (aMac in mac) buf.append(String.format("%02X:", aMac))
                    if (buf.isNotEmpty()) buf.deleteCharAt(buf.length - 1)
                    return buf.toString().replace(":", "")
                }
            } catch (ignored: Exception) {
            }

            // for now eat exceptions
            return ""
        }

        fun getBestContrastColor(color: String): Int {
            val backColor = Color.parseColor(color)
            val l = 0.2126 * Color.red(backColor) +
                    0.7152 * Color.green(backColor) +
                    0.0722 * Color.blue(backColor)
            return if (l <= 128) textLightColor()
            else textDarkColor()
        }

        @ColorInt
        fun textLightColor(): Int {
            return ResourcesCompat.getColor(getContext().resources, R.color.text_light, null)
        }

        @ColorInt
        fun textDarkColor(): Int {
            return ResourcesCompat.getColor(getContext().resources, R.color.text_dark, null)
        }

        fun manipulateColor(color: Int, factor: Float): Int {
            val a = Color.alpha(color)
            val r = (Color.red(color) * factor).roundToInt()
            val g = (Color.green(color) * factor).roundToInt()
            val b = (Color.blue(color) * factor).roundToInt()
            return Color.argb(
                a,
                min(r, 255),
                min(g, 255),
                min(b, 255)
            )
        }

        fun getColorWithAlpha(colorId: Int, alpha: Int): Int {
            val color = ResourcesCompat.getColor(getContext().resources, colorId, null)

            val red = Color.red(color)
            val blue = Color.blue(color)
            val green = Color.green(color)

            return Color.argb(alpha, red, green, blue)
        }

        @SuppressLint("SourceLockedOrientationActivity")
        fun setScreenRotation(activity: FragmentActivity) {
            val rotation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = activity.display
                display?.rotation ?: Surface.ROTATION_0
            } else {
                @Suppress("DEPRECATION")
                val display = activity.windowManager.defaultDisplay
                display.rotation
            }
            val height: Int
            val width: Int

            val displayMetrics = Resources.getSystem().displayMetrics
            height = displayMetrics.heightPixels
            width = displayMetrics.widthPixels

            if (prefsGetBoolean(Preference.allowScreenRotation)) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                when (rotation) {
                    Surface.ROTATION_90 -> when {
                        width > height -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        else -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    }
                    Surface.ROTATION_180 -> when {
                        height > width -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                        else -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    }
                    Surface.ROTATION_270 -> when {
                        width > height -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        else -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                    Surface.ROTATION_0 ->
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else -> when {
                        height > width -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        else -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                }
            }
        }

        fun getScreenWidth(@NonNull activity: FragmentActivity): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = activity.windowManager.currentWindowMetrics
                val bounds = windowMetrics.bounds
                val insets: Insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars()
                )
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                    activity.resources.configuration.smallestScreenWidthDp < 600
                ) { // landscape and phone
                    val navigationBarSize: Int = insets.right + insets.left
                    bounds.width() - navigationBarSize
                } else { // portrait or tablet
                    bounds.width()
                }
            } else {
                val outMetrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.getMetrics(outMetrics)
                outMetrics.widthPixels
            }
        }

        fun getScreenHeight(@NonNull activity: FragmentActivity): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = activity.windowManager.currentWindowMetrics
                val bounds = windowMetrics.bounds
                val insets: Insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars()
                )
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                    activity.resources.configuration.smallestScreenWidthDp < 600
                ) { // landscape and phone
                    bounds.height()
                } else { // portrait or tablet
                    val navigationBarSize: Int = insets.bottom
                    bounds.height() - navigationBarSize
                }
            } else {
                val outMetrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.getMetrics(outMetrics)
                outMetrics.heightPixels
            }
        }

        fun getSystemBarsHeight(activity: FragmentActivity): Int {
            // Valores de la pantalla actual
            // status bar height
            var statusBarHeight = 0
            val resourceId1: Int =
                activity.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId1 > 0) {
                statusBarHeight = activity.resources.getDimensionPixelSize(resourceId1)
            }

            // action bar height
            val styledAttributes: TypedArray =
                activity.theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
            val actionBarHeight = styledAttributes.getDimension(0, 0f).toInt()
            styledAttributes.recycle()

            // navigation bar height
            var navigationBarHeight = 0
            val resourceId2: Int =
                activity.resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId2 > 0) {
                navigationBarHeight = activity.resources.getDimensionPixelSize(resourceId2)
            }

            return statusBarHeight + actionBarHeight + navigationBarHeight
        }

        fun isTablet(): Boolean {
            return getContext().resources.getBoolean(R.bool.isTab)
        }

        // region Keyboard
        fun isKeyboardVisible(): Boolean {
            val imm = getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            return imm != null && imm.isActive
        }

        fun showKeyboard(activity: AppCompatActivity) {
            if (!KeyboardVisibilityEvent.isKeyboardVisible(activity)) {
                val imm =
                    activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(activity.window.decorView.rootView, 0)
            }
        }

        fun closeKeyboard(activity: AppCompatActivity) {
            if (KeyboardVisibilityEvent.isKeyboardVisible(activity)) {
                val imm =
                    activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                val cf = activity.currentFocus
                if (cf != null) {
                    imm.hideSoftInputFromWindow(cf.windowToken, 0)
                }
            }
        }
        // endregion

        fun getMultiplier(): Int {
            return prefs?.getInt(
                Preference.scanMultiplier.key,
                Preference.scanMultiplier.defaultValue as Int
            ) ?: 1
        }

        fun setMultiplier(value: Int) {
            with(prefs!!.edit())
            {
                putInt(Preference.scanMultiplier.key, value)
                apply()
            }
        }

        //region PREFERENCES
        private var prefs: SharedPreferences? = null
        fun startPrefs() {
            prefs = PreferenceManager.getDefaultSharedPreferences(getContext())
        }

        fun prefsIsInitialized(): Boolean {
            return prefs != null
        }

        fun cleanPrefs(): Boolean {
            if (prefs == null) {
                return false
            }

            return try {
                prefs!!.edit().clear().apply()
                true
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                false
            }
        }

        @Suppress("unused")
        private fun prefsGetByKey(key: String): Any? {
            if (prefs == null) {
                return null
            }

            return prefs!!.all[key]
        }

        @Suppress("unused")
        private fun prefsCleanKey(key: String): Boolean {
            if (prefs == null) {
                return false
            }

            return try {
                with(prefs!!.edit())
                {
                    remove(key).apply()
                }
                true
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                false
            }
        }

        fun prefsPutInt(key: String, value: Int): Boolean {
            if (prefs == null) {
                return false
            }

            return try {
                with(prefs!!.edit())
                {
                    putInt(key, value).apply()
                }
                true
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                false
            }
        }

        fun prefsPutBoolean(key: String, value: Boolean): Boolean {
            if (prefs == null) {
                return false
            }

            return try {
                with(prefs!!.edit())
                {
                    putBoolean(key, value).apply()
                }
                true
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                false
            }
        }

        fun prefsPutString(key: String, value: String): Boolean {
            if (prefs == null) {
                return false
            }

            return try {
                with(prefs!!.edit())
                {
                    putString(key, value).apply()
                }
                true
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                false
            }
        }

        fun prefsPutStringSet(key: String, value: Set<String>): Boolean {
            if (prefs == null) {
                return false
            }

            return try {
                with(prefs!!.edit())
                {
                    putStringSet(key, value).apply()
                }
                true
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                false
            }
        }

        fun prefsGetStringSet(key: String, value: ArrayList<String>): Set<String>? {
            if (prefs == null) {
                return value.toSet()
            }

            return try {
                prefs!!.getStringSet(key, value.toSet())
            } catch (ex: java.lang.Exception) {
                value.toSet()
            }
        }

        fun prefsGetString(p: Preference): String {
            return privPrefsGetString(p.key, p.defaultValue.toString())
        }

        private fun privPrefsGetString(key: String, defValue: String): String {
            if (prefs == null) {
                return defValue
            }

            return try {
                prefs!!.getString(key, defValue) ?: defValue
            } catch (ex: java.lang.Exception) {
                defValue
            }
        }

        fun prefsGetInt(p: Preference): Int {
            return prefsGetInt(p.key, p.defaultValue as Int)
        }

        private fun prefsGetInt(key: String, defValue: Int): Int {
            if (prefs == null) {
                return defValue
            }

            return try {
                prefs!!.getInt(key, defValue)
            } catch (ex: java.lang.Exception) {
                return try {
                    prefs!!.getString(key, defValue.toString())?.toInt() ?: defValue
                } catch (ex: java.lang.Exception) {
                    return defValue
                }
            }
        }

        fun prefsGetLong(p: Preference): Long {
            return prefsGetLong(p.key, p.defaultValue as Long)
        }

        private fun prefsGetLong(key: String, defValue: Long): Long {
            if (prefs == null) {
                return defValue
            }

            return try {
                prefs!!.getLong(key, defValue)
            } catch (ex: java.lang.Exception) {
                return try {
                    prefs!!.getString(key, defValue.toString())?.toLong() ?: defValue
                } catch (ex: java.lang.Exception) {
                    return defValue
                }
            }
        }

        fun prefsGetFloat(p: Preference): Float {
            return prefsGetFloat(p.key, p.defaultValue as Float)
        }

        private fun prefsGetFloat(key: String, defValue: Float): Float {
            if (prefs == null) {
                return defValue
            }

            return try {
                prefs!!.getFloat(key, defValue)
            } catch (ex: java.lang.Exception) {
                return try {
                    prefs!!.getString(key, defValue.toString())?.toFloat() ?: defValue
                } catch (ex: java.lang.Exception) {
                    return defValue
                }
            }
        }

        fun prefsGetBoolean(p: Preference): Boolean {
            return privPrefsGetBoolean(p.key, p.defaultValue as Boolean)
        }

        private fun privPrefsGetBoolean(key: String, defValue: Boolean): Boolean {
            if (prefs == null) {
                return defValue
            }

            return try {
                prefs!!.getBoolean(key, defValue)
            } catch (ex: java.lang.Exception) {
                return try {
                    prefs!!.getString(key, defValue.toString())?.toBoolean() ?: defValue
                } catch (ex: java.lang.Exception) {
                    return defValue
                }
            }
        }
        //endregion PREFERENCES

        fun getFiles(directoryPath: String): ArrayList<String>? {
            val filesArrayList = ArrayList<String>()
            val f = File(directoryPath)

            f.mkdirs()
            val files = f.listFiles()
            if (files == null || files.isEmpty())
                return null
            else {
                files
                    .filter { it.name.endsWith(".json") }
                    .mapTo(filesArrayList) { it.name }
            }

            return filesArrayList
        }

        fun getJsonFromFile(filename: String): String {
            if (filename.isEmpty()) {
                return ""
            }

            val file = File(filename)
            if (!file.exists()) {
                Log.e("WC", "No existe el archivo: ${file.absolutePath}")
                return ""
            }

            val stream = FileInputStream(file)
            val jsonStr: String
            try {
                val fc: FileChannel = stream.channel
                val bb: MappedByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())

                jsonStr = Charset.defaultCharset().decode(bb).toString()
            } catch (ex: Exception) {
                ex.printStackTrace()
                return ""
            } finally {
                stream.close()
            }

            return jsonStr
        }

        // region ImageControl CONF
        val useImageControl: Boolean
            get() {
                return prefsGetBoolean(Preference.useImageControl)
            }

        val wsIcUrl: String //"https://dev.dacosys.com/Milestone11/ic/s1/service.php"
            get() {
                return prefsGetString(Preference.icWsServer)
            }

        val wsIcNamespace: String  //"https://dev.dacosys.com/Milestone11/ic/s1"
            get() {
                return prefsGetString(Preference.icWsNamespace)
            }

        val wsIcProxy: String
            get() {
                return prefsGetString(Preference.icWsProxy)
            }

        val wsIcProxyPort: Int
            get() {
                return prefsGetInt(Preference.icWsProxyPort)
            }

        val wsIcUseProxy: Boolean
            get() {
                return prefsGetBoolean(Preference.icWsUseProxy)
            }

        val wsIcProxyUser: String
            get() {
                return prefsGetString(Preference.icWsProxyUser)
            }

        val wsIcProxyPass: String
            get() {
                return prefsGetString(Preference.icWsProxyPass)
            }

        val icUser: String
            get() {
                return prefsGetString(Preference.icUser)
            }

        val icPass: String
            get() {
                return prefsGetString(Preference.icPass)
            }

        val wsIcUser: String
            get() {
                return prefsGetString(Preference.icWsUser)
            }

        val wsIcPass: String
            get() {
                return prefsGetString(Preference.icWsPass)
            }

        val maxHeightOrWidth: Int
            get() {
                return prefsGetInt(Preference.icPhotoMaxHeightOrWidth)
            }
        // endregion
    }
}