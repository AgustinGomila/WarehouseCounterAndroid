package com.dacosys.warehouseCounter.dataBase.itemCode

import android.provider.BaseColumns

/**
 * Created by Agustin on 28/12/2016.
 */

object ItemCodeContract {
    fun getAllColumns(): Array<String> {
        return arrayOf(
            ItemCodeEntry.ITEM_ID,
            ItemCodeEntry.CODE,
            ItemCodeEntry.QTY,
            ItemCodeEntry.TO_UPLOAD
        )
    }

    abstract class ItemCodeEntry : BaseColumns {
        companion object {
            const val TABLE_NAME = "item_code"

            const val ITEM_ID = "item_id"
            const val CODE = "code"
            const val QTY = "qty"
            const val TO_UPLOAD = "to_upload"
        }
    }
}
