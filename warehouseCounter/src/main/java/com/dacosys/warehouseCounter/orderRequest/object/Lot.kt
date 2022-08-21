package com.dacosys.warehouseCounter.orderRequest.`object`

import android.os.Parcelable
import com.google.gson.*
import java.lang.reflect.Type

class Lot() : Parcelable, JsonDeserializer<Lot>, JsonSerializer<Lot> {
    override fun serialize(
        src: Lot?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?,
    ): JsonElement {
        val jsonObject = JsonObject()
        if (src != null) {
            jsonObject.addProperty("lotId", src.lotId)
            jsonObject.addProperty("code", src.code)
            jsonObject.addProperty("active", src.active)
        }

        return jsonObject
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): Lot {
        val jsonObject = json.asJsonObject

        val lot = Lot()

        lot.lotId = jsonObject.get("lotId")?.asLong
        lot.code = (jsonObject.get("code")?.asString) ?: ""
        lot.active = jsonObject.get("active")?.asBoolean

        return lot
    }

    var lotId: Long? = null
    var code: String = ""
    var active: Boolean? = null

    constructor(parcel: android.os.Parcel) : this() {
        this.lotId = parcel.readValue(Long::class.java.classLoader) as? Long
        this.code = parcel.readString() ?: ""
        this.active = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
    }

    constructor(
        lotId: Long?,
        code: String,
        active: Boolean?,
    ) : this() {
        this.lotId = lotId
        this.code = code
        this.active = active
    }

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeValue(lotId)
        parcel.writeString(code)
        parcel.writeValue(active)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            !is Lot -> false
            null -> false
            else -> equals(this.lotId, other.lotId!!)
        }
    }

    fun equals(a: Any?, b: Any): Boolean {
        return a != null && a == b
    }

    override fun hashCode(): Int {
        return this.lotId!!.toInt()
    }

    companion object CREATOR : Parcelable.Creator<Lot> {
        override fun createFromParcel(parcel: android.os.Parcel): Lot {
            return Lot(parcel)
        }

        override fun newArray(size: Int): Array<Lot?> {
            return arrayOfNulls(size)
        }

        fun toLot(lots: ArrayList<Lot>): ArrayList<Lot> {
            val allLots = ArrayList<Lot>()

            for (lot in lots) {
                val temp = Lot(
                    lot.lotId,
                    lot.code,
                    lot.active
                )

                allLots.add(temp)
            }

            return allLots
        }
    }
}
