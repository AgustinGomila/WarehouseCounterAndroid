package com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class PtlContent(
    @SerialName(ID_KEY) var id: Long,
    @SerialName(EXTERNAL_ID_KEY) var externalId: String?,
    @SerialName(ORDER_ID_KEY) var orderId: Long,
    @SerialName(ORDER_DESCRIPTION_KEY) var orderDescription: String,
    @SerialName(ITEM_ID_KEY) var itemId: Long,
    @SerialName(ITEM_KEY) var item: List<PtlItem>,
    @SerialName(QTY_REQUESTED_KEY) var qtyRequested: Double,
    @SerialName(QTY_COLLECTED_KEY) var qtyCollected: Double,
    @SerialName(QTY_PENDING_KEY) var qtyPending: Double,
    @SerialName(LOT_ID_KEY) var lotId: Long?,
    @SerialName(ROW_CREATION_DATE_KEY) var rowCreationDate: String,
    @SerialName(ROW_MODIFICATION_DATE_KEY) var rowModificationDate: String,

    @Transient val contentStatus: ContentStatus =
        if (qtyCollected == qtyRequested) ContentStatus.QTY_EQUAL
        else if (qtyCollected > qtyRequested) ContentStatus.QTY_MORE
        else ContentStatus.QTY_LESS
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        externalId = parcel.readString() ?: "",
        orderId = parcel.readLong(),
        orderDescription = parcel.readString() ?: "",
        itemId = parcel.readLong(),
        item = parcel.createTypedArrayList(PtlItem.CREATOR) ?: emptyList(),
        qtyRequested = parcel.readDouble(),
        qtyCollected = parcel.readDouble(),
        qtyPending = parcel.readDouble(),
        lotId = parcel.readValue(Long::class.java.classLoader) as? Long,
        rowCreationDate = parcel.readString() ?: "",
        rowModificationDate = parcel.readString() ?: ""
    )

    companion object CREATOR : Parcelable.Creator<PtlContent> {
        override fun createFromParcel(parcel: Parcel): PtlContent {
            return PtlContent(parcel)
        }

        override fun newArray(size: Int): Array<PtlContent?> {
            return arrayOfNulls(size)
        }

        const val ID_KEY = "id"
        const val EXTERNAL_ID_KEY = "external_id"
        const val ORDER_ID_KEY = "order_id"
        const val ORDER_DESCRIPTION_KEY = "order_description"
        const val ITEM_ID_KEY = "item_id"
        const val ITEM_KEY = "item"
        const val QTY_REQUESTED_KEY = "qty_requested"
        const val QTY_COLLECTED_KEY = "qty_collected"
        const val QTY_PENDING_KEY = "qty_pending"
        const val LOT_ID_KEY = "lot_id"
        const val ROW_CREATION_DATE_KEY = "row_creation_date"
        const val ROW_MODIFICATION_DATE_KEY = "row_modification_date"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(externalId)
        parcel.writeLong(orderId)
        parcel.writeString(orderDescription)
        parcel.writeLong(itemId)
        parcel.writeParcelableArray(item.toTypedArray(), flags)
        parcel.writeDouble(qtyRequested)
        parcel.writeDouble(qtyCollected)
        parcel.writeDouble(qtyPending)
        parcel.writeValue(lotId)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
    }

    override fun describeContents(): Int {
        return 0
    }
}

