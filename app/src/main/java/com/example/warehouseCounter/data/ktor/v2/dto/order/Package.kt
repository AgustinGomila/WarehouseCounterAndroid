package com.example.warehouseCounter.data.ktor.v2.dto.order

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Package(
    @SerialName(EXTERNAL_ID_KEY) var externalId: String = "",
    @SerialName(CODE_KEY) var code: String = "",
) : Parcelable {
    constructor(parcel: Parcel) : this(
        externalId = parcel.readString() ?: "",
        code = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(externalId)
        parcel.writeString(code)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Package> {
        const val EXTERNAL_ID_KEY = "externalId"
        const val CODE_KEY = "code"

        const val PACKAGE_LIST_KEY = "packages"

        override fun createFromParcel(parcel: Parcel): Package {
            return Package(parcel)
        }

        override fun newArray(size: Int): Array<Package?> {
            return arrayOfNulls(size)
        }
    }
}
