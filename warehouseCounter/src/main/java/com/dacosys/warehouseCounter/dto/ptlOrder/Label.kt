package com.dacosys.warehouseCounter.dto.ptlOrder

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Label(
    @SerialName(BARCODE_KEY) val barcode: String,
    @SerialName(SEARCH_STRING_KEY) val searchString: Long,
    @SerialName(TYPE_KEY) val type: String,
    @SerialName(ID_KEY) val id: Long,
    @SerialName(EXT_ID_KEY) val extId: String,
    @SerialName(DESCRIPTION_KEY) val description: String,
    @SerialName(BARCODE_LABEL_TEMPLATE_ID_KEY) val barcodeLabelTemplateId: String,
    @SerialName(BARCODE_LABEL_TEMPLATE_DESCRIPTION_KEY) val barcodeLabelTemplateDescription: String,
    @SerialName(BODY_KEY) val body: String,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        barcode = parcel.readString() ?: "",
        searchString = parcel.readLong(),
        type = parcel.readString() ?: "",
        id = parcel.readLong(),
        extId = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        barcodeLabelTemplateId = parcel.readString() ?: "",
        barcodeLabelTemplateDescription = parcel.readString() ?: "",
        body = parcel.readString() ?: ""
    )

    companion object CREATOR : Parcelable.Creator<Label> {
        override fun createFromParcel(parcel: Parcel): Label {
            return Label(parcel)
        }

        override fun newArray(size: Int): Array<Label?> {
            return arrayOfNulls(size)
        }

        const val BARCODE_KEY = "barcode"
        const val SEARCH_STRING_KEY = "searchString"
        const val TYPE_KEY = "type"
        const val ID_KEY = "id"
        const val EXT_ID_KEY = "extId"
        const val DESCRIPTION_KEY = "description"
        const val BARCODE_LABEL_TEMPLATE_ID_KEY = "barcodeLabelTemplateId"
        const val BARCODE_LABEL_TEMPLATE_DESCRIPTION_KEY = "barcodeLabelTemplateDescription"
        const val BODY_KEY = "body"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(barcode)
        parcel.writeLong(searchString)
        parcel.writeString(type)
        parcel.writeLong(id)
        parcel.writeString(extId)
        parcel.writeString(description)
        parcel.writeString(barcodeLabelTemplateId)
        parcel.writeString(barcodeLabelTemplateDescription)
        parcel.writeString(body)
    }

    override fun describeContents(): Int {
        return 0
    }
}