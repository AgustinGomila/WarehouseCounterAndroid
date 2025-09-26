package com.example.warehouseCounter.data.ktor.v2.dto.item

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class ItemCodeResponse(
    @SerialName(CODE_KEY) var code: String = "",
    @SerialName(ID_KEY) var id: Long = 0L,
    @SerialName(ITEM_ID_KEY) var itemId: Long = 0L,
    @SerialName(QTY_KEY) var qty: Double = 0.0,
    @SerialName(ROW_CREATION_DATE_KEY) var rowCreationDate: String = "",
    @SerialName(ROW_MODIFICATION_DATE_KEY) var rowModificationDate: String = "",
) : Parcelable {
    constructor(parcel: Parcel) : this(
        code = parcel.readString() ?: "",
        id = parcel.readLong(),
        itemId = parcel.readLong(),
        qty = parcel.readDouble(),
        rowCreationDate = parcel.readString() ?: "",
        rowModificationDate = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(code)
        parcel.writeLong(id)
        parcel.writeLong(itemId)
        parcel.writeDouble(qty)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ItemCodeResponse> {
        const val CODE_KEY = "code"
        const val ID_KEY = "id"
        const val ITEM_ID_KEY = "itemId"
        const val QTY_KEY = "qty"
        const val ROW_CREATION_DATE_KEY = "rowCreationDate"
        const val ROW_MODIFICATION_DATE_KEY = "rowModificationDate"

        override fun createFromParcel(parcel: Parcel): ItemCodeResponse {
            return ItemCodeResponse(parcel)
        }

        override fun newArray(size: Int): Array<ItemCodeResponse?> {
            return arrayOfNulls(size)
        }
    }
}
