package com.dacosys.warehouseCounter.ktor.v1.dto.warehouse

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WarehouseArea(
    @SerialName(ID_KEY) var id: Long,
    @SerialName(EXT_ID_KEY) var extId: String,
    @SerialName(DESCRIPTION_KEY) var description: String,
    @SerialName(WAREHOUSE_ID_KEY) var warehouseId: Long,
    @SerialName(WAREHOUSE_DESCRIPTION_KEY) var warehouseDescription: String,
    @SerialName(STATUS_KEY) var status: String,
    @SerialName(CREATION_DATE_KEY) var creationDate: String,
    @SerialName(MODIFICATION_DATE_KEY) var modificationDate: String,
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

        const val ID_KEY = "id"
        const val EXT_ID_KEY = "extId"
        const val DESCRIPTION_KEY = "description"
        const val WAREHOUSE_ID_KEY = "warehouseId"
        const val WAREHOUSE_DESCRIPTION_KEY = "warehouseDescription"
        const val STATUS_KEY = "status"
        const val CREATION_DATE_KEY = "creationDate"
        const val MODIFICATION_DATE_KEY = "modificationDate"
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