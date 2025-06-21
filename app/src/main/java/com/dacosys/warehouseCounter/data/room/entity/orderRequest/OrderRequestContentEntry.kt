package com.dacosys.warehouseCounter.data.room.entity.orderRequest

abstract class OrderRequestContentEntry {
    companion object {
        const val TABLE_NAME = "order_request_content"

        const val ORDER_REQUEST_CONTENT_ID = "_id"
        const val ORDER_REQUEST_ID = "order_request_id"
        const val ITEM_ID = "item_id"
        const val ITEM_DESCRIPTION = "item_description"
        const val ITEM_CODE_READ = "item_code_read"
        const val ITEM_EAN = "item_ean"
        const val ITEM_PRICE = "item_price"
        const val ITEM_ACTIVE = "item_active"
        const val ITEM_EXTERNAL_ID = "item_external_id"
        const val ITEM_CATEGORY_ID = "item_category_id"
        const val LOT_ENABLED = "item_lot_enabled"
        const val LOT_ID = "lot_id"
        const val LOT_CODE = "lot_code"
        const val LOT_ACTIVE = "lot_active"
        const val QTY_REQUESTED = "qty_requested"
        const val QTY_COLLECTED = "qty_collected"
    }
}
