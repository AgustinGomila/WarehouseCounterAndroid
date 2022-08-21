package com.dacosys.warehouseCounter.orderRequest.`object`

import android.os.Parcelable
import com.google.gson.*
import java.lang.reflect.Type

class Item() : Parcelable, JsonDeserializer<Item>, JsonSerializer<Item> {
    override fun serialize(
        src: Item?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?,
    ): JsonElement {
        val jsonObject = JsonObject()
        if (src != null) {
            jsonObject.addProperty("itemId", src.itemId)
            jsonObject.addProperty("itemDescription", src.itemDescription)
            jsonObject.addProperty("codeRead", src.codeRead)
            jsonObject.addProperty("ean", src.ean)
            jsonObject.addProperty("price", src.price)
            jsonObject.addProperty("active", src.active)
            jsonObject.addProperty("externalId", src.externalId)
            jsonObject.addProperty("itemCategoryId", src.itemCategoryId)
            jsonObject.addProperty("itemCategoryStr", src.itemCategoryStr)
            jsonObject.addProperty("lotEnabled", src.lotEnabled)
        }

        return jsonObject
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): Item {
        val jsonObject = json.asJsonObject

        val item = Item()

        item.itemId = jsonObject.get("itemId")?.asLong
        item.itemDescription = (jsonObject.get("itemDescription")?.asString) ?: ""
        item.codeRead = (jsonObject.get("codeRead")?.asString) ?: ""
        item.ean = (jsonObject.get("ean")?.asString) ?: ""
        item.price = (jsonObject.get("price")?.asDouble) ?: 0.toDouble()
        item.active = jsonObject.get("active")?.asBoolean
        item.externalId = jsonObject.get("externalId")?.asString
        item.itemCategoryId = jsonObject.get("itemCategoryId")?.asLong
        item.itemCategoryStr = (jsonObject.get("itemCategoryStr")?.asString) ?: ""
        item.lotEnabled = jsonObject.get("lotEnabled")?.asBoolean

        return item
    }

    var itemId: Long? = null
    var itemDescription: String = ""
    var codeRead: String = ""
    var ean: String = ""
    var price: Double? = null
    var active: Boolean? = null
    var externalId: String? = null
    var itemCategoryId: Long? = null
    var itemCategoryStr: String = ""
    var lotEnabled: Boolean? = null

    constructor(parcel: android.os.Parcel) : this() {
        itemId = parcel.readValue(Long::class.java.classLoader) as? Long
        itemDescription = parcel.readString() ?: ""
        codeRead = parcel.readString() ?: ""
        ean = parcel.readString() ?: ""
        price = parcel.readValue(Double::class.java.classLoader) as? Double
        active = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        externalId = parcel.readString() ?: ""
        itemCategoryId = parcel.readValue(Long::class.java.classLoader) as? Long
        itemCategoryStr = parcel.readString() ?: ""
        lotEnabled = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
    }

    constructor(item: com.dacosys.warehouseCounter.item.`object`.Item) : this() {
        itemId = item.itemId
        itemDescription = item.description
        codeRead = ""
        ean = item.ean
        price = item.price
        active = item.active
        externalId = item.externalId
        itemCategoryId = item.itemCategoryId
        itemCategoryStr = item.itemCategoryStr
        lotEnabled = item.lotEnabled
    }

    constructor(item: com.dacosys.warehouseCounter.item.`object`.Item, code: String) : this() {
        itemId = item.itemId
        itemDescription = item.description
        codeRead = code
        ean = item.ean
        price = item.price
        active = item.active
        externalId = item.externalId
        itemCategoryId = item.itemCategoryId
        itemCategoryStr = item.itemCategoryStr
        lotEnabled = item.lotEnabled
    }

    constructor(
        itemId: Long?,
        itemDescription: String,
        codeRead: String,
        ean: String,
        price: Double?,
        active: Boolean?,
        externalId: String?,
        itemCategoryId: Long?,
        itemCategoryStr: String,
        lotEnabled: Boolean?,
    ) : this() {
        this.itemId = itemId
        this.itemDescription = itemDescription
        this.codeRead = codeRead
        this.ean = ean
        this.price = price
        this.active = active
        this.externalId = externalId
        this.itemCategoryId = itemCategoryId
        this.itemCategoryStr = itemCategoryStr
        this.lotEnabled = lotEnabled
    }

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
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
        return when (other) {
            !is Item -> false
            null -> false
            else -> equals(this.itemId, other.itemId!!)
        }
    }

    fun equals(a: Any?, b: Any): Boolean {
        return a != null && a == b
    }

    override fun hashCode(): Int {
        return this.itemId!!.toInt()
    }

    companion object CREATOR : Parcelable.Creator<Item> {
        override fun createFromParcel(parcel: android.os.Parcel): Item {
            return Item(parcel)
        }

        override fun newArray(size: Int): Array<Item?> {
            return arrayOfNulls(size)
        }

        fun toItem(items: ArrayList<com.dacosys.warehouseCounter.item.`object`.Item>): ArrayList<Item> {
            val allItems = ArrayList<Item>()

            for (item in items) {
                val temp = Item(
                    item.itemId,
                    item.description,
                    "",
                    item.ean,
                    item.price,
                    item.active,
                    item.externalId,
                    item.itemCategoryId,
                    item.itemCategoryStr,
                    item.lotEnabled
                )

                allItems.add(temp)
            }

            return allItems
        }
    }
}
