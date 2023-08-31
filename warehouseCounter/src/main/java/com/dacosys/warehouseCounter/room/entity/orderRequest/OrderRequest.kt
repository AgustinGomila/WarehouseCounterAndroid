package com.dacosys.warehouseCounter.room.entity.orderRequest

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequest as OrderRequestKtor
import com.dacosys.warehouseCounter.room.entity.orderRequest.OrderRequestEntry as Entry

@Entity(
    tableName = Entry.TABLE_NAME,
    indices = [
        Index(value = [Entry.DESCRIPTION], name = "IDX_${Entry.TABLE_NAME}_${Entry.DESCRIPTION}"),
        Index(value = [Entry.EXTERNAL_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.EXTERNAL_ID}"),
        Index(value = [Entry.ORDER_TYPE_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.ORDER_TYPE_ID}"),
        Index(value = [Entry.CLIENT_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.CLIENT_ID}"),
        Index(value = [Entry.USER_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.USER_ID}"),
    ]
)
data class OrderRequest(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = Entry.ORDER_REQUEST_ID) var orderRequestId: Long = 0L,
    @ColumnInfo(name = Entry.CLIENT_ID) var clientId: Long = 0L,
    @ColumnInfo(name = Entry.COMPLETED, defaultValue = "0") var completed: Int = 0,
    @ColumnInfo(name = Entry.CREATION_DATE) var creationDate: String = "",
    @ColumnInfo(name = Entry.DESCRIPTION) var description: String = "",
    @ColumnInfo(name = Entry.EXTERNAL_ID) var externalId: String = "",
    @ColumnInfo(name = Entry.FINISH_DATE) var finishDate: String = "",
    @ColumnInfo(name = Entry.ORDER_TYPE_DESCRIPTION) var orderTypeDescription: String = "",
    @ColumnInfo(name = Entry.ORDER_TYPE_ID) var orderTypeId: Int = 1,
    @ColumnInfo(name = Entry.RESULT_ALLOW_DIFF, defaultValue = "0") var resultAllowDiff: Int = 0,
    @ColumnInfo(name = Entry.RESULT_ALLOW_MOD, defaultValue = "0") var resultAllowMod: Int = 0,
    @ColumnInfo(name = Entry.RESULT_DIFF_PRODUCT, defaultValue = "0") var resultDiffProduct: Int = 0,
    @ColumnInfo(name = Entry.RESULT_DIFF_QTY, defaultValue = "0") var resultDiffQty: Int = 0,
    @ColumnInfo(name = Entry.START_DATE) var startDate: String = "",
    @ColumnInfo(name = Entry.USER_ID) var userId: Long = 0L,
    @ColumnInfo(name = Entry.ZONE) var zone: String = "",
) : Parcelable {
    constructor(parcel: Parcel) : this(
        orderRequestId = parcel.readLong(),
        clientId = parcel.readLong(),
        completed = parcel.readInt(),
        creationDate = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        externalId = parcel.readString() ?: "",
        finishDate = parcel.readString() ?: "",
        orderTypeDescription = parcel.readString() ?: "",
        orderTypeId = parcel.readInt(),
        resultAllowDiff = parcel.readInt(),
        resultAllowMod = parcel.readInt(),
        resultDiffProduct = parcel.readInt(),
        resultDiffQty = parcel.readInt(),
        startDate = parcel.readString() ?: "",
        userId = parcel.readLong(),
        zone = parcel.readString() ?: ""
    )

    val toKtor: OrderRequestKtor
        get() {
            return OrderRequestKtor(
                orderRequestId = this.orderRequestId,
                externalId = this.externalId,
                creationDate = this.creationDate,
                description = this.description,
                zone = this.zone,
                orderTypeId = this.orderTypeId.toLong(),
                orderTypeDescription = this.orderTypeDescription,
                resultAllowDiff = this.resultAllowDiff == 1,
                resultAllowMod = this.resultAllowMod == 1,
                resultDiffProduct = this.resultDiffProduct == 1,
                resultDiffQty = this.resultDiffQty == 1,
                completed = this.completed == 1,
                startDate = this.startDate,
                finishDate = this.finishDate,
                clientId = this.clientId,
                userId = this.userId
            )
        }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(orderRequestId)
        parcel.writeLong(clientId)
        parcel.writeInt(completed)
        parcel.writeString(creationDate)
        parcel.writeString(description)
        parcel.writeString(externalId)
        parcel.writeString(finishDate)
        parcel.writeString(orderTypeDescription)
        parcel.writeInt(orderTypeId)
        parcel.writeInt(resultAllowDiff)
        parcel.writeInt(resultAllowMod)
        parcel.writeInt(resultDiffProduct)
        parcel.writeInt(resultDiffQty)
        parcel.writeString(startDate)
        parcel.writeLong(userId)
        parcel.writeString(zone)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderRequest> {
        override fun createFromParcel(parcel: Parcel): OrderRequest {
            return OrderRequest(parcel)
        }

        override fun newArray(size: Int): Array<OrderRequest?> {
            return arrayOfNulls(size)
        }
    }
}
