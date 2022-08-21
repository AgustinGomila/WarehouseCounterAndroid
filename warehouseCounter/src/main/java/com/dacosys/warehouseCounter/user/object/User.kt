package com.dacosys.warehouseCounter.user.`object`

import android.content.ContentValues
import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.user.dbHelper.UserContract.UserEntry.Companion.ACTIVE
import com.dacosys.warehouseCounter.user.dbHelper.UserContract.UserEntry.Companion.NAME
import com.dacosys.warehouseCounter.user.dbHelper.UserContract.UserEntry.Companion.PASSWORD
import com.dacosys.warehouseCounter.user.dbHelper.UserContract.UserEntry.Companion.USER_ID
import com.dacosys.warehouseCounter.user.dbHelper.UserDbHelper

class User : Parcelable {
    var userId: Long = 0
    private var dataRead: Boolean = false

    fun setDataRead() {
        dataRead = true
    }

    override fun toString(): String {
        return name
    }

    constructor(
        userId: Long,
        name: String,
        active: Boolean,
        password: String?,
    ) {
        this.userId = userId
        this.name = name
        this.active = active
        this.password = password

        dataRead = true
    }

    constructor(id: Long, doChecks: Boolean) {
        userId = id

        if (doChecks) {
            refreshData()
        }
    }

    private fun refreshData(): Boolean {
        val i = UserDbHelper()
        val temp = i.selectById(userId)
        dataRead = true

        return when {
            temp != null -> {
                userId = temp.userId
                active = temp.active
                name = temp.name
                password = temp.password

                true
            }
            else -> false
        }
    }

    var name: String = ""
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return ""
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

    var password: String? = null
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return null
                }
            }
            return field
        }

    constructor(parcel: Parcel) {
        userId = parcel.readLong()
        name = parcel.readString() ?: ""
        active = parcel.readByte() != 0.toByte()
        password = parcel.readString() ?: ""

        dataRead = parcel.readByte() != 0.toByte()
    }

    fun toContentValues(): ContentValues {
        val values = ContentValues()
        values.put(USER_ID, userId)
        values.put(NAME, name)
        values.put(ACTIVE, active)
        values.put(PASSWORD, password)
        return values
    }

    fun saveChanges(): Boolean {
        if (!dataRead) {
            if (!refreshData()) {
                return false
            }
        }

        val i = UserDbHelper()
        return i.update(this)
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is User) {
            false
        } else userId == other.userId

        // Custom equality check here.
    }

    override fun hashCode(): Int {
        return userId.toInt()
    }

    class CustomComparator : Comparator<User> {
        override fun compare(o1: User, o2: User): Int {
            if (o1.userId < o2.userId) {
                return -1
            } else if (o1.userId > o2.userId) {
                return 1
            }
            return 0
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(userId)
        parcel.writeString(name)
        parcel.writeByte(if (active) 1 else 0)
        parcel.writeString(password)

        parcel.writeByte(if (dataRead) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }

        fun add(
            name: String,
            active: Boolean,
            password: String,
        ): User? {
            if (name.isEmpty() || password.isEmpty()) {
                return null
            }

            val i = UserDbHelper()

            val newId = i.insert(name, active, password)
            return if (newId < 1) {
                null
            } else {
                i.selectById(newId)
            }
        }

        fun selectById(
            userId: Long,
        ): User? {
            val i = UserDbHelper()
            return i.selectById(userId)
        }

        fun selectByName(
            name: String,
        ): ArrayList<User> {
            val i = UserDbHelper()
            return i.selectByName(name)
        }

        fun equals(a: Any?, b: Any): Boolean {
            return a != null && a == b
        }
    }
}