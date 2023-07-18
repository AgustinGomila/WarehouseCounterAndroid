package com.dacosys.warehouseCounter.ktor.v2.dto.item


import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ItemCodePayload(
    @SerialName(QTY_KEY) var qty: Double = 0.0,
    @SerialName(ITEM_ID_KEY) var itemId: Long = 0L,
    @SerialName(CODE_KEY) var code: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        qty = parcel.readDouble(),
        itemId = parcel.readLong(),
        code = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(qty)
        parcel.writeLong(itemId)
        parcel.writeString(code)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ItemCodePayload> {
        const val QTY_KEY = "qty"
        const val ITEM_ID_KEY = "item_id"
        const val CODE_KEY = "code"

        override fun createFromParcel(parcel: Parcel): ItemCodePayload {
            return ItemCodePayload(parcel)
        }

        override fun newArray(size: Int): Array<ItemCodePayload?> {
            return arrayOfNulls(size)
        }
    }
}