package com.dacosys.warehouseCounter.data.ktor.v2.impl

import io.ktor.http.*

data class ApiFilterParam(
    var columnName: String = "",
    var value: String = "",
    var conditional: String = "",
) {
    companion object {
        fun asParameter(filter: List<ApiFilterParam>): Parameters {
            return Parameters.build {
                filter.forEach {
                    val col = it.columnName
                    val cond =
                        if (it.conditional.isNotEmpty())
                        /* Because: "Operator \"in\" requires multiple operands." */
                            if (it.conditional == ACTION_OPERATOR_IN) "[${it.conditional}][]"
                            else "[${it.conditional}]"
                        else ""
                    val value = it.value

                    if (col.isNotEmpty())
                        this.append("$ACTION_FILTER[${col}]${cond}", value)
                }
            }
        }

        private const val ACTION_FILTER = "filter"

        const val ACTION_OPERATOR_LIKE = "like"
        const val ACTION_OPERATOR_IN = "in"

        const val EXTENSION_ID = "id"
        const val EXTENSION_UUID = "uuid"

        const val EXTENSION_STATUS_ID = "status_id"

        const val EXTENSION_ITEM_CODE_CODE = "code"

        const val EXTENSION_ITEM_CATEGORY_ID = "item.item_category_id"
        const val EXTENSION_ITEM_DESCRIPTION = "item.description"
        const val EXTENSION_ITEM_EAN = "ean"
        const val EXTENSION_ITEM_EXTERNAL_ID = "item.external_id"

        const val EXTENSION_LOCATION_AREA_ID = "warehouse_area_id"
        const val EXTENSION_LOCATION_RACK_ID = "rack_id"

        const val EXTENSION_ORDER_ORDER_ID = "id"
        const val EXTENSION_ORDER_DESCRIPTION = "description"
        const val EXTENSION_ORDER_EXTERNAL_ID = "external_id"

        const val EXTENSION_ORDER_LOCATION_AREA_ID = "order_location.warehouse_area_id"
        const val EXTENSION_ORDER_LOCATION_ORDER_ID = "order_location.order_id"
        const val EXTENSION_ORDER_LOCATION_RACK_ID = "order_location.rack_id"
        const val EXTENSION_ORDER_LOCATION_EAN = "item.ean"
        const val EXTENSION_ORDER_LOCATION_ITEM_ID = "item_id"
        const val EXTENSION_ORDER_LOCATION_ITEM_DESCRIPTION = "item.description"
        const val EXTENSION_ORDER_LOCATION_ITEM_EXTERNAL_ID = "item.external_id"
        const val EXTENSION_ORDER_LOCATION_EXTERNAL_ID = "order.external_id"
    }
}
