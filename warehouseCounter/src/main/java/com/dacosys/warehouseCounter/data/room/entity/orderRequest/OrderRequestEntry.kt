package com.dacosys.warehouseCounter.data.room.entity.orderRequest

abstract class OrderRequestEntry {
    companion object {
        const val TABLE_NAME = "order_request"

        const val ID = "_id"
        const val ORDER_REQUEST_ID = "order_request_id"
        const val CLIENT_ID = "client_id"
        const val COMPLETED = "completed"
        const val CREATION_DATE = "creation_date"
        const val DESCRIPTION = "description"
        const val EXTERNAL_ID = "external_id"
        const val FINISH_DATE = "finish_date"
        const val ORDER_TYPE_DESCRIPTION = "order_type_description"
        const val ORDER_TYPE_ID = "order_type_id"
        const val RESULT_ALLOW_DIFF = "result_allow_diff"
        const val RESULT_ALLOW_MOD = "result_allow_mod"
        const val RESULT_DIFF_PRODUCT = "result_diff_product"
        const val RESULT_DIFF_QTY = "result_diff_qty"
        const val START_DATE = "start_date"
        const val USER_ID = "user_id"
        const val ZONE = "zone"
    }
}
