package com.dacosys.warehouseCounter.ktor.v2.dto.order

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderResponseContent(
    @SerialName(ID_KEY) var id: Long? = null,
    @SerialName(ORDER_ID_KEY) var orderId: Long? = null,
    @SerialName(ITEM_ID_KEY) var itemId: Long? = null,
    @SerialName(QTY_REQUESTED_KEY) var qtyRequested: Double? = null,
    @SerialName(QTY_COLLECTED_KEY) var qtyCollected: Double? = null,
    @SerialName(ROW_CREATION_DATE_KEY) var rowCreationDate: String? = null,
    @SerialName(ROW_MODIFICATION_DATE_KEY) var rowModificationDate: String? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readValue(Long::class.java.classLoader) as? Long,
        orderId = parcel.readValue(Long::class.java.classLoader) as? Long,
        itemId = parcel.readValue(Long::class.java.classLoader) as? Long,
        qtyRequested = parcel.readValue(Double::class.java.classLoader) as? Double,
        qtyCollected = parcel.readValue(Double::class.java.classLoader) as? Double,
        rowCreationDate = parcel.readString(),
        rowModificationDate = parcel.readString()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderResponseContent

        return itemId == other.itemId && orderId == other.orderId
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (orderId?.hashCode() ?: 0)
        result = 31 * result + (itemId?.hashCode() ?: 0)
        result = 31 * result + (qtyRequested?.hashCode() ?: 0)
        result = 31 * result + (qtyCollected?.hashCode() ?: 0)
        result = 31 * result + (rowCreationDate?.hashCode() ?: 0)
        result = 31 * result + (rowModificationDate?.hashCode() ?: 0)
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeValue(orderId)
        parcel.writeValue(itemId)
        parcel.writeValue(qtyRequested)
        parcel.writeValue(qtyCollected)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderResponseContent> {
        const val ID_KEY = "id"
        const val ORDER_ID_KEY = "orderId"
        const val ITEM_ID_KEY = "itemId"
        const val QTY_REQUESTED_KEY = "qtyRequested"
        const val QTY_COLLECTED_KEY = "qtyCollected"
        const val ROW_CREATION_DATE_KEY = "rowCreationDate"
        const val ROW_MODIFICATION_DATE_KEY = "rowModificationDate"

        const val CONTENT_RESPONSE_LIST_KEY = "orderContents"

        override fun createFromParcel(parcel: Parcel): OrderResponseContent {
            return OrderResponseContent(parcel)
        }

        override fun newArray(size: Int): Array<OrderResponseContent?> {
            return arrayOfNulls(size)
        }
    }
}