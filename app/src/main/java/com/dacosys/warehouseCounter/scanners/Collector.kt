package com.dacosys.warehouseCounter.scanners

import android.util.Log
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.scanners.collector.CollectorType

class Collector {
    companion object {
        // Este flag es para reinicializar el colector después de cambiar en Settings.
        var collectorTypeChanged = false

        var collectorType: CollectorType
            get() {
                return try {
                    settingsVm.collectorType
                } catch (ex: Exception) {
                    Log.e(this::class.java.simpleName, ex.message.toString())
                    settingsVm.collectorType = CollectorType.none
                    CollectorType.none
                }
            }
            set(value) {
                settingsVm.collectorType = value
            }

        fun isNfcRequired(): Boolean {
            return settingsVm.useNfc
        }
    }
}