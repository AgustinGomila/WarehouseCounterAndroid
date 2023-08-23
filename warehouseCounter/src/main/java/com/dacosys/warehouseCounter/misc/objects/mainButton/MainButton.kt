@file:Suppress("MemberVisibilityCanBePrivate")

package com.dacosys.warehouseCounter.misc.objects.mainButton

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import java.util.*

/**
 * Created by Agustin on 16/01/2017.
 */

class MainButton(mainButton: Long = 0, description: String = "", iconResource: Int = 0, visibility: Boolean = false) :
    Parcelable {
    var id: Long
    var description: String
    var iconResource: Int
    var visibility: Boolean

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        description = parcel.readString() ?: ""
        iconResource = parcel.readInt()
        visibility = parcel.readByte() != 0.toByte()
    }

    init {
        this.description = description
        this.id = mainButton
        this.iconResource = iconResource
        this.visibility = visibility
    }

    override fun toString(): String {
        return description
    }

    companion object CREATOR : Parcelable.Creator<MainButton> {
        override fun createFromParcel(parcel: Parcel): MainButton {
            return MainButton(parcel)
        }

        override fun newArray(size: Int): Array<MainButton?> {
            return arrayOfNulls(size)
        }

        var PendingCounts =
            MainButton(1, context.getString(R.string.pending_counts), R.drawable.ic_review, true)
        var CompletedCounts =
            MainButton(2, context.getString(R.string.completed_counts), R.drawable.ic_send, true)
        var NewCount =
            MainButton(3, context.getString(R.string.new_count), R.drawable.ic_new_count, true)
        var CodeRead =
            MainButton(4, context.getString(R.string.code_read), R.drawable.ic_coderead, true)
        var PtlOrder =
            MainButton(5, context.getString(R.string.ptl_order), R.drawable.ic_order, true)
        var LinkItemCodes =
            MainButton(6, context.getString(R.string.code_link), R.drawable.ic_barcode_link, true)
        var PrintLabels =
            MainButton(7, context.getString(R.string.print_code), R.drawable.ic_printer, true)
        var OrderLocationLabel =
            MainButton(8, context.getString(R.string.order_location), R.drawable.ic_order_location, true)
        var MoveOrder =
            MainButton(9, context.getString(R.string.move_order), R.drawable.ic_move_order, true)
        var UnboxingOrder =
            MainButton(10, context.getString(R.string.unboxing), R.drawable.ic_unboxing, true)
        var Configuration =
            MainButton(100, context.getString(R.string.configuration), R.drawable.ic_settings, false)

        fun getAll(): ArrayList<MainButton> {
            val allSections = ArrayList<MainButton>()
            Collections.addAll(
                allSections,
                NewCount,
                PendingCounts,
                CompletedCounts,
                CodeRead,
                PtlOrder,
                LinkItemCodes,
                PrintLabels,
                OrderLocationLabel,
                MoveOrder,
                UnboxingOrder,
                Configuration
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getById(mainButtonId: Long): MainButton? {
            return getAll().firstOrNull { it.id == mainButtonId }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(description)
        parcel.writeInt(iconResource)
        parcel.writeByte(if (visibility) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }
}
