package com.dacosys.warehouseCounter.model.price

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Esta clase serializa y deserializa un Json con la
 * siguiente estructura que utiliza la API cuando se produce un error
 * al procesar diferentes solicitudes.
 *
 * {
 *     "id": 28422,
 *     "extId": null,
 *     "itemPriceListId": 2,
 *     "itemPriceListDescription": "Precio de Lista",
 *     "itemPriceListListOrder": 0,
 *     "itemId": 209300,
 *     "itemDescription": "Cinta Pvc 15plus 20mts Negro   ",
 *     "price": "479.2700",
 *     "active": 1,
 *     "creationDate": "2022-12-06 12:25:57",
 *     "modificationDate": "2022-12-06 12:25:57"
 * },
 */

@JsonClass(generateAdapter = true)
class Price() : Parcelable {

    @Json(name = idTag)
    var id: Long = -1

    @Json(name = extIdTag)
    var extId: Long? = null

    @Json(name = itemPriceListIdTag)
    var itemPriceListId: Long = -1

    @Json(name = itemPriceListDescriptionTag)
    var itemPriceListDescription: String = ""

    @Json(name = itemPriceListListOrderTag)
    var itemPriceListListOrder: Int = 0

    @Json(name = itemIdTag)
    var itemId: Long = -1

    @Json(name = itemDescriptionTag)
    var itemDescription: String = ""

    @Json(name = priceTag)
    var price: String = ""

    @Json(name = activeTag)
    var active: Int = 1

    @Json(name = rowCreationDateTag)
    var rowCreationDate: String = ""

    @Json(name = rowModificationDateTag)
    var rowModificationDate: String = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        extId = parcel.readValue(Long::class.java.classLoader) as? Long
        itemPriceListId = parcel.readLong()
        itemPriceListDescription = parcel.readString() ?: ""
        itemPriceListListOrder = parcel.readInt()
        itemId = parcel.readLong()
        itemDescription = parcel.readString() ?: ""
        price = parcel.readString() ?: ""
        active = parcel.readInt()
        rowCreationDate = parcel.readString() ?: ""
        rowModificationDate = parcel.readString() ?: ""
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeValue(extId)
        parcel.writeLong(itemPriceListId)
        parcel.writeString(itemPriceListDescription)
        parcel.writeInt(itemPriceListListOrder)
        parcel.writeLong(itemId)
        parcel.writeString(itemDescription)
        parcel.writeString(price)
        parcel.writeInt(active)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Price

        if (id != other.id) return false
        if (extId != other.extId) return false
        if (itemPriceListId != other.itemPriceListId) return false
        if (itemPriceListDescription != other.itemPriceListDescription) return false
        if (itemPriceListListOrder != other.itemPriceListListOrder) return false
        if (itemId != other.itemId) return false
        if (itemDescription != other.itemDescription) return false
        if (price != other.price) return false
        if (active != other.active) return false
        if (rowCreationDate != other.rowCreationDate) return false
        if (rowModificationDate != other.rowModificationDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (extId?.hashCode() ?: 0)
        result = 31 * result + itemPriceListId.hashCode()
        result = 31 * result + itemPriceListDescription.hashCode()
        result = 31 * result + itemPriceListListOrder
        result = 31 * result + itemId.hashCode()
        result = 31 * result + itemDescription.hashCode()
        result = 31 * result + price.hashCode()
        result = 31 * result + active
        result = 31 * result + rowCreationDate.hashCode()
        result = 31 * result + rowModificationDate.hashCode()
        return result
    }


    companion object CREATOR : Parcelable.Creator<Price> {
        override fun createFromParcel(parcel: Parcel): Price {
            return Price(parcel)
        }

        override fun newArray(size: Int): Array<Price?> {
            return arrayOfNulls(size)
        }

        /**
         * Nombre de campos para el Json de este objeto.
         */

        const val idTag = "id"
        const val extIdTag = "extId"
        const val itemPriceListIdTag = "itemPriceListId"
        const val itemPriceListDescriptionTag = "itemPriceListDescription"
        const val itemPriceListListOrderTag = "itemPriceListListOrder"
        const val itemIdTag = "itemId"
        const val itemDescriptionTag = "itemDescription"
        const val priceTag = "price"
        const val activeTag = "active"
        const val rowCreationDateTag = "rowCreationDate"
        const val rowModificationDateTag = "rowModificationDate"
    }
}