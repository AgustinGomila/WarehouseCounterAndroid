package com.dacosys.warehouseCounter.dto.clientPackage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
class Package {

    @SerialName(installationCodeTag)
    var installationCode: String = ""

    @SerialName(productVersionIdTag)
    var productVersionId: Int = -1

    @SerialName(activeTag)
    var active: Int = 0

    @SerialName(panelTag)
    var panel: Panel = Panel()

    @SerialName(clientPackageContDescTag)
    var clientPackageContDesc: String = ""

    @SerialName(wsTag)
    var ws: Ws = Ws()

    @SerialName(customOptionsTag)
    var customOptions: Map<String, JsonElement> = mapOf()

    @SerialName(clientTag)
    var client: String = ""

    companion object {
        const val installationCodeTag = "installation_code"
        const val productVersionIdTag = "product_version_id"
        const val activeTag = "active"
        const val panelTag = "panel"
        const val clientPackageContDescTag = "client_package_content_description"
        const val wsTag = "ws"
        const val customOptionsTag = "custom_options"
        const val clientTag = "client"

        // Custom options keys
        const val icUserTag = "ic_user"
        const val icPasswordTag = "ic_password"
    }
}

