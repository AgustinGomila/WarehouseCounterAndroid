package com.dacosys.warehouseCounter.dto.clientPackage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PackageResponse(@SerialName(packageTag) val packages: Map<String, Package>) {
    companion object {
        const val packageTag = "packages"
    }
}
