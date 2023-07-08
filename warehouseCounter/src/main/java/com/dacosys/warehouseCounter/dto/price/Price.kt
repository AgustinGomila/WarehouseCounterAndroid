package com.dacosys.warehouseCounter.dto.price

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

@Serializable
class Price() : Parcelable {

    @SerialName(idTag)
    var id: Long = -1

    @SerialName(extIdTag)
    var extId: Long? = null

    @SerialName(itemPriceListIdTag)
    var itemPriceListId: Long = -1

    @SerialName(itemPriceListDescriptionTag)
    var itemPriceListDescription: String = ""

    @SerialName(itemPriceListListOrderTag)
    var itemPriceListListOrder: Int = 0

    @SerialName(itemIdTag)
    var itemId: Long = -1

    @SerialName(itemDescriptionTag)
    var itemDescription: String = ""

    @SerialName(priceTag)
    var price: String = ""

    @SerialName(activeTag)
    var active: Int = 1

    @SerialName(rowCreationDateTag)
    var rowCreationDate: String = ""

    @SerialName(rowModificationDateTag)
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