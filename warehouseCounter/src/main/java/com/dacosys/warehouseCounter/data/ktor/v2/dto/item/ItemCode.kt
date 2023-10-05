package com.dacosys.warehouseCounter.data.ktor.v2.dto.item

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemCode(
    @SerialName(CODE_KEY) var code: String = "",
    @SerialName(ID_KEY) var id: Long? = null,
    @SerialName(ITEM_ID_KEY) var itemId: Long? = null,
    @SerialName(QTY_KEY) var qty: Double? = null,
    @SerialName(CREATION_DATE_KEY) var rowCreationDate: String = "",
    @SerialName(MODIFICATION_DATE_KEY) var rowModificationDate: String = "",
    @SerialName(ITEM_KEY) var item: Item? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        code = parcel.readString() ?: "",
        id = parcel.readValue(Long::class.java.classLoader) as? Long,
        itemId = parcel.readValue(Long::class.java.classLoader) as? Long,
        qty = parcel.readValue(Double::class.java.classLoader) as? Double,
        rowCreationDate = parcel.readString() ?: "",
        rowModificationDate = parcel.readString() ?: "",
        item = parcel.readParcelable(Item::class.java.classLoader)
    )

    companion object CREATOR : Parcelable.Creator<ItemCode> {
        const val CODE_KEY = "code"
        const val ID_KEY = "id"
        const val ITEM_ID_KEY = "itemId"
        const val QTY_KEY = "qty"
        const val CREATION_DATE_KEY = "rowCreationDate"
        const val MODIFICATION_DATE_KEY = "rowModificationDate"
        const val ITEM_KEY = "item"

        const val ITEM_CODES_LIST_KEY = "itemCodes"

        override fun createFromParcel(parcel: Parcel): ItemCode {
            return ItemCode(parcel)
        }

        override fun newArray(size: Int): Array<ItemCode?> {
            return arrayOfNulls(size)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(code)
        parcel.writeValue(id)
        parcel.writeValue(itemId)
        parcel.writeValue(qty)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
        parcel.writeParcelable(item, flags)
    }

    override fun describeContents(): Int {
        return 0
    }
}
