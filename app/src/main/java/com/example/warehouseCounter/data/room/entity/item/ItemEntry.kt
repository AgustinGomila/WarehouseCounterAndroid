package com.example.warehouseCounter.data.room.entity.item

abstract class ItemEntry {
    companion object {
        const val TABLE_NAME = "item"

        const val ITEM_ID = "_id"
        const val DESCRIPTION = "description"
        const val ACTIVE = "active"
        const val PRICE = "price"
        const val EAN = "ean"
        const val ITEM_CATEGORY_ID = "item_category_id"
        const val EXTERNAL_ID = "external_id"
        const val ITEM_CATEGORY_STR = "item_category_str"
        const val LOT_ENABLED = "lot_enabled"
    }
}
