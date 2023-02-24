package com.dacosys.warehouseCounter.room.entity.itemCategory

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import com.dacosys.warehouseCounter.room.entity.itemCategory.ItemCategoryEntry as Entry

@Entity(
    tableName = Entry.TABLE_NAME,
    indices = [
        Index(value = [Entry.PARENT_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.PARENT_ID}"),
        Index(
            value = [Entry.DESCRIPTION], name = "IDX_${Entry.TABLE_NAME}_${Entry.DESCRIPTION}"
        ),
    ]
)
data class ItemCategory(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = Entry.ITEM_CATEGORY_ID) var itemCategoryId: Long = 0L,
    @ColumnInfo(name = Entry.DESCRIPTION) var description: String = "",
    @ColumnInfo(name = Entry.ACTIVE) var active: Int = 1,
    @ColumnInfo(name = Entry.PARENT_ID) var parentId: Long = 0,
    @ColumnInfo(name = Entry.PARENT_STR) @Ignore var parentStr: String = "",
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readLong(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(itemCategoryId)
        parcel.writeString(description)
        parcel.writeInt(active)
        parcel.writeLong(parentId)
        parcel.writeString(parentStr)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ItemCategory> {
        override fun createFromParcel(parcel: Parcel): ItemCategory {
            return ItemCategory(parcel)
        }

        override fun newArray(size: Int): Array<ItemCategory?> {
            return arrayOfNulls(size)
        }
    }
}