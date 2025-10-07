package com.example.warehouseCounter.misc

import com.example.warehouseCounter.WarehouseCounterApp.Companion.applicationName
import io.github.cdimascio.dotenv.DotenvBuilder

class Statics {
    companion object {
        private const val MILESTONE = "M12"
        var appName: String = "${applicationName}${MILESTONE}"
        var downloadDbRequired = false
        val lineSeparator: String = System.lineSeparator()

        // region Variables para DEBUG/DEMO

        private val env by lazy {
            try {
                DotenvBuilder()
                    .directory("/assets")
                    .filename("env")
                    .load()
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }

        val GOD_MODE: Boolean by lazy { env?.get("ENV_GOD_MODE") == "true" }
        val DEMO_MODE: Boolean by lazy { env?.get("ENV_DEMO") == "true" }
        val TEST_MODE: Boolean by lazy { env?.get("ENV_TEST_MODE") == "true" }
        val DOWNLOAD_DB_ALWAYS: Boolean by lazy { env?.get("ENV_DOWNLOAD_DB_ALWAYS") == "true" }
        val LIST_SEPARATOR: Char by lazy { env?.get("ENV_LIST_SEPARATOR")?.first() ?: ',' }
        val DATE_FORMAT: String by lazy { env?.get("ENV_DATE_FORMAT") ?: "yyyy-MM-dd hh:mm:ss" }

        // endregion Variables para DEBUG/DEMO

        // Estos números se corresponden con package_id https://manager.example.com/package/index
        const val APP_VERSION_ID: Int = 8 // WarehouseCounter Milestone12
        const val APP_VERSION_ID_IMAGECONTROL = 13 // ImageControl Milestone13

        // Este es el valor de program_id (Ver archivo Program.cs en el proyecto Identification)
        // Lo utiliza internamente ImageControl para identificar la aplicación que lo está usando.
        // Ver: https://source.cloud.google.com/assetcontrol/libs_windows/+/master:Collector/Identification/Program.cs
        const val INTERNAL_IMAGE_CONTROL_APP_ID: Int = 4
    }
}