package com.dacosys.warehouseCounter.data.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsRepository
import com.dacosys.warehouseCounter.data.settings.SettingsRepository.Companion.cleanKey
import com.dacosys.warehouseCounter.scanners.collector.CollectorType

@Suppress("unused")
class SettingsViewModel : ViewModel() {
    var useBtPrinter: Boolean
        get() = settingsRepository.useBtPrinter.value as Boolean
        set(value) {
            settingsRepository.useBtPrinter.value = value
        }

    var confPassword: String
        get() = settingsRepository.confPassword.value as String
        set(value) {
            settingsRepository.confPassword.value = value
        }

    var printerBtAddress: String
        get() = settingsRepository.printerBtAddress.value as String
        set(value) {
            settingsRepository.printerBtAddress.value = value
        }

    var useNetPrinter: Boolean
        get() = settingsRepository.useNetPrinter.value as Boolean
        set(value) {
            settingsRepository.useNetPrinter.value = value
        }

    var ipNetPrinter: String
        get() = settingsRepository.ipNetPrinter.value as String
        set(value) {
            settingsRepository.ipNetPrinter.value = value
        }

    // Printer Port
    var portNetPrinter: Int
        get() = settingsRepository.portNetPrinter.value as Int
        set(value) {
            settingsRepository.portNetPrinter.value = value
        }

    // Line Separator
    var lineSeparator: String
        get() = settingsRepository.lineSeparator.value as String
        set(value) {
            settingsRepository.lineSeparator.value = value
        }

    // Printer Power
    var printerPower: Int
        get() = settingsRepository.printerPower.value as Int
        set(value) {
            settingsRepository.printerPower.value = value
        }

    // Printer Speed
    var printerSpeed: Int
        get() = settingsRepository.printerSpeed.value as Int
        set(value) {
            settingsRepository.printerSpeed.value = value
        }

    // Printer Column Offset
    var colOffset: Int
        get() = settingsRepository.colOffset.value as Int
        set(value) {
            settingsRepository.colOffset.value = value
        }

    // Printer Row Offset
    var rowOffset: Int
        get() = settingsRepository.rowOffset.value as Int
        set(value) {
            settingsRepository.rowOffset.value = value
        }

    // Printer Template ID
    var barcodeLabelTemplateId: Int
        get() = settingsRepository.barcodeLabelTemplateId.value as Int
        set(value) {
            settingsRepository.barcodeLabelTemplateId.value = value
        }

    // Printer Quantity
    var printerQty: Int
        get() = settingsRepository.printerQty.value as Int
        set(value) {
            settingsRepository.printerQty.value = value
        }

    // Connection Timeout
    var connectionTimeout: Int
        get() = settingsRepository.connectionTimeout.value as Int
        set(value) {
            settingsRepository.connectionTimeout.value = value
        }

    var installationCode: String
        get() = settingsRepository.installationCode.value as String
        set(value) {
            settingsRepository.installationCode.value = value
        }

    // URL Server
    var urlPanel: String
        get() = settingsRepository.urlPanel.value as String
        set(value) {
            settingsRepository.urlPanel.value = value
        }

    var clientEmail: String
        get() = settingsRepository.clientEmail.value as String
        set(value) {
            settingsRepository.clientEmail.value = value
        }

    var clientPassword: String
        get() = settingsRepository.clientPassword.value as String
        set(value) {
            settingsRepository.clientPassword.value = value
        }

    var clientPackage: String
        get() = settingsRepository.clientPackage.value as String
        set(value) {
            settingsRepository.clientPackage.value = value
        }

    // MISC
    var allowScreenRotation: Boolean
        get() = settingsRepository.allowScreenRotation.value as Boolean
        set(value) {
            settingsRepository.allowScreenRotation.value = value
        }

    var collectorType: CollectorType
        get() {
            return try {
                return when (val pref = settingsRepository.collectorType.value) {
                    is CollectorType -> pref
                    is String -> {
                        val id: Int = if (pref.isEmpty()) 0 else pref.toInt()
                        CollectorType.getById(id)
                    }

                    is Int -> {
                        CollectorType.getById(pref)
                    }

                    is Long -> {
                        CollectorType.getById(pref.toInt())
                    }

                    else -> {
                        CollectorType.none
                    }
                }
            } catch (ex: java.lang.Exception) {
                Log.e(this::class.java.simpleName, ex.message.toString())
                cleanKey(settingsRepository.collectorType.key)
                CollectorType.none
            }
        }
        set(value) {
            settingsRepository.collectorType.value = value.id.toString()
        }

    var allowUnknownCodes: Boolean
        get() = settingsRepository.allowUnknownCodes.value as Boolean
        set(value) {
            settingsRepository.allowUnknownCodes.value = value
        }

    // region RFID
    val isRfidRequired: Boolean
        get() {
            if (!useRfid) return false
            return rfidBtAddress.isNotEmpty()
        }

    var rfidBtName: String
        get() = settingsRepository.rfidBtName.value as String
        set(value) {
            settingsRepository.rfidBtName.value = value
        }

    var rfidShowConnectedMessage: Boolean
        get() = settingsRepository.rfidShowConnectedMessage.value as Boolean
        set(value) {
            settingsRepository.rfidShowConnectedMessage.value = value
        }

    var rfidBtAddress: String
        get() = settingsRepository.rfidBtAddress.value as String
        set(value) {
            settingsRepository.rfidBtAddress.value = value
        }

    var useRfid: Boolean
        get() = settingsRepository.useBtRfid.value as Boolean
        set(value) {
            settingsRepository.useBtRfid.value = value
        }

    // IMAGE CONTROL
    var useImageControl: Boolean
        get() = settingsRepository.useImageControl.value as Boolean
        set(value) {
            settingsRepository.useImageControl.value = value
        }

    var icUser: String
        get() = settingsRepository.icUser.value as String
        set(value) {
            settingsRepository.icUser.value = value
        }

    var icPass: String
        get() = settingsRepository.icPass.value as String
        set(value) {
            settingsRepository.icPass.value = value
        }

    var icWsServer: String
        get() = settingsRepository.icWsServer.value as String
        set(value) {
            settingsRepository.icWsServer.value = value
        }

    var icWsNamespace: String
        get() = settingsRepository.icWsNamespace.value as String
        set(value) {
            settingsRepository.icWsNamespace.value = value
        }

    var icWsProxy: String
        get() = settingsRepository.icWsProxy.value as String
        set(value) {
            settingsRepository.icWsProxy.value = value
        }

    var icWsProxyPort: Int
        get() = settingsRepository.icWsProxyPort.value as Int
        set(value) {
            settingsRepository.icWsProxyPort.value = value
        }

    var icWsProxyUser: String
        get() = settingsRepository.icWsProxyUser.value as String
        set(value) {
            settingsRepository.icWsProxyUser.value = value
        }

    var icWsProxyPass: String
        get() = settingsRepository.icWsProxyPass.value as String
        set(value) {
            settingsRepository.icWsProxyPass.value = value
        }

    var icWsUseProxy: Boolean
        get() = settingsRepository.icWsUseProxy.value as Boolean
        set(value) {
            settingsRepository.icWsUseProxy.value = value
        }

    var icWsUser: String
        get() = settingsRepository.icWsUser.value as String
        set(value) {
            settingsRepository.icWsUser.value = value
        }

    var icWsPass: String
        get() = settingsRepository.icWsPass.value as String
        set(value) {
            settingsRepository.icWsPass.value = value
        }

    var icPhotoMaxHeightOrWidth: Int
        get() = settingsRepository.icPhotoMaxHeightOrWidth.value as Int
        set(value) {
            settingsRepository.icPhotoMaxHeightOrWidth.value = value
        }

    // PROXY
    var proxy: String
        get() = settingsRepository.proxy.value as String
        set(value) {
            settingsRepository.proxy.value = value
        }

    var useProxy: Boolean
        get() = settingsRepository.useProxy.value as Boolean
        set(value) {
            settingsRepository.useProxy.value = value
        }

    var proxyPort: Int
        get() = settingsRepository.proxyPort.value as Int
        set(value) {
            settingsRepository.proxyPort.value = value
        }

    var proxyUser: String
        get() = settingsRepository.proxyUser.value as String
        set(value) {
            settingsRepository.proxyUser.value = value
        }

    var proxyPass: String
        get() = settingsRepository.proxyPass.value as String
        set(value) {
            settingsRepository.proxyPass.value = value
        }

    var shakeOnPendingOrders: Boolean
        get() = settingsRepository.shakeOnPendingOrders.value as Boolean
        set(value) {
            settingsRepository.shakeOnPendingOrders.value = value
        }

    var soundOnPendingOrders: Boolean
        get() = settingsRepository.soundOnPendingOrders.value as Boolean
        set(value) {
            settingsRepository.soundOnPendingOrders.value = value
        }

    var sendBarcodeCheckDigit: Boolean
        get() = settingsRepository.sendBarcodeCheckDigit.value as Boolean
        set(value) {
            settingsRepository.sendBarcodeCheckDigit.value = value
        }

    var scanModeMovement: Int
        get() = settingsRepository.scanModeMovement.value as Int
        set(value) {
            settingsRepository.scanModeMovement.value = value
        }

    var scanMultiplier: Int
        get() = settingsRepository.scanMultiplier.value as Int
        set(value) {
            settingsRepository.scanMultiplier.value = value
        }

    var divisionChar: String
        get() = settingsRepository.divisionChar.value as String
        set(value) {
            settingsRepository.divisionChar.value = value
        }

    var showScannedCode: Boolean
        get() = settingsRepository.showScannedCode.value as Boolean
        set(value) {
            settingsRepository.showScannedCode.value = value
        }

    var scanModeCount: Int
        get() = settingsRepository.scanModeCount.value as Int
        set(value) {
            settingsRepository.scanModeCount.value = value
        }

    var showConfButton: Boolean
        get() = settingsRepository.showConfButton.value as Boolean
        set(value) {
            settingsRepository.showConfButton.value = value
        }

    var useNfc: Boolean
        get() = settingsRepository.useNfc.value as Boolean
        set(value) {
            settingsRepository.useNfc.value = value
        }

    var autoSend: Boolean
        get() = settingsRepository.autoSend.value as Boolean
        set(value) {
            settingsRepository.autoSend.value = value
        }

    var autoPrint: Boolean
        get() = settingsRepository.autoPrint.value as Boolean
        set(value) {
            settingsRepository.autoPrint.value = value
        }

    var registryError: Boolean
        get() = settingsRepository.registryError.value as Boolean
        set(value) {
            settingsRepository.registryError.value = value
        }

    var signMandatory: Boolean
        get() = settingsRepository.signMandatory.value as Boolean
        set(value) {
            settingsRepository.signMandatory.value = value
        }

    var requiredDescription: Boolean
        get() = settingsRepository.requiredDescription.value as Boolean
        set(value) {
            settingsRepository.requiredDescription.value = value
        }

    var rfidWritePower: Int
        get() = settingsRepository.rfidWritePower.value as Int
        set(value) {
            settingsRepository.rfidWritePower.value = value
        }

    var rfidReadPower: Int
        get() = settingsRepository.rfidReadPower.value as Int
        set(value) {
            settingsRepository.rfidReadPower.value = value
        }

    var rfidShockOnRead: Boolean
        get() = settingsRepository.rfidShockOnRead.value as Boolean
        set(value) {
            settingsRepository.rfidShockOnRead.value = value
        }

    var rfidPlaySoundOnRead: Boolean
        get() = settingsRepository.rfidPlaySoundOnRead.value as Boolean
        set(value) {
            settingsRepository.rfidPlaySoundOnRead.value = value
        }

    var wcSyncInterval: Int
        get() = settingsRepository.wcSyncInterval.value as Int
        set(value) {
            settingsRepository.wcSyncInterval.value = value
        }

    var wcSyncRefreshOrder: Int
        get() = settingsRepository.wcSyncRefreshOrder.value as Int
        set(value) {
            settingsRepository.wcSyncRefreshOrder.value = value
        }

    var symbologyAztec: Boolean
        get() = settingsRepository.symbologyAztec.value as Boolean
        set(value) {
            settingsRepository.symbologyAztec.value = value
        }

    var symbologyCODABAR: Boolean
        get() = settingsRepository.symbologyCODABAR.value as Boolean
        set(value) {
            settingsRepository.symbologyCODABAR.value = value
        }

    var symbologyCode128: Boolean
        get() = settingsRepository.symbologyCode128.value as Boolean
        set(value) {
            settingsRepository.symbologyCode128.value = value
        }

    var symbologyCode39: Boolean
        get() = settingsRepository.symbologyCode39.value as Boolean
        set(value) {
            settingsRepository.symbologyCode39.value = value
        }

    var symbologyCode93: Boolean
        get() = settingsRepository.symbologyCode93.value as Boolean
        set(value) {
            settingsRepository.symbologyCode93.value = value
        }

    var symbologyDataMatrix: Boolean
        get() = settingsRepository.symbologyDataMatrix.value as Boolean
        set(value) {
            settingsRepository.symbologyDataMatrix.value = value
        }

    var symbologyEAN13: Boolean
        get() = settingsRepository.symbologyEAN13.value as Boolean
        set(value) {
            settingsRepository.symbologyEAN13.value = value
        }

    var symbologyEAN8: Boolean
        get() = settingsRepository.symbologyEAN8.value as Boolean
        set(value) {
            settingsRepository.symbologyEAN8.value = value
        }

    var symbologyITF: Boolean
        get() = settingsRepository.symbologyITF.value as Boolean
        set(value) {
            settingsRepository.symbologyITF.value = value
        }

    var symbologyMaxiCode: Boolean
        get() = settingsRepository.symbologyMaxiCode.value as Boolean
        set(value) {
            settingsRepository.symbologyMaxiCode.value = value
        }

    var symbologyPDF417: Boolean
        get() = settingsRepository.symbologyPDF417.value as Boolean
        set(value) {
            settingsRepository.symbologyPDF417.value = value
        }

    var symbologyQRCode: Boolean
        get() = settingsRepository.symbologyQRCode.value as Boolean
        set(value) {
            settingsRepository.symbologyQRCode.value = value
        }

    var symbologyRSS14: Boolean
        get() = settingsRepository.symbologyRSS14.value as Boolean
        set(value) {
            settingsRepository.symbologyRSS14.value = value
        }

    var symbologyRSSExpanded: Boolean
        get() = settingsRepository.symbologyRSSExpanded.value as Boolean
        set(value) {
            settingsRepository.symbologyRSSExpanded.value = value
        }

    var symbologyUPCA: Boolean
        get() = settingsRepository.symbologyUPCA.value as Boolean
        set(value) {
            settingsRepository.symbologyUPCA.value = value
        }

    var symbologyUPCE: Boolean
        get() = settingsRepository.symbologyUPCE.value as Boolean
        set(value) {
            settingsRepository.symbologyUPCE.value = value
        }

    var symbologyUPCEANExt: Boolean
        get() = settingsRepository.symbologyUPCEANExt.value as Boolean
        set(value) {
            settingsRepository.symbologyUPCEANExt.value = value
        }

    var orderSearchByOrderId: Boolean
        get() = settingsRepository.orderSearchByOrderId.value as Boolean
        set(value) {
            settingsRepository.orderSearchByOrderId.value = value
        }

    var orderSearchByOrderExtId: Boolean
        get() = settingsRepository.orderSearchByOrderExtId.value as Boolean
        set(value) {
            settingsRepository.orderSearchByOrderExtId.value = value
        }

    var orderSearchByOrderDescription: Boolean
        get() = settingsRepository.orderSearchByOrderDescription.value as Boolean
        set(value) {
            settingsRepository.orderSearchByOrderDescription.value = value
        }

    var orderLocationSearchByOrderId: Boolean
        get() = settingsRepository.orderLocationSearchByOrderId.value as Boolean
        set(value) {
            settingsRepository.orderLocationSearchByOrderId.value = value
        }

    var orderLocationSearchByOrderExtId: Boolean
        get() = settingsRepository.orderLocationSearchByOrderExtId.value as Boolean
        set(value) {
            settingsRepository.orderLocationSearchByOrderExtId.value = value
        }

    var orderLocationSearchByArea: Boolean
        get() = settingsRepository.orderLocationSearchByArea.value as Boolean
        set(value) {
            settingsRepository.orderLocationSearchByArea.value = value
        }

    var orderLocationSearchByRack: Boolean
        get() = settingsRepository.orderLocationSearchByRack.value as Boolean
        set(value) {
            settingsRepository.orderLocationSearchByRack.value = value
        }

    var orderLocationSearchByItemCode: Boolean
        get() = settingsRepository.orderLocationSearchByItemCode.value as Boolean
        set(value) {
            settingsRepository.orderLocationSearchByItemCode.value = value
        }

    var orderLocationSearchByItemDescription: Boolean
        get() = settingsRepository.orderLocationSearchByItemDescription.value as Boolean
        set(value) {
            settingsRepository.orderLocationSearchByItemDescription.value = value
        }

    var orderLocationSearchByItemEan: Boolean
        get() = settingsRepository.orderLocationSearchByItemEan.value as Boolean
        set(value) {
            settingsRepository.orderLocationSearchByItemEan.value = value
        }

    var orderLocationSearchByOnlyActive: Boolean
        get() = settingsRepository.orderLocationSearchByOnlyActive.value as Boolean
        set(value) {
            settingsRepository.orderLocationSearchByOnlyActive.value = value
        }

    var orderLocationOnlyActive: Boolean
        get() = settingsRepository.orderLocationOnlyActive.value as Boolean
        set(value) {
            settingsRepository.orderLocationOnlyActive.value = value
        }

    var finishOrder: Boolean
        get() = settingsRepository.finishOrder.value as Boolean
        set(value) {
            settingsRepository.finishOrder.value = value
        }

    var itemSearchByOrderId: Boolean
        get() = settingsRepository.itemSearchByOrderId.value as Boolean
        set(value) {
            settingsRepository.itemSearchByOrderId.value = value
        }

    var itemSearchByCategory: Boolean
        get() = settingsRepository.itemSearchByCategory.value as Boolean
        set(value) {
            settingsRepository.itemSearchByCategory.value = value
        }

    var itemSearchByOrderExtId: Boolean
        get() = settingsRepository.itemSearchByOrderExtId.value as Boolean
        set(value) {
            settingsRepository.itemSearchByOrderExtId.value = value
        }

    var itemSearchByWarehouse: Boolean
        get() = settingsRepository.itemSearchByWarehouse.value as Boolean
        set(value) {
            settingsRepository.itemSearchByWarehouse.value = value
        }

    var itemSearchByArea: Boolean
        get() = settingsRepository.itemSearchByArea.value as Boolean
        set(value) {
            settingsRepository.itemSearchByArea.value = value
        }

    var itemSearchByRack: Boolean
        get() = settingsRepository.itemSearchByRack.value as Boolean
        set(value) {
            settingsRepository.itemSearchByRack.value = value
        }

    var itemSearchByItemExternalId: Boolean
        get() = settingsRepository.itemSearchByItemExternalId.value as Boolean
        set(value) {
            settingsRepository.itemSearchByItemExternalId.value = value
        }

    var itemSearchByItemDescription: Boolean
        get() = settingsRepository.itemSearchByItemDescription.value as Boolean
        set(value) {
            settingsRepository.itemSearchByItemDescription.value = value
        }

    var itemSearchByItemEan: Boolean
        get() = settingsRepository.itemSearchByItemEan.value as Boolean
        set(value) {
            settingsRepository.itemSearchByItemEan.value = value
        }

    var itemSearchByOnlyActive: Boolean
        get() = settingsRepository.itemSearchByOnlyActive.value as Boolean
        set(value) {
            settingsRepository.itemSearchByOnlyActive.value = value
        }

    var linkCodeSearchByCategory: Boolean
        get() = settingsRepository.linkCodeSearchByCategory.value as Boolean
        set(value) {
            settingsRepository.linkCodeSearchByCategory.value = value
        }
    var linkCodeSearchByItemEan: Boolean
        get() = settingsRepository.linkCodeSearchByItemEan.value as Boolean
        set(value) {
            settingsRepository.linkCodeSearchByItemEan.value = value
        }

    var linkCodeSearchByItemDescription: Boolean
        get() = settingsRepository.linkCodeSearchByItemDescription.value as Boolean
        set(value) {
            settingsRepository.linkCodeSearchByItemDescription.value = value
        }

    var locationSearchByWarehouse: Boolean
        get() = settingsRepository.locationSearchByWarehouse.value as Boolean
        set(value) {
            settingsRepository.locationSearchByWarehouse.value = value
        }
    var locationSearchByArea: Boolean
        get() = settingsRepository.locationSearchByArea.value as Boolean
        set(value) {
            settingsRepository.locationSearchByArea.value = value
        }

    var locationSearchByRack: Boolean
        get() = settingsRepository.locationSearchByRack.value as Boolean
        set(value) {
            settingsRepository.locationSearchByRack.value = value
        }

    @Suppress("UNCHECKED_CAST")
    var orderRequestVisibleStatus: Set<String>
        get() = settingsRepository.orderRequestVisibleStatus.value as Set<String>
        set(value) {
            settingsRepository.orderRequestVisibleStatus.value = value
        }

    var flCameraPortraitLocX: Int
        get() = settingsRepository.flCameraPortraitLocX.value as Int
        set(value) {
            settingsRepository.flCameraPortraitLocX.value = value
        }

    var flCameraPortraitLocY: Int
        get() = settingsRepository.flCameraPortraitLocY.value as Int
        set(value) {
            settingsRepository.flCameraPortraitLocY.value = value
        }

    var flCameraPortraitWidth: Int
        get() = settingsRepository.flCameraPortraitWidth.value as Int
        set(value) {
            settingsRepository.flCameraPortraitWidth.value = value
        }

    var flCameraPortraitHeight: Int
        get() = settingsRepository.flCameraPortraitHeight.value as Int
        set(value) {
            settingsRepository.flCameraPortraitHeight.value = value
        }

    var flCameraLandscapeLocX: Int
        get() = settingsRepository.flCameraLandscapeLocX.value as Int
        set(value) {
            settingsRepository.flCameraLandscapeLocX.value = value
        }

    var flCameraLandscapeLocY: Int
        get() = settingsRepository.flCameraLandscapeLocY.value as Int
        set(value) {
            settingsRepository.flCameraLandscapeLocY.value = value
        }

    var flCameraLandscapeWidth: Int
        get() = settingsRepository.flCameraLandscapeWidth.value as Int
        set(value) {
            settingsRepository.flCameraLandscapeWidth.value = value
        }

    var flCameraLandscapeHeight: Int
        get() = settingsRepository.flCameraLandscapeHeight.value as Int
        set(value) {
            settingsRepository.flCameraLandscapeHeight.value = value
        }

    var flCameraContinuousMode: Boolean
        get() = settingsRepository.flCameraContinuousMode.value as Boolean
        set(value) {
            settingsRepository.flCameraContinuousMode.value = value
        }

    var flCameraFilterRepeatedReads: Boolean
        get() = settingsRepository.flCameraFilterRepeatedReads.value as Boolean
        set(value) {
            settingsRepository.flCameraFilterRepeatedReads.value = value
        }

    var selectPtlOrderShowCheckBoxes: Boolean
        get() = settingsRepository.selectPtlOrderShowCheckBoxes.value as Boolean
        set(value) {
            settingsRepository.selectPtlOrderShowCheckBoxes.value = value
        }

    var editItems: Boolean
        get() = settingsRepository.editItems.value as Boolean
        set(value) {
            settingsRepository.editItems.value = value
        }

    var linkCodeShowImages: Boolean
        get() = settingsRepository.linkCodeShowImages.value as Boolean
        set(value) {
            settingsRepository.linkCodeShowImages.value = value
        }

    var linkCodeShowCheckBoxes: Boolean
        get() = settingsRepository.linkCodeShowCheckBoxes.value as Boolean
        set(value) {
            settingsRepository.linkCodeShowCheckBoxes.value = value
        }

    var itemSelectShowImages: Boolean
        get() = settingsRepository.itemSelectShowImages.value as Boolean
        set(value) {
            settingsRepository.itemSelectShowImages.value = value
        }

    var itemSelectShowCheckBoxes: Boolean
        get() = settingsRepository.itemSelectShowCheckBoxes.value as Boolean
        set(value) {
            settingsRepository.itemSelectShowCheckBoxes.value = value
        }

    var inboxShowCheckBoxes: Boolean
        get() = settingsRepository.inboxShowCheckBoxes.value as Boolean
        set(value) {
            settingsRepository.inboxShowCheckBoxes.value = value
        }

    var outboxShowCheckBoxes: Boolean
        get() = settingsRepository.outboxShowCheckBoxes.value as Boolean
        set(value) {
            settingsRepository.outboxShowCheckBoxes.value = value
        }

    var defaultItemTemplateId: Long
        get() = settingsRepository.defaultItemTemplateId.value as Long
        set(value) {
            settingsRepository.defaultItemTemplateId.value = value
        }

    var defaultOrderTemplateId: Long
        get() = settingsRepository.defaultOrderTemplateId.value as Long
        set(value) {
            settingsRepository.defaultOrderTemplateId.value = value
        }

    var defaultWaTemplateId: Long
        get() = settingsRepository.defaultWaTemplateId.value as Long
        set(value) {
            settingsRepository.defaultWaTemplateId.value = value
        }

    var defaultRackTemplateId: Long
        get() = settingsRepository.defaultRackTemplateId.value as Long
        set(value) {
            settingsRepository.defaultRackTemplateId.value = value
        }

    var categoryViewHeight: Int
        get() = settingsRepository.categoryViewHeight.value as Int
        set(value) {
            settingsRepository.categoryViewHeight.value = value
        }

    var itemViewHeight: Int
        get() = settingsRepository.itemViewHeight.value as Int
        set(value) {
            settingsRepository.itemViewHeight.value = value
        }

    var locationViewHeight: Int
        get() = settingsRepository.locationViewHeight.value as Int
        set(value) {
            settingsRepository.locationViewHeight.value = value
        }

    var templateViewHeight: Int
        get() = settingsRepository.templateViewHeight.value as Int
        set(value) {
            settingsRepository.templateViewHeight.value = value
        }

    var clientViewHeight: Int
        get() = settingsRepository.clientViewHeight.value as Int
        set(value) {
            settingsRepository.clientViewHeight.value = value
        }

    var decimalSeparator: Char
        get() = settingsRepository.decimalSeparator.value as Char
        set(value) {
            settingsRepository.decimalSeparator.value = value
        }

    var decimalPlaces: Int
        get() = settingsRepository.decimalPlaces.value as Int
        set(value) {
            settingsRepository.decimalPlaces.value = value
        }

    var defaultPageSize: Int
        get() = settingsRepository.defaultPageSize.value as Int
        set(value) {
            settingsRepository.defaultPageSize.value = value
        }
}
