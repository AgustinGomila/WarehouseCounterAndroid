package com.dacosys.warehouseCounter.moshi.clientPackage

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Package {

    @Json(name = installationCodeTag)
    var installationCode: String = ""

    @Json(name = productVersionIdTag)
    var productVersionId: Int = -1

    @Json(name = activeTag)
    var active: Int = 0

    @Json(name = panelTag)
    var panel: Panel = Panel()

    @Json(name = clientPackageContDescTag)
    var clientPackageContDesc: String = ""

    @Json(name = wsTag)
    var ws: Ws = Ws()

    @Json(name = customOptionsTag)
    var customOptions: Map<String, String> = mapOf()

    @Json(name = clientTag)
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

@JsonClass(generateAdapter = true)
class Panel {
    @Json(name = urlTag)
    var url: String = ""

    companion object {
        const val urlTag = "url"
    }
}

@JsonClass(generateAdapter = true)
class Ws {
    @Json(name = urlTag)
    var url: String = ""

    @Json(name = namespaceTag)
    var namespace: String = ""

    @Json(name = userTag)
    var user: String = ""

    @Json(name = passwordTag)
    var password: String = ""

    companion object {
        const val urlTag = "url"
        const val namespaceTag = "namespace"
        const val userTag = "ws_user"
        const val passwordTag = "ws_password"
    }
}

