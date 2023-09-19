package com.dacosys.warehouseCounter.misc

import com.dacosys.imageControl.ImageControl
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import org.koin.core.context.GlobalContext

class ImageControl {
    companion object {
        val imageControl: ImageControl
            get() = GlobalContext.get().get()

        fun closeImageControl() {
            imageControl.cleanInstance()
        }

        fun setupImageControl() {
            imageControl.cleanInstance()

            imageControl.userId = Statics.currentUserId
            imageControl.userName = Statics.currentUserName

            imageControl.appAllowScreenRotation = settingsVm.allowScreenRotation
            imageControl.useImageControl = settingsVm.useImageControl
            imageControl.wsIcUrl = settingsVm.icWsServer
            imageControl.wsIcNamespace = settingsVm.icWsNamespace
            imageControl.wsIcProxy = settingsVm.icWsProxy
            imageControl.wsIcProxyPort = settingsVm.icWsProxyPort
            imageControl.wsIcUseProxy = settingsVm.icWsUseProxy
            imageControl.wsIcProxyUser = settingsVm.icWsProxyUser
            imageControl.wsIcProxyPass = settingsVm.icWsProxyPass
            imageControl.icUser = settingsVm.icUser
            imageControl.icPass = settingsVm.icPass
            imageControl.wsIcUser = settingsVm.icWsUser
            imageControl.wsIcPass = settingsVm.icWsPass
            imageControl.maxHeightOrWidth = settingsVm.icPhotoMaxHeightOrWidth
        }
    }
}
