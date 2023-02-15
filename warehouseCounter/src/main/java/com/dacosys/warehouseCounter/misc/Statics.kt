package com.dacosys.warehouseCounter.misc

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.dataBase.itemCode.ItemCodeDbHelper
import com.dacosys.warehouseCounter.model.errorLog.ErrorLog
import com.dacosys.warehouseCounter.model.itemCode.ItemCode
import com.dacosys.warehouseCounter.model.user.User
import com.dacosys.warehouseCounter.moshi.clientPackage.Package
import com.dacosys.warehouseCounter.moshi.clientPackage.Package.Companion.icPasswordTag
import com.dacosys.warehouseCounter.moshi.clientPackage.Package.Companion.icUserTag
import com.dacosys.warehouseCounter.moshi.token.TokenObject
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
import com.dacosys.warehouseCounter.retrofit.functions.GetClientPackages
import com.dacosys.warehouseCounter.retrofit.result.PackagesResult
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.scanners.vh75.Vh75Bt
import com.dacosys.warehouseCounter.settings.Preference
import com.dacosys.warehouseCounter.settings.QRConfigType
import com.dacosys.warehouseCounter.settings.QRConfigType.CREATOR.QRConfigApp
import com.dacosys.warehouseCounter.settings.QRConfigType.CREATOR.QRConfigClientAccount
import com.dacosys.warehouseCounter.settings.QRConfigType.CREATOR.QRConfigImageControl
import com.dacosys.warehouseCounter.settings.QRConfigType.CREATOR.QRConfigWebservice
import com.dacosys.warehouseCounter.settings.SettingsRepository.Companion.getByKey
import com.dacosys.warehouseCounter.sync.ProgressStatus
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanOptions
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.json.JSONObject
import java.io.*
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.net.NetworkInterface
import java.net.URL
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.min
import kotlin.math.roundToInt

class Statics {
    companion object : DialogInterface.OnMultiChoiceClickListener {
        const val testMode = false

        var appName: String = "${getApplicationName()}M12"

        // Este flag es para reinicializar el colector después de cambiar en Settings.
        var collectorTypeChanged = false

        var Token: TokenObject = TokenObject()

        fun cleanToken() {
            Token = TokenObject("", "")
        }

        // region Operaciones de escritura en almacenamiento de Ordenes

        fun writeJsonToFile(v: View, filename: String, value: String, completed: Boolean): Boolean {
            if (!isExternalStorageWritable) {
                val res =
                    context().getString(R.string.error_external_storage_not_available_for_reading_or_writing)
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
                val res =
                    context().getString(R.string.an_error_occurred_while_trying_to_save_the_count)
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
            val sv = settingViewModel()
            // Setup ImageControl
            com.dacosys.imageControl.Statics.appAllowScreenRotation = sv.allowScreenRotation

            com.dacosys.imageControl.Statics.currentUserId = currentUserId
            com.dacosys.imageControl.Statics.currentUserName = currentUserName
            com.dacosys.imageControl.Statics.newInstance()

            com.dacosys.imageControl.Statics.useImageControl = sv.useImageControl
            com.dacosys.imageControl.Statics.wsIcUrl = sv.icWsServer
            com.dacosys.imageControl.Statics.wsIcNamespace = sv.icWsNamespace
            com.dacosys.imageControl.Statics.wsIcProxy = sv.icWsProxy
            com.dacosys.imageControl.Statics.wsIcProxyPort = sv.icWsProxyPort
            com.dacosys.imageControl.Statics.wsIcUseProxy = sv.icWsUseProxy
            com.dacosys.imageControl.Statics.wsIcProxyUser = sv.icWsProxyUser
            com.dacosys.imageControl.Statics.wsIcProxyPass = sv.icWsProxyPass
            com.dacosys.imageControl.Statics.icUser = sv.icUser
            com.dacosys.imageControl.Statics.icPass = sv.icPass
            com.dacosys.imageControl.Statics.wsIcUser = sv.icWsUser
            com.dacosys.imageControl.Statics.wsIcPass = sv.icWsPass
            com.dacosys.imageControl.Statics.maxHeightOrWidth = sv.icPhotoMaxHeightOrWidth
        }

        fun getScanOptions(): ScanOptions {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            options.setPrompt(context().getString(R.string.place_the_code_in_the_rectangle_of_the_viewer_to_capture_it))
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
        var superDemoMode = false

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

        const val WC_ROOT_PATH = "/warehouse_counter"
        private const val PENDING_COUNT_PATH = "/pending_counts"
        private const val COMPLETED_COUNT_PATH = "/completed_counts"
        private const val ERROR_LOG_PATH = "/error_log"

        fun getPendingPath(): File {
            return File("${context().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}$WC_ROOT_PATH/${settingViewModel().installationCode}$PENDING_COUNT_PATH/")
        }

        fun getCompletedPath(): File {
            return File("${context().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}$WC_ROOT_PATH/${settingViewModel().installationCode}$COMPLETED_COUNT_PATH/")
        }

        fun getLogPath(): File {
            return File("${context().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}$WC_ROOT_PATH/${settingViewModel().installationCode}$ERROR_LOG_PATH/")
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
                context().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
            val outFileName = context().getDatabasePath(IMAGE_CONTROL_DATABASE_NAME).toString()

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
            val outFileName = context().getDatabasePath(DATABASE_NAME).toString()

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
            val sv = settingViewModel()
            return if (sv.useBtRfid) {
                if (Rfid.rfidDevice == null) {
                    true
                } else {
                    if ((Rfid.rfidDevice is Vh75Bt)) {
                        (Rfid.rfidDevice as Vh75Bt).mState == Vh75Bt.STATE_NONE
                    } else false
                }
            } else false
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

        // region BLUETOOTH PRINTER THINGS
        var printerBluetoothDevice: BluetoothDevice? = null
            get() {
                if (field == null) {
                    refreshBluetoothPrinter()
                }
                return field
            }

        private fun refreshBluetoothPrinter() {
            val sv = settingViewModel()
            if (sv.useBtPrinter) {
                val printerMacAddress = sv.printerBtAddress
                if (printerMacAddress.isEmpty()) {
                    return
                }

                val bluetoothManager =
                    context().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val mBluetoothAdapter = bluetoothManager.adapter

                if (ActivityCompat.checkSelfPermission(
                        context(), Manifest.permission.BLUETOOTH_CONNECT
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
            onEvent: (PackagesResult) -> Unit,
            scanCode: String,
            mode: QRConfigType,
        ) {
            val mainJson = JSONObject(scanCode)
            val mainTag = when {
                mainJson.has("config") && mode == QRConfigClientAccount -> "config"
                mainJson.has(appName) && mode != QRConfigClientAccount -> appName
                else -> ""
            }

            if (mainTag.isEmpty()) {
                onEvent.invoke(
                    PackagesResult(
                        status = ProgressStatus.crashed,
                        msg = context().getString(R.string.invalid_code)
                    )
                )
                return
            }

            val confJson = mainJson.getJSONObject(mainTag)
            val sp = settingRepository()

            when (mode) {
                QRConfigClientAccount -> {
                    // Package Client Setup
                    val installationCode =
                        if (confJson.has(sp.installationCode.key)) confJson.getString(sp.installationCode.key) else ""
                    val email =
                        if (confJson.has(sp.clientEmail.key)) confJson.getString(sp.clientEmail.key) else ""
                    val password =
                        if (confJson.has(sp.clientPassword.key)) confJson.getString(sp.clientPassword.key) else ""

                    if (email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                        getConfig(
                            onEvent = onEvent,
                            email = email,
                            password = password,
                            installationCode = installationCode
                        )
                    } else {
                        onEvent.invoke(
                            PackagesResult(
                                status = ProgressStatus.crashed,
                                clientEmail = email,
                                clientPassword = password,
                                msg = context().getString(R.string.invalid_code)
                            )
                        )
                    }
                }
                QRConfigWebservice, QRConfigApp, QRConfigImageControl -> {
                    tryToLoadConfig(confJson)
                    onEvent.invoke(
                        PackagesResult(
                            status = ProgressStatus.success, msg = when (mode) {
                                QRConfigImageControl -> context().getString(R.string.imagecontrol_configured)
                                QRConfigWebservice -> context().getString(R.string.server_configured)
                                else -> context().getString(R.string.configuration_applied)
                            }
                        )
                    )
                }
                else -> {
                    onEvent.invoke(
                        PackagesResult(
                            status = ProgressStatus.crashed,
                            msg = context().getString(R.string.invalid_code)
                        )
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
                val builder = AlertDialog.Builder(activity).setTitle(R.string.configuration_qr_code)
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
                if (p.value is Int) {
                    val value = getByKey(p.key)?.value as Int
                    if (value != p.value) {
                        jsonObject.put(p.key, value)
                    }
                } else if (p.value is Boolean) {
                    val value = getByKey(p.key)?.value as Boolean
                    if (value != p.value) {
                        jsonObject.put(p.key, value)
                    }
                } else if (p.value is String) {
                    val value = getByKey(p.key)?.value as String
                    if (value != p.value && value.isNotEmpty()) {
                        jsonObject.put(p.key, value)
                    }
                } else if (p.value is Long) {
                    val value = getByKey(p.key)?.value as Long
                    if (value != p.value) {
                        jsonObject.put(p.key, value)
                    }
                } else if (p.value is Float) {
                    val value = getByKey(p.key)?.value as Float
                    if (value != p.value) {
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
            val applicationInfo = context().applicationInfo
            return when (val stringId = applicationInfo.labelRes) {
                0 -> applicationInfo.nonLocalizedLabel.toString()
                else -> context().getString(stringId)
            }
        }

        private fun tryToLoadConfig(conf: JSONObject) {
            for (prefName in conf.keys()) {
                val tempPref = getByKey(prefName) ?: continue
                try {
                    when (tempPref.value) {
                        is String -> tempPref.value = conf.getString(prefName)
                        is Boolean -> tempPref.value = conf.getBoolean(prefName)
                        is Int -> tempPref.value = conf.getInt(prefName)
                        is Float -> tempPref.value = conf.getDouble(prefName).toFloat()
                        is Long -> tempPref.value = conf.getLong(prefName)
                        else -> tempPref.value = conf.getString(prefName)
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

        // endregion SOME CONFIG VALUES AND PREFERENCES FUNCTIONS

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

            val sv = settingViewModel()
            avoidSetupProxyDialog = true

            val alert: AlertDialog.Builder = AlertDialog.Builder(activity)
            alert.setTitle(context().getString(R.string.configure_proxy_question))

            val proxyEditText = EditText(activity)
            proxyEditText.hint = context().getString(R.string.proxy)
            proxyEditText.isFocusable = true
            proxyEditText.isFocusableInTouchMode = true

            val proxyPortEditText = EditText(activity)
            proxyPortEditText.inputType = InputType.TYPE_CLASS_NUMBER
            proxyPortEditText.hint = context().getString(R.string.port)
            proxyPortEditText.isFocusable = true
            proxyPortEditText.isFocusableInTouchMode = true

            val proxyUserEditText = EditText(activity)
            proxyUserEditText.inputType = InputType.TYPE_CLASS_TEXT
            proxyUserEditText.hint = context().getString(R.string.user)
            proxyUserEditText.isFocusable = true
            proxyUserEditText.isFocusableInTouchMode = true

            val proxyPassEditText = TextInputEditText(activity)
            proxyPassEditText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            proxyPassEditText.hint = context().getString(R.string.password)
            proxyPassEditText.isFocusable = true
            proxyPassEditText.isFocusableInTouchMode = true
            proxyPassEditText.typeface = Typeface.DEFAULT
            proxyPassEditText.transformationMethod = PasswordTransformationMethod()

            val inputLayout = TextInputLayout(context())
            inputLayout.endIconMode = END_ICON_PASSWORD_TOGGLE
            inputLayout.addView(proxyPassEditText)

            val layout = LinearLayout(context())
            layout.orientation = LinearLayout.VERTICAL

            layout.addView(proxyEditText)
            layout.addView(proxyPortEditText)
            layout.addView(proxyUserEditText)
            layout.addView(inputLayout)

            alert.setView(layout)
            alert.setNegativeButton(R.string.no) { _, _ ->
                sv.useProxy = false
            }
            alert.setPositiveButton(R.string.yes) { _, _ ->

                val proxy = proxyEditText.text
                val port = proxyPortEditText.text
                val user = proxyUserEditText.text
                val pass = proxyPassEditText.text

                sv.useProxy = true
                sv.proxy = proxy.toString()

                if (port != null) {
                    sv.proxyPort = Integer.parseInt(port.toString())
                }

                if (user.isNotEmpty()) {
                    sv.proxyUser = user.toString()
                }

                if (pass != null && pass.isNotEmpty()) {
                    sv.proxyPass = pass.toString()
                }
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
        private var allProductsArray: ArrayList<Package> = ArrayList()
        private var validProductsArray: ArrayList<Package> = ArrayList()
        private var selected: BooleanArray? = null

        override fun onClick(dialog: DialogInterface?, which: Int, isChecked: Boolean) {
            if (isChecked) {
                val tempProdVersionId = validProductsArray[which].productVersionId

                for (i in 0 until validProductsArray.size) {
                    if ((selected ?: return)[i]) {
                        val prodVerId = validProductsArray[i].productVersionId
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
            callback: TaskConfigPanelEnded,
            weakAct: WeakReference<FragmentActivity>,
            allPackage: ArrayList<Package>,
            email: String,
            password: String,
            onEventData: (SnackBarEventData) -> Unit = {},
        ) {
            val activity = weakAct.get() ?: return
            if (activity.isFinishing) return

            allProductsArray.clear()
            for (pack in allPackage) {
                val pvId = pack.productVersionId
                if (ValidProducts.contains(pvId.toString()) && !allProductsArray.contains(pack)) {
                    allProductsArray.add(pack)
                }
            }

            if (!allProductsArray.any()) {
                onEventData(
                    SnackBarEventData(
                        context().getString(R.string.there_are_no_valid_products_for_the_selected_client),
                        ERROR
                    )
                )
                return
            }

            if (allProductsArray.size == 1) {
                val productVersionId = allProductsArray[0].productVersionId
                if (productVersionId == APP_VERSION_ID || productVersionId == APP_VERSION_ID_IMAGECONTROL) {
                    setConfigPanel(
                        callback = callback,
                        packArray = arrayListOf(allProductsArray[0]),
                        email = email,
                        password = password,
                        onEventData = onEventData
                    )
                    return
                } else {
                    onEventData(
                        SnackBarEventData(
                            context().getString(R.string.there_are_no_valid_products_for_the_selected_client),
                            ERROR
                        )
                    )
                    return
                }
            }

            var validProducts = false
            validProductsArray.clear()
            val client = allProductsArray[0].client
            val listItems: ArrayList<String> = ArrayList()

            for (pack in allProductsArray) {
                val productVersionId = pack.productVersionId

                // WarehouseCounter M12 o ImageControl M11
                if (productVersionId == APP_VERSION_ID || productVersionId == APP_VERSION_ID_IMAGECONTROL) {
                    validProducts = true
                    val clientPackage = pack.clientPackageContDesc

                    listItems.add(clientPackage)
                    validProductsArray.add(pack)
                }
            }

            if (!validProducts) {
                onEventData(
                    SnackBarEventData(
                        context().getString(R.string.there_are_no_valid_products_for_the_selected_client),
                        ERROR
                    )
                )
                return
            }

            selected = BooleanArray(validProductsArray.size)

            val cw = ContextThemeWrapper(activity, R.style.AlertDialogTheme)
            val builder = AlertDialog.Builder(cw)

            val title = TextView(activity)
            title.text =
                String.format("%s - %s", client, context().getString(R.string.select_package))
            title.textSize = 16F
            title.gravity = Gravity.CENTER_HORIZONTAL
            builder.setCustomTitle(title)

            builder.setMultiChoiceItems(listItems.toTypedArray(), selected, this)

            builder.setPositiveButton(R.string.accept) { dialog, _ ->
                val selectedPacks: ArrayList<Package> = ArrayList()
                for ((i, prod) in validProductsArray.withIndex()) {
                    if ((selected ?: return@setPositiveButton)[i]) {
                        selectedPacks.add(prod)
                    }
                }

                if (selectedPacks.size > 0) {
                    setConfigPanel(
                        callback = callback,
                        packArray = selectedPacks,
                        email = email,
                        password = password,
                        onEventData = onEventData
                    )
                }
                dialog.dismiss()
            }

            val layoutDefault = ResourcesCompat.getDrawable(
                context().resources, R.drawable.layout_thin_border, null
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
            callback: TaskConfigPanelEnded,
            packArray: ArrayList<Package>,
            email: String,
            password: String,
            onEventData: (SnackBarEventData) -> Unit = {},
        ) {
            for (pack in packArray) {
                val active = pack.active
                if (active == 0) {
                    onEventData(
                        SnackBarEventData(
                            context().getString(R.string.inactive_installation), ERROR
                        )
                    )
                    continue
                }

                // PANEL DE CONFIGURACIÓN
                val productId = pack.productVersionId
                val panelJsonObj = pack.panel
                val appUrl = panelJsonObj.url

                if (appUrl.isEmpty()) {
                    onEventData(
                        SnackBarEventData(
                            context().getString(R.string.app_panel_url_can_not_be_obtained), ERROR
                        )
                    )
                    return
                }

                val clientPackage = pack.clientPackageContDesc
                val installationCode = pack.installationCode
                val wsJsonObj = pack.ws
                val url = wsJsonObj.url
                val namespace = wsJsonObj.namespace
                val user = wsJsonObj.user
                val pass = wsJsonObj.password

                var icUser = ""
                var icPass = ""

                val customOptJsonObj = pack.customOptions
                if (customOptJsonObj.isNotEmpty()) {
                    icUser = customOptJsonObj[icUserTag] ?: ""
                    icPass = customOptJsonObj[icPasswordTag] ?: ""
                }

                val sv = settingViewModel()
                if (productId == APP_VERSION_ID) {
                    sv.urlPanel = appUrl
                    sv.installationCode = installationCode
                    sv.clientPackage = clientPackage
                    sv.clientEmail = email
                    sv.clientPassword = password

                    // Configuración y refresco de la conexión
                    DynamicRetrofit.start(URL(settingViewModel().urlPanel))
                } else if (productId == APP_VERSION_ID_IMAGECONTROL) {
                    sv.useImageControl = true
                    sv.icWsServer = url
                    sv.icWsNamespace = namespace
                    sv.icWsUser = user
                    sv.icWsPass = pass
                    sv.icUser = icUser
                    sv.icPass = icPass
                }
            }

            downloadDbRequired = true
            callback.onTaskConfigPanelEnded(ProgressStatus.finished)
        }
        // endregion

        fun getConfig(
            onEvent: (PackagesResult) -> Unit,
            email: String,
            password: String,
            installationCode: String = "",
        ) {
            if (email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                thread {
                    val get = GetClientPackages(onEvent)
                    get.addParams(
                        email = email, password = password, installationCode = installationCode
                    )
                    get.execute()
                }
            }
        }

        private fun isDebuggable(): Boolean {
            return 0 != context().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
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
            val operatingSystem = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"

            val pm = context().packageManager
            val pInfo = pm.getPackageInfo(context().packageName, 0)
            var appName = "Unknown"
            if (pInfo != null) {
                appName = "${pm.getApplicationLabel(pInfo.applicationInfo)} ${
                    context().getString(R.string.app_milestone)
                } ${pInfo.versionName}"
            }

            val processorId =
                Settings.Secure.getString(context().contentResolver, Settings.Secure.ANDROID_ID)
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

            val collectorData = JSONObject()
            collectorData.put("ip", ip).put("macAddress", macAddress)
                .put("operatingSystem", operatingSystem).put("appName", appName)
                .put("processorId", processorId).put("deviceName", deviceName)

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
                                if (isIPv4) return hostAddress
                            } else {
                                if (!isIPv4) {
                                    val delimiter = hostAddress.indexOf('%') // drop ip6 zone suffix
                                    return if (delimiter < 0) hostAddress.uppercase(Locale.getDefault()) else hostAddress.substring(
                                        0, delimiter
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

        fun getBestContrastColorId(colorId: Int): Int {
            return try {
                val color = ResourcesCompat.getColor(context().resources, colorId, null)
                val backColor = Color.rgb(Color.red(color), Color.green(color), Color.blue(color))
                val l =
                    0.2126 * Color.red(backColor) + 0.7152 * Color.green(backColor) + 0.0722 * Color.blue(
                        backColor
                    )
                if (l <= 128) textLightColor()
                else textDarkColor()
            } catch (ex: Exception) {
                Log.e(this::class.java.simpleName, ex.message.toString())
                textDarkColor()
            }
        }

        fun getBestContrastColor(color: String): Int {
            val backColor = Color.parseColor(color)
            val l =
                0.2126 * Color.red(backColor) + 0.7152 * Color.green(backColor) + 0.0722 * Color.blue(
                    backColor
                )
            return if (l <= 128) textLightColor()
            else textDarkColor()
        }

        @ColorInt
        fun textLightColor(): Int {
            return ResourcesCompat.getColor(context().resources, R.color.text_light, null)
        }

        @ColorInt
        fun textDarkColor(): Int {
            return ResourcesCompat.getColor(context().resources, R.color.text_dark, null)
        }

        fun manipulateColor(color: Int, factor: Float): Int {
            val a = Color.alpha(color)
            val r = (Color.red(color) * factor).roundToInt()
            val g = (Color.green(color) * factor).roundToInt()
            val b = (Color.blue(color) * factor).roundToInt()
            return Color.argb(a, min(r, 255), min(g, 255), min(b, 255))
        }

        fun getColorWithAlpha(colorId: Int, alpha: Int): Int {
            val color = ResourcesCompat.getColor(context().resources, colorId, null)

            val red = Color.red(color)
            val blue = Color.blue(color)
            val green = Color.green(color)

            return Color.argb(alpha, red, green, blue)
        }

        @SuppressLint("SourceLockedOrientationActivity")
        fun setScreenRotation(activity: FragmentActivity) {
            val viewModel = settingViewModel()

            val rotation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = activity.display
                display?.rotation ?: Surface.ROTATION_0
            } else {
                @Suppress("DEPRECATION") val display = activity.windowManager.defaultDisplay
                display.rotation
            }
            val height: Int
            val width: Int

            val displayMetrics = Resources.getSystem().displayMetrics
            height = displayMetrics.heightPixels
            width = displayMetrics.widthPixels

            if (viewModel.allowScreenRotation) {
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
                    Surface.ROTATION_0 -> activity.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else -> when {
                        height > width -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        else -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                }
            }
        }

        fun getScreenWidth(activity: FragmentActivity): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = activity.windowManager.currentWindowMetrics
                val bounds = windowMetrics.bounds
                val insets: Insets =
                    windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && activity.resources.configuration.smallestScreenWidthDp < 600) { // landscape and phone
                    val navigationBarSize: Int = insets.right + insets.left
                    bounds.width() - navigationBarSize
                } else { // portrait or tablet
                    bounds.width()
                }
            } else {
                val outMetrics = DisplayMetrics()
                @Suppress("DEPRECATION") activity.windowManager.defaultDisplay.getMetrics(outMetrics)
                outMetrics.widthPixels
            }
        }

        fun getScreenHeight(activity: FragmentActivity): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = activity.windowManager.currentWindowMetrics
                val bounds = windowMetrics.bounds
                val insets: Insets =
                    windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && activity.resources.configuration.smallestScreenWidthDp < 600) { // landscape and phone
                    bounds.height()
                } else { // portrait or tablet
                    val navigationBarSize: Int = insets.bottom
                    bounds.height() - navigationBarSize
                }
            } else {
                val outMetrics = DisplayMetrics()
                @Suppress("DEPRECATION") activity.windowManager.defaultDisplay.getMetrics(outMetrics)
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
            return context().resources.getBoolean(R.bool.isTab)
        }

        // region Keyboard
        fun isKeyboardVisible(): Boolean {
            val imm =
                context().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
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

        fun getFiles(directoryPath: String): ArrayList<String>? {
            val filesArrayList = ArrayList<String>()
            val f = File(directoryPath)

            f.mkdirs()
            val files = f.listFiles()
            if (files == null || files.isEmpty()) return null
            else {
                files.filter { it.name.endsWith(".json") }.mapTo(filesArrayList) { it.name }
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
        // endregion
    }
}