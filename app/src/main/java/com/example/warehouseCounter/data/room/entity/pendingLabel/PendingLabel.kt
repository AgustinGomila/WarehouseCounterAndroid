package com.example.warehouseCounter.data.room.entity.pendingLabel

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = PendingLabelEntry.TABLE_NAME)
data class PendingLabel(
    @PrimaryKey @ColumnInfo(name = PendingLabelEntry.ID) var id: Long = 0L
) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readLong())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PendingLabel> {
        override fun createFromParcel(parcel: Parcel): PendingLabel {
            return PendingLabel(parcel)
        }

        override fun newArray(size: Int): Array<PendingLabel?> {
            return arrayOfNulls(size)
        }
    }
}

