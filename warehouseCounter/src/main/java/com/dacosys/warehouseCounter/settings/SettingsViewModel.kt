@file:Suppress("MemberVisibilityCanBePrivate")

package com.dacosys.warehouseCounter.settings

import androidx.lifecycle.ViewModel
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.misc.Statics

@Suppress("unused")
class SettingsViewModel : ViewModel() {
    var useBtPrinter: Boolean
        get() = settingRepository.useBtPrinter.value as Boolean
        set(value) {
            settingRepository.useBtPrinter.value = value
        }

    var confPassword: String
        get() = settingRepository.confPassword.value as String
        set(value) {
            settingRepository.confPassword.value = value
        }

    var printerBtAddress: String
        get() = settingRepository.printerBtAddress.value as String
        set(value) {
            settingRepository.printerBtAddress.value = value
        }

    var useNetPrinter: Boolean
        get() = settingRepository.useNetPrinter.value as Boolean
        set(value) {
            settingRepository.useNetPrinter.value = value
        }

    var ipNetPrinter: String
        get() = settingRepository.ipNetPrinter.value as String
        set(value) {
            settingRepository.ipNetPrinter.value = value
        }

    // Printer Port
    var portNetPrinter: Int
        get() = settingRepository.portNetPrinter.value as Int
        set(value) {
            settingRepository.portNetPrinter.value = value
        }

    // Line Separator
    var lineSeparator: String
        get() = settingRepository.lineSeparator.value as String
        set(value) {
            settingRepository.lineSeparator.value = value
        }

    // Printer Power
    var printerPower: Int
        get() = settingRepository.printerPower.value as Int
        set(value) {
            settingRepository.printerPower.value = value
        }

    // Printer Speed
    var printerSpeed: Int
        get() = settingRepository.printerSpeed.value as Int
        set(value) {
            settingRepository.printerSpeed.value = value
        }

    // Printer Column Offset
    var colOffset: Int
        get() = settingRepository.colOffset.value as Int
        set(value) {
            settingRepository.colOffset.value = value
        }

    // Printer Row Offset
    var rowOffset: Int
        get() = settingRepository.rowOffset.value as Int
        set(value) {
            settingRepository.rowOffset.value = value
        }

    // Printer Template ID
    var barcodeLabelTemplateId: Int
        get() = settingRepository.barcodeLabelTemplateId.value as Int
        set(value) {
            settingRepository.barcodeLabelTemplateId.value = value
        }

    // Connection Timeout
    var connectionTimeout: Int
        get() = settingRepository.connectionTimeout.value as Int
        set(value) {
            settingRepository.connectionTimeout.value = value
        }

    var installationCode: String
        get() = settingRepository.installationCode.value as String
        set(value) {
            settingRepository.installationCode.value = value
        }

    // URL Server
    var urlPanel: String
        get() = settingRepository.urlPanel.value as String
        set(value) {
            settingRepository.urlPanel.value = value
        }

    var clientEmail: String
        get() = settingRepository.clientEmail.value as String
        set(value) {
            settingRepository.clientEmail.value = value
        }

    var clientPassword: String
        get() = settingRepository.clientPassword.value as String
        set(value) {
            settingRepository.clientPassword.value = value
        }

    var clientPackage: String
        get() = settingRepository.clientPackage.value as String
        set(value) {
            settingRepository.clientPackage.value = value
        }

    // MISC
    var allowScreenRotation: Boolean
        get() = settingRepository.allowScreenRotation.value as Boolean
        set(value) {
            settingRepository.allowScreenRotation.value = value
        }

    var collectorType: String
        get() = settingRepository.collectorType.value as String
        set(value) {
            settingRepository.collectorType.value = value
        }

    var allowUnknownCodes: Boolean
        get() = settingRepository.allowUnknownCodes.value as Boolean
        set(value) {
            settingRepository.allowUnknownCodes.value = value
        }

    var rfidBtAddress: String
        get() = settingRepository.rfidBtAddress.value as String
        set(value) {
            settingRepository.rfidBtAddress.value = value
        }

    var useBtRfid: Boolean
        get() = settingRepository.useBtRfid.value as Boolean
        set(value) {
            settingRepository.useBtRfid.value = value
        }

    // IMAGE CONTROL
    var useImageControl: Boolean
        get() = settingRepository.useImageControl.value as Boolean
        set(value) {
            settingRepository.useImageControl.value = value
        }

    var icUser: String
        get() = settingRepository.icUser.value as String
        set(value) {
            settingRepository.icUser.value = value
        }

    var icPass: String
        get() = settingRepository.icPass.value as String
        set(value) {
            settingRepository.icPass.value = value
        }

    var icWsServer: String
        get() = settingRepository.icWsServer.value as String
        set(value) {
            settingRepository.icWsServer.value = value
        }

    var icWsNamespace: String
        get() = settingRepository.icWsNamespace.value as String
        set(value) {
            settingRepository.icWsNamespace.value = value
        }

    var icWsProxy: String
        get() = settingRepository.icWsProxy.value as String
        set(value) {
            settingRepository.icWsProxy.value = value
        }

    var icWsProxyPort: Int
        get() = settingRepository.icWsProxyPort.value as Int
        set(value) {
            settingRepository.icWsProxyPort.value = value
        }

    var icWsProxyUser: String
        get() = settingRepository.icWsProxyUser.value as String
        set(value) {
            settingRepository.icWsProxyUser.value = value
        }

    var icWsProxyPass: String
        get() = settingRepository.icWsProxyPass.value as String
        set(value) {
            settingRepository.icWsProxyPass.value = value
        }

    var icWsUseProxy: Boolean
        get() = settingRepository.icWsUseProxy.value as Boolean
        set(value) {
            settingRepository.icWsUseProxy.value = value
        }

    var icWsUser: String
        get() = settingRepository.icWsUser.value as String
        set(value) {
            settingRepository.icWsUser.value = value
        }

    var icWsPass: String
        get() = settingRepository.icWsPass.value as String
        set(value) {
            settingRepository.icWsPass.value = value
        }

    var icPhotoMaxHeightOrWidth: Int
        get() = settingRepository.icPhotoMaxHeightOrWidth.value as Int
        set(value) {
            settingRepository.icPhotoMaxHeightOrWidth.value = value
        }

    // PROXY
    var proxy: String
        get() = settingRepository.proxy.value as String
        set(value) {
            settingRepository.proxy.value = value
        }

    var useProxy: Boolean
        get() = settingRepository.useProxy.value as Boolean
        set(value) {
            settingRepository.useProxy.value = value
        }

    var proxyPort: Int
        get() = settingRepository.proxyPort.value as Int
        set(value) {
            settingRepository.proxyPort.value = value
        }

    var proxyUser: String
        get() = settingRepository.proxyUser.value as String
        set(value) {
            settingRepository.proxyUser.value = value
        }

    var proxyPass: String
        get() = settingRepository.proxyPass.value as String
        set(value) {
            settingRepository.proxyPass.value = value
        }

    var shakeOnPendingOrders: Boolean
        get() = settingRepository.shakeOnPendingOrders.value as Boolean
        set(value) {
            settingRepository.shakeOnPendingOrders.value = value
        }

    var soundOnPendingOrders: Boolean
        get() = settingRepository.soundOnPendingOrders.value as Boolean
        set(value) {
            settingRepository.soundOnPendingOrders.value = value
        }

    var sendBarcodeCheckDigit: Boolean
        get() = settingRepository.sendBarcodeCheckDigit.value as Boolean
        set(value) {
            settingRepository.sendBarcodeCheckDigit.value = value
        }

    var scanModeMovement: Int
        get() = settingRepository.scanModeMovement.value as Int
        set(value) {
            settingRepository.scanModeMovement.value = value
        }

    var scanMultiplier: Int
        get() = settingRepository.scanMultiplier.value as Int
        set(value) {
            settingRepository.scanMultiplier.value = value
        }

    var divisionChar: String
        get() = settingRepository.divisionChar.value as String
        set(value) {
            settingRepository.divisionChar.value = value
        }

    var showScannedCode: Boolean
        get() = settingRepository.showScannedCode.value as Boolean
        set(value) {
            settingRepository.showScannedCode.value = value
        }

    var scanModeCount: Int
        get() = settingRepository.scanModeCount.value as Int
        set(value) {
            settingRepository.scanModeCount.value = value
        }

    var showConfButton: Boolean
        get() = settingRepository.showConfButton.value as Boolean
        set(value) {
            settingRepository.showConfButton.value = value
        }

    var useNfc: Boolean
        get() = settingRepository.useNfc.value as Boolean
        set(value) {
            settingRepository.useNfc.value = value
        }

    var autoSend: Boolean
        get() = settingRepository.autoSend.value as Boolean
        set(value) {
            settingRepository.autoSend.value = value
        }

    var registryError: Boolean
        get() = settingRepository.registryError.value as Boolean
        set(value) {
            settingRepository.registryError.value = value
        }

    var signMandatory: Boolean
        get() = settingRepository.signMandatory.value as Boolean
        set(value) {
            settingRepository.signMandatory.value = value
        }

    var requiredDescription: Boolean
        get() = settingRepository.requiredDescription.value as Boolean
        set(value) {
            settingRepository.requiredDescription.value = value
        }

    var rfidWritePower: Int
        get() = settingRepository.rfidWritePower.value as Int
        set(value) {
            settingRepository.rfidWritePower.value = value
        }

    var rfidReadPower: Int
        get() = settingRepository.rfidReadPower.value as Int
        set(value) {
            settingRepository.rfidReadPower.value = value
        }

    var rfidShockOnRead: Boolean
        get() = settingRepository.rfidShockOnRead.value as Boolean
        set(value) {
            settingRepository.rfidShockOnRead.value = value
        }

    var rfidPlaySoundOnRead: Boolean
        get() = settingRepository.rfidPlaySoundOnRead.value as Boolean
        set(value) {
            settingRepository.rfidPlaySoundOnRead.value = value
        }

    var wcSyncInterval: Int
        get() = settingRepository.wcSyncInterval.value as Int
        set(value) {
            settingRepository.wcSyncInterval.value = value
        }

    var wcSyncRefreshOrder: Int
        get() = settingRepository.wcSyncRefreshOrder.value as Int
        set(value) {
            settingRepository.wcSyncRefreshOrder.value = value
        }

    var symbologyAztec: Boolean
        get() = settingRepository.symbologyAztec.value as Boolean
        set(value) {
            settingRepository.symbologyAztec.value = value
        }

    var symbologyCODABAR: Boolean
        get() = settingRepository.symbologyCODABAR.value as Boolean
        set(value) {
            settingRepository.symbologyCODABAR.value = value
        }

    var symbologyCode128: Boolean
        get() = settingRepository.symbologyCode128.value as Boolean
        set(value) {
            settingRepository.symbologyCode128.value = value
        }

    var symbologyCode39: Boolean
        get() = settingRepository.symbologyCode39.value as Boolean
        set(value) {
            settingRepository.symbologyCode39.value = value
        }

    var symbologyCode93: Boolean
        get() = settingRepository.symbologyCode93.value as Boolean
        set(value) {
            settingRepository.symbologyCode93.value = value
        }

    var symbologyDataMatrix: Boolean
        get() = settingRepository.symbologyDataMatrix.value as Boolean
        set(value) {
            settingRepository.symbologyDataMatrix.value = value
        }

    var symbologyEAN13: Boolean
        get() = settingRepository.symbologyEAN13.value as Boolean
        set(value) {
            settingRepository.symbologyEAN13.value = value
        }

    var symbologyEAN8: Boolean
        get() = settingRepository.symbologyEAN8.value as Boolean
        set(value) {
            settingRepository.symbologyEAN8.value = value
        }

    var symbologyITF: Boolean
        get() = settingRepository.symbologyITF.value as Boolean
        set(value) {
            settingRepository.symbologyITF.value = value
        }

    var symbologyMaxiCode: Boolean
        get() = settingRepository.symbologyMaxiCode.value as Boolean
        set(value) {
            settingRepository.symbologyMaxiCode.value = value
        }

    var symbologyPDF417: Boolean
        get() = settingRepository.symbologyPDF417.value as Boolean
        set(value) {
            settingRepository.symbologyPDF417.value = value
        }

    var symbologyQRCode: Boolean
        get() = settingRepository.symbologyQRCode.value as Boolean
        set(value) {
            settingRepository.symbologyQRCode.value = value
        }

    var symbologyRSS14: Boolean
        get() = settingRepository.symbologyRSS14.value as Boolean
        set(value) {
            settingRepository.symbologyRSS14.value = value
        }

    var symbologyRSSExpanded: Boolean
        get() = settingRepository.symbologyRSSExpanded.value as Boolean
        set(value) {
            settingRepository.symbologyRSSExpanded.value = value
        }

    var symbologyUPCA: Boolean
        get() = settingRepository.symbologyUPCA.value as Boolean
        set(value) {
            settingRepository.symbologyUPCA.value = value
        }

    var symbologyUPCE: Boolean
        get() = settingRepository.symbologyUPCE.value as Boolean
        set(value) {
            settingRepository.symbologyUPCE.value = value
        }

    var symbologyUPCEANExt: Boolean
        get() = settingRepository.symbologyUPCEANExt.value as Boolean
        set(value) {
            settingRepository.symbologyUPCEANExt.value = value
        }

    var selectItemSearchByItemCategory: Boolean
        get() = settingRepository.selectItemSearchByItemCategory.value as Boolean
        set(value) {
            settingRepository.selectItemSearchByItemCategory.value = value
        }

    var selectItemSearchByItemEan: Boolean
        get() = settingRepository.selectItemSearchByItemEan.value as Boolean
        set(value) {
            settingRepository.selectItemSearchByItemEan.value = value
        }

    var selectItemOnlyActive: Boolean
        get() = settingRepository.selectItemOnlyActive.value as Boolean
        set(value) {
            settingRepository.selectItemOnlyActive.value = value
        }

    var finishOrder: Boolean
        get() = settingRepository.finishOrder.value as Boolean
        set(value) {
            settingRepository.finishOrder.value = value
        }

    @Suppress("UNCHECKED_CAST")
    var orderRequestVisibleStatus: Set<String>
        get() = settingRepository.orderRequestVisibleStatus.value as Set<String>
        set(value) {
            settingRepository.orderRequestVisibleStatus.value = value
        }

    var flCameraPortraitLocX: Int
        get() = settingRepository.flCameraPortraitLocX.value as Int
        set(value) {
            settingRepository.flCameraPortraitLocX.value = value
        }

    var flCameraPortraitLocY: Int
        get() = settingRepository.flCameraPortraitLocY.value as Int
        set(value) {
            settingRepository.flCameraPortraitLocY.value = value
        }

    var flCameraPortraitWidth: Int
        get() = settingRepository.flCameraPortraitWidth.value as Int
        set(value) {
            settingRepository.flCameraPortraitWidth.value = value
        }

    var flCameraPortraitHeight: Int
        get() = settingRepository.flCameraPortraitHeight.value as Int
        set(value) {
            settingRepository.flCameraPortraitHeight.value = value
        }

    var flCameraLandscapeLocX: Int
        get() = settingRepository.flCameraLandscapeLocX.value as Int
        set(value) {
            settingRepository.flCameraLandscapeLocX.value = value
        }

    var flCameraLandscapeLocY: Int
        get() = settingRepository.flCameraLandscapeLocY.value as Int
        set(value) {
            settingRepository.flCameraLandscapeLocY.value = value
        }

    var flCameraLandscapeWidth: Int
        get() = settingRepository.flCameraLandscapeWidth.value as Int
        set(value) {
            settingRepository.flCameraLandscapeWidth.value = value
        }

    var flCameraLandscapeHeight: Int
        get() = settingRepository.flCameraLandscapeHeight.value as Int
        set(value) {
            settingRepository.flCameraLandscapeHeight.value = value
        }

    var flCameraContinuousMode: Boolean
        get() = settingRepository.flCameraContinuousMode.value as Boolean
        set(value) {
            settingRepository.flCameraContinuousMode.value = value
        }

    var flCameraFilterRepeatedReads: Boolean
        get() = settingRepository.flCameraFilterRepeatedReads.value as Boolean
        set(value) {
            settingRepository.flCameraFilterRepeatedReads.value = value
        }

    var selectPtlOrderShowCheckBoxes: Boolean
        get() = settingRepository.selectPtlOrderShowCheckBoxes.value as Boolean
        set(value) {
            settingRepository.selectPtlOrderShowCheckBoxes.value = value
        }

    var editItems: Boolean
        get() = settingRepository.editItems.value as Boolean
        set(value) {
            settingRepository.editItems.value = value
        }

    var linkCodeShowImages: Boolean
        get() = settingRepository.linkCodeShowImages.value as Boolean
        set(value) {
            settingRepository.linkCodeShowImages.value = value
        }

    var linkCodeShowCheckBoxes: Boolean
        get() = settingRepository.linkCodeShowCheckBoxes.value as Boolean
        set(value) {
            settingRepository.linkCodeShowCheckBoxes.value = value
        }

    var itemSelectShowImages: Boolean
        get() = settingRepository.itemSelectShowImages.value as Boolean
        set(value) {
            settingRepository.itemSelectShowImages.value = value
        }

    var itemSelectShowCheckBoxes: Boolean
        get() = settingRepository.itemSelectShowCheckBoxes.value as Boolean
        set(value) {
            settingRepository.itemSelectShowCheckBoxes.value = value
        }

    var inboxShowCheckBoxes: Boolean
        get() = settingRepository.inboxShowCheckBoxes.value as Boolean
        set(value) {
            settingRepository.inboxShowCheckBoxes.value = value
        }

    var outboxShowCheckBoxes: Boolean
        get() = settingRepository.outboxShowCheckBoxes.value as Boolean
        set(value) {
            settingRepository.outboxShowCheckBoxes.value = value
        }

    fun setupImageControl() {
        // Setup ImageControl
        com.dacosys.imageControl.Statics.appAllowScreenRotation = allowScreenRotation

        com.dacosys.imageControl.Statics.currentUserId = Statics.currentUserId
        com.dacosys.imageControl.Statics.currentUserName = Statics.currentUserName
        com.dacosys.imageControl.Statics.newInstance()

        com.dacosys.imageControl.Statics.useImageControl = useImageControl
        com.dacosys.imageControl.Statics.wsIcUrl = icWsServer
        com.dacosys.imageControl.Statics.wsIcNamespace = icWsNamespace
        com.dacosys.imageControl.Statics.wsIcProxy = icWsProxy
        com.dacosys.imageControl.Statics.wsIcProxyPort = icWsProxyPort
        com.dacosys.imageControl.Statics.wsIcUseProxy = icWsUseProxy
        com.dacosys.imageControl.Statics.wsIcProxyUser = icWsProxyUser
        com.dacosys.imageControl.Statics.wsIcProxyPass = icWsProxyPass
        com.dacosys.imageControl.Statics.icUser = icUser
        com.dacosys.imageControl.Statics.icPass = icPass
        com.dacosys.imageControl.Statics.wsIcUser = icWsUser
        com.dacosys.imageControl.Statics.wsIcPass = icWsPass
        com.dacosys.imageControl.Statics.maxHeightOrWidth = icPhotoMaxHeightOrWidth
    }
}
