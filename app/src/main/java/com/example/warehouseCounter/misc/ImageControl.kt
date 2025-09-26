package com.example.warehouseCounter.misc

import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.WarehouseCounterApp.Companion.imageControl
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.misc.trust.CustomSSLContext

class ImageControl {

    companion object {

        var state: Int = 0

        fun closeImageControl() {
            imageControl.cleanInstance()
        }

        fun setupImageControl() {
            imageControl.cleanInstance()

            imageControl.appAllowScreenRotation = settingsVm.allowScreenRotation

//            imageControl.userId = Statics.currentUserId.orZero()
//            imageControl.userName = Statics.currentUserName

            imageControl.useImageControl = settingsVm.useImageControl
//            imageControl.wsIcUrl = settingsVm.wsIcUrl
//            imageControl.wsIcNamespace = settingsVm.wsIcNamespace
//            imageControl.wsIcProxy = settingsVm.wsIcProxy
//            imageControl.wsIcProxyPort = settingsVm.wsIcProxyPort.toInt()
//            imageControl.wsIcUseProxy = settingsVm.wsIcUseProxy
//            imageControl.wsIcProxyUser = settingsVm.wsIcProxyUser
//            imageControl.wsIcProxyPass = settingsVm.wsIcProxyPass
//            imageControl.icUser = settingsVm.icUser
//            imageControl.icPass = settingsVm.icPass
//            imageControl.wsIcUser = settingsVm.wsIcUser
//            imageControl.wsIcPass = settingsVm.wsIcPass
//            imageControl.maxHeightOrWidth = settingsVm.maxHeightOrWidth.toInt()
            imageControl.connectionTimeout = 2000 // settingsVm.connectionTimeout

            val sslContext = CustomSSLContext.createCustomSSLContext()
            if (sslContext != null) {
                imageControl.sslSocketFactory = sslContext.socketFactory
            }
        }
    }
}

data class Table(val id: Int, val tableName: String, val description: String) {
    companion object {
        var orderRequest = Table(1, "order_request", context.getString(R.string.order_request))
        var item = Table(2, "item", context.getString(R.string.item))
        var itemCategory = Table(3, "item_category", context.getString(R.string.item_category))
        var user = Table(4, "user", context.getString(R.string.user))
        var warehouse = Table(5, "warehouse", context.getString(R.string.warehouse))
        var warehouseArea = Table(6, "warehouse_area", context.getString(R.string.area))
        var rack = Table(7, "rack", context.getString(R.string.rack))

        fun getAll(): List<Table> {
            return listOf(
                orderRequest,
                item,
                itemCategory,
                user,
                warehouse,
                warehouseArea,
                rack,
            )
        }

        fun getById(id: Int): Table? {
            return getAll().firstOrNull { it.id == id }
        }
    }
}