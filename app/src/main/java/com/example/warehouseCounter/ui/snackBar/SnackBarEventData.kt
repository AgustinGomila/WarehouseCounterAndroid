package com.example.warehouseCounter.ui.snackBar

import android.os.Parcel
import android.os.Parcelable

data class SnackBarEventData(
    val text: String = "",
    val snackBarType: Int = -1,
) : Parcelable {
    constructor(text: String, type: SnackBarType) : this(
        text = text, snackBarType = type.snackBarTypeId
    )

    constructor(parcel: Parcel) : this(
        parcel.readString().orEmpty(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(text)
        parcel.writeInt(snackBarType)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SnackBarEventData> {
        override fun createFromParcel(parcel: Parcel): SnackBarEventData {
            return SnackBarEventData(parcel)
        }

        override fun newArray(size: Int): Array<SnackBarEventData?> {
            return arrayOfNulls(size)
        }
    }
}