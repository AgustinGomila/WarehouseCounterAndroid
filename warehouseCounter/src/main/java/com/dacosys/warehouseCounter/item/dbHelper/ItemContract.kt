package com.dacosys.warehouseCounter.item.dbHelper

import android.provider.BaseColumns

/**
 * Created by Agustin on 28/12/2016.
 */

object ItemContract {
    fun getAllColumns(): Array<String> {
        return arrayOf(
            ItemEntry.ITEM_ID,
            ItemEntry.DESCRIPTION,
            ItemEntry.ACTIVE,
            ItemEntry.PRICE,
            ItemEntry.EAN,
            ItemEntry.ITEM_CATEGORY_ID,
            ItemEntry.EXTERNAL_ID,
            ItemEntry.ITEM_CATEGORY_STR,
            ItemEntry.LOT_ENABLED
        )
    }

    abstract class ItemEntry : BaseColumns {
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
}
