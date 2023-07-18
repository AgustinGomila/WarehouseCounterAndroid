package com.dacosys.warehouseCounter.room.entity.orderRequest

abstract class LogEntry {
    companion object {
        const val TABLE_NAME = "log"

        const val ID = "_id"
        const val ORDER_REQUEST_ID = "order_request_id"
        const val CLIENT_ID = "client_id"
        const val USER_ID = "user_id"
        const val ITEM_ID = "item_id"
        const val ITEM_DESCRIPTION = "item_description"
        const val ITEM_CODE = "item_code"
        const val SCANNED_CODE = "scanned_code"
        const val VARIATION_QTY = "variation_qty"
        const val FINAL_QTY = "final_qty"
        const val DATE = "date"
    }
}