package com.dacosys.warehouseCounter.ktor.v2.dto.location

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Rack(
    @SerialName(ID_KEY) var id: Long = 0L,
    @SerialName(EXT_ID_KEY) var extId: String = "",
    @SerialName(CODE_KEY) var code: String = "",
    @SerialName(LEVELS_KEY) var levels: Int = 0,
    @SerialName(ACTIVE_KEY) var active: Boolean = false,
    @SerialName(WAREHOUSE_AREA_ID_KEY) var warehouseAreaId: Long = 0L,
    @SerialName(CREATION_DATE_KEY) var creationDate: String = "",
    @SerialName(MODIFICATION_DATE_KEY) var modificationDate: String = "",

    @SerialName(WAREHOUSE_AREA_KEY) var warehouseArea: WarehouseArea? = null,
) : Parcelable, Location() {

    override fun id(): Long = id
    override fun description(): String = code
    override fun locationType(): LocationType = LocationType.RACK

    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        extId = parcel.readString() ?: "",
        code = parcel.readString() ?: "",
        levels = parcel.readInt(),
        active = parcel.readValue(Boolean::class.java.classLoader) as Boolean,
        warehouseAreaId = parcel.readLong(),
        creationDate = parcel.readString() ?: "",
        modificationDate = parcel.readString() ?: "",

        warehouseArea = parcel.readParcelable(WarehouseArea::class.java.classLoader)
    )

    val warehouseAreaDescription: String
        get() {
            return warehouseArea?.description ?: ""
        }

    companion object CREATOR : Parcelable.Creator<Rack> {
        override fun createFromParcel(parcel: Parcel): Rack {
            return Rack(parcel)
        }

        override fun newArray(size: Int): Array<Rack?> {
            return arrayOfNulls(size)
        }

        const val ID_KEY = "id"
        const val EXT_ID_KEY = "external_id"
        const val CODE_KEY = "code"
        const val LEVELS_KEY = "levels"
        const val ACTIVE_KEY = "active"
        const val WAREHOUSE_AREA_ID_KEY = "warehouse_area_id"
        const val CREATION_DATE_KEY = "row_creation_date"
        const val MODIFICATION_DATE_KEY = "row_modification_date"

        const val WAREHOUSE_AREA_KEY = "warehouseArea"

        const val RACK_LIST_KEY = "racks"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(extId)
        parcel.writeString(code)
        parcel.writeInt(levels)
        parcel.writeValue(active)
        parcel.writeLong(warehouseAreaId)
        parcel.writeString(creationDate)
        parcel.writeString(modificationDate)

        parcel.writeParcelable(warehouseArea, flags)
    }

    override fun describeContents(): Int {
        return 0
    }
}
