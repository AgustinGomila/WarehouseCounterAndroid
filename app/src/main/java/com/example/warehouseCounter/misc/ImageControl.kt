package com.example.warehouseCounter.misc

import com.dacosys.imageControl.dto.UserAuthResult
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.WarehouseCounterApp.Companion.imageControl
import com.example.warehouseCounter.data.settings.SettingsViewModel
import com.example.warehouseCounter.misc.trust.CustomSSLContext

class ImageControl {

    companion object {
        fun closeImageControl() {
            if (ic == null) return
            ic.cleanInstance()
        }

        val ic = imageControl

        fun checkImageControlUser(): UserAuthResult? {
            if (ic == null) return null
            return ic.webservice.imageControlUserCheck()
        }

        fun setupImageControl() {
            if (ic == null) return
            val svm = SettingsViewModel()

            ic.cleanInstance()

            ic.appAllowScreenRotation = svm.allowScreenRotation

            // val currentUser = currentUser()
            // if (currentUser != null) {
            //     ic.userId = currentUser.id
            //     ic.userName = currentUser.name
            // }

            ic.useImageControl = svm.useImageControl
            // ic.wsIcUrl = svm.wsIcUrl
            // ic.wsIcNamespace = svm.wsIcNamespace
            // ic.wsIcProxy = svm.wsIcProxy
            // ic.wsIcProxyPort = svm.wsIcProxyPort
            // ic.wsIcUseProxy = svm.wsIcUseProxy
            // ic.wsIcProxyUser = svm.wsIcProxyUser
            // ic.wsIcProxyPass = svm.wsIcProxyPass
            ic.icUser = svm.icUser
            ic.icPass = svm.icPass
            // ic.wsIcUser = svm.wsIcUser
            // ic.wsIcPass = svm.wsIcPass
            // ic.maxHeightOrWidth = svm.maxHeightOrWidth
            ic.connectionTimeout = svm.connectionTimeout

            val sslContext = CustomSSLContext.createCustomSSLContext()
            ic.sslSocketFactory = sslContext?.socketFactory
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