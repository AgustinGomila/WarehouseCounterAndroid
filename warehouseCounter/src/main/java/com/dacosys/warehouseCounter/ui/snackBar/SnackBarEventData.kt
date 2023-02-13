package com.dacosys.warehouseCounter.ui.snackBar

import android.os.Parcel
import android.os.Parcelable

data class SnackBarEventData(
    val text: String = "",
    val snackBarType: SnackBarType,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().orEmpty(),
        parcel.readParcelable(SnackBarType::class.java.classLoader) ?: SnackBarType.INFO
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(text)
        parcel.writeParcelable(snackBarType, flags)
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