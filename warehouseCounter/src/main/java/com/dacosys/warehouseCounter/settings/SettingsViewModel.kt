@file:Suppress("MemberVisibilityCanBePrivate")

package com.dacosys.warehouseCounter.settings

import androidx.lifecycle.ViewModel

@Suppress("unused")
class SettingsViewModel(private val store: SettingsRepository) : ViewModel() {
    var useBtPrinter: Boolean
        get() {
            return store.useBtPrinter.value as Boolean
        }
        set(value) {
            store.useBtPrinter.value = value
        }

    var confPassword: String
        get() {
            return store.confPassword.value as String
        }
        set(value) {
            store.confPassword.value = value
        }

    var printerBtAddress: String
        get() {
            return store.printerBtAddress.value as String
        }
        set(value) {
            store.printerBtAddress.value = value
        }

    var useNetPrinter: Boolean
        get() {
            return store.useNetPrinter.value as Boolean
        }
        set(value) {
            store.useNetPrinter.value = value
        }

    var ipNetPrinter: String
        get() {
            return store.ipNetPrinter.value as String
        }
        set(value) {
            store.ipNetPrinter.value = value
        }

    // Printer Port
    var portNetPrinter: Int
        get() {
            return store.portNetPrinter.value as Int
        }
        set(value) {
            store.portNetPrinter.value = value
        }

    // Line Separator
    var lineSeparator: String
        get() {
            return store.lineSeparator.value as String
        }
        set(value) {
            store.lineSeparator.value = value
        }

    // Printer Power
    var printerPower: Int
        get() {
            return store.printerPower.value as Int
        }
        set(value) {
            store.printerPower.value = value
        }

    // Printer Speed
    var printerSpeed: Int
        get() {
            return store.printerSpeed.value as Int
        }
        set(value) {
            store.printerSpeed.value = value
        }

    // Printer Column Offset
    var colOffset: Int
        get() {
            return store.colOffset.value as Int
        }
        set(value) {
            store.colOffset.value = value
        }

    // Printer Row Offset
    var rowOffset: Int
        get() {
            return store.rowOffset.value as Int
        }
        set(value) {
            store.rowOffset.value = value
        }

    // Printer Template Id
    var barcodeLabelTemplateId: Int
        get() {
            return store.barcodeLabelTemplateId.value as Int
        }
        set(value) {
            store.barcodeLabelTemplateId.value = value
        }

    // Connection Timeout
    var connectionTimeout: Int
        get() {
            return store.connectionTimeout.value as Int
        }
        set(value) {
            store.connectionTimeout.value = value
        }

    var installationCode: String
        get() {
            return store.installationCode.value as String
        }
        set(value) {
            store.installationCode.value = value
        }

    // URL Server
    var urlPanel: String
        get() {
            return store.urlPanel.value as String
        }
        set(value) {
            store.urlPanel.value = value
        }

    var clientEmail: String
        get() {
            return store.clientEmail.value as String
        }
        set(value) {
            store.clientEmail.value = value
        }

    var clientPassword: String
        get() {
            return store.clientPassword.value as String
        }
        set(value) {
            store.clientPassword.value = value
        }

    var clientPackage: String
        get() {
            return store.clientPackage.value as String
        }
        set(value) {
            store.clientPackage.value = value
        }

    // MISC
    var allowScreenRotation: Boolean
        get() {
            return store.allowScreenRotation.value as Boolean
        }
        set(value) {
            store.allowScreenRotation.value = value
        }

    var collectorType: Int
        get() {
            return store.collectorType.value as Int
        }
        set(value) {
            store.collectorType.value = value
        }

    var allowUnknownCodes: Boolean
        get() {
            return store.allowUnknownCodes.value as Boolean
        }
        set(value) {
            store.allowUnknownCodes.value = value
        }

    var rfidBtAddress: String
        get() {
            return store.rfidBtAddress.value as String
        }
        set(value) {
            store.rfidBtAddress.value = value
        }

    var useBtRfid: Boolean
        get() {
            return store.useBtRfid.value as Boolean
        }
        set(value) {
            store.useBtRfid.value = value
        }

    // IMAGE CONTROL
    var useImageControl: Boolean
        get() {
            return store.useImageControl.value as Boolean
        }
        set(value) {
            store.useImageControl.value = value
        }

    var icUser: String
        get() {
            return store.icUser.value as String
        }
        set(value) {
            store.icUser.value = value
        }

    var icPass: String
        get() {
            return store.icPass.value as String
        }
        set(value) {
            store.icPass.value = value
        }

    var icWsServer: String
        get() {
            return store.icWsServer.value as String
        }
        set(value) {
            store.icWsServer.value = value
        }

    var icWsNamespace: String
        get() {
            return store.icWsNamespace.value as String
        }
        set(value) {
            store.icWsNamespace.value = value
        }

    var icWsProxy: String
        get() {
            return store.icWsProxy.value as String
        }
        set(value) {
            store.icWsProxy.value = value
        }

    var icWsProxyPort: Int
        get() {
            return store.icWsProxyPort.value as Int
        }
        set(value) {
            store.icWsProxyPort.value = value
        }

    var icWsProxyUser: String
        get() {
            return store.icWsProxyUser.value as String
        }
        set(value) {
            store.icWsProxyUser.value = value
        }

    var icWsProxyPass: String
        get() {
            return store.icWsProxyPass.value as String
        }
        set(value) {
            store.icWsProxyPass.value = value
        }

    var icWsUseProxy: Boolean
        get() {
            return store.icWsUseProxy.value as Boolean
        }
        set(value) {
            store.icWsUseProxy.value = value
        }

    var icWsUser: String
        get() {
            return store.icWsUser.value as String
        }
        set(value) {
            store.icWsUser.value = value
        }

    var icWsPass: String
        get() {
            return store.icWsPass.value as String
        }
        set(value) {
            store.icWsPass.value = value
        }

    var icPhotoMaxHeightOrWidth: Int
        get() {
            return store.icPhotoMaxHeightOrWidth.value as Int
        }
        set(value) {
            store.icPhotoMaxHeightOrWidth.value = value
        }

    // PROXY
    var proxy: String
        get() {
            return store.proxy.value as String
        }
        set(value) {
            store.proxy.value = value
        }

    var useProxy: Boolean
        get() {
            return store.useProxy.value as Boolean
        }
        set(value) {
            store.useProxy.value = value
        }

    var proxyPort: Int
        get() {
            return store.proxyPort.value as Int
        }
        set(value) {
            store.proxyPort.value = value
        }

    var proxyUser: String
        get() {
            return store.proxyUser.value as String
        }
        set(value) {
            store.proxyUser.value = value
        }

    var proxyPass: String
        get() {
            return store.proxyPass.value as String
        }
        set(value) {
            store.proxyPass.value = value
        }

    var shakeOnPendingOrders: Boolean
        get() {
            return store.shakeOnPendingOrders.value as Boolean
        }
        set(value) {
            store.shakeOnPendingOrders.value = value
        }

    var soundOnPendingOrders: Boolean
        get() {
            return store.soundOnPendingOrders.value as Boolean
        }
        set(value) {
            store.soundOnPendingOrders.value = value
        }

    var sendBarcodeCheckDigit: Boolean
        get() {
            return store.sendBarcodeCheckDigit.value as Boolean
        }
        set(value) {
            store.sendBarcodeCheckDigit.value = value
        }

    var scanModeMovement: Int
        get() {
            return store.scanModeMovement.value as Int
        }
        set(value) {
            store.scanModeMovement.value = value
        }

    var scanMultiplier: Int
        get() {
            return store.scanMultiplier.value as Int
        }
        set(value) {
            store.scanMultiplier.value = value
        }

    var divisionChar: String
        get() {
            return store.divisionChar.value as String
        }
        set(value) {
            store.divisionChar.value = value
        }

    var showScannedCode: Boolean
        get() {
            return store.showScannedCode.value as Boolean
        }
        set(value) {
            store.showScannedCode.value = value
        }

    var scanModeCount: Int
        get() {
            return store.scanModeCount.value as Int
        }
        set(value) {
            store.scanModeCount.value = value
        }

    var showConfButton: Boolean
        get() {
            return store.showConfButton.value as Boolean
        }
        set(value) {
            store.showConfButton.value = value
        }

    var useNfc: Boolean
        get() {
            return store.useNfc.value as Boolean
        }
        set(value) {
            store.useNfc.value = value
        }

    var autoSend: Boolean
        get() {
            return store.autoSend.value as Boolean
        }
        set(value) {
            store.autoSend.value = value
        }

    var registryError: Boolean
        get() {
            return store.registryError.value as Boolean
        }
        set(value) {
            store.registryError.value = value
        }

    var signMandatory: Boolean
        get() {
            return store.signMandatory.value as Boolean
        }
        set(value) {
            store.signMandatory.value = value
        }

    var requiredDescription: Boolean
        get() {
            return store.requiredDescription.value as Boolean
        }
        set(value) {
            store.requiredDescription.value = value
        }

    var rfidWritePower: Int
        get() {
            return store.rfidWritePower.value as Int
        }
        set(value) {
            store.rfidWritePower.value = value
        }

    var rfidReadPower: Int
        get() {
            return store.rfidReadPower.value as Int
        }
        set(value) {
            store.rfidReadPower.value = value
        }

    var rfidShockOnRead: Boolean
        get() {
            return store.rfidShockOnRead.value as Boolean
        }
        set(value) {
            store.rfidShockOnRead.value = value
        }

    var rfidPlaySoundOnRead: Boolean
        get() {
            return store.rfidPlaySoundOnRead.value as Boolean
        }
        set(value) {
            store.rfidPlaySoundOnRead.value = value
        }

    var wcSyncInterval: Int
        get() {
            return store.wcSyncInterval.value as Int
        }
        set(value) {
            store.wcSyncInterval.value = value
        }

    var symbologyAztec: Boolean
        get() {
            return store.symbologyAztec.value as Boolean
        }
        set(value) {
            store.symbologyAztec.value = value
        }

    var symbologyCODABAR: Boolean
        get() {
            return store.symbologyCODABAR.value as Boolean
        }
        set(value) {
            store.symbologyCODABAR.value = value
        }

    var symbologyCode128: Boolean
        get() {
            return store.symbologyCode128.value as Boolean
        }
        set(value) {
            store.symbologyCode128.value = value
        }

    var symbologyCode39: Boolean
        get() {
            return store.symbologyCode39.value as Boolean
        }
        set(value) {
            store.symbologyCode39.value = value
        }

    var symbologyCode93: Boolean
        get() {
            return store.symbologyCode93.value as Boolean
        }
        set(value) {
            store.symbologyCode93.value = value
        }

    var symbologyDataMatrix: Boolean
        get() {
            return store.symbologyDataMatrix.value as Boolean
        }
        set(value) {
            store.symbologyDataMatrix.value = value
        }

    var symbologyEAN13: Boolean
        get() {
            return store.symbologyEAN13.value as Boolean
        }
        set(value) {
            store.symbologyEAN13.value = value
        }

    var symbologyEAN8: Boolean
        get() {
            return store.symbologyEAN8.value as Boolean
        }
        set(value) {
            store.symbologyEAN8.value = value
        }

    var symbologyITF: Boolean
        get() {
            return store.symbologyITF.value as Boolean
        }
        set(value) {
            store.symbologyITF.value = value
        }

    var symbologyMaxiCode: Boolean
        get() {
            return store.symbologyMaxiCode.value as Boolean
        }
        set(value) {
            store.symbologyMaxiCode.value = value
        }

    var symbologyPDF417: Boolean
        get() {
            return store.symbologyPDF417.value as Boolean
        }
        set(value) {
            store.symbologyPDF417.value = value
        }

    var symbologyQRCode: Boolean
        get() {
            return store.symbologyQRCode.value as Boolean
        }
        set(value) {
            store.symbologyQRCode.value = value
        }

    var symbologyRSS14: Boolean
        get() {
            return store.symbologyRSS14.value as Boolean
        }
        set(value) {
            store.symbologyRSS14.value = value
        }

    var symbologyRSSExpanded: Boolean
        get() {
            return store.symbologyRSSExpanded.value as Boolean
        }
        set(value) {
            store.symbologyRSSExpanded.value = value
        }

    var symbologyUPCA: Boolean
        get() {
            return store.symbologyUPCA.value as Boolean
        }
        set(value) {
            store.symbologyUPCA.value = value
        }

    var symbologyUPCE: Boolean
        get() {
            return store.symbologyUPCE.value as Boolean
        }
        set(value) {
            store.symbologyUPCE.value = value
        }

    var symbologyUPCEANExt: Boolean
        get() {
            return store.symbologyUPCEANExt.value as Boolean
        }
        set(value) {
            store.symbologyUPCEANExt.value = value
        }

    var selectItemSearchByItemCategory: Boolean
        get() {
            return store.selectItemSearchByItemCategory.value as Boolean
        }
        set(value) {
            store.selectItemSearchByItemCategory.value = value
        }

    var selectItemSearchByItemEan: Boolean
        get() {
            return store.selectItemSearchByItemEan.value as Boolean
        }
        set(value) {
            store.selectItemSearchByItemEan.value = value
        }

    var selectItemOnlyActive: Boolean
        get() {
            return store.selectItemOnlyActive.value as Boolean
        }
        set(value) {
            store.selectItemOnlyActive.value = value
        }

    var finishOrder: Boolean
        get() {
            return store.finishOrder.value as Boolean
        }
        set(value) {
            store.finishOrder.value = value
        }

    @Suppress("UNCHECKED_CAST")
    var orderRequestVisibleStatus: Set<String>
        get() {
            return store.orderRequestVisibleStatus.value as Set<String>
        }
        set(value) {
            store.orderRequestVisibleStatus.value = value
        }

    var flCameraPortraitLocX: Int
        get() {
            return store.flCameraPortraitLocX.value as Int
        }
        set(value) {
            store.flCameraPortraitLocX.value = value
        }

    var flCameraPortraitLocY: Int
        get() {
            return store.flCameraPortraitLocY.value as Int
        }
        set(value) {
            store.flCameraPortraitLocY.value = value
        }

    var flCameraPortraitWidth: Int
        get() {
            return store.flCameraPortraitWidth.value as Int
        }
        set(value) {
            store.flCameraPortraitWidth.value = value
        }

    var flCameraPortraitHeight: Int
        get() {
            return store.flCameraPortraitHeight.value as Int
        }
        set(value) {
            store.flCameraPortraitHeight.value = value
        }

    var flCameraLandscapeLocX: Int
        get() {
            return store.flCameraLandscapeLocX.value as Int
        }
        set(value) {
            store.flCameraLandscapeLocX.value = value
        }

    var flCameraLandscapeLocY: Int
        get() {
            return store.flCameraLandscapeLocY.value as Int
        }
        set(value) {
            store.flCameraLandscapeLocY.value = value
        }

    var flCameraLandscapeWidth: Int
        get() {
            return store.flCameraLandscapeWidth.value as Int
        }
        set(value) {
            store.flCameraLandscapeWidth.value = value
        }

    var flCameraLandscapeHeight: Int
        get() {
            return store.flCameraLandscapeHeight.value as Int
        }
        set(value) {
            store.flCameraLandscapeHeight.value = value
        }

    var flCameraContinuousMode: Boolean
        get() {
            return store.flCameraContinuousMode.value as Boolean
        }
        set(value) {
            store.flCameraContinuousMode.value = value
        }

    var flCameraFilterRepeatedReads: Boolean
        get() {
            return store.flCameraFilterRepeatedReads.value as Boolean
        }
        set(value) {
            store.flCameraFilterRepeatedReads.value = value
        }
}
