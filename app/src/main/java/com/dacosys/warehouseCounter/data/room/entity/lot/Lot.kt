package com.dacosys.warehouseCounter.data.room.entity.lot

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dacosys.warehouseCounter.data.room.entity.lot.LotEntry as Entry

@Entity(
    tableName = Entry.TABLE_NAME,
    indices = [
        Index(value = [Entry.CODE], name = "IDX_${Entry.TABLE_NAME}_${Entry.CODE}"),
    ]
)
data class Lot(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = Entry.LOT_ID) var lotId: Long = 0L,
    @ColumnInfo(name = Entry.CODE) var code: String = "",
    @ColumnInfo(name = Entry.ACTIVE) var active: Int = 1,
)
