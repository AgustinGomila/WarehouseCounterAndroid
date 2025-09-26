package com.example.warehouseCounter.data.ktor.v2.dto.location

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
) : Parcelable, Location {
    override var locationType: LocationType
        get() = LocationType.RACK
        set(value) {
            locationType = value
        }

    override var locationId: Long
        get() = id
        set(value) {
            locationId = value
        }

    override var locationParentStr: String
        get() = warehouseArea?.description ?: ""
        set(value) {
            locationParentStr = value
        }

    override var locationExternalId: String
        get() = extId
        set(value) {
            locationExternalId = value
        }

    override var locationDescription: String
        get() = code
        set(value) {
            locationDescription = value
        }

    override var locationStatus: Status?
        get() = null
        set(value) {
            locationStatus = value
        }

    override var locationActive: Boolean
        get() = true
        set(value) {
            locationActive = value
        }

    override var hashCode: Int
        get() = hashCode()
        set(value) {
            hashCode = value
        }

    override var location: Location
        get() = this
        set(value) {
            location = value
        }

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Rack

        if (id != other.id) return false
        if (extId != other.extId) return false
        if (code != other.code) return false
        if (levels != other.levels) return false
        if (active != other.active) return false
        if (warehouseAreaId != other.warehouseAreaId) return false
        if (creationDate != other.creationDate) return false
        return modificationDate == other.modificationDate
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + extId.hashCode()
        result = 31 * result + code.hashCode()
        result = 31 * result + levels
        result = 31 * result + active.hashCode()
        result = 31 * result + warehouseAreaId.hashCode()
        result = 31 * result + creationDate.hashCode()
        result = 31 * result + modificationDate.hashCode()
        return result
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
}
