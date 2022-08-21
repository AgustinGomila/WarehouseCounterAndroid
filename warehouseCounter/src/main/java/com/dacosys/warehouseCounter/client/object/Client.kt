package com.dacosys.warehouseCounter.client.`object`

import android.content.ContentValues
import android.os.Parcelable
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.ACTIVE
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.ADDRESS
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.CITY
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.CLIENT_ID
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.CONTACT_NAME
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.COUNTRY_ID
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.LATITUDE
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.LONGITUDE
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.NAME
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.PHONE
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.TAX_NUMBER
import com.dacosys.warehouseCounter.client.dbHelper.ClientContract.ClientEntry.Companion.USER_ID
import com.dacosys.warehouseCounter.client.dbHelper.ClientDbHelper

class Client : Parcelable {
    fun setDataRead() {
        this.dataRead = true
    }

    override fun toString(): String {
        return name
    }

    private var dataRead: Boolean = false

    constructor(
        clientId: Long,
        name: String,
        contactName: String?,
        phone: String?,
        address: String?,
        city: String?,
        userId: Long?,
        active: Boolean,
        latitude: Double?,
        longitude: Double?,
        countryId: Long?,
        taxNumber: String?,
    ) {
        this.clientId = clientId
        this.name = name
        this.contactName = contactName
        this.phone = phone
        this.address = address
        this.city = city
        this.userId = userId
        this.active = active
        this.latitude = latitude
        this.longitude = longitude
        this.countryId = countryId
        this.taxNumber = taxNumber

        dataRead = true
    }

    constructor(id: Long, doChecks: Boolean) {
        clientId = id

        if (doChecks) {
            refreshData()
        }
    }

    private fun refreshData(): Boolean {
        val i = ClientDbHelper()
        val temp = i.selectById(this.clientId)

        dataRead = true
        return when {
            temp != null -> {
                clientId = temp.clientId
                active = temp.active
                address = temp.address
                city = temp.city
                contactName = temp.contactName
                countryId = temp.countryId
                latitude = temp.latitude
                longitude = temp.longitude
                name = temp.name
                phone = temp.phone
                userId = temp.userId
                taxNumber = temp.taxNumber

                true
            }
            else -> false
        }
    }

    var clientId: Long = 0
    var name: String = ""

    var contactName: String? = null
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return null
                }
            }
            return field
        }

    var phone: String? = null
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return null
                }
            }
            return field
        }

    var address: String? = null
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return null
                }
            }
            return field
        }

    var city: String? = null
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return null
                }
            }
            return field
        }

    var active: Boolean = false
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return false
                }
            }
            return field
        }

    var latitude: Double? = null
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return null
                }
            }
            return field
        }

    var longitude: Double? = null
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return null
                }
            }
            return field
        }

    var userId: Long? = null
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return null
                }
            }
            return field
        }

    var countryId: Long? = null
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return null
                }
            }
            return field
        }

    var taxNumber: String? = null
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return null
                }
            }
            return field
        }

    constructor(parcel: android.os.Parcel) {
        this.clientId = parcel.readLong()
        this.name = parcel.readString() ?: ""
        this.contactName = parcel.readString() ?: ""
        this.phone = parcel.readString() ?: ""
        this.address = parcel.readString() ?: ""
        this.city = parcel.readString() ?: ""
        this.userId = parcel.readLong()
        this.active = parcel.readByte() != 0.toByte()
        this.latitude = parcel.readDouble()
        this.longitude = parcel.readDouble()
        this.countryId = parcel.readLong()
        this.taxNumber = parcel.readString() ?: ""

        dataRead = parcel.readByte() != 0.toByte()
    }

    fun toContentValues(): ContentValues {
        val values = ContentValues()

        values.put(CLIENT_ID, clientId)
        values.put(NAME, name)
        values.put(CONTACT_NAME, contactName)
        values.put(PHONE, phone)
        values.put(ADDRESS, address)
        values.put(CITY, city)
        values.put(USER_ID, userId)
        values.put(ACTIVE, active)
        values.put(LATITUDE, latitude)
        values.put(LONGITUDE, longitude)
        values.put(COUNTRY_ID, countryId)
        values.put(TAX_NUMBER, taxNumber)

        return values
    }

    fun saveChanges(): Boolean {
        if (!dataRead) {
            if (!refreshData()) {
                return false
            }
        }

        val i = ClientDbHelper()
        return i.update(this)
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is Client) {
            false
        } else equals(this.clientId, other.clientId)
    }

    override fun hashCode(): Int {
        return this.clientId.toInt()
    }

    class CustomComparator : Comparator<Client> {
        override fun compare(o1: Client, o2: Client): Int {
            if (o1.clientId < o2.clientId) {
                return -1
            } else if (o1.clientId > o2.clientId) {
                return 1
            }
            return 0
        }
    }

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeLong(clientId)
        parcel.writeString(name)
        parcel.writeString(contactName)
        parcel.writeString(phone)
        parcel.writeString(address)
        parcel.writeString(city)
        parcel.writeLong(userId ?: 0L)
        parcel.writeByte(if (active) 1 else 0)
        parcel.writeDouble(latitude ?: 0.toDouble())
        parcel.writeDouble(longitude ?: 0.toDouble())
        parcel.writeLong(countryId ?: 0L)
        parcel.writeString(taxNumber)

        parcel.writeByte(if (dataRead) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun equals(a: Any?, b: Any): Boolean {
        return a != null && a == b
    }

    companion object CREATOR : Parcelable.Creator<Client> {
        override fun createFromParcel(parcel: android.os.Parcel): Client {
            return Client(parcel)
        }

        override fun newArray(size: Int): Array<Client?> {
            return arrayOfNulls(size)
        }

        fun add(
            name: String,
            contactName: String,
            phone: String,
            address: String,
            city: String,
            userId: Long,
            active: Boolean,
            latitude: Double,
            longitude: Double,
            countryId: Long,
            taxNumber: String,
        ): Client? {
            if (name.isEmpty()) {
                return null
            }

            val i = ClientDbHelper()

            val newId = i.insert(
                name,
                contactName,
                phone,
                address,
                city,
                userId,
                active,
                latitude,
                longitude,
                countryId,
                taxNumber
            )

            return if (newId < 1) {
                null
            } else {
                i.selectById(newId)
            }
        }
    }
}