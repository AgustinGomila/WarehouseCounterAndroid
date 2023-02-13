package com.dacosys.warehouseCounter.model.orderRequest

import android.os.Parcelable
import com.google.gson.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

class OrderRequestContent() :
    Parcelable,
    JsonDeserializer<OrderRequestContent>,
    JsonSerializer<OrderRequestContent> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): OrderRequestContent {
        val jsonObject = json.asJsonObject

        val orc = OrderRequestContent()

        orc.item = context.deserialize(
            jsonObject.get("item"),
            Item::class.java
        )
        orc.lot = context.deserialize(
            jsonObject.get("lot"),
            Lot::class.java
        )
        orc.qty = context.deserialize(
            jsonObject.get("qty"),
            Qty::class.java
        )

        return orc
    }

    override fun serialize(
        src: OrderRequestContent?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?,
    ): JsonElement {
        val jsonObject = JsonObject()
        if (src != null) {
            jsonObject.add(
                "item",
                Item().serialize(
                    src.item,
                    Item::class.java,
                    context
                )
            )
            jsonObject.add(
                "lot",
                Lot().serialize(
                    src.lot,
                    Lot::class.java,
                    context
                )
            )
            jsonObject.add(
                "qty",
                Qty().serialize(
                    src.qty,
                    Qty::class.java,
                    context
                )
            )
        }

        return jsonObject
    }

    // region Variables de la clase
    @Expose
    @SerializedName("item")
    var item: Item? = null

    @Expose
    @SerializedName("lot")
    var lot: Lot? = null

    @Expose
    @SerializedName("qty")
    var qty: Qty? = null
    // endregion

    constructor(parcel: android.os.Parcel) : this() {
        item = parcel.readParcelable(Item::class.java.classLoader)
        lot = parcel.readParcelable(Lot::class.java.classLoader)
        qty = parcel.readParcelable(Qty::class.java.classLoader)
    }

    constructor(item: Item, lot: Lot?, qty: Qty) : this() {
        this.item = item
        this.lot = lot
        this.qty = qty
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            !is OrderRequestContent -> false
            null -> false
            else -> equals(this.item, other.item!!)
        }
    }

    fun equals(a: Any?, b: Any): Boolean {
        return a != null && a == b
    }

    override fun hashCode(): Int {
        return this.item!!.itemId!!.toInt()
    }

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeParcelable(item, flags)
        parcel.writeParcelable(lot, flags)
        parcel.writeParcelable(qty, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderRequestContent> {
        override fun createFromParcel(parcel: android.os.Parcel): OrderRequestContent {
            return OrderRequestContent(parcel)
        }

        override fun newArray(size: Int): Array<OrderRequestContent?> {
            return arrayOfNulls(size)
        }

        fun fromJson(json: String): OrderRequestContent? {
            // Configure GSON
            val gSon = GsonBuilder()
                .registerTypeAdapter(
                    OrderRequestContent::class.java,
                    OrderRequestContent()
                )
                .registerTypeAdapter(
                    Item::class.java,
                    Item()
                )
                .registerTypeAdapter(
                    Lot::class.java,
                    Lot()
                )
                .registerTypeAdapter(
                    Qty::class.java,
                    Qty()
                )
                .excludeFieldsWithoutExposeAnnotation()
                .setLenient()
                .create()

            return gSon.fromJson(json, OrderRequestContent::class.java)
        }

        fun toJson(or: OrderRequestContent): String {
            val gSon = GsonBuilder()
                .registerTypeAdapter(
                    OrderRequestContent::class.java,
                    OrderRequestContent()
                )
                .registerTypeAdapter(
                    Item::class.java,
                    Item()
                )
                .registerTypeAdapter(
                    Lot::class.java,
                    Lot()
                )
                .registerTypeAdapter(
                    Qty::class.java,
                    Qty()
                )
                .excludeFieldsWithoutExposeAnnotation()
                .setLenient()
                .create()

            val x = gSon.toJson(or)
            println(x)

            // Serialization
            return x
        }
    }
}
