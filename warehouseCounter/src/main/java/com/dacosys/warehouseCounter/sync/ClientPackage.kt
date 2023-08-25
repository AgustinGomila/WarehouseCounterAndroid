package com.dacosys.warehouseCounter.sync

import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.InsetDrawable
import android.util.Log
import android.util.Size
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.ktor.v1.functions.GetClientPackages
import com.dacosys.warehouseCounter.ktor.v1.service.PackagesResult
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.appName
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.settings.Preference
import com.dacosys.warehouseCounter.settings.SettingsRepository
import com.dacosys.warehouseCounter.settings.utils.QRConfigType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONObject
import java.lang.ref.WeakReference
import com.dacosys.warehouseCounter.ktor.v1.dto.clientPackage.Package as ClientPackage

class ClientPackage {
    companion object : DialogInterface.OnMultiChoiceClickListener {

        // region Selección automática de paquetes del cliente
        private var allProductArray: ArrayList<ClientPackage> =
            ArrayList()
        private var validProductsArray: ArrayList<ClientPackage> =
            ArrayList()
        private var selected: BooleanArray = booleanArrayOf()

        override fun onClick(dialog: DialogInterface?, which: Int, isChecked: Boolean) {
            if (isChecked) {
                val tempProdVersionId = validProductsArray[which].productVersionId

                for (i in 0 until validProductsArray.size) {
                    if (selected[i]) {
                        val prodVerId = validProductsArray[i].productVersionId
                        if (prodVerId == tempProdVersionId) {
                            selected[i] = false
                            (dialog as AlertDialog).listView.setItemChecked(i, false)
                        }
                    }
                }
            }

            selected[which] = isChecked
        }

        private val validProducts: ArrayList<String>
            get() {
                val r: ArrayList<String> = ArrayList()
                r.add(Statics.APP_VERSION_ID.toString())
                r.add(Statics.APP_VERSION_ID_IMAGECONTROL.toString())
                return r
            }

        fun selectClientPackage(
            callback: TaskConfigPanelEnded,
            weakAct: WeakReference<FragmentActivity>,
            allPackage: ArrayList<ClientPackage>,
            email: String,
            password: String,
            onEventData: (SnackBarEventData) -> Unit = {},
        ) {
            val activity = weakAct.get() ?: return
            if (activity.isFinishing) return

            allProductArray.clear()
            for (pack in allPackage) {
                val pvId = pack.productVersionId
                if (validProducts.contains(pvId.toString()) && !allProductArray.contains(pack)) {
                    allProductArray.add(pack)
                }
            }

            if (!allProductArray.any()) {
                onEventData(
                    SnackBarEventData(
                        WarehouseCounterApp.context.getString(R.string.there_are_no_valid_products_for_the_selected_client),
                        SnackBarType.ERROR
                    )
                )
                return
            }

            if (allProductArray.size == 1) {
                val productVersionId = allProductArray[0].productVersionId
                if (productVersionId == Statics.APP_VERSION_ID || productVersionId == Statics.APP_VERSION_ID_IMAGECONTROL) {
                    setConfigPanel(
                        callback = callback,
                        packArray = arrayListOf(allProductArray[0]),
                        email = email,
                        password = password,
                        onEventData = onEventData
                    )
                    return
                } else {
                    onEventData(
                        SnackBarEventData(
                            WarehouseCounterApp.context.getString(R.string.there_are_no_valid_products_for_the_selected_client),
                            SnackBarType.ERROR
                        )
                    )
                    return
                }
            }

            var validProducts = false
            validProductsArray.clear()
            val client = allProductArray[0].client
            val listItems: ArrayList<String> = ArrayList()

            for (pack in allProductArray) {
                val productVersionId = pack.productVersionId

                // WarehouseCounter M12 or ImageControl M11
                if (productVersionId == Statics.APP_VERSION_ID || productVersionId == Statics.APP_VERSION_ID_IMAGECONTROL) {
                    validProducts = true
                    val clientPackage = pack.clientPackageContDesc

                    listItems.add(clientPackage)
                    validProductsArray.add(pack)
                }
            }

            if (!validProducts) {
                onEventData(
                    SnackBarEventData(
                        WarehouseCounterApp.context.getString(R.string.there_are_no_valid_products_for_the_selected_client),
                        SnackBarType.ERROR
                    )
                )
                return
            }

            selected = BooleanArray(validProductsArray.size)

            val cw = ContextThemeWrapper(activity, R.style.AlertDialogTheme)
            val builder = AlertDialog.Builder(cw)

            val title = TextView(activity)
            title.text = String.format(
                "%s - %s", client, WarehouseCounterApp.context.getString(R.string.select_package)
            )
            title.textSize = 16F
            title.gravity = Gravity.CENTER_HORIZONTAL
            builder.setCustomTitle(title)

            builder.setMultiChoiceItems(listItems.toTypedArray(), selected, this)

            builder.setPositiveButton(R.string.accept) { dialog, _ ->
                val selectedPacks: ArrayList<ClientPackage> =
                    ArrayList()
                for ((i, prod) in validProductsArray.withIndex()) {
                    if (selected[i]) {
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
                WarehouseCounterApp.context.resources, R.drawable.layout_thin_border, null
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
            packArray: ArrayList<ClientPackage>,
            email: String,
            password: String,
            onEventData: (SnackBarEventData) -> Unit = {},
        ) {
            for (pack in packArray) {
                val active = pack.active
                if (active == 0) {
                    onEventData(
                        SnackBarEventData(
                            WarehouseCounterApp.context.getString(R.string.inactive_installation),
                            SnackBarType.ERROR
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
                            WarehouseCounterApp.context.getString(R.string.app_panel_url_can_not_be_obtained),
                            SnackBarType.ERROR
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
                for ((key, value) in customOptJsonObj) {
                    if (key == ClientPackage.IC_USER_KEY) {
                        icUser = when (value) {
                            is JsonPrimitive -> value.content
                            else -> value.toString()
                        }
                    } else if (key == ClientPackage.IC_PASSWORD_KEY) {
                        icPass = when (value) {
                            is JsonPrimitive -> value.content
                            else -> value.toString()
                        }
                    }
                }

                val sv = WarehouseCounterApp.settingViewModel
                if (productId == Statics.APP_VERSION_ID) {
                    sv.urlPanel = appUrl
                    sv.installationCode = installationCode
                    sv.clientPackage = clientPackage
                    sv.clientEmail = email
                    sv.clientPassword = password
                } else if (productId == Statics.APP_VERSION_ID_IMAGECONTROL) {
                    sv.useImageControl = true
                    sv.icWsServer = url
                    sv.icWsNamespace = namespace
                    sv.icWsUser = user
                    sv.icWsPass = pass
                    sv.icUser = icUser
                    sv.icPass = icPass
                }
            }

            Statics.downloadDbRequired = true
            callback.onTaskConfigPanelEnded(ProgressStatus.finished)
        }
        // endregion

        fun getConfigFromScannedCode(
            onEvent: (PackagesResult) -> Unit,
            scanCode: String,
            mode: QRConfigType,
        ) {
            val mainJson = JSONObject(scanCode)
            val mainTag = when {
                mainJson.has("config") && mode == QRConfigType.QRConfigClientAccount -> "config"
                mainJson.has(appName) && mode != QRConfigType.QRConfigClientAccount -> appName
                else -> ""
            }

            if (mainTag.isEmpty()) {
                onEvent.invoke(
                    PackagesResult(
                        status = ProgressStatus.crashed,
                        msg = WarehouseCounterApp.context.getString(R.string.invalid_code)
                    )
                )
                return
            }

            val confJson = mainJson.getJSONObject(mainTag)
            val sp = WarehouseCounterApp.settingRepository

            when (mode) {
                QRConfigType.QRConfigClientAccount -> {
                    // Package Client Setup
                    val installationCode =
                        if (confJson.has(sp.installationCode.key)) confJson.getString(sp.installationCode.key) else ""
                    val email =
                        if (confJson.has(sp.clientEmail.key)) confJson.getString(sp.clientEmail.key) else ""
                    val password =
                        if (confJson.has(sp.clientPassword.key)) confJson.getString(sp.clientPassword.key) else ""

                    if (email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                        GetClientPackages.getConfig(
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
                                msg = WarehouseCounterApp.context.getString(R.string.invalid_code)
                            )
                        )
                    }
                }

                QRConfigType.QRConfigWebservice, QRConfigType.QRConfigApp, QRConfigType.QRConfigImageControl -> {
                    tryToLoadConfig(confJson)
                    onEvent.invoke(
                        PackagesResult(
                            status = ProgressStatus.success, msg = when (mode) {
                                QRConfigType.QRConfigImageControl -> WarehouseCounterApp.context.getString(
                                    R.string.imagecontrol_configured
                                )

                                QRConfigType.QRConfigWebservice -> WarehouseCounterApp.context.getString(
                                    R.string.server_configured
                                )

                                else -> WarehouseCounterApp.context.getString(R.string.configuration_applied)
                            }
                        )
                    )
                }

                else -> {
                    onEvent.invoke(
                        PackagesResult(
                            status = ProgressStatus.crashed,
                            msg = WarehouseCounterApp.context.getString(R.string.invalid_code)
                        )
                    )
                }
            }
        }

        fun generateQrCode(screenSize: Size, data: String, onFinish: ((Bitmap) -> Unit) = { }) {
            val writer = QRCodeWriter()
            try {
                var w: Int = screenSize.width
                val h: Int = screenSize.height
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

                onFinish.invoke(bmp)
            } catch (e: WriterException) {
                e.printStackTrace()
            }
        }

        fun getBarcodeForConfig(ps: java.util.ArrayList<Preference>, mainTag: String): String {
            val jsonObject = JSONObject()

            for (p in ps) {
                if (p.value is Int) {
                    val value = SettingsRepository.getByKey(p.key)?.value as Int
                    if (value != p.default) {
                        jsonObject.put(p.key, value)
                    }
                } else if (p.value is Boolean) {
                    val value = SettingsRepository.getByKey(p.key)?.value as Boolean
                    if (value != p.default) {
                        jsonObject.put(p.key, value)
                    }
                } else if (p.value is String) {
                    val value = SettingsRepository.getByKey(p.key)?.value as String
                    if (value != p.default && value.isNotEmpty()) {
                        jsonObject.put(p.key, value)
                    }
                } else if (p.value is Long) {
                    val value = SettingsRepository.getByKey(p.key)?.value as Long
                    if (value != p.default) {
                        jsonObject.put(p.key, value)
                    }
                } else if (p.value is Float) {
                    val value = SettingsRepository.getByKey(p.key)?.value as Float
                    if (value != p.default) {
                        jsonObject.put(p.key, value)
                    }
                }
            }

            val jsonRes = JSONObject()
            jsonRes.put(mainTag, jsonObject)

            Log.d(this::class.java.simpleName, jsonRes.toString())
            return jsonRes.toString()
        }

        private fun tryToLoadConfig(conf: JSONObject) {
            for (prefName in conf.keys()) {
                val tempPref = SettingsRepository.getByKey(prefName) ?: continue
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
    }
}
