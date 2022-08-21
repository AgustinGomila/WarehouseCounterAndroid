package com.dacosys.warehouseCounter.confirmStatus

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import java.util.*

class ConfirmStatus : Parcelable {
    var id: Int = 0
    var description: String = ""

    constructor(confirmStatusId: Int, description: String) {
        this.description = description
        this.id = confirmStatusId
    }

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is ConfirmStatus) {
            false
        } else this.id == other.id
    }

    override fun hashCode(): Int {
        return this.id
    }

    constructor(parcel: Parcel) {
        id = parcel.readInt()
        description = parcel.readString() ?: ""
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ConfirmStatus> {
        override fun createFromParcel(parcel: Parcel): ConfirmStatus {
            return ConfirmStatus(parcel)
        }

        override fun newArray(size: Int): Array<ConfirmStatus?> {
            return arrayOfNulls(size)
        }

        var cancel =
            ConfirmStatus(0, Statics.WarehouseCounter.getContext().getString(R.string.cancel))
        var modify =
            ConfirmStatus(1, Statics.WarehouseCounter.getContext().getString(R.string.modify))
        var confirm =
            ConfirmStatus(2, Statics.WarehouseCounter.getContext().getString(R.string.confirm))

        fun getAll(): ArrayList<ConfirmStatus> {
            val allSections = ArrayList<ConfirmStatus>()
            Collections.addAll(
                allSections,
                cancel,
                modify,
                confirm
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getById(confirmStatusId: Int): ConfirmStatus? {
            return getAll().firstOrNull { it.id == confirmStatusId }
        }
    }
}