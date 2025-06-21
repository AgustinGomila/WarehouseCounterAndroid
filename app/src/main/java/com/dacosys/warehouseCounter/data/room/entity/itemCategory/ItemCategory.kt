package com.dacosys.warehouseCounter.data.room.entity.itemCategory

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import com.dacosys.warehouseCounter.data.room.entity.itemCategory.ItemCategoryEntry as Entry

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
        itemCategoryId = parcel.readLong(),
        description = parcel.readString() ?: "",
        active = parcel.readInt(),
        parentId = parcel.readLong(),
        parentStr = parcel.readString() ?: ""
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

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemCategory

        return itemCategoryId == other.itemCategoryId
    }

    override fun hashCode(): Int {
        var result = itemCategoryId.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + active
        result = 31 * result + parentId.hashCode()
        result = 31 * result + parentStr.hashCode()
        return result
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
