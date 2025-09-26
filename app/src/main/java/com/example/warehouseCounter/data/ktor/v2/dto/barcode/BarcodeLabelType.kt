package com.example.warehouseCounter.data.ktor.v2.dto.barcode

import android.os.Parcel
import android.os.Parcelable
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class BarcodeLabelType(
    @SerialName(BARCODE_LABEL_TYPE_ID_KEY) var id: Long = 0,
    @SerialName(DESCRIPTION_KEY) var description: String = "",
) : Parcelable {
    constructor(parcel: Parcel) : this() {
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

    /** For fragment adapter */
    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BarcodeLabelType

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    companion object CREATOR : Parcelable.Creator<BarcodeLabelType> {
        const val BARCODE_LABEL_TYPE_ID_KEY = "barcodeLabelTypeId"
        const val DESCRIPTION_KEY = "description"

        override fun createFromParcel(parcel: Parcel): BarcodeLabelType {
            return BarcodeLabelType(parcel)
        }

        override fun newArray(size: Int): Array<BarcodeLabelType?> {
            return arrayOfNulls(size)
        }

        /*
        const LOT = 1;
        const RACK = 2;
        const WAREHOUSE_AREA = 3;
        const ORDER = 4;
        const ITEM = 5;
         */

        var notDefined = BarcodeLabelType(0, context.getString(R.string.not_defined))
        var item = BarcodeLabelType(1, context.getString(R.string.item))
        var warehouseArea = BarcodeLabelType(2, context.getString(R.string.area))
        var rack = BarcodeLabelType(3, context.getString(R.string.rack))
        var order = BarcodeLabelType(4, context.getString(R.string.order))
        var lot = BarcodeLabelType(5, context.getString(R.string.lot))

        fun getAll(): ArrayList<BarcodeLabelType> {
            val allSections = ArrayList<BarcodeLabelType>()
            Collections.addAll(
                allSections,
                item,
                warehouseArea,
                rack,
                order,
                lot
            )

            Collections.sort(allSections, CustomComparator())
            return allSections
        }

        fun getLocations(): ArrayList<BarcodeLabelType> {
            val allSections = ArrayList<BarcodeLabelType>()
            Collections.addAll(
                allSections,
                warehouseArea,
                rack,
            )

            Collections.sort(allSections, CustomComparator())
            return allSections
        }

        class CustomComparator : Comparator<BarcodeLabelType> {
            override fun compare(o1: BarcodeLabelType, o2: BarcodeLabelType): Int {
                if (o1.id < o2.id) {
                    return -1
                } else if (o1.id > o2.id) {
                    return 1
                }
                return 0
            }
        }

        private fun getAllIdAsString(): ArrayList<String> {
            val allSections = ArrayList<String>()
            Collections.addAll(
                allSections,
                item.id.toString(),
                warehouseArea.id.toString(),
                rack.id.toString(),
                order.id.toString(),
                lot.id.toString()
            )

            return ArrayList(allSections.sortedWith(compareBy { it }))
        }

        fun getAllIdAsSet(): Set<String> {
            return getAllIdAsString().toHashSet()
        }

        fun getById(typeId: Long): BarcodeLabelType {
            return getAll().firstOrNull { it.id == typeId } ?: notDefined
        }
    }
}
