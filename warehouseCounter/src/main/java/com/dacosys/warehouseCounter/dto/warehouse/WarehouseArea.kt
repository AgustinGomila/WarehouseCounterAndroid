package com.dacosys.warehouseCounter.dto.warehouse

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WarehouseArea(
    @SerialName(idTag) var id: Long,
    @SerialName(extIdTag) var extId: String,
    @SerialName(descriptionTag) var description: String,
    @SerialName(warehouseIdTag) var warehouseId: Long,
    @SerialName(warehouseDescriptionTag) var warehouseDescription: String,
    @SerialName(statusTag) var status: String,
    @SerialName(creationDateTag) var creationDate: String,
    @SerialName(modificationDateTag) var modificationDate: String,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        extId = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        warehouseId = parcel.readLong(),
        warehouseDescription = parcel.readString() ?: "",
        status = parcel.readString() ?: "",
        creationDate = parcel.readString() ?: "",
        modificationDate = parcel.readString() ?: ""
    )

    companion object CREATOR : Parcelable.Creator<WarehouseArea> {
        override fun createFromParcel(parcel: Parcel): WarehouseArea {
            return WarehouseArea(parcel)
        }

        override fun newArray(size: Int): Array<WarehouseArea?> {
            return arrayOfNulls(size)
        }

        const val idTag = "id"
        const val extIdTag = "extId"
        const val descriptionTag = "description"
        const val warehouseIdTag = "warehouseId"
        const val warehouseDescriptionTag = "warehouseDescription"
        const val statusTag = "status"
        const val creationDateTag = "creationDate"
        const val modificationDateTag = "modificationDate"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(extId)
        parcel.writeString(description)
        parcel.writeLong(warehouseId)
        parcel.writeString(warehouseDescription)
        parcel.writeString(status)
        parcel.writeString(creationDate)
        parcel.writeString(modificationDate)
    }

    override fun describeContents(): Int {
        return 0
    }
}