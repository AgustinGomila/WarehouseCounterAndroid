package com.dacosys.warehouseCounter.misc.objects.status

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConfirmStatus

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }

    companion object CREATOR : Parcelable.Creator<ConfirmStatus> {
        override fun createFromParcel(parcel: Parcel): ConfirmStatus {
            return ConfirmStatus(parcel)
        }

        override fun newArray(size: Int): Array<ConfirmStatus?> {
            return arrayOfNulls(size)
        }

        var cancel = ConfirmStatus(0, context().getString(R.string.cancel))
        var modify = ConfirmStatus(1, context().getString(R.string.modify))
        var confirm = ConfirmStatus(2, context().getString(R.string.confirm))

        fun getAll(): ArrayList<ConfirmStatus> {
            val allSections = ArrayList<ConfirmStatus>()
            Collections.addAll(allSections, cancel, modify, confirm)

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getById(confirmStatusId: Int): ConfirmStatus? {
            return getAll().firstOrNull { it.id == confirmStatusId }
        }
    }
}