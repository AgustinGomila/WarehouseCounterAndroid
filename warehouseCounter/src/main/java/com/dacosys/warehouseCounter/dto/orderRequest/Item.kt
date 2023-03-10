package com.dacosys.warehouseCounter.dto.orderRequest

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.dacosys.warehouseCounter.room.entity.item.Item as ItemRoom

@JsonClass(generateAdapter = true)
class Item() : Parcelable {
    @Json(name = "itemId")
    var itemId: Long? = null

    @Json(name = "itemDescription")
    var itemDescription: String = ""

    @Json(name = "codeRead")
    var codeRead: String = ""

    @Json(name = "ean")
    var ean: String = ""

    @Json(name = "price")
    var price: Double? = null

    @Json(name = "active")
    var active: Boolean? = null

    @Json(name = "externalId")
    var externalId: String? = null

    @Json(name = "itemCategoryId")
    var itemCategoryId: Long? = null

    @Json(name = "itemCategoryStr")
    var itemCategoryStr: String = ""

    @Json(name = "lotEnabled")
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

        if (itemId != other.itemId) return false

        return true
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
