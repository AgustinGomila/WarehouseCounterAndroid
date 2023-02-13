package com.dacosys.warehouseCounter.model.table

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class Table : Parcelable {
    var tableId: Int = 0
    var tableName: String = ""
    var description: String = ""

    constructor(tableId: Int, tableName: String, description: String) {
        this.description = description
        this.tableName = tableName
        this.tableId = tableId
    }

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is Table) {
            false
        } else this.tableId == other.tableId
    }

    override fun hashCode(): Int {
        return this.tableId
    }

    constructor(parcel: Parcel) {
        tableId = parcel.readInt()
        description = parcel.readString() ?: ""
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        with(parcel) {
            writeInt(tableId)
            writeString(description)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Table> {
        override fun createFromParcel(parcel: Parcel): Table {
            return Table(parcel)
        }

        override fun newArray(size: Int): Array<Table?> {
            return arrayOfNulls(size)
        }

        var orderRequest = Table(6, "order_request", "Solicitud de conteo")

        fun getAll(): ArrayList<Table> {
            val allSections = ArrayList<Table>()
            Collections.addAll(
                allSections,
                orderRequest
            )

            return ArrayList(allSections.sortedWith(compareBy { it.tableId }))
        }

        fun getById(tableId: Int): Table? {
            return getAll().firstOrNull { it.tableId == tableId }
        }
    }
}