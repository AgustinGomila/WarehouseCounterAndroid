package com.dacosys.warehouseCounter.ktor.v1.dto.price

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

    @SerialName(ID_KEY)
    var id: Long = -1

    @SerialName(EXT_ID_KEY)
    var extId: Long? = null

    @SerialName(ITEM_PRICE_LIST_ID_KEY)
    var itemPriceListId: Long = -1

    @SerialName(ITEM_PRICE_LIST_DESCRIPTION_KEY)
    var itemPriceListDescription: String = ""

    @SerialName(ITEM_PRICE_LIST_LIST_ORDER_KEY)
    var itemPriceListListOrder: Int = 0

    @SerialName(ITEM_ID_KEY)
    var itemId: Long = -1

    @SerialName(ITEM_DESCRIPTION_KEY)
    var itemDescription: String = ""

    @SerialName(PRICE_KEY)
    var price: String = ""

    @SerialName(ACTIVE_KEY)
    var active: Int = 1

    @SerialName(ROW_CREATION_DATE_KEY)
    var rowCreationDate: String = ""

    @SerialName(ROW_MODIFICATION_DATE_KEY)
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

        const val ID_KEY = "id"
        const val EXT_ID_KEY = "extId"
        const val ITEM_PRICE_LIST_ID_KEY = "itemPriceListId"
        const val ITEM_PRICE_LIST_DESCRIPTION_KEY = "itemPriceListDescription"
        const val ITEM_PRICE_LIST_LIST_ORDER_KEY = "itemPriceListListOrder"
        const val ITEM_ID_KEY = "itemId"
        const val ITEM_DESCRIPTION_KEY = "itemDescription"
        const val PRICE_KEY = "price"
        const val ACTIVE_KEY = "active"
        const val ROW_CREATION_DATE_KEY = "rowCreationDate"
        const val ROW_MODIFICATION_DATE_KEY = "rowModificationDate"
    }
}