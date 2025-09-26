package com.example.warehouseCounter.data.room.entity.orderRequest

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.warehouseCounter.data.ktor.v2.dto.order.Log as LogKtor
import com.example.warehouseCounter.data.room.entity.orderRequest.LogEntry as Entry

@Entity(
    tableName = Entry.TABLE_NAME,
    indices = [
        Index(value = [Entry.ORDER_REQUEST_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.ORDER_REQUEST_ID}"),
        Index(value = [Entry.CLIENT_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.CLIENT_ID}"),
        Index(value = [Entry.USER_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.USER_ID}"),
        Index(value = [Entry.ITEM_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.ITEM_ID}"),
        Index(value = [Entry.ITEM_DESCRIPTION], name = "IDX_${Entry.TABLE_NAME}_${Entry.ITEM_DESCRIPTION}"),
        Index(value = [Entry.ITEM_CODE], name = "IDX_${Entry.TABLE_NAME}_${Entry.ITEM_CODE}"),
        Index(value = [Entry.SCANNED_CODE], name = "IDX_${Entry.TABLE_NAME}_${Entry.SCANNED_CODE}"),
    ]
)
data class Log(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = Entry.ID) var id: Long = 0L,
    @ColumnInfo(name = Entry.ORDER_REQUEST_ID) var orderRequestId: Long = 0L,
    @ColumnInfo(name = Entry.CLIENT_ID) var clientId: Long? = null,
    @ColumnInfo(name = Entry.USER_ID) var userId: Long? = null,
    @ColumnInfo(name = Entry.ITEM_ID) var itemId: Long? = null,
    @ColumnInfo(name = Entry.ITEM_DESCRIPTION) var itemDescription: String = "",
    @ColumnInfo(name = Entry.ITEM_CODE) var itemCode: String = "",
    @ColumnInfo(name = Entry.SCANNED_CODE) var scannedCode: String = "",
    @ColumnInfo(name = Entry.VARIATION_QTY) var variationQty: Double? = null,
    @ColumnInfo(name = Entry.FINAL_QTY) var finalQty: Double? = null,
    @ColumnInfo(name = Entry.DATE) var date: String? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        orderRequestId = parcel.readLong(),
        clientId = parcel.readValue(Long::class.java.classLoader) as? Long,
        userId = parcel.readValue(Long::class.java.classLoader) as? Long,
        itemId = parcel.readValue(Long::class.java.classLoader) as? Long,
        itemDescription = parcel.readString() ?: "",
        itemCode = parcel.readString() ?: "",
        scannedCode = parcel.readString() ?: "",
        variationQty = parcel.readValue(Double::class.java.classLoader) as? Double,
        finalQty = parcel.readValue(Double::class.java.classLoader) as? Double,
        date = parcel.readString()
    )

    val toLogKtor: LogKtor
        get() {
            return LogKtor(
                clientId = clientId,
                userId = userId,
                itemId = itemId,
                itemDescription = itemDescription,
                itemCode = itemCode,
                scannedCode = scannedCode,
                variationQty = variationQty,
                finalQty = finalQty,
                date = date
            )
        }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeLong(orderRequestId)
        parcel.writeValue(clientId)
        parcel.writeValue(userId)
        parcel.writeValue(itemId)
        parcel.writeString(itemDescription)
        parcel.writeString(itemCode)
        parcel.writeString(scannedCode)
        parcel.writeValue(variationQty)
        parcel.writeValue(finalQty)
        parcel.writeString(date)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Log> {
        override fun createFromParcel(parcel: Parcel): Log {
            return Log(parcel)
        }

        override fun newArray(size: Int): Array<Log?> {
            return arrayOfNulls(size)
        }
    }
}
