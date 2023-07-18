package com.dacosys.warehouseCounter.room.entity.orderRequest

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequestContent as OrderRequestContentKtor
import com.dacosys.warehouseCounter.room.entity.orderRequest.OrderRequestContentEntry as Entry

@Entity(
    tableName = Entry.TABLE_NAME,
    indices = [
        Index(value = [Entry.ORDER_REQUEST_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.ORDER_REQUEST_ID}"),
        Index(value = [Entry.ITEM_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.ITEM_ID}"),
        Index(value = [Entry.ITEM_EAN], name = "IDX_${Entry.TABLE_NAME}_${Entry.ITEM_EAN}"),
        Index(value = [Entry.ITEM_DESCRIPTION], name = "IDX_${Entry.TABLE_NAME}_${Entry.ITEM_DESCRIPTION}"),
        Index(value = [Entry.ITEM_CATEGORY_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.ITEM_CATEGORY_ID}"),
        Index(value = [Entry.ITEM_EXTERNAL_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.ITEM_EXTERNAL_ID}"),
        Index(value = [Entry.ITEM_ACTIVE], name = "IDX_${Entry.TABLE_NAME}_${Entry.ITEM_ACTIVE}"),
    ]
)
data class OrderRequestContent(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = Entry.ORDER_REQUEST_CONTENT_ID) var id: Long = 0L,
    @ColumnInfo(name = Entry.ORDER_REQUEST_ID) var orderRequestId: Long = 0L,
    @ColumnInfo(name = Entry.ITEM_ID) var itemId: Long = 0L,
    @ColumnInfo(name = Entry.ITEM_DESCRIPTION) var itemDescription: String = "",
    @ColumnInfo(name = Entry.ITEM_CODE_READ) var codeRead: String = "",
    @ColumnInfo(name = Entry.ITEM_EAN) var ean: String = "",
    @ColumnInfo(name = Entry.ITEM_PRICE) var price: Float? = 0f,
    @ColumnInfo(name = Entry.ITEM_ACTIVE, defaultValue = "0") var itemActive: Int = 0,
    @ColumnInfo(name = Entry.ITEM_EXTERNAL_ID) var externalId: String = "",
    @ColumnInfo(name = Entry.ITEM_CATEGORY_ID) var itemCategoryId: Long = 0L,
    @ColumnInfo(name = Entry.LOT_ENABLED, defaultValue = "0") var lotEnabled: Int = 0,
    @ColumnInfo(name = Entry.LOT_ID) var lotId: Long = 0L,
    @ColumnInfo(name = Entry.LOT_CODE) var lotCode: String = "",
    @ColumnInfo(name = Entry.LOT_ACTIVE, defaultValue = "0") var lotActive: Int = 0,
    @ColumnInfo(name = Entry.QTY_REQUESTED) var qtyRequested: Double? = null,
    @ColumnInfo(name = Entry.QTY_COLLECTED) var qtyCollected: Double? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        orderRequestId = parcel.readLong(),
        itemId = parcel.readLong(),
        itemDescription = parcel.readString() ?: "",
        codeRead = parcel.readString() ?: "",
        ean = parcel.readString() ?: "",
        price = parcel.readValue(Float::class.java.classLoader) as? Float,
        itemActive = parcel.readInt(),
        externalId = parcel.readString() ?: "",
        itemCategoryId = parcel.readLong(),
        lotEnabled = parcel.readInt(),
        lotId = parcel.readLong(),
        lotCode = parcel.readString() ?: "",
        lotActive = parcel.readInt(),
        qtyRequested = parcel.readValue(Double::class.java.classLoader) as? Double,
        qtyCollected = parcel.readValue(Double::class.java.classLoader) as? Double
    )

    val toRequestContentKtor: OrderRequestContentKtor
        get() {
            return OrderRequestContentKtor(
                itemId = this.itemId,
                itemDescription = this.itemDescription,
                codeRead = this.codeRead,
                ean = this.ean,
                price = this.price?.toDouble() ?: 0.0,
                itemActive = this.itemActive == 1,
                externalId = this.externalId,
                itemCategoryId = this.itemCategoryId,
                lotEnabled = this.lotEnabled == 1,
                lotId = this.lotId,
                lotCode = this.lotCode,
                lotActive = this.lotActive == 1,
                qtyRequested = this.qtyRequested ?: 0.0,
                qtyCollected = this.qtyCollected ?: 0.0,
            )
        }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeLong(orderRequestId)
        parcel.writeLong(itemId)
        parcel.writeString(itemDescription)
        parcel.writeString(codeRead)
        parcel.writeString(ean)
        parcel.writeValue(price)
        parcel.writeInt(itemActive)
        parcel.writeString(externalId)
        parcel.writeLong(itemCategoryId)
        parcel.writeInt(lotEnabled)
        parcel.writeLong(lotId)
        parcel.writeString(lotCode)
        parcel.writeInt(lotActive)
        parcel.writeValue(qtyRequested)
        parcel.writeValue(qtyCollected)
    }

    override fun describeContents(): Int {
        return 0
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