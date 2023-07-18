package com.dacosys.warehouseCounter.ktor.v2.dto.item

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemCategory(
    @SerialName(ACTIVE_KEY) var active: Boolean = false,
    @SerialName(DESCRIPTION_KEY) var description: String = "",
    @SerialName(EXTERNAL_ID_KEY) var externalId: String = "",
    @SerialName(PARENT_ID_KEY) var parentId: Long = 0L,
    @SerialName(ID_KEY) var id: Long = 0L,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        active = parcel.readByte() != 0.toByte(),
        description = parcel.readString() ?: "",
        externalId = parcel.readString() ?: "",
        parentId = parcel.readLong(),
        id = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (active) 1 else 0)
        parcel.writeString(description)
        parcel.writeString(externalId)
        parcel.writeLong(parentId)
        parcel.writeLong(id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ItemCategory> {
        const val ACTIVE_KEY = "active"
        const val DESCRIPTION_KEY = "description"
        const val PARENT_ID_KEY = "parentId"
        const val EXTERNAL_ID_KEY = "externalId"
        const val ID_KEY = "id"

        override fun createFromParcel(parcel: Parcel): ItemCategory {
            return ItemCategory(parcel)
        }

        override fun newArray(size: Int): Array<ItemCategory?> {
            return arrayOfNulls(size)
        }
    }
}