package com.dacosys.warehouseCounter.lot.`object`

import android.content.ContentValues
import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.lot.dbHelper.LotContract.LotEntry.Companion.ACTIVE
import com.dacosys.warehouseCounter.lot.dbHelper.LotContract.LotEntry.Companion.CODE
import com.dacosys.warehouseCounter.lot.dbHelper.LotContract.LotEntry.Companion.LOT_ID
import com.dacosys.warehouseCounter.lot.dbHelper.LotDbHelper

class Lot : Parcelable {
    var lotId: Long = 0
    private var dataRead: Boolean = false

    constructor()

    constructor(
        lotId: Long,
        code: String,
        active: Boolean,
    ) {
        this.lotId = lotId
        this.code = code
        this.active = active

        dataRead = true
    }

    constructor(id: Long, doChecks: Boolean) {
        lotId = id

        if (doChecks) {
            refreshData()
        }
    }

    override fun toString(): String {
        return code
    }

    var code: String = ""
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

    constructor(parcel: Parcel) : this() {
        lotId = parcel.readLong()
        code = parcel.readString() ?: ""
        active = parcel.readByte() != 0.toByte()

        dataRead = parcel.readByte() != 0.toByte()
    }

    fun toContentValues(): ContentValues {
        val values = ContentValues()
        values.put(LOT_ID, lotId)
        values.put(CODE, code)
        values.put(ACTIVE, active)

        return values
    }

    fun saveChanges(): Boolean {
        if (!dataRead) {
            if (!refreshData()) {
                return false
            }
        }

        val i = LotDbHelper()
        return i.update(this)
    }

    private fun refreshData(): Boolean {
        val i = LotDbHelper()
        val temp = i.selectById(this.lotId) ?: return false

        lotId = temp.lotId
        active = temp.active
        code = temp.code

        dataRead = true

        return true
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is Lot) {
            false
        } else this.lotId == other.lotId

        // Custom equality check here.
    }

    override fun hashCode(): Int {
        return this.lotId.toInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(lotId)
        parcel.writeString(code)
        parcel.writeByte(if (active) 1 else 0)

        parcel.writeByte(if (dataRead) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Lot> {
        override fun createFromParcel(parcel: Parcel): Lot {
            return Lot(parcel)
        }

        override fun newArray(size: Int): Array<Lot?> {
            return arrayOfNulls(size)
        }

        fun add(
            code: String,
            active: Boolean,
        ): Lot? {
            if (code.isEmpty()) {
                return null
            }

            val i = LotDbHelper()
            val newId = i.insert(
                code,
                active
            )!!

            return if (newId < 1) {
                null
            } else {
                i.selectById(newId)
            }
        }
    }
}