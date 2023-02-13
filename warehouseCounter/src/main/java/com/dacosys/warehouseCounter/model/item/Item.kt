package com.dacosys.warehouseCounter.model.item

import android.content.ContentValues
import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.dataBase.item.ItemContract.ItemEntry.Companion.ACTIVE
import com.dacosys.warehouseCounter.dataBase.item.ItemContract.ItemEntry.Companion.DESCRIPTION
import com.dacosys.warehouseCounter.dataBase.item.ItemContract.ItemEntry.Companion.EAN
import com.dacosys.warehouseCounter.dataBase.item.ItemContract.ItemEntry.Companion.EXTERNAL_ID
import com.dacosys.warehouseCounter.dataBase.item.ItemContract.ItemEntry.Companion.ITEM_CATEGORY_ID
import com.dacosys.warehouseCounter.dataBase.item.ItemContract.ItemEntry.Companion.ITEM_ID
import com.dacosys.warehouseCounter.dataBase.item.ItemContract.ItemEntry.Companion.LOT_ENABLED
import com.dacosys.warehouseCounter.dataBase.item.ItemContract.ItemEntry.Companion.PRICE
import com.dacosys.warehouseCounter.dataBase.item.ItemDbHelper

class Item : Parcelable {
    var itemId: Long = 0
    private var dataRead: Boolean = false

    constructor()

    constructor(
        itemId: Long,
        description: String,
        active: Boolean,
        price: Double?,
        ean: String,
        itemCategoryId: Long,
        externalId: String?,
        itemCategoryStr: String,
        lotEnabled: Boolean,
    ) {
        this.itemId = itemId
        this.description = description
        this.active = active
        this.price = price
        this.ean = ean
        this.itemCategoryId = itemCategoryId
        this.externalId = externalId
        this.itemCategoryStr = itemCategoryStr
        this.lotEnabled = lotEnabled

        dataRead = true
    }

    constructor(id: Long, doChecks: Boolean) {
        itemId = id

        if (doChecks) {
            refreshData()
        }
    }

    override fun toString(): String {
        return ean
    }

    var description: String = ""
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

    var price: Double? = null
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return 0.0
                }
            }
            return field
        }

    var ean: String = ""
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return ""
                }
            }
            return field
        }

    var itemCategoryId: Long = 0
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return 0
                }
            }
            return field
        }

    var itemCategoryStr: String = ""
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return ""
                }
            }
            return field
        }

    var externalId: String? = null
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return ""
                }
            }
            return field
        }

    var lotEnabled: Boolean = false
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return false
                }
            }
            return field
        }

    constructor(parcel: Parcel) : this() {
        itemId = parcel.readLong()
        description = parcel.readString() ?: ""
        active = parcel.readByte() != 0.toByte()
        price = parcel.readDouble()
        ean = parcel.readString() ?: ""
        itemCategoryId = parcel.readLong()
        externalId = parcel.readString() ?: ""
        itemCategoryStr = parcel.readString() ?: ""
        lotEnabled = parcel.readByte() != 0.toByte()

        dataRead = parcel.readByte() != 0.toByte()
    }

    fun toContentValues(): ContentValues {
        val values = ContentValues()
        values.put(ITEM_ID, itemId)
        values.put(DESCRIPTION, description)
        values.put(ACTIVE, active)
        values.put(PRICE, price)
        values.put(EAN, ean)
        values.put(ITEM_CATEGORY_ID, itemCategoryId)
        values.put(EXTERNAL_ID, externalId)
        values.put(LOT_ENABLED, lotEnabled)
        //values.put(ITEM_CATEGORY_STR, itemCategoryStr)

        return values
    }

    fun saveChanges(): Boolean {
        if (!dataRead) {
            if (!refreshData()) {
                return false
            }
        }

        val i = ItemDbHelper()
        return i.update(this)
    }

    private fun refreshData(): Boolean {
        val i = ItemDbHelper()
        val temp = i.selectById(this.itemId) ?: return false

        itemId = temp.itemId
        active = temp.active
        description = temp.description
        ean = temp.ean
        externalId = temp.externalId
        itemCategoryId = temp.itemCategoryId
        price = temp.price
        itemCategoryStr = temp.itemCategoryStr
        lotEnabled = temp.lotEnabled

        dataRead = true

        return true
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is Item) {
            false
        } else this.itemId == other.itemId

        // Custom equality check here.
    }

    override fun hashCode(): Int {
        return this.itemId.toInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(itemId)
        parcel.writeString(description)
        parcel.writeByte(if (active) 1 else 0)
        parcel.writeDouble(price ?: 0.toDouble())
        parcel.writeString(ean)
        parcel.writeLong(itemCategoryId)
        parcel.writeString(externalId)
        parcel.writeString(itemCategoryStr)
        parcel.writeByte(if (lotEnabled) 1 else 0)

        parcel.writeByte(if (dataRead) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Item> {
        override fun createFromParcel(parcel: Parcel): Item {
            return Item(parcel)
        }

        override fun newArray(size: Int): Array<Item?> {
            return arrayOfNulls(size)
        }

        fun add(
            description: String,
            ean: String,
            price: Double,
            active: Boolean,
            itemCategoryId: Long,
            externalId: String,
            lotEnabled: Boolean,
        ): Item? {
            if (description.isEmpty() || itemCategoryId < 1) {
                return null
            }

            val i = ItemDbHelper()
            val newId = i.insert(
                description,
                ean,
                price,
                active,
                itemCategoryId,
                externalId,
                lotEnabled
            )!!

            return if (newId < 1) {
                null
            } else {
                i.selectById(newId)
            }
        }
    }
}