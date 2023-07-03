package com.dacosys.warehouseCounter.misc

import android.os.Parcel
import android.os.Parcelable

class ParcelLong() : Parcelable {
    var value: Long = 0L

    constructor(value: Long) : this() {
        this.value = value
    }

    constructor(parcel: Parcel) : this() {
        value = parcel.readLong()
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeLong(value)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR :
        Parcelable.Creator<ParcelLong> {
        override fun createFromParcel(parcel: Parcel): ParcelLong {
            return ParcelLong(
                parcel
            )
        }

        override fun newArray(size: Int): Array<ParcelLong?> {
            return arrayOfNulls(size)
        }
    }
}