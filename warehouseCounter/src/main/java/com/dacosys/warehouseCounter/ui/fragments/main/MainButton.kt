@file:Suppress("MemberVisibilityCanBePrivate")

package com.dacosys.warehouseCounter.ui.fragments.main

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import java.util.*

/**
 * Created by Agustin on 16/01/2017.
 */

class MainButton(
    mainButton: Long = 0,
    description: String = "",
    iconResource: Int = 0,
    visibility: Boolean = false,
    backColor: Int = 0
) :
    Parcelable {
    var id: Long
    var description: String
    var iconResource: Int
    var visibility: Boolean
    var backColor: Int

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        description = parcel.readString() ?: ""
        iconResource = parcel.readInt()
        visibility = parcel.readByte() != 0.toByte()
        backColor = parcel.readInt()
    }

    init {
        this.description = description
        this.id = mainButton
        this.iconResource = iconResource
        this.visibility = visibility
        this.backColor = backColor
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
            MainButton(
                mainButton = 1,
                description = context.getString(R.string.pending_counts),
                iconResource = R.drawable.ic_review,
                visibility = true,
                backColor = R.color.button_pending
            )
        var CompletedCounts =
            MainButton(
                mainButton = 2,
                description = context.getString(R.string.completed_counts),
                iconResource = R.drawable.ic_send,
                visibility = true,
                backColor = R.color.button_completed
            )
        var NewCount =
            MainButton(
                mainButton = 3,
                description = context.getString(R.string.new_count),
                iconResource = R.drawable.ic_new_count,
                visibility = true,
                backColor = R.color.button_new_count
            )
        var CodeRead =
            MainButton(
                mainButton = 4,
                description = context.getString(R.string.code_read),
                iconResource = R.drawable.ic_coderead,
                visibility = true,
                backColor = R.color.button_code_read
            )
        var PtlOrder =
            MainButton(
                mainButton = 5,
                description = context.getString(R.string.ptl_order),
                iconResource = R.drawable.ic_order,
                visibility = true,
                backColor = R.color.button_ptl_order
            )
        var LinkItemCodes =
            MainButton(
                mainButton = 6,
                description = context.getString(R.string.code_link),
                iconResource = R.drawable.ic_barcode_link,
                visibility = true,
                backColor = R.color.button_link_code
            )
        var PrintLabels =
            MainButton(
                mainButton = 7,
                description = context.getString(R.string.print_code),
                iconResource = R.drawable.ic_printer,
                visibility = true,
                backColor = R.color.button_print_label
            )
        var OrderLocation =
            MainButton(
                mainButton = 8,
                description = context.getString(R.string.order_location),
                iconResource = R.drawable.ic_order_location,
                visibility = true,
                backColor = R.color.button_location_order
            )
        var MoveOrder =
            MainButton(
                mainButton = 9,
                description = context.getString(R.string.move_order),
                iconResource = R.drawable.ic_move_order,
                visibility = true,
                backColor = R.color.button_move_order
            )
        var PackUnpackOrder =
            MainButton(
                mainButton = 10,
                description = context.getString(R.string.pack_unpack),
                iconResource = R.drawable.ic_unboxing,
                visibility = true,
                backColor = R.color.button_pack_unpack
            )
        var TestButton =
            MainButton(
                mainButton = 50,
                description = context.getString(R.string.test),
                iconResource = R.drawable.ic_test,
                visibility = false,
                backColor = R.color.button_test
            )
        var Configuration =
            MainButton(
                mainButton = 100,
                description = context.getString(R.string.configuration),
                iconResource = R.drawable.ic_settings,
                visibility = false,
                backColor = R.color.button_configuration
            )

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
                OrderLocation,
                MoveOrder,
                PackUnpackOrder,
                TestButton,
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
        parcel.writeInt(backColor)
    }

    override fun describeContents(): Int {
        return 0
    }
}
