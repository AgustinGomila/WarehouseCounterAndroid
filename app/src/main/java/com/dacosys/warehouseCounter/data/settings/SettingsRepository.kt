package com.dacosys.warehouseCounter.data.settings

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.sharedPreferences
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.ui.utils.Screen
import java.util.*

class SettingsRepository {

    var confPassword = Preference(
        key = "conf_password",
        description = context.getString(R.string.conf_password),
        default = "9876"
    )
    var showConfButton = Preference(
        key = "conf_general_show_conf_button",
        description = context.getString(R.string.conf_general_show_conf_button),
        default = true
    )
    var collectorType = Preference(
        key = "collector_type",
        description = context.getString(R.string.collector_type),
        default = "0"
    )
    var useBtPrinter = Preference(
        key = "conf_printer_use_bt_default",
        description = context.getString(R.string.conf_printer_use_bt_default),
        default = false
    )
    var printerBtAddress = Preference(
        key = "printer_bt_address",
        description = context.getString(R.string.printer_bt_address),
        default = ""
    )
    var useBtRfid = Preference(
        key = "conf_rfid_use_bt_default",
        description = context.getString(R.string.conf_rfid_use_bt_default),
        default = false
    )
    var rfidBtAddress = Preference(
        key = "rfid_bt_address",
        description = context.getString(R.string.rfid_bt_address),
        default = ""
    )
    var rfidReadPower = Preference(
        key = "rfid_read_power",
        description = context.getString(R.string.rfid_read_power),
        default = 26
    )
    var rfidWritePower = Preference(
        key = "rfid_write_power",
        description = context.getString(R.string.rfid_write_power),
        default = 10
    )
    var rfidSkipSameRead = Preference(
        key = "skip_same_on_rfid_read",
        description = context.getString(R.string.skip_same_on_rfid_read),
        default = false
    )
    var rfidShockOnRead = Preference(
        key = "shock_on_rfid_read",
        description = context.getString(R.string.shock_on_rfid_read),
        default = false
    )
    var rfidPlaySoundOnRead = Preference(
        key = "play_sound_on_rfid_read",
        description = context.getString(R.string.play_sound_on_rfid_read),
        default = false
    )
    var rfidBtName: Preference = Preference(
        key = "rfid_bluetooth_name",
        description = context.getString(R.string.rfid_device_name),
        default = ""
    )
    var rfidShowConnectedMessage: Preference =
        Preference(
            key = "show_connected_rfid_message",
            description = context.getString(R.string.show_connected_rfid_message),
            default = false
        )

    /* region ImageControl WebService */
    var useImageControl = Preference(
        key = "use_image_control",
        description = context.getString(R.string.use_image_control),
        default = false
    )
    var icUser = Preference(
        key = "ic_user",
        description = context.getString(R.string.ic_user),
        default = ""
    )
    var icPass = Preference(
        key = "ic_pass",
        description = context.getString(R.string.ic_pass),
        default = ""
    )
    var icWsServer = Preference(
        key = "ic_ws_server",
        description = context.getString(R.string.ic_ws_server),
        default = ""
    )
    var icWsNamespace = Preference(
        key = "ic_ws_namespace",
        description = context.getString(R.string.ic_ws_namespace),
        default = ""
    )
    var icWsProxy = Preference(
        key = "ic_ws_proxy",
        description = context.getString(R.string.ic_ws_proxy),
        default = ""
    )
    var icWsUseProxy = Preference(
        key = "ic_ws_use_proxy",
        description = context.getString(R.string.ic_ws_use_proxy),
        default = false
    )
    var icWsProxyPort = Preference(
        key = "ic_ws_proxy_port",
        description = context.getString(R.string.ic_ws_proxy_port),
        default = 0
    )
    var icWsProxyUser = Preference(
        key = "ic_ws_proxy_user",
        description = context.getString(R.string.ic_ws_proxy_user),
        default = ""
    )
    var icWsProxyPass = Preference(
        key = "ic_ws_proxy_pass",
        description = context.getString(R.string.ic_ws_proxy_pass),
        default = ""
    )
    var icWsUser = Preference(
        key = "ic_ws_user",
        description = context.getString(R.string.ic_ws_user),
        default = ""
    )
    var icWsPass = Preference(
        key = "ic_ws_pass",
        description = context.getString(R.string.ic_ws_pass),
        default = ""
    )
    //endregion ImageControl WebService */

    var useNfc = Preference(
        key = "conf_nfc_use_default",
        description = context.getString(R.string.conf_nfc_use_default),
        default = false
    )
    var autoSend = Preference(
        key = "auto_send",
        description = context.getString(R.string.auto_send),
        default = true
    )
    var autoPrint = Preference(
        key = "auto_print",
        description = context.getString(R.string.automatic_printing),
        default = false
    )
    var registryError = Preference(
        key = "conf_general_registry_error",
        description = context.getString(R.string.conf_general_registry_error),
        default = false
    )
    var signMandatory = Preference(
        key = "conf_general_sign_mandatory",
        description = context.getString(R.string.conf_general_sign_mandatory),
        default = false
    )
    var requiredDescription = Preference(
        key = "required_description",
        description = context.getString(R.string.required_description),
        default = false
    )
    var shakeOnPendingOrders = Preference(
        key = "conf_shake_on_pending_orders",
        description = context.getString(R.string.shake_on_pending_orders),
        default = true
    )
    var soundOnPendingOrders = Preference(
        key = "conf_sound_on_pending_orders",
        description = context.getString(R.string.sound_on_pending_orders),
        default = true
    )
    var sendBarcodeCheckDigit = Preference(
        key = "send_check_digit",
        description = context.getString(R.string.check_digit),
        default = true
    )
    var scanModeMovement = Preference(
        key = "scan_mode_movement",
        description = context.getString(R.string.scan_mode_movement),
        default = 0
    )
    var scanMultiplier = Preference(
        key = "scan_multiplier",
        description = context.getString(R.string.scan_multiplier),
        default = 1
    )
    var orderRequestVisibleStatus = Preference(
        key = "order_request_visible_status",
        description = context.getString(R.string.order_request_visible_status),
        default = OrderRequestType.getAllIdAsSet()
    )
    var divisionChar = Preference(
        key = "division_char_text",
        description = context.getString(R.string.division_char),
        default = "."
    )
    var allowScreenRotation = Preference(
        key = "allow_screen_rotation",
        description = context.getString(R.string.allow_screen_rotation),
        default = true
    )
    var showScannedCode = Preference(
        key = "show_scanned_code",
        description = context.getString(R.string.show_scanned_code),
        default = true
    )
    var scanModeCount = Preference(
        key = "scan_mode_count",
        description = context.getString(R.string.scan_mode_count),
        default = 0
    )
    var finishOrder = Preference(
        key = "finish_order",
        description = context.getString(R.string.finish_order),
        default = true
    )
    var allowUnknownCodes = Preference(
        key = "allow_unknown_codes",
        description = context.getString(R.string.allow_unknown_codes),
        default = true
    )
    var icPhotoMaxHeightOrWidth = Preference(
        key = "ic_photo_max_height_or_width",
        description = context.getString(R.string.max_height_or_width),
        default = 1280
    )
    var useNetPrinter = Preference(
        key = "conf_printer_use_net_default",
        description = context.getString(R.string.use_net_printer),
        default = false
    )
    var ipNetPrinter = Preference(
        key = "printer_net_ip",
        description = context.getString(R.string.printer_ip_net),
        default = "0.0.0.0"
    )
    var portNetPrinter = Preference(
        key = "printer_net_port",
        description = context.getString(R.string.printer_port),
        default = 9100
    )
    var lineSeparator = Preference(
        key = "line_separator",
        description = context.getString(R.string.line_separator),
        default = Char(10).toString()
    )
    var printerPower = Preference(
        key = "printer_power",
        description = context.getString(R.string.printer_power),
        default = 5
    )
    var printerSpeed = Preference(
        key = "printer_speed",
        description = context.getString(R.string.printer_speed),
        default = 1
    )
    var printerQty = Preference(
        key = "printer_qty",
        description = context.getString(R.string.amount_of_labels),
        default = 1
    )
    var connectionTimeout = Preference(
        key = "connection_timeout",
        description = context.getString(R.string.timeout),
        default = 20
    )
    var colOffset = Preference(
        key = "col_offset",
        description = context.getString(R.string.column_offset),
        default = 0
    )
    var rowOffset = Preference(
        key = "row_offset",
        description = context.getString(R.string.row_offset),
        default = 0
    )
    var barcodeLabelTemplateId = Preference(
        key = "barcode_label_template_id",
        description = context.getString(R.string.default_template),
        default = -1
    )
    var selectPtlOrderShowCheckBoxes = Preference(
        key = "select_ptl_order_show_checkboxes",
        description = context.getString(R.string.show_checkboxes),
        default = false
    )
    var editItems = Preference(
        key = "edit_items",
        description = context.getString(R.string.edit_items),
        default = false
    )

    var linkCodeShowImages = Preference(
        key = "link_code_show_images",
        description = context.getString(R.string.show_images),
        default = false
    )

    var linkCodeShowCheckBoxes = Preference(
        key = "link_code_show_checkboxes",
        description = context.getString(R.string.show_checkboxes),
        default = false
    )

    var itemSelectShowImages = Preference(
        key = "item_select_show_images",
        description = context.getString(R.string.show_images),
        default = false
    )

    var itemSelectShowCheckBoxes = Preference(
        key = "item_select_show_checkboxes",
        description = context.getString(R.string.show_checkboxes),
        default = false
    )

    var inboxShowCheckBoxes = Preference(
        key = "inbox_show_checkboxes",
        description = context.getString(R.string.show_checkboxes),
        default = false
    )

    var outboxShowCheckBoxes = Preference(
        key = "outbox_show_checkboxes",
        description = context.getString(R.string.show_checkboxes),
        default = false
    )

    var defaultItemTemplateId = Preference(
        key = "default_item_template_id",
        description = context.getString(R.string.default_template_for_items),
        default = 0L
    )

    var defaultOrderTemplateId = Preference(
        key = "default_order_template_id",
        description = context.getString(R.string.default_template_for_orders),
        default = 0L
    )

    var defaultWaTemplateId = Preference(
        key = "default_wa_template_id",
        description = context.getString(R.string.default_template_for_areas),
        default = 0L
    )

    var defaultRackTemplateId = Preference(
        key = "default_rack_template_id",
        description = context.getString(R.string.default_template_for_racks),
        default = 0L
    )

    var clientViewHeight = Preference(
        key = "client_view_height",
        description = context.getString(R.string.default_client_adapter_view_height),
        default = if (Screen.isTablet()) 202 else 115
    )

    var templateViewHeight = Preference(
        key = "template_view_height",
        description = context.getString(R.string.default_template_adapter_view_height),
        default = if (Screen.isTablet()) 186 else 107
    )

    var categoryViewHeight = Preference(
        key = "category_view_height",
        description = context.getString(R.string.default_category_adapter_view_height),
        default = if (Screen.isTablet()) 186 else 107
    )

    var itemViewHeight = Preference(
        key = "item_view_height",
        description = context.getString(R.string.default_item_adapter_view_height),
        default = if (Screen.isTablet()) 251 else 143
    )

    var locationViewHeight = Preference(
        key = "location_view_height",
        description = context.getString(R.string.default_location_adapter_view_height),
        default = if (Screen.isTablet()) 202 else 153
    )

    //region FloatingCamera Position and Size
    var flCameraPortraitLocX = Preference(
        key = "fl_camera_portrait_loc_x",
        description = "fl_camera_portrait_loc_x",
        default = 100
    )
    var flCameraPortraitLocY = Preference(
        key = "fl_camera_portrait_loc_y",
        description = "fl_camera_portrait_loc_y",
        default = 200
    )
    var flCameraPortraitWidth = Preference(
        key = "fl_camera_portrait_width",
        description = "fl_camera_portrait_width",
        default = 600
    )
    var flCameraPortraitHeight = Preference(
        key = "fl_camera_portrait_height",
        description = "fl_camera_portrait_height",
        default = 400
    )
    var flCameraLandscapeLocX = Preference(
        key = "fl_camera_landscape_loc_x",
        description = "fl_camera_landscape_loc_x",
        default = 100
    )
    var flCameraLandscapeLocY = Preference(
        key = "fl_camera_landscape_loc_y",
        description = "fl_camera_landscape_loc_y",
        default = 200
    )
    var flCameraLandscapeWidth = Preference(
        key = "fl_camera_landscape_width",
        description = "fl_camera_landscape_width",
        default = 600
    )
    var flCameraLandscapeHeight = Preference(
        key = "fl_camera_landscape_height",
        description = "fl_camera_landscape_height",
        default = 400
    )
    var flCameraContinuousMode = Preference(
        key = "fl_camera_continuous_mode",
        description = "fl_camera_continuous_mode",
        default = true
    )
    var flCameraFilterRepeatedReads = Preference(
        key = "fl_camera_filter_repeated_reads",
        description = "fl_camera_filter_repeated_reads",
        default = true
    )
    // endregion

    /* region Symbologies */
    var symbologyAztec = Preference(
        key = "symbology_aztec",
        description = context.getString(R.string.aztec),
        default = false
    )
    var symbologyCODABAR = Preference(
        key = "symbology_codabar",
        description = context.getString(R.string.codabar),
        default = false
    )
    var symbologyCode128 = Preference(
        key = "symbology_code_128",
        description = context.getString(R.string.code_128),
        default = true
    )
    var symbologyCode39 = Preference(
        key = "symbology_code_39",
        description = context.getString(R.string.code_39),
        default = true
    )
    var symbologyCode93 = Preference(
        key = "symbology_code_93",
        description = context.getString(R.string.code_93),
        default = false
    )
    var symbologyDataMatrix = Preference(
        key = "symbology_data_matrix",
        description = context.getString(R.string.data_matrix),
        default = true
    )
    var symbologyEAN13 = Preference(
        key = "symbology_ean_13",
        description = context.getString(R.string.ean_13),
        default = true
    )
    var symbologyEAN8 = Preference(
        key = "symbology_ean_8",
        description = context.getString(R.string.ean_8),
        default = true
    )
    var symbologyITF = Preference(
        key = "symbology_itf",
        description = context.getString(R.string.itf),
        default = false
    )
    var symbologyMaxiCode = Preference(
        key = "symbology_maxicode",
        description = context.getString(R.string.maxicode),
        default = false
    )
    var symbologyPDF417 = Preference(
        key = "symbology_pdf417",
        description = context.getString(R.string.pdf417),
        default = false
    )
    var symbologyQRCode = Preference(
        key = "symbology_qr_code",
        description = context.getString(R.string.qr_code),
        default = true
    )
    var symbologyRSS14 = Preference(
        key = "symbology_rss_14",
        description = context.getString(R.string.rss_14),
        default = false
    )
    var symbologyRSSExpanded = Preference(
        key = "symbology_rss_expanded",
        description = context.getString(R.string.rss_expanded),
        default = false
    )
    var symbologyUPCA = Preference(
        key = "symbology_upc_a",
        description = context.getString(R.string.upc_a),
        default = false
    )
    var symbologyUPCE = Preference(
        key = "symbology_upc_e",
        description = context.getString(R.string.upc_e),
        default = false
    )
    var symbologyUPCEANExt = Preference(
        key = "symbology_upc_ean_extension",
        description = context.getString(R.string.upc_ean_extension),
        default = false
    )
    //endregion Symbologies */

    /* region WarehouseCounter Server */
    var clientPackage = Preference(
        key = "client_package",
        description = context.getString(R.string.client_package),
        default = ""
    )
    var installationCode = Preference(
        key = "installation_code",
        description = context.getString(R.string.installation_code),
        default = ""
    )
    var urlPanel = Preference(
        key = "url_panel",
        description = context.getString(R.string.url_panel),
        default = ""
    )
    var clientEmail = Preference(
        key = "client_email",
        description = context.getString(R.string.client_email),
        default = ""
    )
    var clientPassword = Preference(
        key = "client_password",
        description = context.getString(R.string.client_password),
        default = ""
    )
    var proxy = Preference(
        key = "proxy",
        description = context.getString(R.string.proxy),
        default = ""
    )
    var useProxy = Preference(
        key = "use_proxy",
        description = context.getString(R.string.use_proxy),
        default = false
    )
    var proxyPort = Preference(
        key = "proxy_port",
        description = context.getString(R.string.proxy_port),
        default = 8080
    )
    var wcSyncInterval = Preference(
        key = "sync_interval",
        description = context.getString(R.string.sync_interval),
        default = 60
    )
    var proxyUser = Preference(
        key = "proxy_user",
        description = context.getString(R.string.proxy_user),
        default = ""
    )
    var proxyPass = Preference(
        key = "proxy_pass",
        description = context.getString(R.string.proxy_pass),
        default = ""
    )
    var wcSyncRefreshOrder = Preference(
        key = "sync_refresh_order",
        description = context.getString(R.string.refresh_order_interval),
        default = 15
    )
    //endregion WarehouseCounter WebService */

    //region Impresión de pedidos
    var orderSearchByOrderId = Preference(
        key = "order_search_by_order_id",
        description = context.getString(R.string.order_location_search_by_order_id),
        default = true
    )
    var orderSearchByOrderExtId = Preference(
        key = "order_search_by_order_external_id",
        description = context.getString(R.string.order_location_search_by_order_external_id),
        default = false
    )
    var orderSearchByOrderDescription = Preference(
        key = "order_search_by_order_description",
        description = context.getString(R.string.order_location_search_by_item_description),
        default = true
    )
    //endregion Impresión de pedidos

    //region Actividad de búsqueda de pedidos
    var orderLocationSearchByOrderId = Preference(
        key = "order_location_search_by_order_id",
        description = context.getString(R.string.order_location_search_by_order_id),
        default = true
    )
    var orderLocationSearchByOrderExtId = Preference(
        key = "order_location_search_by_order_external_id",
        description = context.getString(R.string.order_location_search_by_order_external_id),
        default = false
    )
    var orderLocationSearchByArea = Preference(
        key = "order_location_search_by_area",
        description = context.getString(R.string.order_location_search_by_area),
        default = true
    )
    var orderLocationSearchByRack = Preference(
        key = "order_location_search_by_rack",
        description = context.getString(R.string.order_location_search_by_rack),
        default = true
    )
    var orderLocationSearchByItemDescription = Preference(
        key = "order_location_search_by_item_description",
        description = context.getString(R.string.order_location_search_by_item_description),
        default = true
    )
    var orderLocationSearchByItemCode = Preference(
        key = "order_location_search_by_item_code",
        description = context.getString(R.string.order_location_search_by_item_code),
        default = true
    )
    var orderLocationSearchByItemEan = Preference(
        key = "order_location_search_by_item_ean",
        description = context.getString(R.string.order_location_search_by_item_ean),
        default = true
    )
    var orderLocationSearchByOnlyActive = Preference(
        key = "order_location_search_by_only_active",
        description = context.getString(R.string.only_active),
        default = true
    )
    var orderLocationOnlyActive = Preference(
        key = "order_location_only_active",
        description = context.getString(R.string.only_active),
        default = true
    )
    //endregion

    //region Actividad de búsqueda de ítems
    var itemSearchByOrderId = Preference(
        key = "item_search_by_order_id",
        description = context.getString(R.string.search_by_order_id),
        default = true
    )
    var itemSearchByOrderExtId = Preference(
        key = "item_search_by_order_external_id",
        description = context.getString(R.string.search_by_order_external_id),
        default = false
    )
    var itemSearchByCategory = Preference(
        key = "item_search_by_category",
        description = context.getString(R.string.search_by_category),
        default = false
    )
    var itemSearchByWarehouse = Preference(
        key = "item_search_by_warehouse",
        description = context.getString(R.string.search_by_warehouse),
        default = false
    )
    var itemSearchByArea = Preference(
        key = "item_search_by_area",
        description = context.getString(R.string.search_by_area),
        default = true
    )
    var itemSearchByRack = Preference(
        key = "item_search_by_rack",
        description = context.getString(R.string.search_by_rack),
        default = true
    )
    var itemSearchByItemDescription = Preference(
        key = "item_search_by_item_description",
        description = context.getString(R.string.search_by_item_description),
        default = true
    )
    var itemSearchByItemExternalId = Preference(
        key = "item_search_by_item_external_id",
        description = context.getString(R.string.search_by_item_external_id),
        default = true
    )
    var itemSearchByItemEan = Preference(
        key = "item_search_by_item_ean",
        description = context.getString(R.string.search_by_item_ean),
        default = true
    )
    var itemSearchByOnlyActive = Preference(
        key = "item_search_by_only_active",
        description = context.getString(R.string.only_active),
        default = true
    )
    var itemOnlyActive = Preference(
        key = "item_only_active",
        description = context.getString(R.string.only_active),
        default = true
    )
    //endregion

    var linkCodeSearchByCategory = Preference(
        key = "link_code_search_by_category",
        description = context.getString(R.string.search_by_category),
        default = false
    )
    var linkCodeSearchByItemDescription = Preference(
        key = "link_code_search_by_item_description",
        description = context.getString(R.string.search_by_item_description),
        default = true
    )
    var linkCodeSearchByItemEan = Preference(
        key = "link_code_search_by_item_ean",
        description = context.getString(R.string.search_by_item_ean),
        default = true
    )

    var locationSearchByWarehouse = Preference(
        key = "location_search_by_warehouse",
        description = context.getString(R.string.search_by_warehouse),
        default = false
    )
    var locationSearchByArea = Preference(
        key = "location_search_by_area",
        description = context.getString(R.string.search_by_area),
        default = true
    )
    var locationSearchByRack = Preference(
        key = "location_search_by_rack",
        description = context.getString(R.string.search_by_rack),
        default = true
    )

    var decimalSeparator = Preference(
        key = "decimal_separator",
        description = context.getString(R.string.decimal_separator),
        default = '.'
    )
    var decimalPlaces = Preference(
        key = "decimal_places",
        description = context.getString(R.string.decimal_places),
        default = 0
    )

    var defaultPageSize = Preference(
        key = "default_page_size",
        description = context.getString(R.string.default_page_size),
        default = 250
    )

    companion object {
        fun getAll(): ArrayList<Preference> {
            val sr = settingsRepository
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sr.autoSend,
                sr.autoPrint,
                sr.registryError,
                sr.clientEmail,
                sr.clientPassword,
                sr.collectorType,
                sr.confPassword,
                sr.divisionChar,
                sr.allowScreenRotation,
                sr.showScannedCode,
                sr.sendBarcodeCheckDigit,

                sr.connectionTimeout,
                sr.colOffset,
                sr.rowOffset,
                sr.barcodeLabelTemplateId,

                sr.useImageControl,
                sr.icPass,
                sr.icUser,
                sr.icWsNamespace,
                sr.icWsPass,
                sr.icWsProxy,
                sr.icWsProxyPort,
                sr.icWsProxyUser,
                sr.icWsProxyPass,
                sr.icWsServer,
                sr.icWsUseProxy,
                sr.icWsUser,

                sr.orderRequestVisibleStatus,
                sr.selectPtlOrderShowCheckBoxes,
                sr.editItems,
                sr.linkCodeShowImages,
                sr.linkCodeShowCheckBoxes,
                sr.itemSelectShowImages,
                sr.itemSelectShowCheckBoxes,
                sr.inboxShowCheckBoxes,
                sr.outboxShowCheckBoxes,

                sr.defaultItemTemplateId,
                sr.defaultOrderTemplateId,
                sr.defaultWaTemplateId,
                sr.defaultRackTemplateId,

                sr.printerBtAddress,

                sr.proxy,
                sr.proxyPass,
                sr.proxyPort,
                sr.proxyUser,

                sr.requiredDescription,

                sr.rfidBtAddress,
                sr.rfidPlaySoundOnRead,
                sr.rfidReadPower,
                sr.rfidShockOnRead,
                sr.rfidSkipSameRead,
                sr.rfidWritePower,

                sr.scanModeMovement,
                sr.scanModeCount,
                sr.scanMultiplier,

                sr.finishOrder,

                sr.allowUnknownCodes,

                sr.icPhotoMaxHeightOrWidth,

                sr.useBtPrinter,
                sr.printerBtAddress,
                sr.useNetPrinter,
                sr.ipNetPrinter,
                sr.portNetPrinter,
                sr.printerPower,
                sr.printerSpeed,
                sr.printerQty,

                sr.shakeOnPendingOrders,
                sr.showConfButton,
                sr.signMandatory,
                sr.soundOnPendingOrders,

                sr.clientPackage,
                sr.installationCode,
                sr.urlPanel,

                sr.useBtPrinter,
                sr.useBtRfid,
                sr.useNfc,
                sr.useProxy,

                sr.wcSyncInterval,
                sr.wcSyncRefreshOrder,

                sr.flCameraPortraitLocX,
                sr.flCameraPortraitLocY,
                sr.flCameraPortraitWidth,
                sr.flCameraPortraitHeight,
                sr.flCameraLandscapeLocX,
                sr.flCameraLandscapeLocY,
                sr.flCameraLandscapeWidth,
                sr.flCameraLandscapeHeight,
                sr.flCameraContinuousMode,
                sr.flCameraFilterRepeatedReads,

                sr.symbologyPDF417,
                sr.symbologyAztec,
                sr.symbologyQRCode,
                sr.symbologyCODABAR,
                sr.symbologyCode128,
                sr.symbologyCode39,
                sr.symbologyCode93,
                sr.symbologyDataMatrix,
                sr.symbologyEAN13,
                sr.symbologyEAN8,
                sr.symbologyMaxiCode,
                sr.symbologyRSS14,
                sr.symbologyRSSExpanded,
                sr.symbologyUPCA,
                sr.symbologyUPCE,

                sr.orderLocationSearchByOrderId,
                sr.orderLocationSearchByOrderExtId,
                sr.orderLocationSearchByArea,
                sr.orderLocationSearchByRack,
                sr.orderLocationSearchByItemDescription,
                sr.orderLocationSearchByItemCode,
                sr.orderLocationSearchByItemEan,
                sr.orderLocationSearchByOnlyActive,
                sr.orderLocationOnlyActive,

                sr.orderSearchByOrderDescription,
                sr.orderSearchByOrderExtId,
                sr.orderSearchByOrderId,

                sr.itemSearchByOrderId,
                sr.itemSearchByOrderExtId,
                sr.itemSearchByWarehouse,
                sr.itemSearchByArea,
                sr.itemSearchByRack,
                sr.itemSearchByItemDescription,
                sr.itemSearchByItemExternalId,
                sr.itemSearchByItemEan,
                sr.itemSearchByOnlyActive,
                sr.itemOnlyActive,

                sr.linkCodeSearchByItemEan,
                sr.linkCodeSearchByItemDescription,
                sr.linkCodeSearchByCategory,

                sr.locationSearchByRack,
                sr.locationSearchByArea,
                sr.locationSearchByWarehouse,

                sr.clientViewHeight,
                sr.itemViewHeight,
                sr.categoryViewHeight,
                sr.locationViewHeight,
                sr.templateViewHeight,

                sr.decimalSeparator,
                sr.decimalPlaces,

                sr.defaultPageSize,
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getSymbology(): ArrayList<Preference> {
            val sr = settingsRepository
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sr.symbologyPDF417,
                sr.symbologyAztec,
                sr.symbologyQRCode,
                sr.symbologyCODABAR,
                sr.symbologyCode128,
                sr.symbologyCode39,
                sr.symbologyCode93,
                sr.symbologyDataMatrix,
                sr.symbologyEAN13,
                sr.symbologyEAN8,
                sr.symbologyMaxiCode,
                sr.symbologyRSS14,
                sr.symbologyRSSExpanded,
                sr.symbologyUPCA,
                sr.symbologyUPCE
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getAppConf(): ArrayList<Preference> {
            val sr = settingsRepository
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sr.soundOnPendingOrders,
                sr.shakeOnPendingOrders,
                sr.allowScreenRotation,
                sr.showScannedCode,
                sr.autoSend,
                sr.autoPrint,
                sr.sendBarcodeCheckDigit,
                sr.collectorType,
                sr.confPassword,
                sr.registryError,
                sr.showConfButton
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getImageControl(): ArrayList<Preference> {
            val sr = settingsRepository
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sr.useImageControl,
                sr.icPass,
                sr.icUser,
                sr.icWsNamespace,
                sr.icWsPass,
                sr.icWsProxy,
                sr.icWsProxyPort,
                sr.icWsProxyUser,
                sr.icWsProxyPass,
                sr.icWsServer,
                sr.icWsUseProxy,
                sr.icWsUser
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getClientPackage(): ArrayList<Preference> {
            val sr = settingsRepository
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sr.urlPanel,
                sr.clientEmail,
                sr.clientPassword,
                sr.installationCode,
                sr.clientPackage
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getClient(): ArrayList<Preference> {
            val sr = settingsRepository
            val allSections = ArrayList<Preference>()
            Collections.addAll(allSections, sr.clientEmail, sr.clientPassword, sr.installationCode)

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getAllSelectOrderLocationVisibleControls(): ArrayList<Preference> {
            val sr = settingsRepository
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sr.orderLocationSearchByOrderId,
                sr.orderLocationSearchByOrderExtId,
                sr.orderLocationSearchByArea,
                sr.orderLocationSearchByRack,
                sr.orderLocationSearchByItemDescription,
                sr.orderLocationSearchByItemCode,
                sr.orderLocationSearchByItemEan
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getAllSelectOrderVisibleControls(): ArrayList<Preference> {
            val sr = settingsRepository
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sr.orderSearchByOrderId,
                sr.orderSearchByOrderExtId,
                sr.orderSearchByOrderDescription
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getAllSelectItemVisibleControls(): ArrayList<Preference> {
            val sr = settingsRepository
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sr.itemSearchByCategory,
                sr.itemSearchByItemDescription,
                sr.itemSearchByItemEan,
                sr.itemSearchByItemExternalId
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getAllLinkCodeVisibleControls(): ArrayList<Preference> {
            val sr = settingsRepository
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sr.linkCodeSearchByItemEan,
                sr.linkCodeSearchByItemDescription,
                sr.linkCodeSearchByCategory
            )

            return ArrayList(allSections.sortedWith(compareBy { it.key }))
        }

        fun getAllSelectLocationVisibleControls(): ArrayList<Preference> {
            val sr = settingsRepository
            val allSections = ArrayList<Preference>()
            Collections.addAll(
                allSections,
                sr.locationSearchByWarehouse,
                sr.locationSearchByArea,
                sr.locationSearchByRack
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

        fun cleanKey(key: String): Boolean {
            return try {
                with(sharedPreferences.edit()) {
                    remove(key).apply()
                }
                true
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                false
            }
        }
    }
}
