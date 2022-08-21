package com.dacosys.warehouseCounter.lot.dbHelper

import android.provider.BaseColumns

/**
 * Created by Agustin on 28/12/2016.
 */

object LotContract {
    fun getAllColumns(): Array<String> {
        return arrayOf(
            LotEntry.LOT_ID,
            LotEntry.CODE,
            LotEntry.ACTIVE
        )
    }

    abstract class LotEntry : BaseColumns {
        companion object {
            const val TABLE_NAME = "lot"

            const val LOT_ID = "_id"
            const val CODE = "code"
            const val ACTIVE = "active"
        }
    }
}
