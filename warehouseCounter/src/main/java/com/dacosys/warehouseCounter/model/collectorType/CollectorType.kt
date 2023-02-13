package com.dacosys.warehouseCounter.model.collectorType

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class CollectorType : Parcelable {
    var id: Int = 0
    var description: String = ""

    constructor(collectorTypeId: Int, description: String) {
        this.description = description
        this.id = collectorTypeId
    }

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is CollectorType) {
            false
        } else this.id == other.id
    }

    override fun hashCode(): Int {
        return this.id.hashCode()
    }

    constructor(parcel: Parcel) {
        id = parcel.readInt()
        description = parcel.readString() ?: ""
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        with(parcel) {
            writeInt(id)
            writeString(description)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CollectorType> {
        override fun createFromParcel(parcel: Parcel): CollectorType {
            return CollectorType(parcel)
        }

        override fun newArray(size: Int): Array<CollectorType?> {
            return arrayOfNulls(size)
        }

        var none = CollectorType(0, "No configurado")
        var honeywell = CollectorType(1, "Honeywell")
        var honeywellNative = CollectorType(2, "Honeywell (nativo)")
        var zebra = CollectorType(3, "Zebra")

        fun getAll(): ArrayList<CollectorType> {
            val allSections = ArrayList<CollectorType>()
            Collections.addAll(
                allSections,
                none,
                honeywell,
                honeywellNative,
                zebra
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getById(collectorTypeId: Int): CollectorType {
            return getAll().firstOrNull { it.id == collectorTypeId } ?: none
        }
    }
}