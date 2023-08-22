package com.dacosys.warehouseCounter.ktor.v2.dto.location

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Status(
    @SerialName(ID_KEY) var id: Long,
    @SerialName(DESCRIPTION_KEY) var description: String,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        description = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Status

        return id == other.id
    }

    companion object CREATOR : Parcelable.Creator<Status> {
        const val ID_KEY = "id"
        const val DESCRIPTION_KEY = "description"
        override fun createFromParcel(parcel: Parcel): Status {
            return Status(parcel)
        }

        override fun newArray(size: Int): Array<Status?> {
            return arrayOfNulls(size)
        }
    }
}
