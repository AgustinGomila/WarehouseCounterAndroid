package com.dacosys.warehouseCounter.log.`object`

import android.os.Parcelable
import com.google.gson.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

class LogContent : Parcelable, JsonDeserializer<LogContent>, JsonSerializer<LogContent> {
    override fun serialize(
        src: LogContent?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?,
    ): JsonElement {
        val jsonObject = JsonObject()
        if (src != null) {
            jsonObject.addProperty("itemId", src.itemId)
            jsonObject.addProperty("itemStr", src.itemStr)
            jsonObject.addProperty("itemCode", src.itemCode)
            jsonObject.addProperty("lotId", src.lotId)
            jsonObject.addProperty("lotCode", src.lotCode)
            jsonObject.addProperty("scannedCode", src.scannedCode)
            jsonObject.addProperty("variationQty", src.variationQty)
            jsonObject.addProperty("finalQty", src.finalQty)
        }

        return jsonObject
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): LogContent {
        val jsonObject = json.asJsonObject

        val itemId = jsonObject.get("itemId")?.asLong
        val itemStr = jsonObject.get("itemStr")?.asString
        val itemCode = jsonObject.get("itemCode")?.asString
        val lotId = jsonObject.get("lotId")?.asLong
        val lotCode = jsonObject.get("lotCode")?.asString
        val scannedCode = jsonObject.get("scannedCode")?.asString
        val variationQty = jsonObject.get("variationQty")?.asDouble
        val finalQty = jsonObject.get("finalQty")?.asDouble

        val logCont = LogContent()

        logCont.itemId = itemId
        logCont.itemStr = itemStr ?: ""
        logCont.itemCode = itemCode ?: ""
        logCont.lotId = lotId
        logCont.lotCode = lotCode ?: ""
        logCont.scannedCode = scannedCode ?: ""
        logCont.variationQty = variationQty
        logCont.finalQty = finalQty

        return logCont
    }

    @Expose
    @SerializedName("itemId")
    var itemId: Long? = null

    @Expose
    @SerializedName("lotId")
    var lotId: Long? = null

    @Expose
    @SerializedName("itemStr")
    var itemStr: String = ""

    @Expose
    @SerializedName("itemCode")
    var itemCode: String = ""

    @Expose
    @SerializedName("lotCode")
    var lotCode: String = ""

    @Expose
    @SerializedName("scannedCode")
    var scannedCode: String = ""

    @Expose
    @SerializedName("variationQty")
    var variationQty: Double? = null

    @Expose
    @SerializedName("finalQty")
    var finalQty: Double? = null

    @Expose
    @SerializedName("date")
    var date: String? = null

    constructor(parcel: android.os.Parcel) : this() {
        itemId = parcel.readLong()
        itemStr = parcel.readString() ?: ""
        itemCode = parcel.readString() ?: ""
        lotId = parcel.readLong()
        lotCode = parcel.readString() ?: ""
        scannedCode = parcel.readString() ?: ""
        variationQty = parcel.readDouble()
        finalQty = parcel.readDouble()
        date = parcel.readString() ?: ""
    }

    constructor()

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
    ) {
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

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeLong(itemId ?: 0)
        parcel.writeString(itemStr)
        parcel.writeString(itemCode)
        parcel.writeLong(lotId ?: 0)
        parcel.writeString(lotCode)
        parcel.writeString(scannedCode)
        parcel.writeDouble(variationQty ?: 0.toDouble())
        parcel.writeDouble(finalQty ?: 0.toDouble())
        parcel.writeString(date)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LogContent> {
        override fun createFromParcel(parcel: android.os.Parcel): LogContent {
            return LogContent(parcel)
        }

        override fun newArray(size: Int): Array<LogContent?> {
            return arrayOfNulls(size)
        }
    }
}
