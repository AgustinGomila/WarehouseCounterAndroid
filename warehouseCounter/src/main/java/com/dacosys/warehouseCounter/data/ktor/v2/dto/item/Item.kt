package com.dacosys.warehouseCounter.data.ktor.v2.dto.item

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestContent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.dacosys.warehouseCounter.data.room.entity.item.Item as ItemRoom

@Serializable
data class Item(
    @SerialName(ACTIVE_KEY) var active: Boolean = false,
    @SerialName(DESCRIPTION_KEY) var description: String = "",
    @SerialName(EAN_KEY) var ean: String = "",
    @SerialName(EXTERNAL_ID_KEY) var externalId: String? = null,
    @SerialName(ID_KEY) var id: Long = 0L,
    @SerialName(UUID_KEY) var uuid: String = "",
    @SerialName(ITEM_CATEGORY_ID_KEY) var itemCategoryId: Long? = null,
    @SerialName(LOT_ENABLED_KEY) var lotEnabled: Boolean = false,
    @SerialName(PRICE_KEY) var price: Double = 0.0,
    @SerialName(ROW_CREATION_DATE_KEY) var rowCreationDate: String = "",
    @SerialName(ROW_MODIFICATION_DATE_KEY) var rowModificationDate: String = "",
    @SerialName(ITEM_CATEGORY_KEY) var itemCategory: ItemCategory? = null,
    @SerialName(PRICE_LIST_KEY) var prices: List<Price>? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        active = parcel.readByte() != 0.toByte(),
        description = parcel.readString() ?: "",
        ean = parcel.readString() ?: "",
        externalId = parcel.readString() ?: "",
        id = parcel.readLong(),
        uuid = parcel.readString() ?: "",
        itemCategoryId = parcel.readLong(),
        lotEnabled = parcel.readByte() != 0.toByte(),
        price = parcel.readDouble(),
        rowCreationDate = parcel.readString() ?: "",
        rowModificationDate = parcel.readString() ?: "",
        itemCategory = parcel.readParcelable(ItemCategory::class.java.classLoader),

        prices = parcel.createTypedArrayList(Price)?.toList() ?: listOf()
    )

    fun toRoom(): ItemRoom {
        return ItemRoom(
            itemId = id,
            description = description,
            active = if (active) 1 else 0,
            price = price.toFloat(),
            ean = ean,
            itemCategoryId = itemCategoryId ?: 0L,
            externalId = externalId,
            lotEnabled = if (lotEnabled) 1 else 0,
            itemCategoryStr = itemCategory?.description ?: "",
        )
    }

    fun toOrderRequestContent(scannedCode: String): OrderRequestContent {
        return OrderRequestContent(
            codeRead = scannedCode,
            ean = ean,
            externalId = externalId,
            itemActive = active,
            itemCategoryId = itemCategoryId,
            itemDescription = description,
            itemId = id,
            lotActive = lotEnabled,
            lotCode = "",
            lotEnabled = lotEnabled,
            lotId = 0L,
            price = price,
            qtyCollected = 0.0,
            qtyRequested = 0.0,
        )
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (active) 1 else 0)
        parcel.writeString(description)
        parcel.writeString(ean)
        parcel.writeString(externalId)
        parcel.writeLong(id)
        parcel.writeString(uuid)
        parcel.writeValue(itemCategoryId)
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
        const val UUID_KEY = "uuid"
        const val ITEM_CATEGORY_ID_KEY = "itemCategoryId"
        const val LOT_ENABLED_KEY = "lotEnabled"
        const val PRICE_KEY = "price"
        const val ROW_CREATION_DATE_KEY = "rowCreationDate"
        const val ROW_MODIFICATION_DATE_KEY = "rowModificationDate"
        const val ITEM_CATEGORY_KEY = "itemCategory"

        const val PRICE_LIST_KEY = "itemPriceListContents"
        const val ITEM_LIST_KEY = "items"

        override fun createFromParcel(parcel: Parcel): Item {
            return Item(parcel)
        }

        override fun newArray(size: Int): Array<Item?> {
            return arrayOfNulls(size)
        }
    }
}
