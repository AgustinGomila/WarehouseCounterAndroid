package com.dacosys.warehouseCounter.settings

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class QRConfigType : Parcelable {
    var id: Int = 0
    private var description: String = ""

    constructor(id: Int, description: String) {
        this.description = description
        this.id = id
    }

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is QRConfigType) {
            false
        } else this.id == other.id
    }

    override fun hashCode(): Int {
        return this.id
    }

    constructor(parcel: Parcel) {
        id = parcel.readInt()
        description = parcel.readString() ?: ""
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<QRConfigType> {
        override fun createFromParcel(parcel: Parcel): QRConfigType {
            return QRConfigType(parcel)
        }

        override fun newArray(size: Int): Array<QRConfigType?> {
            return arrayOfNulls(size)
        }

        var QRConfigApp = QRConfigType(
            id = 0, description = "App"
        )
        var QRConfigWebservice = QRConfigType(
            id = 1, description = "Webservice"
        )
        var QRConfigClientAccount = QRConfigType(
            id = 2, description = "ClientAccount"
        )
        var QRConfigImageControl = QRConfigType(
            id = 3, description = "ImageControl"
        )

        fun getAll(): ArrayList<QRConfigType> {
            val allSections = ArrayList<QRConfigType>()
            Collections.addAll(
                allSections,
                QRConfigApp,
                QRConfigWebservice,
                QRConfigClientAccount,
                QRConfigImageControl
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getAllIdAsString(): ArrayList<String> {
            val allSections = ArrayList<String>()
            Collections.addAll(
                allSections,
                QRConfigApp.id.toString(),
                QRConfigWebservice.id.toString(),
                QRConfigClientAccount.id.toString(),
                QRConfigImageControl.id.toString()
            )

            return ArrayList(allSections.sortedWith(compareBy { it }))
        }

        fun getById(typeId: Int): QRConfigType? {
            return getAll().firstOrNull { it.id == typeId }
        }
    }
}