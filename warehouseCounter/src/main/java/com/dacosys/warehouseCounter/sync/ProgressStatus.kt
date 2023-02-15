package com.dacosys.warehouseCounter.sync

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import java.util.*

class ProgressStatus : Parcelable {
    var id: Int = 0
    var description: String = ""

    constructor(ProgressStatusId: Int, description: String) {
        this.description = description
        this.id = ProgressStatusId
    }

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is ProgressStatus) {
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

    companion object CREATOR : Parcelable.Creator<ProgressStatus> {
        override fun createFromParcel(parcel: Parcel): ProgressStatus {
            return ProgressStatus(parcel)
        }

        override fun newArray(size: Int): Array<ProgressStatus?> {
            return arrayOfNulls(size)
        }

        var unknown = ProgressStatus(0, context().getString(R.string.unknown))
        var starting = ProgressStatus(1, context().getString(R.string.starting))
        var running = ProgressStatus(2, context().getString(R.string.running))
        var success = ProgressStatus(3, context().getString(R.string.success))
        var canceled = ProgressStatus(4, context().getString(R.string.canceled))
        var crashed = ProgressStatus(5, context().getString(R.string.crashed))
        var finished = ProgressStatus(6, context().getString(R.string.finished))

        fun getAll(): ArrayList<ProgressStatus> {
            val allSections = ArrayList<ProgressStatus>()
            Collections.addAll(
                allSections, unknown, starting, running, success, canceled, crashed, finished
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getEnd(): ArrayList<ProgressStatus> {
            val allSections = ArrayList<ProgressStatus>()
            Collections.addAll(
                allSections, success, canceled, crashed, finished
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getById(ProgressStatusId: Int): ProgressStatus? {
            return getAll().firstOrNull { it.id == ProgressStatusId }
        }
    }
}