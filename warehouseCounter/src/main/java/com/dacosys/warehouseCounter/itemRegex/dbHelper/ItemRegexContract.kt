package com.dacosys.warehouseCounter.itemRegex.dbHelper

import android.provider.BaseColumns

/**
 * Created by Agustin on 28/12/2016.
 */

object ItemRegexContract {
    fun getAllColumns(): Array<String> {
        return arrayOf(
            ItemRegexEntry.ITEM_REGEX_ID,
            ItemRegexEntry.DESCRIPTION,
            ItemRegexEntry.REGEX,
            ItemRegexEntry.JSON_CONFIG,
            ItemRegexEntry.CODE_LENGTH,
            ItemRegexEntry.ACTIVE
        )
    }

    abstract class ItemRegexEntry : BaseColumns {
        companion object {
            const val TABLE_NAME = "item_regex"

            const val ITEM_REGEX_ID = "_id"
            const val DESCRIPTION = "description"
            const val REGEX = "regex"
            const val JSON_CONFIG = "json_config"
            const val CODE_LENGTH = "code_length"
            const val ACTIVE = "active"
        }
    }
}
