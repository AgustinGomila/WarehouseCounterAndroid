package com.dacosys.warehouseCounter.ui.snackBar

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import java.util.*

class SnackBarType(
    var id: Int,
    var description: String,
    var duration: Int,
    var backColor: Int,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        description = parcel.readString() ?: "",
        duration = parcel.readInt(),
        backColor = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(description)
        parcel.writeInt(duration)
        parcel.writeInt(backColor)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return description
    }

    companion object CREATOR {
        var ERROR = SnackBarType(
            id = 0,
            description = context.getString(R.string.error),
            duration = 3500,
            backColor = R.color.firebrick,
        )
        var INFO = SnackBarType(
            id = 1,
            description = context.getString(R.string.information),
            duration = 1500,
            backColor = R.drawable.snackbar_info
        )
        var RUNNING = SnackBarType(
            id = 2,
            description = context.getString(R.string.running),
            duration = 750,
            backColor = R.drawable.snackbar_running
        )
        var SUCCESS = SnackBarType(
            3,
            context.getString(R.string.success),
            duration = 1500,
            backColor = R.drawable.snackbar_success
        )
        var ADD = SnackBarType(
            id = 4,
            description = context.getString(R.string.add),
            duration = 1000,
            backColor = R.drawable.snackbar_add
        )
        var UPDATE = SnackBarType(
            id = 5,
            description = context.getString(R.string.update),
            duration = 1000,
            backColor = R.drawable.snackbar_update
        )
        var REMOVE = SnackBarType(
            id = 6,
            description = context.getString(R.string.remove),
            duration = 1000,
            backColor = R.drawable.snackbar_remove
        )

        fun getAll(): ArrayList<SnackBarType> {
            val allSections = ArrayList<SnackBarType>()
            Collections.addAll(allSections, ERROR, INFO, RUNNING, SUCCESS, ADD, UPDATE, REMOVE)

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getById(id: Long): SnackBarType {
            return getAll().firstOrNull { it.id == id.toInt() } ?: INFO
        }
    }
}