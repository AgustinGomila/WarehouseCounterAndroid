package com.dacosys.warehouseCounter.data.room.entity.itemCode

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.ItemCode as ItemCodeKtor
import com.dacosys.warehouseCounter.data.room.entity.itemCode.ItemCodeEntry as Entry

@Entity(
    tableName = Entry.TABLE_NAME,
    indices = [
        Index(value = [Entry.ITEM_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.ITEM_ID}"),
        Index(value = [Entry.CODE], name = "IDX_${Entry.TABLE_NAME}_${Entry.CODE}"),
    ]
)
data class ItemCode(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = Entry.ID) val id: Long = 0L,
    @ColumnInfo(name = Entry.ITEM_ID) val itemId: Long? = 0L,
    @ColumnInfo(name = Entry.CODE) val code: String? = "",
    @ColumnInfo(name = Entry.QTY) val qty: Double? = null,
    @ColumnInfo(name = Entry.TO_UPLOAD) var toUpload: Int = 0,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        itemId = parcel.readValue(Long::class.java.classLoader) as? Long,
        code = parcel.readString(),
        qty = parcel.readValue(Double::class.java.classLoader) as? Double,
        toUpload = parcel.readInt()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemCode

        return id == other.id
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (itemId?.hashCode() ?: 0)
        result = 31 * result + (code?.hashCode() ?: 0)
        result = 31 * result + (qty?.hashCode() ?: 0)
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeValue(itemId)
        parcel.writeString(code)
        parcel.writeValue(qty)
        parcel.writeInt(toUpload)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun toKtor(): ItemCodeKtor {
        return ItemCodeKtor(
            id = id,
            itemId = itemId,
            code = code ?: "",
            qty = qty
        )
    }

    companion object CREATOR : Parcelable.Creator<ItemCode> {
        override fun createFromParcel(parcel: Parcel): ItemCode {
            return ItemCode(parcel)
        }

        override fun newArray(size: Int): Array<ItemCode?> {
            return arrayOfNulls(size)
        }
    }
}

