package com.dacosys.warehouseCounter.dto.database

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class DatabaseDataCont {

    @Json(name = databaseTag)
    var databaseData: DatabaseData = DatabaseData()

    companion object {
        const val databaseTag = "database"
    }
}

@JsonClass(generateAdapter = true)
class DatabaseData {

    @Json(name = dbFileTag)
    var dbFile: String = ""

    @Json(name = dbDataTag)
    var dbDate: String = ""

    companion object {
        const val dbFileTag = "db_file"
        const val dbDataTag = "db_file_date"
    }
}