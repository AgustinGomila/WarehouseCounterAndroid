package com.example.warehouseCounter.misc

import com.example.warehouseCounter.WarehouseCounterApp.Companion.applicationName

class Statics {
    companion object {
        var appName: String = "${applicationName}M12"

        var downloadDbRequired = false

        val lineSeparator: String = System.lineSeparator()
        const val DATE_FORMAT: String = "yyyy-MM-dd hh:mm:ss"
        const val LIST_SEPARATOR: Char = ','

        /**
         * Modo DEMO
         */
        const val DEMO_MODE = false
        const val SUPER_DEMO_MODE = false
        const val DOWNLOAD_DB_ALWAYS = false
        const val TEST_MODE = false

        // Estos números se corresponden con package_id https://manager.example.com/package/index
        const val APP_VERSION_ID: Int = 8 // WarehouseCounter Milestone12
        const val APP_VERSION_ID_IMAGECONTROL = 13 // ImageControl Milestone13

        // Este es el valor de program_id (Ver archivo Program.cs en el proyecto Identification)
        // Lo utiliza internamente ImageControl para identificar la aplicación que lo está usando.
        // Ver: https://source.cloud.google.com/assetcontrol/libs_windows/+/master:Collector/Identification/Program.cs
        const val INTERNAL_IMAGE_CONTROL_APP_ID: Int = 4

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
