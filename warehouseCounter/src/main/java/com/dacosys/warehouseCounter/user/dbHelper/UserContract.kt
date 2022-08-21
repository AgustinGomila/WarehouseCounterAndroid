package com.dacosys.warehouseCounter.user.dbHelper

import android.provider.BaseColumns

/**
 * Created by Agustin on 28/12/2016.
 */

object UserContract {
    fun getAllColumns(): Array<String> {
        return arrayOf(UserEntry.USER_ID, UserEntry.NAME, UserEntry.ACTIVE, UserEntry.PASSWORD)
    }

    abstract class UserEntry : BaseColumns {
        companion object {
            const val TABLE_NAME = "user"

            const val USER_ID = "_id"
            const val NAME = "name"
            const val ACTIVE = "active"
            const val PASSWORD = "password"
        }
    }
}
