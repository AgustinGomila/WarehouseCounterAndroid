package com.dacosys.warehouseCounter.dto.database

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DatabaseDataIntermediate {

    @SerialName(databaseTag)
    var databaseData: DatabaseData = DatabaseData()

    companion object {
        const val databaseTag = "database"
    }
}