package com.example.warehouseCounter.data.ktor.v1.dto.database

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DatabaseData {

    @SerialName(DB_FILE_KEY)
    var dbFile: String = ""

    @SerialName(DB_DATA_KEY)
    var dbDate: String = ""

    companion object {
        const val DB_FILE_KEY = "db_file"
        const val DB_DATA_KEY = "db_file_date"
    }
}
