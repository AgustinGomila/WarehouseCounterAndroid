package com.example.warehouseCounter.data.room.entity.lot

abstract class LotEntry {
    companion object {
        const val TABLE_NAME = "lot"

        const val LOT_ID = "_id"
        const val CODE = "code"
        const val ACTIVE = "active"
    }
}
