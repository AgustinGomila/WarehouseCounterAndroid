package com.dacosys.warehouseCounter.ui.snackBar

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import java.util.*

class SnackBarType : Parcelable {
    override fun toString(): String {
        return description
    }

    constructor(
        snackBarTypeId: Int,
        description: String,
        duration: Int,
        backColor: Int,
        foreColor: Int,
    ) {
        this.snackBarTypeId = snackBarTypeId
        this.description = description
        this.duration = duration
        this.backColor = backColor
        this.foreColor = foreColor
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(snackBarTypeId)
        parcel.writeString(description)
        parcel.writeInt(duration)
        parcel.writeInt(backColor)
        parcel.writeInt(foreColor)
    }

    override fun describeContents(): Int {
        return 0
    }

    var snackBarTypeId: Int = 0
    var description: String = ""
    var duration: Int = 0
    var backColor: Int = 0
    var foreColor: Int = 0

    constructor(parcel: Parcel) {
        snackBarTypeId = parcel.readInt()
        description = parcel.readString() ?: ""
        duration = parcel.readInt()
        backColor = parcel.readInt()
        foreColor = parcel.readInt()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SnackBarType

        return snackBarTypeId == other.snackBarTypeId
    }

    override fun hashCode(): Int {
        return snackBarTypeId.hashCode()
    }

    companion object CREATOR : Parcelable.Creator<SnackBarType> {

        var ERROR = SnackBarType(
            snackBarTypeId = 0,
            description = context.getString(R.string.error),
            duration = 3500,
            backColor = R.drawable.snackbar_error,
            foreColor = R.color.firebrick
        )
        var INFO = SnackBarType(
            snackBarTypeId = 1,
            description = context.getString(R.string.information),
            duration = 1500,
            backColor = R.drawable.snackbar_info,
            foreColor = R.color.goldenrod
        )
        var RUNNING = SnackBarType(
            snackBarTypeId = 2,
            description = context.getString(R.string.running),
            duration = 750,
            backColor = R.drawable.snackbar_running,
            foreColor = R.color.lightskyblue
        )
        var SUCCESS = SnackBarType(
            3,
            context.getString(R.string.success),
            duration = 1500,
            backColor = R.drawable.snackbar_success,
            foreColor = R.color.seagreen
        )
        var ADD = SnackBarType(
            snackBarTypeId = 4,
            description = context.getString(R.string.add),
            duration = 1000,
            backColor = R.drawable.snackbar_add,
            foreColor = R.color.cadetblue
        )
        var UPDATE = SnackBarType(
            snackBarTypeId = 5,
            description = context.getString(R.string.update),
            duration = 1000,
            backColor = R.drawable.snackbar_update,
            foreColor = R.color.steelblue
        )
        var REMOVE = SnackBarType(
            snackBarTypeId = 6,
            description = context.getString(R.string.remove),
            duration = 1000,
            backColor = R.drawable.snackbar_remove,
            foreColor = R.color.orangered
        )

        fun getAll(): ArrayList<SnackBarType> {
            val allSections = ArrayList<SnackBarType>()
            Collections.addAll(
                allSections,
                ERROR,
                INFO,
                RUNNING,
                SUCCESS,
                ADD,
                UPDATE,
                REMOVE
            )

            return ArrayList(allSections.sortedWith(compareBy { it.snackBarTypeId }))
        }

        fun getFinish(): ArrayList<SnackBarType> {
            val allSections = ArrayList<SnackBarType>()
            Collections.addAll(allSections, ERROR, SUCCESS)
            return ArrayList(allSections.sortedWith(compareBy { it.snackBarTypeId }))
        }

        fun getById(typeId: Int): SnackBarType {
            return getAll().firstOrNull { it.snackBarTypeId == typeId } ?: INFO
        }

        override fun createFromParcel(source: Parcel): SnackBarType {
            return SnackBarType(source)
        }

        override fun newArray(size: Int): Array<SnackBarType?> {
            return arrayOfNulls(size)
        }
    }
}