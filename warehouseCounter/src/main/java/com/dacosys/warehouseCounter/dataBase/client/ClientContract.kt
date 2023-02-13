package com.dacosys.warehouseCounter.dataBase.client

import android.provider.BaseColumns

/**
 * Created by Agustin on 28/12/2016.
 */

object ClientContract {
    fun getAllColumns(): Array<String> {
        return arrayOf(
            ClientEntry.CLIENT_ID,
            ClientEntry.NAME,
            ClientEntry.CONTACT_NAME,
            ClientEntry.PHONE,
            ClientEntry.ADDRESS,
            ClientEntry.CITY,
            ClientEntry.USER_ID,
            ClientEntry.ACTIVE,
            ClientEntry.LATITUDE,
            ClientEntry.LONGITUDE,
            ClientEntry.COUNTRY_ID,
            ClientEntry.TAX_NUMBER
        )
    }

    abstract class ClientEntry : BaseColumns {
        companion object {
            const val TABLE_NAME = "client"

            const val CLIENT_ID = "_id"
            const val NAME = "name"
            const val CONTACT_NAME = "contact_name"
            const val PHONE = "phone"
            const val ADDRESS = "address"
            const val CITY = "city"
            const val USER_ID = "user_id"
            const val ACTIVE = "active"
            const val LATITUDE = "latitude"
            const val LONGITUDE = "longitude"
            const val COUNTRY_ID = "country_id"
            const val TAX_NUMBER = "tax_number"
        }
    }
}