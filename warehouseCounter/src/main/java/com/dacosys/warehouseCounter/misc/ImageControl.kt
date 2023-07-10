package com.dacosys.warehouseCounter.misc

import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel

class ImageControl {
    companion object {
        fun closeImageControl() {
            com.dacosys.imageControl.Statics.cleanInstance()
        }

        fun setupImageControl() {
            // Setup ImageControl
            com.dacosys.imageControl.Statics.appAllowScreenRotation = settingViewModel.allowScreenRotation

            com.dacosys.imageControl.Statics.currentUserId = Statics.currentUserId
            com.dacosys.imageControl.Statics.currentUserName = Statics.currentUserName
            com.dacosys.imageControl.Statics.newInstance()

            com.dacosys.imageControl.Statics.useImageControl = settingViewModel.useImageControl
            com.dacosys.imageControl.Statics.wsIcUrl = settingViewModel.icWsServer
            com.dacosys.imageControl.Statics.wsIcNamespace = settingViewModel.icWsNamespace
            com.dacosys.imageControl.Statics.wsIcProxy = settingViewModel.icWsProxy
            com.dacosys.imageControl.Statics.wsIcProxyPort = settingViewModel.icWsProxyPort
            com.dacosys.imageControl.Statics.wsIcUseProxy = settingViewModel.icWsUseProxy
            com.dacosys.imageControl.Statics.wsIcProxyUser = settingViewModel.icWsProxyUser
            com.dacosys.imageControl.Statics.wsIcProxyPass = settingViewModel.icWsProxyPass
            com.dacosys.imageControl.Statics.icUser = settingViewModel.icUser
            com.dacosys.imageControl.Statics.icPass = settingViewModel.icPass
            com.dacosys.imageControl.Statics.wsIcUser = settingViewModel.icWsUser
            com.dacosys.imageControl.Statics.wsIcPass = settingViewModel.icWsPass
            com.dacosys.imageControl.Statics.maxHeightOrWidth = settingViewModel.icPhotoMaxHeightOrWidth
        }
    }
}