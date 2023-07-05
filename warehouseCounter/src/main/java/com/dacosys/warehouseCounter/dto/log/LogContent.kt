package com.dacosys.warehouseCounter.dto.log

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Ignore
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class LogContent() : Parcelable {
    @Json(name = "itemId")
    var itemId: Long? = null

    @Json(name = "lotId")
    var lotId: Long? = null

    @Json(name = "itemStr")
    var itemStr: String = ""

    @Json(name = "itemCode")
    var itemCode: String = ""

    @Json(name = "lotCode")
    var lotCode: String = ""

    @Json(name = "scannedCode")
    var scannedCode: String = ""

    @Json(name = "variationQty")
    var variationQty: Double? = null

    @Json(name = "finalQty")
    var finalQty: Double? = null

    @Json(name = "date")
    var date: String? = null

    @Ignore
    val contentStatus: LogContentStatus =
        if ((finalQty ?: 0.0) <= 0.0) LogContentStatus.SOME_QTY
        else LogContentStatus.QTY_ZERO

    constructor(parcel: Parcel) : this() {
        itemId = parcel.readValue(Long::class.java.classLoader) as? Long
        lotId = parcel.readValue(Long::class.java.classLoader) as? Long
        itemStr = parcel.readString() ?: ""
        itemCode = parcel.readString() ?: ""
        lotCode = parcel.readString() ?: ""
        scannedCode = parcel.readString() ?: ""
        variationQty = parcel.readValue(Double::class.java.classLoader) as? Double
        finalQty = parcel.readValue(Double::class.java.classLoader) as? Double
        date = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(itemId)
        parcel.writeValue(lotId)
        parcel.writeString(itemStr)
        parcel.writeString(itemCode)
        parcel.writeString(lotCode)
        parcel.writeString(scannedCode)
        parcel.writeValue(variationQty)
        parcel.writeValue(finalQty)
        parcel.writeString(date)
    }

    constructor(
        itemId: Long?,
        itemStr: String,
        itemCode: String,
        lotId: Long?,
        lotCode: String,
        scannedCode: String,
        variationQty: Double?,
        finalQty: Double?,
        date: String,
    ) : this() {
        this.itemId = itemId
        this.itemStr = itemStr
        this.itemCode = itemCode
        this.lotId = lotId
        this.lotCode = lotCode
        this.scannedCode = scannedCode
        this.variationQty = variationQty
        this.finalQty = finalQty
        this.date = date
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun hashCode(): Int {
        var result = itemId?.hashCode() ?: 0
        result = 31 * result + (lotId?.hashCode() ?: 0)
        result = 31 * result + scannedCode.hashCode()
        result = 31 * result + (variationQty?.hashCode() ?: 0)
        result = 31 * result + (finalQty?.hashCode() ?: 0)
        result = 31 * result + (date?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LogContent

        if (itemId != other.itemId) return false
        if (lotId != other.lotId) return false
        if (scannedCode != other.scannedCode) return false
        if (variationQty != other.variationQty) return false
        if (finalQty != other.finalQty) return false
        return date == other.date
    }

    companion object CREATOR : Parcelable.Creator<LogContent> {
        override fun createFromParcel(parcel: Parcel): LogContent {
            return LogContent(parcel)
        }

        override fun newArray(size: Int): Array<LogContent?> {
            return arrayOfNulls(size)
        }
    }
}
