package com.dacosys.warehouseCounter.ktor.v2.dto.location

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WarehouseArea(
    @SerialName(ID_KEY) var id: Long = 0L,
    @SerialName(EXTERNAL_ID_KEY) var externalId: String = "",
    @SerialName(DESCRIPTION_KEY) var description: String = "",
    @SerialName(WAREHOUSE_ID_KEY) var warehouseId: Long = 0L,
    @SerialName(ACRONYM_KEY) var acronym: String = "",
    @SerialName(STATUS_ID_KEY) var statusId: Int = 0,
    @SerialName(CREATION_DATE_KEY) var creationDate: String = "",
    @SerialName(MODIFICATION_DATE_KEY) var modificationDate: String = "",

    @SerialName(WAREHOUSE_KEY) var warehouse: Warehouse? = null,
    @SerialName(STATUS_KEY) var status: Status? = null,
    @SerialName(PTL_LIST_KEY) var ptlList: List<String>? = null,

    ) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        externalId = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        warehouseId = parcel.readLong(),
        acronym = parcel.readString() ?: "",
        statusId = parcel.readInt(),
        creationDate = parcel.readString() ?: "",
        modificationDate = parcel.readString() ?: "",

        warehouse = parcel.readParcelable(Warehouse::class.java.classLoader),
        status = parcel.readParcelable(Status::class.java.classLoader),
        ptlList = parcel.createStringArrayList()
    )

    val warehouseDescription: String
        get() {
            return warehouse?.description ?: ""
        }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(externalId)
        parcel.writeString(description)
        parcel.writeLong(warehouseId)
        parcel.writeString(acronym)
        parcel.writeInt(statusId)
        parcel.writeString(creationDate)
        parcel.writeString(modificationDate)

        parcel.writeParcelable(warehouse, flags)
        parcel.writeParcelable(status, flags)
        parcel.writeStringList(ptlList)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WarehouseArea> {
        const val ID_KEY = "id"
        const val ACRONYM_KEY = "acronym"
        const val DESCRIPTION_KEY = "description"
        const val EXTERNAL_ID_KEY = "externalId"
        const val WAREHOUSE_ID_KEY = "warehouseId"
        const val STATUS_ID_KEY = "statusId"
        const val CREATION_DATE_KEY = "rowCreationDate"
        const val MODIFICATION_DATE_KEY = "rowModificationDate"

        const val WAREHOUSE_KEY = "warehouse"
        const val STATUS_KEY = "status"
        const val PTL_LIST_KEY = "ptls"

        const val WAREHOUSE_AREA_LIST_KEY = "warehouseAreas"

        override fun createFromParcel(parcel: Parcel): WarehouseArea {
            return WarehouseArea(parcel)
        }

        override fun newArray(size: Int): Array<WarehouseArea?> {
            return arrayOfNulls(size)
        }
    }
}