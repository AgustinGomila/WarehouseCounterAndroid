package com.example.warehouseCounter.data.room.entity.itemCategory

abstract class ItemCategoryEntry {
    companion object {
        const val TABLE_NAME = "item_category"

        const val ITEM_CATEGORY_ID = "_id"
        const val DESCRIPTION = "description"
        const val ACTIVE = "active"
        const val PARENT_ID = "parent_id"

        const val PARENT_STR = "parent_str"
    }
}
