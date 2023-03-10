package com.dacosys.warehouseCounter.dto.ptlOrder

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PickItem(
    @Json(name = ID_KEY) val id: Long,
    @Json(name = EXTERNAL_ID_KEY) val externalId: String?,
    @Json(name = ORDER_ID_KEY) val orderId: Long,
    @Json(name = ITEM_ID_KEY) val itemId: Long,
    @Json(name = QTY_REQUESTED_KEY) val qtyRequested: Int,
    @Json(name = QTY_COLLECTED_KEY) val qtyCollected: Int,
    @Json(name = LOT_ID_KEY) val lotId: String?,
    @Json(name = ROW_CREATION_DATE_KEY) val rowCreationDate: String,
    @Json(name = ROW_MODIFICATION_DATE_KEY) val rowModificationDate: String,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        externalId = parcel.readString(),
        orderId = parcel.readLong(),
        itemId = parcel.readLong(),
        qtyRequested = parcel.readInt(),
        qtyCollected = parcel.readInt(),
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
        const val QTY_REQUESTED_KEY = "qty_requested"
        const val QTY_COLLECTED_KEY = "qty_collected"
        const val LOT_ID_KEY = "lot_id"
        const val ROW_CREATION_DATE_KEY = "row_creation_date"
        const val ROW_MODIFICATION_DATE_KEY = "row_modification_date"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(externalId)
        parcel.writeLong(orderId)
        parcel.writeLong(itemId)
        parcel.writeInt(qtyRequested)
        parcel.writeInt(qtyCollected)
        parcel.writeString(lotId)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
    }

    override fun describeContents(): Int {
        return 0
    }
}