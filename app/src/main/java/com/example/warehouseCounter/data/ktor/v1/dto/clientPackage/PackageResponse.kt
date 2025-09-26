package com.example.warehouseCounter.data.ktor.v1.dto.clientPackage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PackageResponse(
    @SerialName(PACKAGE_KEY) val packages: Map<String, Package>
) {
    companion object {
        const val PACKAGE_KEY = "packages"
    }
}
