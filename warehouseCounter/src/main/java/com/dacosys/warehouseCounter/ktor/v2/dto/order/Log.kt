package com.dacosys.warehouseCounter.ktor.v2.dto.order

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.dacosys.warehouseCounter.room.entity.orderRequest.Log as LogRoom

@Serializable
data class Log(
    @SerialName(CLIENT_ID_KEY) var clientId: Long? = null,
    @SerialName(USER_ID_KEY) var userId: Long? = null,
    @SerialName(ITEM_ID_KEY) var itemId: Long? = null,
    @SerialName(ITEM_DESCRIPTION_KEY) var itemDescription: String = "",
    @SerialName(ITEM_CODE_KEY) var itemCode: String = "",
    @SerialName(SCANNED_CODE_KEY) var scannedCode: String = "",
    @SerialName(VARIATION_QTY_KEY) var variationQty: Double? = null,
    @SerialName(FINAL_QTY_KEY) var finalQty: Double? = null,
    @SerialName(DATE_KEY) var date: String? = null,
) : Parcelable {
    var filename: String = ""

    val logStatus: LogStatus
        get() {
            return if ((finalQty ?: 0.0) <= 0.0) LogStatus.SOME_QTY
            else LogStatus.QTY_ZERO
        }

    constructor(parcel: Parcel) : this(
        clientId = parcel.readValue(Long::class.java.classLoader) as? Long,
        userId = parcel.readValue(Long::class.java.classLoader) as? Long,
        itemId = parcel.readValue(Long::class.java.classLoader) as? Long,
        itemDescription = parcel.readString() ?: "",
        itemCode = parcel.readString() ?: "",
        scannedCode = parcel.readString() ?: "",
        variationQty = parcel.readValue(Double::class.java.classLoader) as? Double,
        finalQty = parcel.readValue(Double::class.java.classLoader) as? Double,
        date = parcel.readString()
    ) {
        filename = parcel.readString() ?: ""
    }

    fun toRoom(id: Long): LogRoom {
        return LogRoom(
            orderRequestId = id,
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

    override fun toString(): String {
        return itemCode
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(clientId)
        parcel.writeValue(userId)
        parcel.writeValue(itemId)
        parcel.writeString(itemDescription)
        parcel.writeString(itemCode)
        parcel.writeString(scannedCode)
        parcel.writeValue(variationQty)
        parcel.writeValue(finalQty)
        parcel.writeString(date)
        parcel.writeString(filename)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun hashCode(): Int {
        var result = clientId?.hashCode() ?: 0
        result = 31 * result + (userId?.hashCode() ?: 0)
        result = 31 * result + (itemId?.hashCode() ?: 0)
        result = 31 * result + itemDescription.hashCode()
        result = 31 * result + itemCode.hashCode()
        result = 31 * result + scannedCode.hashCode()
        result = 31 * result + (variationQty?.hashCode() ?: 0)
        result = 31 * result + (finalQty?.hashCode() ?: 0)
        result = 31 * result + (date?.hashCode() ?: 0)
        result = 31 * result + filename.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Log

        if (clientId != other.clientId) return false
        if (userId != other.userId) return false
        if (itemId != other.itemId) return false
        if (itemDescription != other.itemDescription) return false
        if (itemCode != other.itemCode) return false
        if (scannedCode != other.scannedCode) return false
        if (variationQty != other.variationQty) return false
        if (finalQty != other.finalQty) return false
        if (date != other.date) return false
        if (filename != other.filename) return false

        return true
    }

    companion object CREATOR : Parcelable.Creator<Log> {
        const val CLIENT_ID_KEY = "clientId"
        const val USER_ID_KEY = "userId"
        const val ITEM_ID_KEY = "itemId"
        const val ITEM_DESCRIPTION_KEY = "itemDescription"
        const val ITEM_CODE_KEY = "itemCode"
        const val SCANNED_CODE_KEY = "scannedCode"
        const val VARIATION_QTY_KEY = "variationQty"
        const val FINAL_QTY_KEY = "finalQty"
        const val DATE_KEY = "date"

        const val LOG_LIST_KEY = "logs"

        override fun createFromParcel(parcel: Parcel): Log {
            return Log(parcel)
        }

        override fun newArray(size: Int): Array<Log?> {
            return arrayOfNulls(size)
        }
    }
}