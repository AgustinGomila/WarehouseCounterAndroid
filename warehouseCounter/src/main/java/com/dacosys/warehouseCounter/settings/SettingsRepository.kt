package com.dacosys.warehouseCounter.settings

import android.content.SharedPreferences
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.moshi.orderRequest.OrderRequestType
import java.util.*

class SettingsRepository(private val prefs: SharedPreferences) {

    var confPassword = Preference(
        prefs = prefs,
        key = "conf_password",
        description = context().getString(R.string.conf_password),
        default = "9876"
    )
    var showConfButton = Preference(
        prefs = prefs,
        key = "conf_general_show_conf_button",
        description = context().getString(R.string.conf_general_show_conf_button),
        default = true
    )
    var collectorType = Preference(
        prefs = prefs,
        key = "collector_type",
        description = context().getString(R.string.collector_type),
        default = 0
    )
    var useBtPrinter = Preference(
        prefs = prefs,
        key = "conf_printer_use_bt_default",
        description = context().getString(R.string.conf_printer_use_bt_default),
        default = false
    )
    var printerBtAddress = Preference(
        prefs = prefs,
        key = "printer_bt_address",
        description = context().getString(R.string.printer_bt_address),
        default = ""
    )
    var useBtRfid = Preference(
        prefs = prefs,
        key = "conf_rfid_use_bt_default",
        description = context().getString(R.string.conf_rfid_use_bt_default),
        default = false
    )
    var rfidBtAddress = Preference(
        prefs = prefs,
        key = "rfid_bt_address",
        description = context().getString(R.string.rfid_bt_address),
        default = ""
    )
    var rfidReadPower = Preference(
        prefs = prefs,
        key = "rfid_read_power",
        description = context().getString(R.string.rfid_read_power),
        default = 26
    )
    var rfidWritePower = Preference(
        prefs = prefs,
        key = "rfid_write_power",
        description = context().getString(R.string.rfid_write_power),
        default = 10
    )
    var rfidSkipSameRead = Preference(
        prefs = prefs,
        key = "skip_same_on_rfid_read",
        description = context().getString(R.string.skip_same_on_rfid_read),
        default = false
    )
    var rfidShockOnRead = Preference(
        prefs = prefs,
        key = "shock_on_rfid_read",
        description = context().getString(R.string.shock_on_rfid_read),
        default = false
    )
    var rfidPlaySoundOnRead = Preference(
        prefs = prefs,
        key = "play_sound_on_rfid_read",
        description = context().getString(R.string.play_sound_on_rfid_read),
        default = false
    )

    /* region ImageControl WebService */
    var useImageControl = Preference(
        prefs = prefs,
        key = "use_image_control",
        description = context().getString(R.string.use_image_control),
        default = false
    )
    var icUser = Preference(
        prefs = prefs,
        key = "ic_user",
        description = context().getString(R.string.ic_user),
        default = ""
    )
    var icPass = Preference(
        prefs = prefs,
        key = "ic_pass",
        description = context().getString(R.string.ic_pass),
        default = ""
    )
    var icWsServer = Preference(
        prefs = prefs,
        key = "ic_ws_server",
        description = context().getString(R.string.ic_ws_server),
        default = ""
    )
    var icWsNamespace = Preference(
        prefs = prefs,
        key = "ic_ws_namespace",
        description = context().getString(R.string.ic_ws_namespace),
        default = ""
    )
    var icWsProxy = Preference(
        prefs = prefs,
        key = "ic_ws_proxy",
        description = context().getString(R.string.ic_ws_proxy),
        default = ""
    )
    var icWsUseProxy = Preference(
        prefs = prefs,
        key = "ic_ws_use_proxy",
        description = context().getString(R.string.ic_ws_use_proxy),
        default = false
    )
    var icWsProxyPort = Preference(
        prefs = prefs,
        key = "ic_ws_proxy_port",
        description = context().getString(R.string.ic_ws_proxy_port),
        default = 0
    )
    var icWsProxyUser = Preference(
        prefs = prefs,
        key = "ic_ws_proxy_user",
        description = context().getString(R.string.ic_ws_proxy_user),
        default = ""
    )
    var icWsProxyPass = Preference(
        prefs = prefs,
        key = "ic_ws_proxy_pass",
        description = context().getString(R.string.ic_ws_proxy_pass),
        default = ""
    )
    var icWsUser = Preference(
        prefs = prefs,
        key = "ic_ws_user",
        description = context().getString(R.string.ic_ws_user),
        default = ""
    )
    var icWsPass = Preference(
        prefs = prefs,
        key = "ic_ws_pass",
        description = context().getString(R.string.ic_ws_pass),
        default = ""
    )
    /* endregion ImageControl WebService */

    var useNfc = Preference(
        prefs = prefs,
        key = "conf_nfc_use_default",
        description = context().getString(R.string.conf_nfc_use_default),
        default = false
    )
    var autoSend = Preference(
        prefs = prefs,
        key = "auto_send",
        description = context().getString(R.string.auto_send),
        default = true
    )
    var registryError = Preference(
        prefs = prefs,
        key = "conf_general_registry_error",
        description = context().getString(R.string.conf_general_registry_error),
        default = false
    )
    var signMandatory = Preference(
        prefs = prefs,
        key = "conf_general_sign_mandatory",
        description = context().getString(R.string.conf_general_sign_mandatory),
        default = false
    )
    var requiredDescription = Preference(
        prefs = prefs,
        key = "required_description",
        description = context().getString(R.string.required_description),
        default = false
    )
    var shakeOnPendingOrders = Preference(
        prefs = prefs,
        key = "conf_shake_on_pending_orders",
        description = context().getString(R.string.shake_on_pending_orders),
        default = true
    )
    var soundOnPendingOrders = Preference(
        prefs = prefs,
        key = "conf_sound_on_pending_orders",
        description = context().getString(R.string.sound_on_pending_orders),
        default = true
    )
    var sendBarcodeCheckDigit = Preference(
        prefs = prefs,
        key = "send_check_digit",
        description = context().getString(R.string.check_digit),
        default = true
    )
    var scanModeMovement = Preference(
        prefs = prefs,
        key = "scan_mode_movement",
        description = context().getString(R.string.scan_mode_movement),
        default = 0
    )
    var scanMultiplier = Preference(
        prefs = prefs,
        key = "scan_multiplier",
        description = context().getString(R.string.scan_multiplier),
        default = 1
    )
    var orderRequestVisibleStatus = Preference(
        prefs = prefs,
        key = "order_request_visible_status",
        description = context().getString(R.string.order_request_visible_status),
        default = OrderRequestType.getAllIdAsSet()
    )
    var divisionChar = Preference(
        prefs = prefs,
        key = "division_char_text",
        description = context().getString(R.string.division_char),
        default = "."
    )
    var allowScreenRotation = Preference(
        prefs = prefs,
        key = "allow_screen_rotation",
        description = context().getString(R.string.allow_screen_rotation),
        default = true
    )
    var showScannedCode = Preference(
        prefs = prefs,
        key = "show_scanned_code",
        description = context().getString(R.string.show_scanned_code),
        default = true
    )
    var scanModeCount = Preference(
        prefs = prefs,
        key = "scan_mode_count",
        description = context().getString(R.string.scan_mode_count),
        default = 0
    )
    var finishOrder = Preference(
        prefs = prefs,
        key = "finish_order",
        description = context().getString(R.string.finish_order),
        default = true
    )
    var allowUnknownCodes = Preference(
        prefs = prefs,
        key = "allow_unknown_codes",
        description = context().getString(R.string.allow_unknown_codes),
        default = true
    )
    var icPhotoMaxHeightOrWidth = Preference(
        prefs = prefs,
        key = "ic_photo_max_height_or_width",
        description = context().getString(R.string.max_height_or_width),
        default = 1280
    )
    var useNetPrinter = Preference(
        prefs = prefs,
        key = "conf_printer_use_net_default",
        description = context().getString(R.string.use_net_printer),
        default = false
    )
    var ipNetPrinter = Preference(
        prefs = prefs,
        key = "printer_net_ip",
        description = context().getString(R.string.printer_ip_net),
        default = "0.0.0.0"
    )
    var portNetPrinter = Preference(
        prefs = prefs,
        key = "printer_net_port",
        description = context().getString(R.string.printer_port),
        default = "9100"
    )
    var lineSeparator = Preference(
        prefs = prefs,
        key = "line_separator",
        description = context().getString(R.string.line_separator),
        default = Char(10).toString()
    )
    var printerPower = Preference(
        prefs = prefs,
        key = "printer_power",
        description = context().getString(R.string.printer_power),
        default = "5"
    )
    var printerSpeed = Preference(
        prefs = prefs,
        key = "printer_speed",
        description = context().getString(R.string.printer_speed),
        default = "1"
    )
    var connectionTimeout = Preference(
        prefs = prefs,
        key = "connection_timeout",
        description = context().getString(R.string.timeout),
        default = 20
    )
    var colOffset = Preference(
        prefs = prefs,
        key = "col_offset",
        description = context().getString(R.string.column_offset),
        default = 0
    )
    var rowOffset = Preference(
        prefs = prefs,
        key = "row_offset",
        description = context().getString(R.string.row_offset),
        default = 0
    )
    var barcodeLabelTemplateId = Preference(
        prefs = prefs,
        key = "barcode_label_template_id",
        description = context().getString(R.string.default_template),
        default = -1
    )

    // region FloatingCamera Position and Size
    var flCameraPortraitLocX = Preference(
        prefs = prefs,
        key = "fl_camera_portrait_loc_x",
        description = "fl_camera_portrait_loc_x",
        default = 100
    )
    var flCameraPortraitLocY = Preference(
        prefs = prefs,
        key = "fl_camera_portrait_loc_y",
        description = "fl_camera_portrait_loc_y",
        default = 200
    )
    var flCameraPortraitWidth = Preference(
        prefs = prefs,
        key = "fl_camera_portrait_width",
        description = "fl_camera_portrait_width",
        default = 600
    )
    var flCameraPortraitHeight = Preference(
        prefs = prefs,
        key = "fl_camera_portrait_height",
        description = "fl_camera_portrait_height",
        default = 400
    )
    var flCameraLandscapeLocX = Preference(
        prefs = prefs,
        key = "fl_camera_landscape_loc_x",
        description = "fl_camera_landscape_loc_x",
        default = 100
    )
    var flCameraLandscapeLocY = Preference(
        prefs = prefs,
        key = "fl_camera_landscape_loc_y",
        description = "fl_camera_landscape_loc_y",
        default = 200
    )
    var flCameraLandscapeWidth = Preference(
        prefs = prefs,
        key = "fl_camera_landscape_width",
        description = "fl_camera_landscape_width",
        default = 600
    )
    var flCameraLandscapeHeight = Preference(
        prefs = prefs,
        key = "fl_camera_landscape_height",
        description = "fl_camera_landscape_height",
        default = 400
    )
    var flCameraContinuousMode = Preference(
        prefs = prefs,
        key = "fl_camera_continuous_mode",
        description = "fl_camera_continuous_mode",
        default = true
    )
    var flCameraFilterRepeatedReads = Preference(
        prefs = prefs,
        key = "fl_camera_filter_repeated_reads",
        description = "fl_camera_filter_repeated_reads",
        default = true
    )
    // endregion

    /* region Symbologies */
    var symbologyAztec = Preference(
        prefs = prefs,
        key = "symbology_aztec",
        description = context().getString(R.string.aztec),
        default = false
    )
    var symbologyCODABAR = Preference(
        prefs = prefs,
        key = "symbology_codabar",
        description = context().getString(R.string.codabar),
        default = false
    )
    var symbologyCode128 = Preference(
        prefs = prefs,
        key = "symbology_code_128",
        description = context().getString(R.string.code_128),
        default = true
    )
    var symbologyCode39 = Preference(
        prefs = prefs,
        key = "symbology_code_39",
        description = context().getString(R.string.code_39),
        default = true
    )
    var symbologyCode93 = Preference(
        prefs = prefs,
        key = "symbology_code_93",
        description = context().getString(R.string.code_93),
        default = false
    )
    var symbologyDataMatrix = Preference(
        prefs = prefs,
        key = "symbology_data_matrix",
        description = context().getString(R.string.data_matrix),
        default = true
    )
    var symbologyEAN13 = Preference(
        prefs = prefs,
        key = "symbology_ean_13",
        description = context().getString(R.string.ean_13),
        default = true
    )
    var symbologyEAN8 = Preference(
        prefs = prefs,
        key = "symbology_ean_8",
        description = context().getString(R.string.ean_8),
        default = true
    )
    var symbologyITF = Preference(
        prefs = prefs,
        key = "symbology_itf",
        description = context().getString(R.string.itf),
        default = false
    )
    var symbologyMaxiCode = Preference(
        prefs = prefs,
        key = "symbology_maxicode",
        description = context().getString(R.string.maxicode),
        default = false
    )
    var symbologyPDF417 = Preference(
        prefs = prefs,
        key = "symbology_pdf417",
        description = context().getString(R.string.pdf417),
        default = false
    )
    var symbologyQRCode = Preference(
        prefs = prefs,
        key = "symbology_qr_code",
        description = context().getString(R.string.qr_code),
        default = true
    )
    var symbologyRSS14 = Preference(
        prefs = prefs,
        key = "symbology_rss_14",
        description = context().getString(R.string.rss_14),
        default = false
    )
    var symbologyRSSExpanded = Preference(
        prefs = prefs,
        key = "symbology_rss_expanded",
        description = context().getString(R.string.rss_expanded),
        default = false
    )
    var symbologyUPCA = Preference(
        prefs = prefs,
        key = "symbology_upc_a",
        description = context().getString(R.string.upc_a),
        default = false
    )
    var symbologyUPCE = Preference(
        prefs = prefs,
        key = "symbology_upc_e",
        description = context().getString(R.string.upc_e),
        default = false
    )
    var symbologyUPCEANExt = Preference(
        prefs = prefs,
        key = "symbology_upc_ean_extension",
        description = context().getString(R.string.upc_ean_extension),
        default = false
    )
    /* endregion Symbologies */

    /* region WarehouseCounter Server */
    var clientPackage = Preference(
        prefs = prefs,
        key = "client_package",
        description = context().getString(R.string.client_package),
        default = ""
    )
    var installationCode = Preference(
        prefs = prefs,
        key = "installation_code",
        description = context().getString(R.string.installation_code),
        default = ""
    )
    var urlPanel = Preference(
        prefs = prefs,
        key = "url_panel",
        description = context().getString(R.string.url_panel),
        default = ""
    )
    var clientEmail = Preference(
        prefs = prefs,
        key = "client_email",
        description = context().getString(R.string.client_email),
        default = ""
    )
    var clientPassword = Preference(
        prefs = prefs,
        key = "client_password",
        description = context().getString(R.string.client_password),
        default = ""
    )
    var proxy = Preference(
        prefs = prefs,
        key = "proxy",
        description = context().getString(R.string.proxy),
        default = ""
    )
    var useProxy = Preference(
        prefs = prefs,
        key = "use_proxy",
        description = context().getString(R.string.use_proxy),
        default = false
    )
    var proxyPort = Preference(
        prefs = prefs,
        key = "proxy_port",
        description = context().getString(R.string.proxy_port),
        default = 8080
    )
    var wcSyncInterval = Preference(
        prefs = prefs,
        key = "ftp_sync_interval",
        description = context().getString(R.string.sync_interval),
        default = 60
    )
    var proxyUser = Preference(
        prefs = prefs,
        key = "proxy_user",
        description = context().getString(R.string.proxy_user),
        default = ""
    )
    var proxyPass = Preference(
        prefs = prefs,
        key = "proxy_pass",
        description = context().getString(R.string.proxy_pass),
        default = ""
    )
    /* endregion WarehouseCounter WebService */

    // region Opciones de la actividad de selectora de Items
    var selectItemSearchByItemEan = Preference(
        prefs = prefs,
        key = "item_select_search_by_item_ean",
        description = context().getString(R.string.item_select_search_by_item_ean),
        default = true
    )
    var selectItemSearchByItemCategory = Preference(
        prefs = prefs,
        key = "item_select_search_by_item_category",
        description = context().getString(R.string.item_select_search_by_item_category),
        default = true
    )
    var selectItemOnlyActive = Preference(
        prefs = prefs,
        key = "item_select_only_active",
        description = context().getString(R.string.only_active),
        default = true
    )
    /* endregion WarehouseCounter WebService */

    fun get(): SharedPreferences {
        return prefs
    }

    companion object {
        fun getAll(): ArrayList<Preference> {
            val sp = settingRepository()
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sp.autoSend,
                sp.registryError,
                sp.clientEmail,
                sp.clientPassword,
                sp.collectorType,
                sp.confPassword,
                sp.divisionChar,
                sp.allowScreenRotation,
                sp.showScannedCode,
                sp.sendBarcodeCheckDigit,

                sp.connectionTimeout,
                sp.colOffset,
                sp.rowOffset,
                sp.barcodeLabelTemplateId,

                sp.useImageControl,
                sp.icPass,
                sp.icUser,
                sp.icWsNamespace,
                sp.icWsPass,
                sp.icWsProxy,
                sp.icWsProxyPort,
                sp.icWsProxyUser,
                sp.icWsProxyPass,
                sp.icWsServer,
                sp.icWsUseProxy,
                sp.icWsUser,

                sp.orderRequestVisibleStatus,

                sp.printerBtAddress,

                sp.proxy,
                sp.proxyPass,
                sp.proxyPort,
                sp.proxyUser,

                sp.requiredDescription,

                sp.rfidBtAddress,
                sp.rfidPlaySoundOnRead,
                sp.rfidReadPower,
                sp.rfidShockOnRead,
                sp.rfidSkipSameRead,
                sp.rfidWritePower,

                sp.scanModeMovement,
                sp.scanModeCount,
                sp.scanMultiplier,

                sp.finishOrder,

                sp.allowUnknownCodes,

                sp.icPhotoMaxHeightOrWidth,

                sp.useBtPrinter,
                sp.printerBtAddress,
                sp.useNetPrinter,
                sp.ipNetPrinter,
                sp.portNetPrinter,
                sp.printerPower,
                sp.printerSpeed,

                sp.selectItemSearchByItemCategory,
                sp.selectItemSearchByItemEan,
                sp.selectItemOnlyActive,

                sp.shakeOnPendingOrders,
                sp.showConfButton,
                sp.signMandatory,
                sp.soundOnPendingOrders,

                sp.clientPackage,
                sp.installationCode,
                sp.urlPanel,

                sp.useBtPrinter,
                sp.useBtRfid,
                sp.useNfc,
                sp.useProxy,

                sp.wcSyncInterval,

                sp.flCameraPortraitLocX,
                sp.flCameraPortraitLocY,
                sp.flCameraPortraitWidth,
                sp.flCameraPortraitHeight,
                sp.flCameraLandscapeLocX,
                sp.flCameraLandscapeLocY,
                sp.flCameraLandscapeWidth,
                sp.flCameraLandscapeHeight,
                sp.flCameraContinuousMode,
                sp.flCameraFilterRepeatedReads,

                sp.symbologyPDF417,
                sp.symbologyAztec,
                sp.symbologyQRCode,
                sp.symbologyCODABAR,
                sp.symbologyCode128,
                sp.symbologyCode39,
                sp.symbologyCode93,
                sp.symbologyDataMatrix,
                sp.symbologyEAN13,
                sp.symbologyEAN8,
                sp.symbologyMaxiCode,
                sp.symbologyRSS14,
                sp.symbologyRSSExpanded,
                sp.symbologyUPCA,
                sp.symbologyUPCE
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getSymbology(): ArrayList<Preference> {
            val sp = settingRepository()
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sp.symbologyPDF417,
                sp.symbologyAztec,
                sp.symbologyQRCode,
                sp.symbologyCODABAR,
                sp.symbologyCode128,
                sp.symbologyCode39,
                sp.symbologyCode93,
                sp.symbologyDataMatrix,
                sp.symbologyEAN13,
                sp.symbologyEAN8,
                sp.symbologyMaxiCode,
                sp.symbologyRSS14,
                sp.symbologyRSSExpanded,
                sp.symbologyUPCA,
                sp.symbologyUPCE
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getAppConf(): ArrayList<Preference> {
            val sp = settingRepository()
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sp.soundOnPendingOrders,
                sp.shakeOnPendingOrders,
                sp.allowScreenRotation,
                sp.showScannedCode,
                sp.autoSend,
                sp.sendBarcodeCheckDigit,
                sp.collectorType,
                sp.confPassword,
                sp.registryError,
                sp.showConfButton
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getImageControl(): ArrayList<Preference> {
            val sp = settingRepository()
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sp.useImageControl,
                sp.icPass,
                sp.icUser,
                sp.icWsNamespace,
                sp.icWsPass,
                sp.icWsProxy,
                sp.icWsProxyPort,
                sp.icWsProxyUser,
                sp.icWsProxyPass,
                sp.icWsServer,
                sp.icWsUseProxy,
                sp.icWsUser
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getClientPackage(): ArrayList<Preference> {
            val sp = settingRepository()
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sp.urlPanel,
                sp.clientEmail,
                sp.clientPassword,
                sp.installationCode,
                sp.clientPackage
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getClient(): ArrayList<Preference> {
            val sp = settingRepository()
            val allSections = ArrayList<Preference>()
            Collections.addAll(allSections, sp.clientEmail, sp.clientPassword, sp.installationCode)

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getAllSelectItemVisibleControls(): ArrayList<Preference> {
            val sp = settingRepository()
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections, sp.selectItemSearchByItemCategory, sp.selectItemSearchByItemEan
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getAllSelectItemVisibleControlsIdAsString(): ArrayList<Preference> {
            val sp = settingRepository()
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections, sp.selectItemSearchByItemCategory, sp.selectItemSearchByItemEan
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
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