package com.dacosys.warehouseCounter.itemCode.`object`

import android.content.ContentValues
import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.item.`object`.Item
import com.dacosys.warehouseCounter.itemCode.dbHelper.ItemCodeContract.ItemCodeEntry.Companion.CODE
import com.dacosys.warehouseCounter.itemCode.dbHelper.ItemCodeContract.ItemCodeEntry.Companion.ITEM_ID
import com.dacosys.warehouseCounter.itemCode.dbHelper.ItemCodeContract.ItemCodeEntry.Companion.QTY
import com.dacosys.warehouseCounter.itemCode.dbHelper.ItemCodeContract.ItemCodeEntry.Companion.TO_UPLOAD
import com.dacosys.warehouseCounter.itemCode.dbHelper.ItemCodeDbHelper
import com.google.gson.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

class ItemCode : Parcelable, JsonSerializer<ItemCode>, JsonDeserializer<ItemCode> {
    override fun serialize(
        src: ItemCode?,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement {
        val jsonObject = JsonObject()
        if (src != null) {
            jsonObject.addProperty("itemId", src.itemId)
            jsonObject.addProperty("code", src.code)
            jsonObject.addProperty("qty", src.qty)
        }

        return jsonObject
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): ItemCode {
        val jsonObject = json.asJsonObject

        val itemId = jsonObject.get("itemId")?.asLong
        val code = jsonObject.get("code")?.asString
        val qty = jsonObject.get("qty")?.asDouble

        val ic = ItemCode()

        ic.itemId = itemId ?: 0L
        ic.code = code ?: ""
        ic.qty = qty ?: 0.toDouble()

        return ic
    }

    private var dataRead: Boolean = false

    val item: Item?
        get() {
            val iDbH = com.dacosys.warehouseCounter.item.dbHelper.ItemDbHelper()
            return iDbH.selectById(itemId)
        }

    constructor()

    constructor(
        itemId: Long,
        code: String,
        qty: Double,
        toUpload: Boolean,
    ) {
        this.itemId = itemId
        this.code = code
        this.qty = qty
        this.toUpload = toUpload

        this.dataRead = true
    }

    @Expose
    @SerializedName("itemId")
    var itemId: Long = 0
        get() {
            return if (!dataRead) {
                0
            } else field
        }

    @Expose
    @SerializedName("code")
    var code: String = ""
        get() {
            return if (!dataRead) {
                ""
            } else field
        }

    @Expose
    @SerializedName("qty")
    var qty: Double = 0.toDouble()
        get() {
            return if (!dataRead) {
                0.toDouble()
            } else field
        }

    var toUpload: Boolean = false
        get() {
            return if (!dataRead) {
                false
            } else field
        }

    constructor(parcel: Parcel) : this() {
        itemId = parcel.readLong()
        code = parcel.readString() ?: ""
        qty = parcel.readDouble()
        toUpload = parcel.readByte() != 0.toByte()

        dataRead = parcel.readByte() != 0.toByte()
    }

    fun toContentValues(): ContentValues {
        val values = ContentValues()
        values.put(ITEM_ID, itemId)
        values.put(CODE, code)
        values.put(QTY, qty)
        values.put(TO_UPLOAD, toUpload)

        return values
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is ItemCode) {
            false
        } else this.itemId == other.itemId && this.code == other.code
    }

    override fun hashCode(): Int {
        var hashCode = 1

        hashCode = (hashCode * 37 + this.itemId).toInt()
        hashCode = hashCode * 37 + this.code.hashCode()

        return hashCode
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(itemId)
        parcel.writeString(code)
        parcel.writeDouble(qty)
        parcel.writeByte(if (toUpload) 1 else 0)

        parcel.writeByte(if (dataRead) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ItemCode> {
        override fun createFromParcel(parcel: Parcel): ItemCode {
            return ItemCode(parcel)
        }

        override fun newArray(size: Int): Array<ItemCode?> {
            return arrayOfNulls(size)
        }

        fun fromJson(json: String): ItemCode? {
            // Configure GSON
            val gSon = GsonBuilder()
                .registerTypeAdapter(
                    ItemCode::class.java,
                    ItemCode()
                )
                .excludeFieldsWithoutExposeAnnotation()
                .setLenient()
                .create()

            return gSon.fromJson(json, ItemCode::class.java)
        }

        fun toJson(or: ItemCode): String {
            val gSon = GsonBuilder()
                .registerTypeAdapter(
                    ItemCode::class.java,
                    ItemCode()
                )
                .excludeFieldsWithoutExposeAnnotation()
                .setLenient()
                .create()

            val x = gSon.toJson(or)
            println(x)

            // Serialization
            return x
        }

        fun add(
            itemId: Long,
            code: String,
            qty: Double,
        ): Boolean {
            if (itemId <= 0 || code.isEmpty() || qty <= 0) {
                return false
            }

            val i = ItemCodeDbHelper()
            return i.insert(itemId, code, qty, true)
        }
    }
}