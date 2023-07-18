package com.dacosys.warehouseCounter.ktor.v2.dto.location

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Warehouse(
    @SerialName(ID_KEY) var id: Long = 0L,
    @SerialName(ACRONYM_KEY) var acronym: String = "",
    @SerialName(DESCRIPTION_KEY) var description: String = "",
    @SerialName(EXTERNAL_ID_KEY) var externalId: String = "",
    @SerialName(ROW_CREATION_DATE_KEY) var rowCreationDate: String = "",
    @SerialName(ROW_MODIFICATION_DATE_KEY) var rowModificationDate: String = "",
    @SerialName(STATUS_ID_KEY) var statusId: Int = 0,
    @SerialName(WAREHOUSE_AREA_LIST_KEY) var areas: List<WarehouseArea>? = null,
    @SerialName(STATUS_KEY) var status: Status? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        acronym = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        externalId = parcel.readString() ?: "",
        rowCreationDate = parcel.readString() ?: "",
        rowModificationDate = parcel.readString() ?: "",
        statusId = parcel.readInt(),
        areas = parcel.createTypedArrayList(WarehouseArea),
        status = parcel.readParcelable(Status::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(acronym)
        parcel.writeString(description)
        parcel.writeString(externalId)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
        parcel.writeInt(statusId)
        parcel.writeTypedList(areas)
        parcel.writeParcelable(status, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Warehouse> {
        const val ID_KEY = "id"
        const val ACRONYM_KEY = "acronym"
        const val DESCRIPTION_KEY = "description"
        const val EXTERNAL_ID_KEY = "externalId"
        const val ROW_CREATION_DATE_KEY = "rowCreationDate"
        const val ROW_MODIFICATION_DATE_KEY = "rowModificationDate"
        const val STATUS_ID_KEY = "statusId"
        const val WAREHOUSE_AREA_LIST_KEY = "warehouseAreas"
        const val STATUS_KEY = "status"

        const val WAREHOUSE_LIST_KEY = "warehouses"

        override fun createFromParcel(parcel: Parcel): Warehouse {
            return Warehouse(parcel)
        }

        override fun newArray(size: Int): Array<Warehouse?> {
            return arrayOfNulls(size)
        }
    }
}