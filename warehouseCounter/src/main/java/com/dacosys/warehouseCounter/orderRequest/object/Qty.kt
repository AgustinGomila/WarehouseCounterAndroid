package com.dacosys.warehouseCounter.orderRequest.`object`

import android.os.Parcelable
import com.google.gson.*
import java.lang.reflect.Type

class Qty() : Parcelable, JsonSerializer<Qty>, JsonDeserializer<Qty> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): Qty {
        val jsonObject = json.asJsonObject

        val qtyRequested = jsonObject.get("qtyRequested")?.asDouble
        val qtyCollected = jsonObject.get("qtyCollected")?.asDouble

        return Qty(qtyRequested, qtyCollected)
    }

    override fun serialize(
        src: Qty?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?,
    ): JsonElement {
        val jsonObject = JsonObject()
        if (src != null) {
            jsonObject.addProperty("qtyRequested", src.qtyRequested)
            jsonObject.addProperty("qtyCollected", src.qtyCollected)
        }

        return jsonObject
    }

    var qtyRequested: Double? = null
    var qtyCollected: Double? = null

    constructor(qtyRequested: Double?, qtyCollected: Double?) : this() {
        this.qtyRequested = qtyRequested
        this.qtyCollected = qtyCollected
    }

    constructor(parcel: android.os.Parcel) : this(
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readValue(Double::class.java.classLoader) as? Double
    )

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeValue(qtyRequested)
        parcel.writeValue(qtyCollected)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Qty> {
        override fun createFromParcel(parcel: android.os.Parcel): Qty {
            return Qty(parcel)
        }

        override fun newArray(size: Int): Array<Qty?> {
            return arrayOfNulls(size)
        }
    }
}