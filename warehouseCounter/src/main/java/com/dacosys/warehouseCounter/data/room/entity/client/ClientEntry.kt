package com.dacosys.warehouseCounter.data.room.entity.client

abstract class ClientEntry {
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
