package com.dacosys.warehouseCounter.misc

import com.dacosys.imageControl.ImageControl
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import org.koin.core.context.GlobalContext

class ImageControl {
    companion object {
        val imageControl: ImageControl
            get() = GlobalContext.get().get()

        fun closeImageControl() {
            imageControl.cleanInstance()
        }

        fun setupImageControl() {
            // Setup ImageControl
            imageControl.cleanInstance()

            imageControl.userId = Statics.currentUserId
            imageControl.userName = Statics.currentUserName

            imageControl.appAllowScreenRotation = settingViewModel.allowScreenRotation
            imageControl.useImageControl = settingViewModel.useImageControl
            imageControl.wsIcUrl = settingViewModel.icWsServer
            imageControl.wsIcNamespace = settingViewModel.icWsNamespace
            imageControl.wsIcProxy = settingViewModel.icWsProxy
            imageControl.wsIcProxyPort = settingViewModel.icWsProxyPort
            imageControl.wsIcUseProxy = settingViewModel.icWsUseProxy
            imageControl.wsIcProxyUser = settingViewModel.icWsProxyUser
            imageControl.wsIcProxyPass = settingViewModel.icWsProxyPass
            imageControl.icUser = settingViewModel.icUser
            imageControl.icPass = settingViewModel.icPass
            imageControl.wsIcUser = settingViewModel.icWsUser
            imageControl.wsIcPass = settingViewModel.icWsPass
            imageControl.maxHeightOrWidth = settingViewModel.icPhotoMaxHeightOrWidth
        }
    }
}
