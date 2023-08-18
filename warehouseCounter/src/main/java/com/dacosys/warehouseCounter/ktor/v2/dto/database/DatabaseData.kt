package com.dacosys.warehouseCounter.ktor.v2.dto.database

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DatabaseData(
    @SerialName(DB_FILE_KEY) var dbFile: String = "",
    @SerialName(DB_DATE_KEY) var dbDate: String = ""
) {
    companion object {
        const val DB_FILE_KEY = "dbFile"
        const val DB_DATE_KEY = "dbFileDate"
    }
}
