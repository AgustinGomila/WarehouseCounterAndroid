package com.dacosys.warehouseCounter.dto.orderRequest

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Lot() : Parcelable {
    @Json(name = "lotId")
    var lotId: Long? = null

    @Json(name = "code")
    var code: String = ""

    @Json(name = "active")
    var active: Boolean? = null

    constructor(parcel: Parcel) : this() {
        lotId = parcel.readValue(Long::class.java.classLoader) as? Long
        code = parcel.readString() ?: ""
        active = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(lotId)
        parcel.writeString(code)
        parcel.writeValue(active)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Lot

        if (lotId != other.lotId) return false

        return true
    }

    override fun hashCode(): Int {
        return lotId?.hashCode() ?: 0
    }

    companion object CREATOR : Parcelable.Creator<Lot> {
        override fun createFromParcel(parcel: Parcel): Lot {
            return Lot(parcel)
        }

        override fun newArray(size: Int): Array<Lot?> {
            return arrayOfNulls(size)
        }
    }
}
