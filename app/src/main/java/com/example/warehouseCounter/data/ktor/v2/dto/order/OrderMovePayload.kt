package com.example.warehouseCounter.data.ktor.v2.dto.order

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderMovePayload(
    @SerialName(ORDER_REQUEST_ID_KEY) var orderRequestId: Long? = null,
    @SerialName(WAREHOUSE_AREA_ID_KEY) var warehouseAreaId: Long? = null,
    @SerialName(RACK_ID_KEY) var rackId: Long? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        orderRequestId = parcel.readValue(Long::class.java.classLoader) as? Long,
        warehouseAreaId = parcel.readValue(Long::class.java.classLoader) as? Long,
        rackId = parcel.readValue(Long::class.java.classLoader) as? Long,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(orderRequestId)
        parcel.writeValue(warehouseAreaId)
        parcel.writeValue(rackId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderMovePayload> {
        const val ORDER_REQUEST_ID_KEY = "orderRequestId"
        const val WAREHOUSE_AREA_ID_KEY = "warehouseAreaId"
        const val RACK_ID_KEY = "rackId"

        override fun createFromParcel(parcel: Parcel): OrderMovePayload {
            return OrderMovePayload(parcel)
        }

        override fun newArray(size: Int): Array<OrderMovePayload?> {
            return arrayOfNulls(size)
        }
    }
}
