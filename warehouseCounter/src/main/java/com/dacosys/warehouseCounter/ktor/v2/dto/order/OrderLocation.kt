package com.dacosys.warehouseCounter.ktor.v2.dto.order

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderLocation(
    @SerialName(ORDER_ID_KEY) var orderId: Long? = null,
    @SerialName(ORDER_EXTERNAL_ID_KEY) var orderExternalId: String = "",
    @SerialName(ORDER_DESCRIPTION_KEY) var orderDescription: String = "",
    @SerialName(QTY_COLLECTED_KEY) var qtyCollected: Double? = null,
    @SerialName(ITEM_EXTERNAL_ID_KEY) var itemExternalId: String = "",
    @SerialName(ITEM_DESCRIPTION_KEY) var itemDescription: String = "",
    @SerialName(ITEM_EAN_KEY) var itemEan: String = "",
    @SerialName(WAREHOUSE_ID_KEY) var warehouseId: Long? = null,
    @SerialName(WAREHOUSE_EXTERNAL_ID_KEY) var warehouseExternalId: String = "",
    @SerialName(WAREHOUSE_DESCRIPTION_KEY) var warehouseDescription: String = "",
    @SerialName(WAREHOUSE_AREA_ID_KEY) var warehouseAreaId: Long? = null,
    @SerialName(WAREHOUSE_AREA_EXTERNAL_ID_KEY) var warehouseAreaExternalId: String = "",
    @SerialName(WAREHOUSE_AREA_DESCRIPTION_KEY) var warehouseAreaDescription: String = "",
    @SerialName(RACK_ID_KEY) var rackId: Long? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        orderId = parcel.readValue(Long::class.java.classLoader) as? Long,
        orderExternalId = parcel.readString() ?: "",
        orderDescription = parcel.readString() ?: "",
        qtyCollected = parcel.readValue(Double::class.java.classLoader) as? Double,
        itemExternalId = parcel.readString() ?: "",
        itemDescription = parcel.readString() ?: "",
        itemEan = parcel.readString() ?: "",
        warehouseId = parcel.readValue(Long::class.java.classLoader) as? Long,
        warehouseExternalId = parcel.readString() ?: "",
        warehouseDescription = parcel.readString() ?: "",
        warehouseAreaId = parcel.readValue(Long::class.java.classLoader) as? Long,
        warehouseAreaExternalId = parcel.readString() ?: "",
        warehouseAreaDescription = parcel.readString() ?: "",
        rackId = parcel.readValue(Long::class.java.classLoader) as? Long
    )

    enum class OrderLocationStatus(val id: Long) {
        NOT_PICKED(0), PICKED(1)
    }

    val itemStatus: OrderLocationStatus
        get() {
            return if ((qtyCollected ?: 0.0) > 0) OrderLocationStatus.PICKED else OrderLocationStatus.NOT_PICKED
        }

    val uniqueId: Long
        get() {
            var result = orderId?.hashCode() ?: 0
            result = 31 * result + orderExternalId.hashCode()
            result = 31 * result + itemExternalId.hashCode()
            result = 31 * result + itemEan.hashCode()
            return result.toLong()
        }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(orderId)
        parcel.writeString(orderExternalId)
        parcel.writeString(orderDescription)
        parcel.writeValue(qtyCollected)
        parcel.writeString(itemExternalId)
        parcel.writeString(itemDescription)
        parcel.writeString(itemEan)
        parcel.writeValue(warehouseId)
        parcel.writeString(warehouseExternalId)
        parcel.writeString(warehouseDescription)
        parcel.writeValue(warehouseAreaId)
        parcel.writeString(warehouseAreaExternalId)
        parcel.writeString(warehouseAreaDescription)
        parcel.writeValue(rackId)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderLocation

        if (orderId != other.orderId) return false
        if (orderExternalId != other.orderExternalId) return false
        if (orderDescription != other.orderDescription) return false
        if (qtyCollected != other.qtyCollected) return false
        if (itemExternalId != other.itemExternalId) return false
        if (itemDescription != other.itemDescription) return false
        if (itemEan != other.itemEan) return false
        if (warehouseId != other.warehouseId) return false
        if (warehouseExternalId != other.warehouseExternalId) return false
        if (warehouseDescription != other.warehouseDescription) return false
        if (warehouseAreaId != other.warehouseAreaId) return false
        if (warehouseAreaExternalId != other.warehouseAreaExternalId) return false
        if (warehouseAreaDescription != other.warehouseAreaDescription) return false
        if (rackId != other.rackId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = orderId?.hashCode() ?: 0
        result = 31 * result + orderExternalId.hashCode()
        result = 31 * result + orderDescription.hashCode()
        result = 31 * result + (qtyCollected?.hashCode() ?: 0)
        result = 31 * result + itemExternalId.hashCode()
        result = 31 * result + itemDescription.hashCode()
        result = 31 * result + itemEan.hashCode()
        result = 31 * result + (warehouseId?.hashCode() ?: 0)
        result = 31 * result + warehouseExternalId.hashCode()
        result = 31 * result + warehouseDescription.hashCode()
        result = 31 * result + (warehouseAreaId?.hashCode() ?: 0)
        result = 31 * result + warehouseAreaExternalId.hashCode()
        result = 31 * result + warehouseAreaDescription.hashCode()
        result = 31 * result + (rackId?.hashCode() ?: 0)
        return result
    }

    companion object CREATOR : Parcelable.Creator<OrderLocation> {
        const val ORDER_ID_KEY = "orderId"
        const val ORDER_EXTERNAL_ID_KEY = "orderExternalId"
        const val ORDER_DESCRIPTION_KEY = "orderDescription"
        const val QTY_COLLECTED_KEY = "qtyCollected"
        const val ITEM_EXTERNAL_ID_KEY = "itemExternalId"
        const val ITEM_DESCRIPTION_KEY = "itemDescription"
        const val ITEM_EAN_KEY = "itemEan"
        const val WAREHOUSE_ID_KEY = "warehouseId"
        const val WAREHOUSE_EXTERNAL_ID_KEY = "warehouseExternalId"
        const val WAREHOUSE_DESCRIPTION_KEY = "warehouseDescription"
        const val WAREHOUSE_AREA_ID_KEY = "warehouseAreaId"
        const val WAREHOUSE_AREA_EXTERNAL_ID_KEY = "warehouseAreaExternalId"
        const val WAREHOUSE_AREA_DESCRIPTION_KEY = "warehouseAreaDescription"
        const val RACK_ID_KEY = "rackId"

        const val ORDER_LOCATION_LIST_KEY = "orderLocations"

        override fun createFromParcel(parcel: Parcel): OrderLocation {
            return OrderLocation(parcel)
        }

        override fun newArray(size: Int): Array<OrderLocation?> {
            return arrayOfNulls(size)
        }
    }
}