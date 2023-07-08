package com.dacosys.warehouseCounter.dto.database

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DatabaseData {

    @SerialName(dbFileTag)
    var dbFile: String = ""

    @SerialName(dbDataTag)
    var dbDate: String = ""

    companion object {
        const val dbFileTag = "db_file"
        const val dbDataTag = "db_file_date"
    }
}