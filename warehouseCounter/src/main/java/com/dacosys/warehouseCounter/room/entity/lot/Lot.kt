package com.dacosys.warehouseCounter.room.entity.lot

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dacosys.warehouseCounter.room.entity.lot.LotEntry as Entry

@Entity(
    tableName = Entry.TABLE_NAME,
    indices = [
        Index(value = [Entry.CODE], name = "IDX_${Entry.TABLE_NAME}_${Entry.CODE}"),
    ]
)
data class Lot(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = Entry.LOT_ID) val lotId: Long = 0L,
    @ColumnInfo(name = Entry.CODE) val code: String,
    @ColumnInfo(name = Entry.ACTIVE) val active: Int,
)