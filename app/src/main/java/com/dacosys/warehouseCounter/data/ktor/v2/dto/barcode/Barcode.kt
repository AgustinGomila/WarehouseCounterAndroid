package com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Barcode(
    @SerialName(BARCODE_KEY) var barcode: String = "",
    @SerialName(SEARCH_STRING_KEY) var searchString: String = "",
    @SerialName(TYPE_KEY) var type: String = "",
    @SerialName(ID_KEY) var id: Long = 0L,
    @SerialName(EXT_ID_KEY) var extId: String = "",
    @SerialName(DESCRIPTION_KEY) var description: String = "",
    @SerialName(BARCODE_LABEL_TEMPLATE_ID_KEY) var barcodeLabelTemplateId: Long = 0L,
    @SerialName(BARCODE_LABEL_TEMPLATE_DESCRIPTION_KEY) var barcodeLabelTemplateDescription: String = "",
    @SerialName(BODY_KEY) var body: String = "",
) : Parcelable {

    constructor(parcel: Parcel) : this(
        barcode = parcel.readString() ?: "",
        searchString = parcel.readString() ?: "",
        type = parcel.readString() ?: "",
        id = parcel.readLong(),
        extId = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        barcodeLabelTemplateId = parcel.readLong(),
        barcodeLabelTemplateDescription = parcel.readString() ?: "",
        body = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(barcode)
        parcel.writeString(searchString)
        parcel.writeString(type)
        parcel.writeLong(id)
        parcel.writeString(extId)
        parcel.writeString(description)
        parcel.writeLong(barcodeLabelTemplateId)
        parcel.writeString(barcodeLabelTemplateDescription)
        parcel.writeString(body)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Barcode> {
        const val BARCODE_KEY = "barcode"
        const val SEARCH_STRING_KEY = "searchString"
        const val TYPE_KEY = "type"
        const val ID_KEY = "id"
        const val EXT_ID_KEY = "extId"
        const val DESCRIPTION_KEY = "description"
        const val BARCODE_LABEL_TEMPLATE_ID_KEY = "barcodeLabelTemplateId"
        const val BARCODE_LABEL_TEMPLATE_DESCRIPTION_KEY = "barcodeLabelTemplateDescription"
        const val BODY_KEY = "body"

        const val BARCODE_LIST_KEY = "barcodes"

        override fun createFromParcel(parcel: Parcel): Barcode {
            return Barcode(parcel)
        }

        override fun newArray(size: Int): Array<Barcode?> {
            return arrayOfNulls(size)
        }
    }
}
