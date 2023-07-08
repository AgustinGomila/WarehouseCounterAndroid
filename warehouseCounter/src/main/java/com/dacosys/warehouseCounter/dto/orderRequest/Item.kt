package com.dacosys.warehouseCounter.dto.orderRequest

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.dacosys.warehouseCounter.room.entity.item.Item as ItemRoom

@Serializable
class Item() : Parcelable {
    @SerialName("itemId")
    var itemId: Long? = null

    @SerialName("itemDescription")
    var itemDescription: String = ""

    @SerialName("codeRead")
    var codeRead: String = ""

    @SerialName("ean")
    var ean: String = ""

    @SerialName("price")
    var price: Double? = null

    @SerialName("active")
    var active: Boolean? = null

    @SerialName("externalId")
    var externalId: String? = null

    @SerialName("itemCategoryId")
    var itemCategoryId: Long? = null

    @SerialName("itemCategoryStr")
    var itemCategoryStr: String = ""

    @SerialName("lotEnabled")
    var lotEnabled: Boolean? = null

    constructor(parcel: Parcel) : this() {
        itemId = parcel.readValue(Long::class.java.classLoader) as? Long
        itemDescription = parcel.readString() ?: ""
        codeRead = parcel.readString() ?: ""
        ean = parcel.readString() ?: ""
        price = parcel.readValue(Double::class.java.classLoader) as? Double
        active = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        externalId = parcel.readString()
        itemCategoryId = parcel.readValue(Long::class.java.classLoader) as? Long
        itemCategoryStr = parcel.readString() ?: ""
        lotEnabled = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
    }

    constructor(item: ItemRoom, code: String) : this() {
        itemId = item.itemId
        itemDescription = item.description
        codeRead = code
        ean = item.ean
        price = item.price?.toDouble()
        active = item.active == 1
        externalId = item.externalId
        itemCategoryId = item.itemCategoryId
        itemCategoryStr = item.itemCategoryStr
        lotEnabled = item.lotEnabled == 1
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(itemId)
        parcel.writeString(itemDescription)
        parcel.writeString(codeRead)
        parcel.writeString(ean)
        parcel.writeValue(price)
        parcel.writeValue(active)
        parcel.writeString(externalId)
        parcel.writeValue(itemCategoryId)
        parcel.writeString(itemCategoryStr)
        parcel.writeValue(lotEnabled)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Item

        return itemId == other.itemId
    }

    override fun hashCode(): Int {
        return itemId?.hashCode() ?: 0
    }

    companion object CREATOR : Parcelable.Creator<Item> {
        override fun createFromParcel(parcel: Parcel): Item {
            return Item(parcel)
        }

        override fun newArray(size: Int): Array<Item?> {
            return arrayOfNulls(size)
        }

        fun fromItemRoom(item: ItemRoom): Item {
            val temp = Item().apply {
                itemId = item.itemId
                itemDescription = item.description
                ean = item.ean
                price = item.price?.toDouble()
                active = item.active == 1
                externalId = item.externalId
                itemCategoryId = item.itemCategoryId
                itemCategoryStr = item.itemCategoryStr
                lotEnabled = item.lotEnabled == 1
            }
            return temp
        }
    }
}
