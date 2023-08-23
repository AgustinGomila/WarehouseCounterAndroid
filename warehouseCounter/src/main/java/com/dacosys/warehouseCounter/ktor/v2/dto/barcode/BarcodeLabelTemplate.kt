package com.dacosys.warehouseCounter.ktor.v2.dto.barcode

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BarcodeLabelTemplate(
    @SerialName(TEMPLATE_ID_KEY) var templateId: Long = 0,
    @SerialName(ACTIVE_KEY) var active: Int = 0,
    @SerialName(ATTRIBUTE_COMPOSITION_ID_A_KEY) var attributeCompositionIdA: Long? = null,
    @SerialName(ATTRIBUTE_COMPOSITION_ID_B_KEY) var attributeCompositionIdB: Long? = null,
    @SerialName(ATTRIBUTE_COMPOSITION_ID_C_KEY) var attributeCompositionIdC: Long? = null,
    @SerialName(BARCODE_LABEL_TYPE_ID_KEY) var barcodeLabelTypeId: Long = 0,
    @SerialName(CONTENT_KEY) var content: String = "",
    @SerialName(DESCRIPTION_KEY) var description: String = "",
    @SerialName(ROW_CREATION_DATE_KEY) var rowCreationDate: String = "",
    @SerialName(ROW_MODIFICATION_DATE_KEY) var rowModificationDate: String = "",
) : Parcelable {
    constructor(parcel: Parcel) : this(
        templateId = parcel.readLong(),
        active = parcel.readInt(),
        attributeCompositionIdA = parcel.readValue(Long::class.java.classLoader) as? Long,
        attributeCompositionIdB = parcel.readValue(Long::class.java.classLoader) as? Long,
        attributeCompositionIdC = parcel.readValue(Long::class.java.classLoader) as? Long,
        barcodeLabelTypeId = parcel.readLong(),
        content = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        rowCreationDate = parcel.readString() ?: "",
        rowModificationDate = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(templateId)
        parcel.writeInt(active)
        parcel.writeValue(attributeCompositionIdA)
        parcel.writeValue(attributeCompositionIdB)
        parcel.writeValue(attributeCompositionIdC)
        parcel.writeLong(barcodeLabelTypeId)
        parcel.writeString(content)
        parcel.writeString(description)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BarcodeLabelTemplate> {
        const val TEMPLATE_ID_KEY = "id"
        const val ACTIVE_KEY = "active"
        const val ATTRIBUTE_COMPOSITION_ID_A_KEY = "attributeCompositionIdA"
        const val ATTRIBUTE_COMPOSITION_ID_B_KEY = "attributeCompositionIdB"
        const val ATTRIBUTE_COMPOSITION_ID_C_KEY = "attributeCompositionIdC"
        const val BARCODE_LABEL_TYPE_ID_KEY = "barcodeLabelTypeId"
        const val CONTENT_KEY = "content"
        const val DESCRIPTION_KEY = "description"
        const val ROW_CREATION_DATE_KEY = "rowCreationDate"
        const val ROW_MODIFICATION_DATE_KEY = "rowModificationDate"

        const val BARCODE_LABEL_TEMPLATE_LIST_KEY = "barcodeLabelTemplates"

        override fun createFromParcel(parcel: Parcel): BarcodeLabelTemplate {
            return BarcodeLabelTemplate(parcel)
        }

        override fun newArray(size: Int): Array<BarcodeLabelTemplate?> {
            return arrayOfNulls(size)
        }
    }
}
