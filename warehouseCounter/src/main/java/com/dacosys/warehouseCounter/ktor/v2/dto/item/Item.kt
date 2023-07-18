package com.dacosys.warehouseCounter.ktor.v2.dto.item

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Item(
    @SerialName(ACTIVE_KEY) var active: Boolean = false,
    @SerialName(DESCRIPTION_KEY) var description: String = "",
    @SerialName(EAN_KEY) var ean: String = "",
    @SerialName(EXTERNAL_ID_KEY) var externalId: String = "",
    @SerialName(ID_KEY) var id: Long = 0L,
    @SerialName(ITEM_CATEGORY_ID_KEY) var itemCategoryId: Long = 0L,
    @SerialName(LOT_ENABLED_KEY) var lotEnabled: Boolean = false,
    @SerialName(PRICE_KEY) var price: Double = 0.0,
    @SerialName(ROW_CREATION_DATE_KEY) var rowCreationDate: String = "",
    @SerialName(ROW_MODIFICATION_DATE_KEY) var rowModificationDate: String = "",
    @SerialName(ITEM_CATEGORY_KEY) var itemCategory: ItemCategory? = null,
    @SerialName(PRICE_LIST_KEY) var prices: List<Price> = listOf(),
) : Parcelable {
    constructor(parcel: Parcel) : this(
        active = parcel.readByte() != 0.toByte(),
        description = parcel.readString() ?: "",
        ean = parcel.readString() ?: "",
        externalId = parcel.readString() ?: "",
        id = parcel.readLong(),
        itemCategoryId = parcel.readLong(),
        lotEnabled = parcel.readByte() != 0.toByte(),
        price = parcel.readDouble(),
        rowCreationDate = parcel.readString() ?: "",
        rowModificationDate = parcel.readString() ?: "",
        itemCategory = parcel.readParcelable(ItemCategory::class.java.classLoader),
        prices = parcel.createTypedArrayList(Price) ?: listOf()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (active) 1 else 0)
        parcel.writeString(description)
        parcel.writeString(ean)
        parcel.writeString(externalId)
        parcel.writeLong(id)
        parcel.writeLong(itemCategoryId)
        parcel.writeByte(if (lotEnabled) 1 else 0)
        parcel.writeDouble(price)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
        parcel.writeParcelable(itemCategory, flags)
        parcel.writeTypedList(prices)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Item> {
        const val ACTIVE_KEY = "active"
        const val DESCRIPTION_KEY = "description"
        const val EAN_KEY = "ean"
        const val EXTERNAL_ID_KEY = "externalId"
        const val ID_KEY = "id"
        const val ITEM_CATEGORY_ID_KEY = "itemCategoryId"
        const val LOT_ENABLED_KEY = "lotEnabled"
        const val PRICE_KEY = "price"
        const val ROW_CREATION_DATE_KEY = "rowCreationDate"
        const val ROW_MODIFICATION_DATE_KEY = "rowModificationDate"
        const val ITEM_CATEGORY_KEY = "itemCategory"
        const val PRICE_LIST_KEY = "prices"

        const val ITEM_LIST_KEY = "items"

        override fun createFromParcel(parcel: Parcel): Item {
            return Item(parcel)
        }

        override fun newArray(size: Int): Array<Item?> {
            return arrayOfNulls(size)
        }
    }
}