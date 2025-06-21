package com.dacosys.warehouseCounter.data.room.entity.itemRegex

abstract class ItemRegexEntry {
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
