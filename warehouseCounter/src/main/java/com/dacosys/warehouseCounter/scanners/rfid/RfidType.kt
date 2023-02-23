package com.dacosys.warehouseCounter.scanners.rfid

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class RfidType : Parcelable {
    var id: Long = 0
    var description: String = ""

    constructor(rfidTypeId: Long, description: String) {
        this.description = description
        this.id = rfidTypeId
    }

    override fun toString(): String {
        return description
    }

    constructor(parcel: Parcel) {
        id = parcel.readLong()
        description = parcel.readString() ?: ""
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RfidType> {
        override fun createFromParcel(parcel: Parcel): RfidType {
            return RfidType(parcel)
        }

        override fun newArray(size: Int): Array<RfidType?> {
            return arrayOfNulls(size)
        }

        private var none = RfidType(0, "Ninguno")
        var vh75 = RfidType(1, "VH75")

        fun getAll(): ArrayList<RfidType> {
            val allSections = ArrayList<RfidType>()
            Collections.addAll(
                allSections,
                none,
                vh75
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getById(rfidTypeId: Long): RfidType? {
            return getAll().firstOrNull { it.id == rfidTypeId }
        }
    }
}