@file:Suppress("unused", "SpellCheckingInspection")

package com.dacosys.warehouseCounter.ktor.v2.impl

data class ApiActionParam(var action: String = "", var extension: Set<String> = setOf()) {

    companion object {
        /**
         * Collection of v2 API extensions and actions keys
         */

        const val ACTION_EXPAND = "expand"

        const val EXTENSION_WAREHOUSE = "warehouse"
        const val EXTENSION_PTL_LIST = "ptls"
        const val EXTENSION_WAREHOUSE_AREA = "warehouseArea"
        const val EXTENSION_WAREHOUSE_AREA_LIST = "warehouseAreas"
        const val EXTENSION_STATUS = "status"

        const val EXTENSION_ITEM_CATEGORY = "itemCategory"
        const val EXTENSION_ITEM_PRICE_LIST_CONTENTS = "itemPriceListContents"

        const val EXTENSION_ORDER_CONTENT_LIST = "orderContents"
        const val EXTENSION_ORDER_SCAN_LOG = "orderScanLogs"

        const val EXTENSION_ORDER_PACKAGE = "order"

        const val EXT_SEPARATOR = ","
    }
}

