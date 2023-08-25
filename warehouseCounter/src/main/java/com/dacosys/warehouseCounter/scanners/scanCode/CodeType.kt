package com.dacosys.warehouseCounter.scanners.scanCode

import android.os.Parcel
import android.os.Parcelable
import java.util.*


class CodeType : Parcelable {
    var id: Long = 0
    var name: String = ""
    var description: String = ""

    constructor(codeTypeId: Long, name: String, description: String) {
        this.description = description
        this.name = name
        this.id = codeTypeId
    }

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is CodeType) {
            false
        } else this.id == other.id
    }

    override fun hashCode(): Int {
        return this.id.hashCode()
    }

    constructor(parcel: Parcel) {
        id = parcel.readLong()
        name = parcel.readString() ?: ""
        description = parcel.readString() ?: ""
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CodeType> {
        override fun createFromParcel(parcel: Parcel): CodeType {
            return CodeType(parcel)
        }

        override fun newArray(size: Int): Array<CodeType?> {
            return arrayOfNulls(size)
        }

        var ean: CodeType = CodeType(1, "item_ean", "EAN")
        var itemCode: CodeType = CodeType(3, "item_code", "Código del ítem")
        var itemId: CodeType = CodeType(4, "item_id", "Ítem Id")
        var order: CodeType = CodeType(5, "order", "Pedido")
        var warehouseArea: CodeType = CodeType(6, "warehouse_area", "Área")
        var rack: CodeType = CodeType(7, "rack", "Rack")

        fun getAll(): ArrayList<CodeType> {
            val allSections = ArrayList<CodeType>()
            Collections.addAll(
                allSections,
                ean,
                itemCode,
                itemId,
                order,
                warehouseArea,
                rack
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getById(codeTypeId: Long): CodeType? {
            return getAll().firstOrNull { it.id == codeTypeId }
        }
    }
}
