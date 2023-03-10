package com.dacosys.warehouseCounter.dto.warehouse

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WarehouseArea(
    @Json(name = idTag) var id: Long,
    @Json(name = extIdTag) var extId: String,
    @Json(name = descriptionTag) var description: String,
    @Json(name = warehouseIdTag) var warehouseId: Long,
    @Json(name = warehouseDescriptionTag) var warehouseDescription: String,
    @Json(name = statusTag) var status: String,
    @Json(name = creationDateTag) var creationDate: String,
    @Json(name = modificationDateTag) var modificationDate: String,
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

@JsonClass(generateAdapter = true)
data class Warehouse(
    @Json(name = areasTag) var areas: List<WarehouseArea>,
) {
    companion object {
        const val areasTag = "warehouse_areas"
    }
}