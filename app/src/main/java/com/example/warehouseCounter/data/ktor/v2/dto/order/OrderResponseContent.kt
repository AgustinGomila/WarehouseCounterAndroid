package com.example.warehouseCounter.data.ktor.v2.dto.order

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
    @SerialName(ITEM_EAN_KEY) var ean: String = "",
    @SerialName(ITEM_EXTERNAL_ID_KEY) var externalId: String? = null,
    @SerialName(ITEM_ACTIVE_KEY) var itemActive: Boolean? = null,
    @SerialName(ITEM_CATEGORY_ID_KEY) var itemCategoryId: Long? = null,
    @SerialName(ITEM_DESCRIPTION_KEY) var itemDescription: String = "",
    @SerialName(LOT_ACTIVE_KEY) var lotActive: Boolean? = null,
    @SerialName(LOT_CODE_KEY) var lotCode: String? = null,
    @SerialName(LOT_ENABLED_KEY) var lotEnabled: Boolean? = null,
    @SerialName(LOT_ID_KEY) var lotId: Long? = null,
    @SerialName(ITEM_PRICE_KEY) var price: Double? = null,
    @SerialName(ROW_CREATION_DATE_KEY) var rowCreationDate: String? = null,
    @SerialName(ROW_MODIFICATION_DATE_KEY) var rowModificationDate: String? = null,

    ) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readValue(Long::class.java.classLoader) as? Long,
        orderId = parcel.readValue(Long::class.java.classLoader) as? Long,
        itemId = parcel.readValue(Long::class.java.classLoader) as? Long,
        qtyRequested = parcel.readValue(Double::class.java.classLoader) as? Double,
        qtyCollected = parcel.readValue(Double::class.java.classLoader) as? Double,
        ean = parcel.readString() ?: "",
        externalId = parcel.readString() ?: "",
        itemActive = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        itemCategoryId = parcel.readValue(Long::class.java.classLoader) as? Long,
        itemDescription = parcel.readString() ?: "",
        lotActive = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        lotCode = parcel.readString() ?: "",
        lotEnabled = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        lotId = parcel.readValue(Long::class.java.classLoader) as? Long,
        price = parcel.readValue(Double::class.java.classLoader) as? Double,
        rowCreationDate = parcel.readString(),
        rowModificationDate = parcel.readString()
    )

    fun toKtor(): OrderRequestContent {
        return OrderRequestContent(
            itemId = this.itemId,
            qtyRequested = this.qtyRequested ?: 0.0,
            qtyCollected = this.qtyCollected ?: 0.0,
            codeRead = this.ean,
            ean = this.ean,
            externalId = this.externalId ?: "",
            itemDescription = this.itemDescription,
            itemActive = this.itemActive,
            itemCategoryId = this.itemCategoryId ?: 0L,
            lotActive = this.lotActive,
            lotCode = this.lotCode ?: "",
            lotEnabled = this.lotEnabled,
            lotId = this.lotId ?: 0L,
            price = this.price
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderResponseContent

        return itemId == other.itemId && orderId == other.orderId
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeValue(orderId)
        parcel.writeValue(itemId)
        parcel.writeValue(qtyRequested)
        parcel.writeValue(qtyCollected)
        parcel.writeString(ean)
        parcel.writeString(externalId)
        parcel.writeValue(itemActive)
        parcel.writeValue(itemCategoryId)
        parcel.writeString(itemDescription)
        parcel.writeValue(lotActive)
        parcel.writeString(lotCode)
        parcel.writeValue(lotEnabled)
        parcel.writeValue(lotId)
        parcel.writeValue(price)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (orderId?.hashCode() ?: 0)
        result = 31 * result + (itemId?.hashCode() ?: 0)
        result = 31 * result + (qtyRequested?.hashCode() ?: 0)
        result = 31 * result + (qtyCollected?.hashCode() ?: 0)
        result = 31 * result + (rowCreationDate?.hashCode() ?: 0)
        result = 31 * result + (rowModificationDate?.hashCode() ?: 0)
        result = 31 * result + ean.hashCode()
        result = 31 * result + (externalId?.hashCode() ?: 0)
        result = 31 * result + (itemActive?.hashCode() ?: 0)
        result = 31 * result + (itemCategoryId?.hashCode() ?: 0)
        result = 31 * result + itemDescription.hashCode()
        result = 31 * result + (lotActive?.hashCode() ?: 0)
        result = 31 * result + (lotCode?.hashCode() ?: 0)
        result = 31 * result + (lotEnabled?.hashCode() ?: 0)
        result = 31 * result + (lotId?.hashCode() ?: 0)
        result = 31 * result + (price?.hashCode() ?: 0)
        return result
    }

    companion object CREATOR : Parcelable.Creator<OrderResponseContent> {
        const val ID_KEY = "id"
        const val ORDER_ID_KEY = "orderId"
        const val ITEM_ID_KEY = "itemId"
        const val QTY_REQUESTED_KEY = "qtyRequested"
        const val QTY_COLLECTED_KEY = "qtyCollected"
        const val ITEM_ACTIVE_KEY = "itemActive"
        const val ITEM_CATEGORY_ID_KEY = "itemCategoryId"
        const val ITEM_DESCRIPTION_KEY = "itemDescription"
        const val ITEM_EAN_KEY = "itemEan"
        const val ITEM_EXTERNAL_ID_KEY = "itemExternalId"
        const val ITEM_PRICE_KEY = "itemPrice"
        const val LOT_ACTIVE_KEY = "lotActive"
        const val LOT_CODE_KEY = "lotCode"
        const val LOT_ENABLED_KEY = "lotEnabled"
        const val LOT_ID_KEY = "lotId"
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
