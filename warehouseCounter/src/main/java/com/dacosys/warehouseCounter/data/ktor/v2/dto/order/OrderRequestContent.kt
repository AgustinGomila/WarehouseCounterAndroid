package com.dacosys.warehouseCounter.data.ktor.v2.dto.order

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.OrderRequestContent as OrderRequestContentRoom

@Serializable
data class OrderRequestContent(
    @SerialName(ITEM_CODE_READ_KEY) var codeRead: String = "",
    @SerialName(ITEM_EAN_KEY) var ean: String = "",
    @SerialName(ITEM_EXTERNAL_ID_KEY) var externalId: String? = null,
    @SerialName(ITEM_ACTIVE_KEY) var itemActive: Boolean? = null,
    @SerialName(ITEM_CATEGORY_ID_KEY) var itemCategoryId: Long? = null,
    @SerialName(ITEM_DESCRIPTION_KEY) var itemDescription: String = "",
    @SerialName(ITEM_ID_KEY) var itemId: Long? = null,
    @SerialName(LOT_ACTIVE_KEY) var lotActive: Boolean? = null,
    @SerialName(LOT_CODE_KEY) var lotCode: String = "",
    @SerialName(LOT_ENABLED_KEY) var lotEnabled: Boolean? = null,
    @SerialName(LOT_ID_KEY) var lotId: Long? = null,
    @SerialName(ITEM_PRICE_KEY) var price: Double? = null,
    @SerialName(QTY_COLLECTED_KEY) var qtyCollected: Double? = null,
    @SerialName(QTY_REQUESTED_KEY) var qtyRequested: Double? = null,
) : Parcelable {

    val contentStatus: ContentStatus
        get() {
            return if (qtyCollected == null || qtyRequested == null) ContentStatus.QTY_DEFAULT
            else if ((qtyCollected ?: 0.0) == (qtyRequested ?: 0.0)) ContentStatus.QTY_EQUAL
            else if ((qtyCollected ?: 0.0) > (qtyRequested ?: 0.0)) ContentStatus.QTY_MORE
            else ContentStatus.QTY_LESS
        }

    fun toRoom(id: Long): OrderRequestContentRoom {
        return OrderRequestContentRoom(
            codeRead = codeRead,
            ean = ean,
            externalId = externalId ?: "",
            itemActive = if (itemActive == true) 1 else 0,
            itemCategoryId = itemCategoryId ?: 0L,
            itemDescription = itemDescription,
            itemId = itemId ?: 0L,
            lotActive = if (lotActive == true) 1 else 0,
            lotCode = lotCode,
            lotEnabled = if (lotEnabled == true) 1 else 0,
            lotId = lotId ?: 0L,
            orderRequestId = id,
            price = price?.toFloat() ?: 0F,
            qtyCollected = qtyCollected ?: 0.0,
            qtyRequested = qtyRequested ?: 0.0,
        )
    }

    constructor(parcel: Parcel) : this(
        codeRead = parcel.readString() ?: "",
        ean = parcel.readString() ?: "",
        externalId = parcel.readString() ?: "",
        itemActive = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        itemCategoryId = parcel.readValue(Long::class.java.classLoader) as? Long,
        itemDescription = parcel.readString() ?: "",
        itemId = parcel.readValue(Long::class.java.classLoader) as? Long,
        lotActive = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        lotCode = parcel.readString() ?: "",
        lotEnabled = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        lotId = parcel.readValue(Long::class.java.classLoader) as? Long,
        price = parcel.readValue(Double::class.java.classLoader) as? Double,
        qtyCollected = parcel.readValue(Double::class.java.classLoader) as? Double,
        qtyRequested = parcel.readValue(Double::class.java.classLoader) as? Double,
    )

    constructor(orCont: OrderResponseContent) : this(
        itemId = orCont.itemId,
        qtyCollected = orCont.qtyCollected,
        qtyRequested = orCont.qtyRequested,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderRequestContent

        return itemId == other.itemId
    }

    override fun hashCode(): Int {
        var result = itemId?.hashCode() ?: 0
        result = 31 * result + itemDescription.hashCode()
        result = 31 * result + codeRead.hashCode()
        result = 31 * result + ean.hashCode()
        result = 31 * result + (price?.hashCode() ?: 0)
        result = 31 * result + (itemActive?.hashCode() ?: 0)
        result = 31 * result + (externalId?.hashCode() ?: 0)
        result = 31 * result + (itemCategoryId?.hashCode() ?: 0)
        result = 31 * result + (lotEnabled?.hashCode() ?: 0)
        result = 31 * result + (lotId?.hashCode() ?: 0)
        result = 31 * result + lotCode.hashCode()
        result = 31 * result + (lotActive?.hashCode() ?: 0)
        result = 31 * result + (qtyRequested?.hashCode() ?: 0)
        result = 31 * result + (qtyCollected?.hashCode() ?: 0)
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(codeRead)
        parcel.writeString(ean)
        parcel.writeString(externalId)
        parcel.writeValue(itemActive)
        parcel.writeValue(itemCategoryId)
        parcel.writeString(itemDescription)
        parcel.writeValue(itemId)
        parcel.writeValue(lotActive)
        parcel.writeString(lotCode)
        parcel.writeValue(lotEnabled)
        parcel.writeValue(lotId)
        parcel.writeValue(price)
        parcel.writeValue(qtyCollected)
        parcel.writeValue(qtyRequested)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderRequestContent> {
        const val ITEM_ACTIVE_KEY = "itemActive"
        const val ITEM_CATEGORY_ID_KEY = "itemCategoryId"
        const val ITEM_CODE_READ_KEY = "itemCodeRead"
        const val ITEM_DESCRIPTION_KEY = "itemDescription"
        const val ITEM_EAN_KEY = "itemEan"
        const val ITEM_EXTERNAL_ID_KEY = "itemExternalId"
        const val ITEM_ID_KEY = "itemId"
        const val ITEM_PRICE_KEY = "itemPrice"
        const val LOT_ACTIVE_KEY = "lotActive"
        const val LOT_CODE_KEY = "lotCode"
        const val LOT_ENABLED_KEY = "itemLotEnabled"
        const val LOT_ID_KEY = "lotId"
        const val QTY_COLLECTED_KEY = "qtyCollected"
        const val QTY_REQUESTED_KEY = "qtyRequested"

        const val CONTENT_LIST_KEY = "contents"

        override fun createFromParcel(parcel: Parcel): OrderRequestContent {
            return OrderRequestContent(parcel)
        }

        override fun newArray(size: Int): Array<OrderRequestContent?> {
            return arrayOfNulls(size)
        }
    }
}
