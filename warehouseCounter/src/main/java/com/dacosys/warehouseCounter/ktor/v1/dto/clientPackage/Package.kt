package com.dacosys.warehouseCounter.ktor.v1.dto.clientPackage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
class Package {

    @SerialName(INSTALLATION_CODE_KEY)
    var installationCode: String = ""

    @SerialName(PRODUCT_VERSION_ID_KEY)
    var productVersionId: Int = -1

    @SerialName(ACTIVE_KEY)
    var active: Int = 0

    @SerialName(PANEL_KEY)
    var panel: Panel = Panel()

    @SerialName(CLIENT_PACKAGE_CONT_DESC_KEY)
    var clientPackageContDesc: String = ""

    @SerialName(WS_KEY)
    var ws: Ws = Ws()

    @SerialName(CUSTOM_OPTIONS_KEY)
    var customOptions: Map<String, JsonElement> = mapOf()

    @SerialName(CLIENT_KEY)
    var client: String = ""

    companion object {
        const val INSTALLATION_CODE_KEY = "installation_code"
        const val PRODUCT_VERSION_ID_KEY = "product_version_id"
        const val ACTIVE_KEY = "active"
        const val PANEL_KEY = "panel"
        const val CLIENT_PACKAGE_CONT_DESC_KEY = "client_package_content_description"
        const val WS_KEY = "ws"
        const val CUSTOM_OPTIONS_KEY = "custom_options"
        const val CLIENT_KEY = "client"

        // Custom options keys
        const val IC_USER_KEY = "ic_user"
        const val IC_PASSWORD_KEY = "ic_password"
    }
}