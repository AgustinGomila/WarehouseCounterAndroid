package com.dacosys.warehouseCounter.data.ktor.v1.dto.ptlOrder

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Client(
    @SerialName(ID_KEY) val id: Long,
    @SerialName(NAME_KEY) val name: String,
    @SerialName(TAX_NUMBER_KEY) val taxNumber: String,
    @SerialName(CONTACT_NAME_KEY) val contactName: String?,
    @SerialName(PHONE_KEY) val phone: String?,
    @SerialName(ADDRESS_KEY) val address: String?,
    @SerialName(CITY_KEY) val city: String,
    @SerialName(ZIP_CODE_KEY) val zipCode: String?,
    @SerialName(COUNTRY_ID_KEY) val countryId: String,
    @SerialName(ACTIVE_KEY) val active: Int,
    @SerialName(LATITUDE_KEY) val latitude: String?,
    @SerialName(LONGITUDE_KEY) val longitude: String?,
    @SerialName(COLLECTOR_USER_ID_KEY) val collectorUserId: Long?,
    @SerialName(ROW_CREATION_DATE_KEY) val rowCreationDate: String,
    @SerialName(ROW_MODIFICATION_DATE_KEY) val rowModificationDate: String,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        name = parcel.readString() ?: "",
        taxNumber = parcel.readString() ?: "",
        contactName = parcel.readString() ?: "",
        phone = parcel.readString() ?: "",
        address = parcel.readString() ?: "",
        city = parcel.readString() ?: "",
        zipCode = parcel.readString() ?: "",
        countryId = parcel.readString() ?: "",
        active = parcel.readInt(),
        latitude = parcel.readString() ?: "",
        longitude = parcel.readString() ?: "",
        collectorUserId = parcel.readValue(Long::class.java.classLoader) as? Long,
        rowCreationDate = parcel.readString() ?: "",
        rowModificationDate = parcel.readString() ?: ""
    )

    companion object CREATOR : Parcelable.Creator<Client> {
        override fun createFromParcel(parcel: Parcel): Client {
            return Client(parcel)
        }

        override fun newArray(size: Int): Array<Client?> {
            return arrayOfNulls(size)
        }

        const val ID_KEY = "id"
        const val NAME_KEY = "name"
        const val TAX_NUMBER_KEY = "tax_number"
        const val CONTACT_NAME_KEY = "contact_name"
        const val PHONE_KEY = "phone"
        const val ADDRESS_KEY = "address"
        const val CITY_KEY = "city"
        const val ZIP_CODE_KEY = "zip_code"
        const val COUNTRY_ID_KEY = "country_id"
        const val ACTIVE_KEY = "active"
        const val LATITUDE_KEY = "latitude"
        const val LONGITUDE_KEY = "longitude"
        const val COLLECTOR_USER_ID_KEY = "collector_user_id"
        const val ROW_CREATION_DATE_KEY = "row_creation_date"
        const val ROW_MODIFICATION_DATE_KEY = "row_modification_date"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeString(taxNumber)
        parcel.writeString(contactName)
        parcel.writeString(phone)
        parcel.writeString(address)
        parcel.writeString(city)
        parcel.writeString(zipCode)
        parcel.writeString(countryId)
        parcel.writeInt(active)
        parcel.writeString(latitude)
        parcel.writeString(longitude)
        parcel.writeValue(collectorUserId)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
    }

    override fun describeContents(): Int {
        return 0
    }
}
