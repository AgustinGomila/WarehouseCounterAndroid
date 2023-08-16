package com.dacosys.warehouseCounter.ktor.v2.impl

data class ApiFilterParam(
    var columnName: String = "",
    var value: String = "",
    var like: Boolean = false,
) {
    companion object {
        const val ACTION_FILTER = "filter"
        const val ACTION_FILTER_LIKE = "like"

       const val EXTENSION_PAGE_NUMBER = "pageNum"

        const val EXTENSION_ORDER_ID = "order_id"
        const val EXTENSION_ORDER_EXTERNAL_ID = "order.external_id"
        const val EXTENSION_ITEM_ID = "item_id"
        const val EXTENSION_ITEM_EXTERNAL_ID = "item.external_id"
        const val EXTENSION_ITEM_DESCRIPTION = "item.description"
        const val EXTENSION_ITEM_CODE = "item.code"
        const val EXTENSION_ITEM_EAN = "item.ean"
        const val EXTENSION_ORDER_LOCATION_RACK_ID = "order_location.rack_id"
        const val EXTENSION_ORDER_LOCATION_AREA_ID = "order_location.warehouse_area_id"
        const val EXTENSION_ORDER_LOCATION_WAREHOUSE_ID = "order_location.warehouse_id"


    }
}
