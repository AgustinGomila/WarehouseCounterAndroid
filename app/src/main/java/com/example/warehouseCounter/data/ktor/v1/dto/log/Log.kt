package com.example.warehouseCounter.data.ktor.v1.dto.log

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class Log() : Parcelable {
    @SerialName("clientId")
    var clientId: Long? = null

    @SerialName("userId")
    var userId: Long? = null

    @SerialName("description")
    var description: String = ""

    @SerialName("content")
    var content: List<LogContent> = listOf()

    @SerialName("creationDate")
    var creationDate: String? = null

    var filename: String = ""

    constructor(parcel: Parcel) : this() {
        filename = parcel.readString() ?: ""
        clientId = parcel.readValue(Long::class.java.classLoader) as? Long
        userId = parcel.readValue(Long::class.java.classLoader) as? Long
        description = parcel.readString() ?: ""
        creationDate = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(filename)
        parcel.writeValue(clientId)
        parcel.writeValue(userId)
        parcel.writeString(description)
        parcel.writeString(creationDate)
    }

    constructor(
        clientId: Long?,
        userId: Long?,
        description: String,
        creationDate: String,
        content: ArrayList<LogContent>,
    ) : this() {
        this.clientId = clientId
        this.userId = userId
        this.description = description
        this.creationDate = creationDate
        this.content = content
    }

    override fun toString(): String {
        return description
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun hashCode(): Int {
        var result = clientId?.hashCode() ?: 0
        result = 31 * result + (userId?.hashCode() ?: 0)
        result = 31 * result + (creationDate?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Log

        if (clientId != other.clientId) return false
        if (userId != other.userId) return false
        return creationDate == other.creationDate
    }

    companion object CREATOR : Parcelable.Creator<Log> {
        override fun createFromParcel(parcel: Parcel): Log {
            return Log(parcel)
        }

        override fun newArray(size: Int): Array<Log?> {
            return arrayOfNulls(size)
        }
    }
}
