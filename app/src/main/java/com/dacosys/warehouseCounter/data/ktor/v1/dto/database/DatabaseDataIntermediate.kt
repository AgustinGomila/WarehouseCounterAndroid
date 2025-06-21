package com.dacosys.warehouseCounter.data.ktor.v1.dto.database

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DatabaseDataIntermediate {

    @SerialName(DATABASE_KEY)
    var databaseData: DatabaseData =
        DatabaseData()

    companion object {
        const val DATABASE_KEY = "database"
    }
}
