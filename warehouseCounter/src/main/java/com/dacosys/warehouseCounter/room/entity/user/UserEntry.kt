package com.dacosys.warehouseCounter.room.entity.user

abstract class UserEntry {
    companion object {
        const val TABLE_NAME = "user"

        const val USER_ID = "_id"
        const val NAME = "name"
        const val ACTIVE = "active"
        const val PASSWORD = "password"
    }
}