package com.dacosys.warehouseCounter.misc

import android.content.pm.ApplicationInfo
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.applicationName
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.data.room.entity.itemCode.ItemCode

class Statics {
    companion object {
        var appName: String = "${applicationName}M12"

        // Este flag es para reinicializar el colector después de cambiar en Settings.
        var collectorTypeChanged = false

        var downloadDbRequired = false

        val lineSeparator: String = System.getProperty("line.separator") ?: "\\r\\n"
        const val DATE_FORMAT: String = "yyyy-MM-dd hh:mm:ss"

        /**
         * Modo DEMO
         */
        const val DEMO_MODE = false
        const val SUPER_DEMO_MODE = false
        const val DOWNLOAD_DB_ALWAYS = false
        const val TEST_MODE = false

        // Estos números se corresponden con package_id https://manager.dacosys.com/package/index
        const val APP_VERSION_ID: Int = 8 // WarehouseCounter Milestone12
        const val APP_VERSION_ID_IMAGECONTROL = 13 // ImageControl Milestone13

        // Este es el valor de program_id (Ver archivo Program.cs en el proyecto Identification)
        // Lo utiliza internamente ImageControl para identificar la aplicación que lo está usando.
        // Ver: https://source.cloud.google.com/assetcontrol/libs_windows/+/master:Collector/Identification/Program.cs
        const val INTERNAL_IMAGE_CONTROL_APP_ID: Int = 4

        // region Colección temporal de ItemCode
        // Reinsertar cuando se haya descargado la base de datos
        private var tempItemCodes: ArrayList<ItemCode> = ArrayList()

        fun insertItemCodes() {
            if (tempItemCodes.isEmpty()) return

            for (f in tempItemCodes) {
                if (f.code.isNullOrEmpty()) continue

                ItemCodeCoroutines.getByCode(f.code) {
                    if (!it.any()) {
                        f.toUpload = 0
                        ItemCodeCoroutines.add(f)
                    }
                }
            }

            tempItemCodes.clear()
        }
        // endregion

        @Suppress("unused")
        inline fun <reified T> toArrayList(
            classToCastTo: Class<T>,
            values: Collection<Any>,
        ): ArrayList<T> {
            val collection = ArrayList<T>()
            for (value in values) {
                collection.add(classToCastTo.cast(value)!!)
            }
            return collection
        }
    }
}
