package com.dacosys.warehouseCounter.dto.orderRequest

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Qty() : Parcelable {
    @Json(name = "qtyRequested")
    var qtyRequested: Double? = null

    @Json(name = "qtyCollected")
    var qtyCollected: Double? = null

    constructor(parcel: Parcel) : this() {
        qtyRequested = parcel.readValue(Double::class.java.classLoader) as? Double
        qtyCollected = parcel.readValue(Double::class.java.classLoader) as? Double
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(qtyRequested)
        parcel.writeValue(qtyCollected)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Qty> {
        override fun createFromParcel(parcel: Parcel): Qty {
            return Qty(parcel)
        }

        override fun newArray(size: Int): Array<Qty?> {
            return arrayOfNulls(size)
        }
    }
}