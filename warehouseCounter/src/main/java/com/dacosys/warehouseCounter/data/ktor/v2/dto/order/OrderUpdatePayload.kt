package com.dacosys.warehouseCounter.data.ktor.v2.dto.order

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderUpdatePayload(
    @SerialName(EXTERNAL_ID_KEY) var externalId: String = "",
    @SerialName(DESCRIPTION_KEY) var description: String = "",
    @SerialName(STATUS_ID_KEY) var statusId: Int = 0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        externalId = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        statusId = parcel.readInt()
    )

    companion object CREATOR : Parcelable.Creator<OrderUpdatePayload> {
        const val EXTERNAL_ID_KEY = "external_id"
        const val DESCRIPTION_KEY = "description"
        const val STATUS_ID_KEY = "status_id"
        override fun createFromParcel(parcel: Parcel): OrderUpdatePayload {
            return OrderUpdatePayload(parcel)
        }

        override fun newArray(size: Int): Array<OrderUpdatePayload?> {
            return arrayOfNulls(size)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(externalId)
        parcel.writeString(description)
        parcel.writeInt(statusId)
    }

    override fun describeContents(): Int {
        return 0
    }
}
