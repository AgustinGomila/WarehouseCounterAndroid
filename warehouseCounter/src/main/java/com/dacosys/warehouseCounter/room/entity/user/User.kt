package com.dacosys.warehouseCounter.room.entity.user

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dacosys.warehouseCounter.room.entity.user.UserEntry as Entry

@Entity(
    tableName = Entry.TABLE_NAME,
    indices = [Index(value = [Entry.NAME], name = "IDX_${Entry.TABLE_NAME}_${Entry.NAME}")]
)
data class User(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = Entry.USER_ID) val userId: Long = 0L,
    @ColumnInfo(name = Entry.NAME) val name: String,
    @ColumnInfo(name = Entry.ACTIVE) val active: Int = 1,
    @ColumnInfo(name = Entry.PASSWORD) val password: String?,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        userId = parcel.readLong(),
        name = parcel.readString() ?: "",
        active = parcel.readInt(),
        password = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(userId)
        parcel.writeString(name)
        parcel.writeInt(active)
        parcel.writeString(password)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        // Los Spinners se llenan automáticamente con esta función
        return name
    }

    override fun hashCode(): Int {
        return userId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        return userId == other.userId
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}