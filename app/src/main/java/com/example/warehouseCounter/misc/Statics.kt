package com.example.warehouseCounter.misc

import com.example.warehouseCounter.WarehouseCounterApp.Companion.applicationName
import io.github.cdimascio.dotenv.DotenvBuilder

class Statics {
    companion object {
        var appName: String = "${applicationName}M12"

        var downloadDbRequired = false

        val lineSeparator: String = System.lineSeparator()
        const val DATE_FORMAT: String = "yyyy-MM-dd hh:mm:ss"
        const val LIST_SEPARATOR: Char = ','

        // region Variables para DEBUG/DEMO

        val GOD_MODE: Boolean =
            try {
                val env = DotenvBuilder()
                    .directory("/assets")
                    .filename("env")
                    .load()

                env["ENV_GOD_MODE"] == "true"
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            }

        val DEMO_MODE: Boolean =
            try {
                val env = DotenvBuilder()
                    .directory("/assets")
                    .filename("env")
                    .load()

                env["ENV_DEMO"] == "true"
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            }

        val TEST_MODE: Boolean =
            try {
                val env = DotenvBuilder()
                    .directory("/assets")
                    .filename("env")
                    .load()

                env["ENV_TEST"] == "true"
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            }

        val DOWNLOAD_DB_ALWAYS: Boolean =
            try {
                val env = DotenvBuilder()
                    .directory("/assets")
                    .filename("env")
                    .load()

                env["ENV_DOWNLOAD_DB_ALWAYS"] == "true"
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            }

        // endregion Variables para DEBUG/DEMO

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