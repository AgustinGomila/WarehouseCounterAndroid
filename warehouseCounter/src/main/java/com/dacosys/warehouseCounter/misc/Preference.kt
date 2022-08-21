package com.dacosys.warehouseCounter.misc

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics.WarehouseCounter.Companion.getContext
import com.dacosys.warehouseCounter.orderRequest.`object`.OrderRequestType
import java.util.*

class Preference : Parcelable {
    var id: Int = 0
    var key: String = ""
    var description: String = ""
    var defaultValue: Any? = null
    var debugValue: Any? = null

    constructor(id: Int, key: String, description: String, defaultValue: Any?, debugValue: Any?) {
        this.id = id
        this.key = key
        this.description = description
        this.defaultValue = defaultValue
        this.debugValue = debugValue
    }

    override fun toString(): String {
        return key
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is Preference) {
            false
        } else this.id == other.id
    }

    override fun hashCode(): Int {
        return this.id
    }

    constructor(parcel: Parcel) {
        id = parcel.readInt()
        key = parcel.readString() ?: ""
        description = parcel.readString() ?: ""
        defaultValue = parcel.readValue(Any().javaClass.classLoader)
        debugValue = parcel.readValue(Any().javaClass.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(key)
        parcel.writeString(description)
        parcel.writeValue(defaultValue)
        parcel.writeValue(debugValue)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Preference> {
        override fun createFromParcel(parcel: Parcel): Preference {
            return Preference(parcel)
        }

        override fun newArray(size: Int): Array<Preference?> {
            return arrayOfNulls(size)
        }

        var confPassword = Preference(
            1,
            "conf_password",
            getContext().getString(R.string.conf_password),
            defaultValue = "9876",
            debugValue = "9876"
        )

        var showConfButton = Preference(
            2,
            "conf_general_show_conf_button",
            getContext().getString(R.string.conf_general_show_conf_button),
            defaultValue = true,
            debugValue = true
        )

        var collectorType = Preference(
            3,
            "collector_type",
            getContext().getString(R.string.collector_type),
            0,
            1
        )

        var useBtPrinter = Preference(
            4,
            "conf_printer_use_bt_default",
            getContext().getString(R.string.conf_printer_use_bt_default),
            defaultValue = false,
            debugValue = false
        )

        var printerBtAddress = Preference(
            5,
            "printer_bt_address",
            getContext().getString(R.string.printer_bt_address),
            "",
            ""
        )

        var useBtRfid = Preference(
            6,
            "conf_rfid_use_bt_default",
            getContext().getString(R.string.conf_rfid_use_bt_default),
            defaultValue = false,
            debugValue = false
        )

        var rfidBtAddress = Preference(
            7,
            "rfid_bt_address",
            getContext().getString(R.string.rfid_bt_address),
            "",
            ""
        )

        var rfidReadPower = Preference(
            8,
            "rfid_read_power",
            getContext().getString(R.string.rfid_read_power),
            26,
            26
        )

        var rfidWritePower = Preference(
            9,
            "rfid_write_power",
            getContext().getString(R.string.rfid_write_power),
            10,
            10
        )

        var rfidSkipSameRead = Preference(
            10,
            "skip_same_on_rfid_read",
            getContext().getString(R.string.skip_same_on_rfid_read),
            defaultValue = false,
            debugValue = false
        )

        var rfidShockOnRead = Preference(
            11,
            "shock_on_rfid_read",
            getContext().getString(R.string.shock_on_rfid_read),
            defaultValue = false,
            debugValue = false
        )

        var rfidPlaySoundOnRead = Preference(
            12,
            "play_sound_on_rfid_read",
            getContext().getString(R.string.play_sound_on_rfid_read),
            defaultValue = false,
            debugValue = false
        )

        /* region ImageControl WebService */
        var useImageControl = Preference(
            13,
            "use_image_control",
            getContext().getString(R.string.use_image_control),
            defaultValue = false,
            debugValue = true
        )

        var icUser = Preference(
            14,
            "ic_user",
            getContext().getString(R.string.ic_user),
            "",
            "test"
        )

        var icPass = Preference(
            15,
            "ic_pass",
            getContext().getString(R.string.ic_pass),
            "",
            "pass"
        )

        var icWsServer = Preference(
            16,
            "ic_ws_server",
            getContext().getString(R.string.ic_ws_server),
            "",
            "https://dev.dacosys.com/Milestone11/ic/s1/service.php"
        )

        var icWsNamespace = Preference(
            17,
            "ic_ws_namespace",
            getContext().getString(R.string.ic_ws_namespace),
            "",
            "https://dev.dacosys.com/ic"
        )

        var icWsProxy = Preference(
            18,
            "ic_ws_proxy",
            getContext().getString(R.string.ic_ws_proxy),
            "",
            ""
        )

        var icWsUseProxy = Preference(
            19,
            "ic_ws_use_proxy",
            getContext().getString(R.string.ic_ws_use_proxy),
            defaultValue = false,
            debugValue = false
        )

        var icWsProxyPort = Preference(
            20,
            "ic_ws_proxy_port",
            getContext().getString(R.string.ic_ws_proxy_port),
            0,
            0
        )

        var icWsProxyUser = Preference(
            21,
            "ic_ws_proxy_user",
            getContext().getString(R.string.ic_ws_proxy_user),
            "",
            ""
        )

        var icWsProxyPass = Preference(
            22,
            "ic_ws_proxy_pass",
            getContext().getString(R.string.ic_ws_proxy_pass),
            "",
            ""
        )

        var icWsUser = Preference(
            23,
            "ic_ws_user",
            getContext().getString(R.string.ic_ws_user),
            "",
            ""
        )

        var icWsPass = Preference(
            24,
            "ic_ws_pass",
            getContext().getString(R.string.ic_ws_pass),
            "",
            ""
        )
        /* endregion ImageControl WebService */

        var useNfc = Preference(
            25,
            "conf_nfc_use_default",
            getContext().getString(R.string.conf_nfc_use_default),
            defaultValue = false,
            debugValue = false
        )

        var autoSend = Preference(
            26,
            "auto_send",
            getContext().getString(R.string.auto_send),
            defaultValue = true,
            debugValue = true
        )

        var registryError = Preference(
            27,
            "conf_general_registry_error",
            getContext().getString(R.string.conf_general_registry_error),
            defaultValue = false,
            debugValue = false
        )

        var signMandatory = Preference(
            28,
            "conf_general_sign_mandatory",
            getContext().getString(R.string.conf_general_sign_mandatory),
            defaultValue = false,
            debugValue = false
        )

        var requiredDescription = Preference(
            29,
            "required_description",
            getContext().getString(R.string.required_description),
            defaultValue = false,
            debugValue = false
        )

        var shakeOnPendingOrders = Preference(
            30,
            "conf_shake_on_pending_orders",
            getContext().getString(R.string.shake_on_pending_orders),
            defaultValue = true,
            debugValue = true
        )

        var soundOnPendingOrders = Preference(
            31,
            "conf_sound_on_pending_orders",
            getContext().getString(R.string.sound_on_pending_orders),
            defaultValue = true,
            debugValue = true
        )

        var sendBarcodeCheckDigit = Preference(
            32,
            "send_check_digit",
            getContext().getString(R.string.check_digit),
            defaultValue = true,
            debugValue = true
        )

        var scanModeMovement = Preference(
            33,
            "scan_mode_movement",
            getContext().getString(R.string.scan_mode_movement),
            defaultValue = 0,
            debugValue = 0
        )

        var scanMultiplier = Preference(
            34,
            "scan_multiplier",
            getContext().getString(R.string.scan_multiplier),
            defaultValue = 1,
            debugValue = 1
        )

        var orderRequestVisibleStatus = Preference(
            35,
            "order_request_visible_status",
            getContext().getString(R.string.order_request_visible_status),
            defaultValue = OrderRequestType.getAllIdAsString(),
            debugValue = OrderRequestType.getAllIdAsString()
        )

        var divisionChar = Preference(
            36,
            "division_char_text",
            getContext().getString(R.string.division_char),
            defaultValue = ".",
            debugValue = "."
        )

        var allowScreenRotation = Preference(
            37,
            "allow_screen_rotation",
            getContext().getString(R.string.allow_screen_rotation),
            defaultValue = true,
            debugValue = true
        )

        var showScannedCode = Preference(
            38,
            "show_scanned_code",
            getContext().getString(R.string.show_scanned_code),
            defaultValue = true,
            debugValue = true
        )

        var scanModeCount = Preference(
            39,
            "scan_mode_count",
            getContext().getString(R.string.scan_mode_count),
            defaultValue = 0,
            debugValue = 0
        )

        var finishOrder = Preference(
            40,
            "finish_order",
            getContext().getString(R.string.finish_order),
            defaultValue = true,
            debugValue = true
        )

        var allowUnknownCodes = Preference(
            41,
            "allow_unknown_codes",
            getContext().getString(R.string.allow_unknown_codes),
            defaultValue = true,
            debugValue = true
        )

        var icPhotoMaxHeightOrWidth = Preference(
            42,
            "ic_photo_max_height_or_width",
            getContext().getString(R.string.max_height_or_width),
            1280,
            1280
        )

        var useNetPrinter = Preference(
            43,
            "conf_printer_use_net_default",
            getContext().getString(R.string.use_net_printer),
            defaultValue = false,
            debugValue = false
        )

        var ipNetPrinter = Preference(
            44,
            "printer_net_ip",
            getContext().getString(R.string.printer_ip_net),
            defaultValue = "0.0.0.0",
            debugValue = "0.0.0.0"
        )

        var portNetPrinter = Preference(
            45,
            "printer_net_port",
            getContext().getString(R.string.printer_port),
            defaultValue = "9100",
            debugValue = "9100"
        )

        var lineSeparator = Preference(
            46,
            "line_separator",
            getContext().getString(R.string.line_separator),
            defaultValue = Char(10).toString(),
            debugValue = Char(10).toString()
        )

        var printerPower = Preference(
            47,
            "printer_power",
            getContext().getString(R.string.printer_power),
            defaultValue = "5",
            debugValue = "5"
        )

        var printerSpeed = Preference(
            48,
            "printer_speed",
            getContext().getString(R.string.printer_speed),
            defaultValue = "1",
            debugValue = "1"
        )

        // region FloatingCamera Position and Size

        var flCameraPortraitLocX = Preference(
            100,
            "fl_camera_portrait_loc_x",
            "fl_camera_portrait_loc_x",
            defaultValue = 100,
            debugValue = 100
        )

        var flCameraPortraitLocY = Preference(
            101,
            "fl_camera_portrait_loc_y",
            "fl_camera_portrait_loc_y",
            defaultValue = 200,
            debugValue = 200
        )

        var flCameraPortraitWidth = Preference(
            102,
            "fl_camera_portrait_width",
            "fl_camera_portrait_width",
            defaultValue = 600,
            debugValue = 600
        )

        var flCameraPortraitHeight = Preference(
            103,
            "fl_camera_portrait_height",
            "fl_camera_portrait_height",
            defaultValue = 400,
            debugValue = 400
        )

        var flCameraLandscapeLocX = Preference(
            104,
            "fl_camera_landscape_loc_x",
            "fl_camera_landscape_loc_x",
            defaultValue = 100,
            debugValue = 100
        )

        var flCameraLandscapeLocY = Preference(
            105,
            "fl_camera_landscape_loc_y",
            "fl_camera_landscape_loc_y",
            defaultValue = 200,
            debugValue = 200
        )

        var flCameraLandscapeWidth = Preference(
            106,
            "fl_camera_landscape_width",
            "fl_camera_landscape_width",
            defaultValue = 600,
            debugValue = 600
        )

        var flCameraLandscapeHeight = Preference(
            107,
            "fl_camera_landscape_height",
            "fl_camera_landscape_height",
            defaultValue = 400,
            debugValue = 400
        )

        var flCameraContinuousMode = Preference(
            108,
            "fl_camera_continuous_mode",
            "fl_camera_continuous_mode",
            defaultValue = true,
            debugValue = true
        )

        var flCameraFilterRepeatedReads = Preference(
            109,
            "fl_camera_filter_repeated_reads",
            "fl_camera_filter_repeated_reads",
            defaultValue = true,
            debugValue = true
        )
        // endregion

        /* region Symbologies */
        var symbologyAztec = Preference(
            50,
            "symbology_aztec",
            getContext().getString(R.string.aztec),
            defaultValue = false,
            debugValue = false
        )

        var symbologyCODABAR = Preference(
            51,
            "symbology_codabar",
            getContext().getString(R.string.codabar),
            defaultValue = false,
            debugValue = false
        )

        var symbologyCode128 = Preference(
            52,
            "symbology_code_128",
            getContext().getString(R.string.code_128),
            defaultValue = true,
            debugValue = true
        )

        var symbologyCode39 = Preference(
            53,
            "symbology_code_39",
            getContext().getString(R.string.code_39),
            defaultValue = true,
            debugValue = true
        )

        var symbologyCode93 = Preference(
            54,
            "symbology_code_93",
            getContext().getString(R.string.code_93),
            defaultValue = false,
            debugValue = false
        )

        var symbologyDataMatrix = Preference(
            55,
            "symbology_data_matrix",
            getContext().getString(R.string.data_matrix),
            defaultValue = true,
            debugValue = true
        )

        var symbologyEAN13 = Preference(
            56,
            "symbology_ean_13",
            getContext().getString(R.string.ean_13),
            defaultValue = true,
            debugValue = true
        )

        var symbologyEAN8 = Preference(
            57,
            "symbology_ean_8",
            getContext().getString(R.string.ean_8),
            defaultValue = true,
            debugValue = true
        )

        var symbologyITF = Preference(
            58,
            "symbology_itf",
            getContext().getString(R.string.itf),
            defaultValue = false,
            debugValue = false
        )

        var symbologyMaxiCode = Preference(
            59,
            "symbology_maxicode",
            getContext().getString(R.string.maxicode),
            defaultValue = false,
            debugValue = false
        )

        var symbologyPDF417 = Preference(
            60,
            "symbology_pdf417",
            getContext().getString(R.string.pdf417),
            defaultValue = false,
            debugValue = false
        )

        var symbologyQRCode = Preference(
            61,
            "symbology_qr_code",
            getContext().getString(R.string.qr_code),
            defaultValue = true,
            debugValue = true
        )

        var symbologyRSS14 = Preference(
            62,
            "symbology_rss_14",
            getContext().getString(R.string.rss_14),
            defaultValue = false,
            debugValue = false
        )

        var symbologyRSSExpanded = Preference(
            63,
            "symbology_rss_expanded",
            getContext().getString(R.string.rss_expanded),
            defaultValue = false,
            debugValue = false
        )

        var symbologyUPCA = Preference(
            64,
            "symbology_upc_a",
            getContext().getString(R.string.upc_a),
            defaultValue = false,
            debugValue = false
        )

        var symbologyUPCE = Preference(
            65,
            "symbology_upc_e",
            getContext().getString(R.string.upc_e),
            defaultValue = false,
            debugValue = false
        )

        var symbologyUPCEANExt = Preference(
            66,
            "symbology_upc_ean_extension",
            getContext().getString(R.string.upc_ean_extension),
            defaultValue = false,
            debugValue = false
        )
        /* endregion Symbologies */

        /* region WarehouseCounter Server 500-600 */
        var clientPackage = Preference(
            498,
            "client_package",
            getContext().getString(R.string.client_package),
            "",
            ""
        )

        var installationCode = Preference(
            499,
            "installation_code",
            getContext().getString(R.string.installation_code),
            "",
            ""
        )

        var urlPanel = Preference(
            500,
            "url_panel",
            getContext().getString(R.string.url_panel),
            "",
            ""
        )

        var clientEmail = Preference(
            501,
            "client_email",
            getContext().getString(R.string.client_email),
            "",
            ""
        )

        var clientPassword = Preference(
            502,
            "client_password",
            getContext().getString(R.string.client_password),
            "",
            ""
        )

        var proxy = Preference(
            509,
            "proxy",
            getContext().getString(R.string.proxy),
            "",
            ""
        )

        var useProxy = Preference(
            510,
            "use_proxy",
            getContext().getString(R.string.use_proxy),
            defaultValue = false,
            debugValue = false
        )

        var proxyPort = Preference(
            511,
            "proxy_port",
            getContext().getString(R.string.proxy_port),
            8080,
            8080
        )

        var wcSyncInterval = Preference(
            512,
            "ftp_sync_interval",
            getContext().getString(R.string.sync_interval),
            60,
            60
        )

        var proxyUser = Preference(
            513,
            "proxy_user",
            getContext().getString(R.string.proxy_user),
            "",
            ""
        )

        var proxyPass = Preference(
            514,
            "proxy_pass",
            getContext().getString(R.string.proxy_pass),
            "",
            ""
        )
        /* endregion WarehouseCounter WebService */

        // region Opciones de la actividad de selectora de Items
        // 1200 - 12..
        var selectItemSearchByItemEan = Preference(
            1200,
            "item_select_search_by_item_ean",
            getContext()
                .getString(R.string.item_select_search_by_item_ean),
            defaultValue = true,
            debugValue = true
        )

        var selectItemSearchByItemCategory = Preference(
            1201,
            "item_select_search_by_item_category",
            getContext()
                .getString(R.string.item_select_search_by_item_category),
            defaultValue = true,
            debugValue = true
        )

        var selectItemOnlyActive = Preference(
            1202,
            "item_select_only_active",
            getContext().getString(R.string.only_active),
            defaultValue = true,
            debugValue = true
        )
        //endregion

        fun getAll(): ArrayList<Preference> {
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                autoSend,
                registryError,
                clientEmail,
                clientPassword,
                collectorType,
                confPassword,
                divisionChar,
                allowScreenRotation,
                showScannedCode,
                sendBarcodeCheckDigit,

                useImageControl,
                icPass,
                icUser,
                icWsNamespace,
                icWsPass,
                icWsProxy,
                icWsProxyPort,
                icWsProxyUser,
                icWsProxyPass,
                icWsServer,
                icWsUseProxy,
                icWsUser,

                orderRequestVisibleStatus,

                printerBtAddress,

                proxy,
                proxyPass,
                proxyPort,
                proxyUser,

                requiredDescription,

                rfidBtAddress,
                rfidPlaySoundOnRead,
                rfidReadPower,
                rfidShockOnRead,
                rfidSkipSameRead,
                rfidWritePower,

                scanModeMovement,
                scanModeCount,
                scanMultiplier,

                finishOrder,

                allowUnknownCodes,

                icPhotoMaxHeightOrWidth,

                useBtPrinter,
                printerBtAddress,
                useNetPrinter,
                ipNetPrinter,
                portNetPrinter,
                printerPower,
                printerSpeed,

                selectItemSearchByItemCategory,
                selectItemSearchByItemEan,
                selectItemOnlyActive,

                shakeOnPendingOrders,
                showConfButton,
                signMandatory,
                soundOnPendingOrders,

                clientPackage,
                installationCode,
                urlPanel,

                useBtPrinter,
                useBtRfid,
                useNfc,
                useProxy,

                wcSyncInterval,

                flCameraPortraitLocX,
                flCameraPortraitLocY,
                flCameraPortraitWidth,
                flCameraPortraitHeight,
                flCameraLandscapeLocX,
                flCameraLandscapeLocY,
                flCameraLandscapeWidth,
                flCameraLandscapeHeight,
                flCameraContinuousMode,
                flCameraFilterRepeatedReads,

                symbologyPDF417,
                symbologyAztec,
                symbologyQRCode,
                symbologyCODABAR,
                symbologyCode128,
                symbologyCode39,
                symbologyCode93,
                symbologyDataMatrix,
                symbologyEAN13,
                symbologyEAN8,
                symbologyMaxiCode,
                symbologyRSS14,
                symbologyRSSExpanded,
                symbologyUPCA,
                symbologyUPCE
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getSymbology(): ArrayList<Preference> {
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                symbologyPDF417,
                symbologyAztec,
                symbologyQRCode,
                symbologyCODABAR,
                symbologyCode128,
                symbologyCode39,
                symbologyCode93,
                symbologyDataMatrix,
                symbologyEAN13,
                symbologyEAN8,
                symbologyMaxiCode,
                symbologyRSS14,
                symbologyRSSExpanded,
                symbologyUPCA,
                symbologyUPCE
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getAppConf(): ArrayList<Preference> {
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                soundOnPendingOrders,
                shakeOnPendingOrders,
                allowScreenRotation,
                showScannedCode,
                autoSend,
                sendBarcodeCheckDigit,
                collectorType,
                confPassword,
                registryError,
                showConfButton
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getImageControl(): ArrayList<Preference> {
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                useImageControl,
                icPass,
                icUser,
                icWsNamespace,
                icWsPass,
                icWsProxy,
                icWsProxyPort,
                icWsProxyUser,
                icWsProxyPass,
                icWsServer,
                icWsUseProxy,
                icWsUser
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getClientPackage(): ArrayList<Preference> {
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                urlPanel,
                clientEmail,
                clientPassword,
                installationCode,
                clientPackage
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getClient(): ArrayList<Preference> {
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                clientEmail,
                clientPassword,
                installationCode
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getAllSelectItemVisibleControls(): ArrayList<Preference> {
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                selectItemSearchByItemCategory,
                selectItemSearchByItemEan
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getAllSelectItemVisibleControlsIdAsString(): ArrayList<String> {
            val allSections = ArrayList<String>()
            Collections.addAll(
                allSections,
                selectItemSearchByItemCategory.id.toString(),
                selectItemSearchByItemEan.id.toString()
            )

            return ArrayList(allSections.sortedWith(compareBy { it }))
        }

        fun getConfigPreferences(): ArrayList<Preference> {
            val allSections = ArrayList<Preference>()
            allSections.addAll(getAppConf())
            allSections.addAll(getImageControl())

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getByKey(key: String): Preference? {
            return getAll().firstOrNull { it.key == key }
        }
    }
}