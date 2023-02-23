package com.dacosys.warehouseCounter.room.entity.item

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import androidx.room.ColumnInfo.NOCASE
import com.dacosys.warehouseCounter.room.entity.item.ItemEntry as Entry

@Entity(
    tableName = Entry.TABLE_NAME,
    indices = [
        Index(value = [Entry.EAN], name = "IDX_${Entry.TABLE_NAME}_${Entry.EAN}"),
        Index(
            value = [Entry.ITEM_CATEGORY_ID],
            name = "IDX_${Entry.TABLE_NAME}_${Entry.ITEM_CATEGORY_ID}"
        ),
        Index(value = [Entry.EXTERNAL_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.EXTERNAL_ID}"),
        Index(value = [Entry.ACTIVE], name = "IDX_${Entry.TABLE_NAME}_${Entry.ACTIVE}"),
    ]
)
data class Item(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = Entry.ITEM_ID) var itemId: Long = 0L,
    @ColumnInfo(name = Entry.DESCRIPTION) var description: String = "",
    @ColumnInfo(name = Entry.ACTIVE) var active: Int = 1,
    @ColumnInfo(name = Entry.PRICE) var price: Float? = 0f,
    @ColumnInfo(name = Entry.EAN, collate = NOCASE) var ean: String = "",
    @ColumnInfo(name = Entry.ITEM_CATEGORY_ID) var itemCategoryId: Long = 1L,
    @ColumnInfo(name = Entry.EXTERNAL_ID) var externalId: String? = null,
    @ColumnInfo(name = Entry.LOT_ENABLED, defaultValue = "0") var lotEnabled: Int = 0,
    @ColumnInfo(name = Entry.ITEM_CATEGORY_STR) @Ignore var itemCategoryStr: String = "",
) : Parcelable {
    constructor(parcel: Parcel) : this(
        itemId = parcel.readLong(),
        description = parcel.readString() ?: "",
        active = parcel.readInt(),
        price = parcel.readValue(Float::class.java.classLoader) as? Float,
        ean = parcel.readString() ?: "",
        itemCategoryId = parcel.readLong(),
        externalId = parcel.readString(),
        lotEnabled = parcel.readInt(),
        itemCategoryStr = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(itemId)
        parcel.writeString(description)
        parcel.writeInt(active)
        parcel.writeValue(price)
        parcel.writeString(ean)
        parcel.writeLong(itemCategoryId)
        parcel.writeString(externalId)
        parcel.writeInt(lotEnabled)
        parcel.writeString(itemCategoryStr)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Item> {
        override fun createFromParcel(parcel: Parcel): Item {
            return Item(parcel)
        }

        override fun newArray(size: Int): Array<Item?> {
            return arrayOfNulls(size)
        }
    }
}