package com.dacosys.warehouseCounter.moshi.log

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
class Log() : Parcelable {
    @Json(name = "clientId")
    var clientId: Long? = null

    @Json(name = "userId")
    var userId: Long? = null

    @Json(name = "description")
    var description: String = ""

    @Json(name = "content")
    var content: List<LogContent> = ArrayList<LogContent>().toList()

    @Json(name = "creationDate")
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

    companion object CREATOR : Parcelable.Creator<Log> {
        override fun createFromParcel(parcel: Parcel): Log {
            return Log(parcel)
        }

        override fun newArray(size: Int): Array<Log?> {
            return arrayOfNulls(size)
        }
    }
}