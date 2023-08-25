package com.dacosys.warehouseCounter.ktor.v2.dto.order

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderMovePayload(
    @SerialName(ORDER_REQUEST_ID_KEY) var orderRequestId: Long? = null,
    @SerialName(WAREHOUSE_AREA_ID_KEY) var warehouseAreaId: Long? = null,
    @SerialName(ORDER_EXTERNAL_ID_KEY) var orderExternalId: String? = null,
    @SerialName(ORDER_PACKAGE_EXTERNAL_ID_KEY) var orderPackageExternalId: String? = null,
    @SerialName(ORDER_PACKAGE_CODE_KEY) var orderPackageCode: String? = null,
    @SerialName(ORDER_DESCRIPTION_KEY) var orderDescription: String? = null,
    @SerialName(WAREHOUSE_AREA_EXTERNAL_ID_KEY) var warehouseAreaExternalId: String? = null,
    @SerialName(RACK_EXTERNAL_ID_KEY) var rackExternalId: String? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        orderRequestId = parcel.readValue(Long::class.java.classLoader) as? Long,
        warehouseAreaId = parcel.readValue(Long::class.java.classLoader) as? Long,
        orderExternalId = parcel.readString(),
        orderPackageExternalId = parcel.readString(),
        orderPackageCode = parcel.readString(),
        orderDescription = parcel.readString(),
        warehouseAreaExternalId = parcel.readString(),
        rackExternalId = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(orderRequestId)
        parcel.writeValue(warehouseAreaId)
        parcel.writeString(orderExternalId)
        parcel.writeString(orderPackageExternalId)
        parcel.writeString(orderPackageCode)
        parcel.writeString(orderDescription)
        parcel.writeString(warehouseAreaExternalId)
        parcel.writeString(rackExternalId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderMovePayload> {
        const val ORDER_REQUEST_ID_KEY = "orderRequestId"
        const val WAREHOUSE_AREA_ID_KEY = "warehouseAreaId"
        const val ORDER_EXTERNAL_ID_KEY = "orderExternalId"
        const val ORDER_PACKAGE_EXTERNAL_ID_KEY = "orderPackageExternalId"
        const val ORDER_PACKAGE_CODE_KEY = "orderPackageCode"
        const val ORDER_DESCRIPTION_KEY = "orderDescription"
        const val WAREHOUSE_AREA_EXTERNAL_ID_KEY = "warehouseAreaExternalId"
        const val RACK_EXTERNAL_ID_KEY = "rackExternalId"

        override fun createFromParcel(parcel: Parcel): OrderMovePayload {
            return OrderMovePayload(parcel)
        }

        override fun newArray(size: Int): Array<OrderMovePayload?> {
            return arrayOfNulls(size)
        }
    }
}
