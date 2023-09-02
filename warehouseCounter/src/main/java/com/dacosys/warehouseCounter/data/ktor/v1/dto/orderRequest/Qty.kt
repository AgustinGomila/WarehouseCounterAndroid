package com.dacosys.warehouseCounter.data.ktor.v1.dto.orderRequest

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Qty() : Parcelable {
    @SerialName("qtyRequested")
    var qtyRequested: Double? = null

    @SerialName("qtyCollected")
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
