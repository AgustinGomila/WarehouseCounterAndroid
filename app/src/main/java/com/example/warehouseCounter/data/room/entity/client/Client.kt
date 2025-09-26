package com.example.warehouseCounter.data.room.entity.client

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.warehouseCounter.data.room.entity.client.ClientEntry as Entry

@Entity(
    tableName = Entry.TABLE_NAME,
    indices = [
        Index(value = [Entry.NAME], name = "IDX_${Entry.TABLE_NAME}_${Entry.NAME}"),
        Index(value = [Entry.CONTACT_NAME], name = "IDX_${Entry.TABLE_NAME}_${Entry.CONTACT_NAME}"),
    ]
)
data class Client(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = Entry.CLIENT_ID) val clientId: Long = 0L,
    @ColumnInfo(name = Entry.NAME) val name: String = "",
    @ColumnInfo(name = Entry.CONTACT_NAME) val contactName: String? = null,
    @ColumnInfo(name = Entry.PHONE) val phone: String? = null,
    @ColumnInfo(name = Entry.ADDRESS) val address: String? = null,
    @ColumnInfo(name = Entry.CITY) val city: String? = null,
    @ColumnInfo(name = Entry.USER_ID) val userId: Int? = null,
    @ColumnInfo(name = Entry.ACTIVE) val active: Int = 1,
    @ColumnInfo(name = Entry.LATITUDE) val latitude: Float? = null,
    @ColumnInfo(name = Entry.LONGITUDE) val longitude: Float? = null,
    @ColumnInfo(name = Entry.COUNTRY_ID) val countryId: Int? = null,
    @ColumnInfo(name = Entry.TAX_NUMBER) val taxNumber: String? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        clientId = parcel.readLong(),
        name = parcel.readString() ?: "",
        contactName = parcel.readString(),
        phone = parcel.readString(),
        address = parcel.readString(),
        city = parcel.readString(),
        userId = parcel.readValue(Int::class.java.classLoader) as? Int,
        active = parcel.readInt(),
        latitude = parcel.readValue(Float::class.java.classLoader) as? Float,
        longitude = parcel.readValue(Float::class.java.classLoader) as? Float,
        countryId = parcel.readValue(Int::class.java.classLoader) as? Int,
        taxNumber = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(clientId)
        parcel.writeString(name)
        parcel.writeString(contactName)
        parcel.writeString(phone)
        parcel.writeString(address)
        parcel.writeString(city)
        parcel.writeValue(userId)
        parcel.writeInt(active)
        parcel.writeValue(latitude)
        parcel.writeValue(longitude)
        parcel.writeValue(countryId)
        parcel.writeString(taxNumber)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Client

        return clientId == other.clientId
    }

    override fun hashCode(): Int {
        var result = clientId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (contactName?.hashCode() ?: 0)
        result = 31 * result + (phone?.hashCode() ?: 0)
        result = 31 * result + (address?.hashCode() ?: 0)
        result = 31 * result + (city?.hashCode() ?: 0)
        result = 31 * result + (userId ?: 0)
        result = 31 * result + active
        result = 31 * result + (latitude?.hashCode() ?: 0)
        result = 31 * result + (longitude?.hashCode() ?: 0)
        result = 31 * result + (countryId ?: 0)
        result = 31 * result + (taxNumber?.hashCode() ?: 0)
        return result
    }

    companion object CREATOR : Parcelable.Creator<Client> {
        override fun createFromParcel(parcel: Parcel): Client {
            return Client(parcel)
        }

        override fun newArray(size: Int): Array<Client?> {
            return arrayOfNulls(size)
        }
    }
}

