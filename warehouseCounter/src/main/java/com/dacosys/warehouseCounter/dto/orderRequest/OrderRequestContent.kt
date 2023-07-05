package com.dacosys.warehouseCounter.dto.orderRequest

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Ignore
import com.dacosys.warehouseCounter.dto.ptlOrder.ContentStatus
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class OrderRequestContent() : Parcelable {
    // region Variables de la clase
    @Json(name = "item")
    var item: Item? = null

    @Json(name = "lot")
    var lot: Lot? = null

    @Json(name = "qty")
    var qty: Qty? = null

    @Ignore
    val contentStatus: ContentStatus =
        if (qty?.qtyCollected == null || qty?.qtyRequested == null) ContentStatus.QTY_DEFAULT
        else if ((qty?.qtyCollected ?: 0.0) == (qty?.qtyRequested ?: 0.0)) ContentStatus.QTY_EQUAL
        else if ((qty?.qtyCollected ?: 0.0) > (qty?.qtyRequested ?: 0.0)) ContentStatus.QTY_MORE
        else ContentStatus.QTY_LESS

    constructor(parcel: Parcel) : this() {
        item = parcel.readParcelable(Item::class.java.classLoader)
        lot = parcel.readParcelable(Lot::class.java.classLoader)
        qty = parcel.readParcelable(Qty::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(item, flags)
        parcel.writeParcelable(lot, flags)
        parcel.writeParcelable(qty, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun hashCode(): Int {
        var result = item?.hashCode() ?: 0
        result = 31 * result + (lot?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderRequestContent

        if (item != other.item) return false
        return lot == other.lot
    }

    companion object CREATOR : Parcelable.Creator<OrderRequestContent> {
        override fun createFromParcel(parcel: Parcel): OrderRequestContent {
            return OrderRequestContent(parcel)
        }

        override fun newArray(size: Int): Array<OrderRequestContent?> {
            return arrayOfNulls(size)
        }
    }
}
