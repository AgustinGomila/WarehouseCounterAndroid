package com.dacosys.warehouseCounter.dto.ptlOrder

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Json

data class PtlItem(
    @Json(name = ID_KEY) val id: Long,
    @Json(name = EXTERNAL_ID_KEY) val externalId: Long,
    @Json(name = DESCRIPTION_KEY) val description: String,
    @Json(name = EAN_KEY) val ean: String,
    @Json(name = PRICE_KEY) val price: Double,
    @Json(name = ITEM_CATEGORY_ID_KEY) val itemCategoryId: Long?,
    @Json(name = EXTERNAL_ID2_KEY) val externalId2: String?,
    @Json(name = ROW_CREATION_DATE_KEY) val rowCreationDate: String,
    @Json(name = ROW_MODIFICATION_DATE_KEY) val rowModificationDate: String,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        externalId = parcel.readLong(),
        description = parcel.readString() ?: "",
        ean = parcel.readString() ?: "",
        price = parcel.readDouble(),
        itemCategoryId = parcel.readValue(Long::class.java.classLoader) as? Long,
        externalId2 = parcel.readString() ?: "",
        rowCreationDate = parcel.readString() ?: "",
        rowModificationDate = parcel.readString() ?: ""
    )

    companion object CREATOR : Parcelable.Creator<PtlItem> {
        override fun createFromParcel(parcel: Parcel): PtlItem {
            return PtlItem(parcel)
        }

        override fun newArray(size: Int): Array<PtlItem?> {
            return arrayOfNulls(size)
        }

        const val ID_KEY = "id"
        const val EXTERNAL_ID_KEY = "ext_id"
        const val DESCRIPTION_KEY = "description"
        const val EAN_KEY = "ean"
        const val PRICE_KEY = "price"
        const val ITEM_CATEGORY_ID_KEY = "item_category_id"
        const val EXTERNAL_ID2_KEY = "external_id"
        const val ROW_CREATION_DATE_KEY = "row_creation_date"
        const val ROW_MODIFICATION_DATE_KEY = "row_modification_date"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeLong(externalId)
        parcel.writeString(description)
        parcel.writeString(ean)
        parcel.writeDouble(price)
        parcel.writeValue(itemCategoryId)
        parcel.writeString(externalId2)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
    }

    override fun describeContents(): Int {
        return 0
    }
}