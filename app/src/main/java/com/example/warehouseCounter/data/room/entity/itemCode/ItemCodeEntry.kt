package com.example.warehouseCounter.data.room.entity.itemCode

abstract class ItemCodeEntry {
    companion object {
        const val TABLE_NAME = "item_code"

        const val ID = "_id"
        const val ITEM_ID = "item_id"
        const val CODE = "code"
        const val QTY = "qty"
        const val TO_UPLOAD = "to_upload"
    }
}
