package com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PickItem(
    @SerialName(ID_KEY) val id: Long,
    @SerialName(EXTERNAL_ID_KEY) val externalId: String?,
    @SerialName(ORDER_ID_KEY) val orderId: Long,
    @SerialName(ORDER_DESCRIPTION_KEY) var orderDescription: String,
    @SerialName(ITEM_ID_KEY) val itemId: Long,
    @SerialName(ITEM_KEY) val item: List<PtlItem>,
    @SerialName(QTY_REQUESTED_KEY) val qtyRequested: String?,
    @SerialName(QTY_COLLECTED_KEY) val qtyCollected: Int,
    @SerialName(QTY_PENDING_KEY) val qtyPending: Int,
    @SerialName(LOT_ID_KEY) val lotId: String?,
    @SerialName(ROW_CREATION_DATE_KEY) val rowCreationDate: String,
    @SerialName(ROW_MODIFICATION_DATE_KEY) val rowModificationDate: String,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        externalId = parcel.readString(),
        orderId = parcel.readLong(),
        orderDescription = parcel.readString() ?: "",
        itemId = parcel.readLong(),
        item = parcel.createTypedArrayList(PtlItem.CREATOR) ?: emptyList(),
        qtyRequested = parcel.readString(),
        qtyCollected = parcel.readInt(),
        qtyPending = parcel.readInt(),
        lotId = parcel.readString(),
        rowCreationDate = parcel.readString() ?: "",
        rowModificationDate = parcel.readString() ?: ""
    )

    companion object CREATOR : Parcelable.Creator<PickItem> {
        override fun createFromParcel(parcel: Parcel): PickItem {
            return PickItem(parcel)
        }

        override fun newArray(size: Int): Array<PickItem?> {
            return arrayOfNulls(size)
        }

        const val ID_KEY = "id"
        const val EXTERNAL_ID_KEY = "external_id"
        const val ORDER_ID_KEY = "order_id"
        const val ITEM_ID_KEY = "item_id"
        const val ITEM_KEY = "item"
        const val ORDER_DESCRIPTION_KEY = "order_description"
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
        parcel.writeString(qtyRequested)
        parcel.writeInt(qtyCollected)
        parcel.writeInt(qtyPending)
        parcel.writeString(lotId)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
    }

    override fun describeContents(): Int {
        return 0
    }
}
