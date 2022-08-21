package com.dacosys.warehouseCounter.itemCategory.`object`

import android.content.ContentValues
import android.os.Parcelable
import com.dacosys.warehouseCounter.itemCategory.dbHelper.ItemCategoryContract.ItemCategoryEntry.Companion.ACTIVE
import com.dacosys.warehouseCounter.itemCategory.dbHelper.ItemCategoryContract.ItemCategoryEntry.Companion.DESCRIPTION
import com.dacosys.warehouseCounter.itemCategory.dbHelper.ItemCategoryContract.ItemCategoryEntry.Companion.ITEM_CATEGORY_ID
import com.dacosys.warehouseCounter.itemCategory.dbHelper.ItemCategoryContract.ItemCategoryEntry.Companion.PARENT_ID
import com.dacosys.warehouseCounter.itemCategory.dbHelper.ItemCategoryDbHelper

class ItemCategory : Parcelable {
    var itemCategoryId: Long = 0
    private var dataRead: Boolean = false

    val parent: ItemCategory?
        get() =
            when {
                parentId == null -> null
                parentId!! > 0 -> ItemCategory(parentId!!, false)
                else -> null
            }

    constructor()

    constructor(
        itemCategoryId: Long,
        description: String,
        active: Boolean,
        parentId: Long,
    ) {
        this.itemCategoryId = itemCategoryId
        this.description = description
        this.active = active
        this.parentId = parentId

        dataRead = true
    }

    constructor(id: Long, doChecks: Boolean) {
        itemCategoryId = id

        if (doChecks) {
            refreshData()
        }
    }

    private fun refreshData(): Boolean {
        val i = ItemCategoryDbHelper()
        val temp = i.selectById(this.itemCategoryId)

        dataRead = true
        return when {
            temp != null -> {
                itemCategoryId = temp.itemCategoryId
                active = temp.active
                description = temp.description
                parentId = temp.parentId

                true
            }
            else -> false
        }
    }

    override fun toString(): String {
        return description
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

    var parentId: Long? = 0
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return null
                }
            }
            return field
        }

    constructor(parcel: android.os.Parcel) : this() {
        itemCategoryId = parcel.readLong()
        description = parcel.readString() ?: ""
        active = parcel.readByte() != 0.toByte()
        parentId = parcel.readLong()

        dataRead = parcel.readByte() != 0.toByte()
    }

    fun toContentValues(): ContentValues {
        val values = ContentValues()
        values.put(ITEM_CATEGORY_ID, itemCategoryId)
        values.put(DESCRIPTION, description)
        values.put(ACTIVE, active)
        values.put(PARENT_ID, parentId)
        return values
    }

    fun saveChanges(): Boolean {
        if (!dataRead) {
            if (!refreshData()) {
                return false
            }
        }

        val i = ItemCategoryDbHelper()
        return i.update(this)
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is ItemCategory) {
            false
        } else equals(this.itemCategoryId, other.itemCategoryId)
    }

    override fun hashCode(): Int {
        return this.itemCategoryId.toInt()
    }

    class CustomComparator : Comparator<ItemCategory> {
        override fun compare(o1: ItemCategory, o2: ItemCategory): Int {
            if (o1.itemCategoryId < o2.itemCategoryId) {
                return -1
            } else if (o1.itemCategoryId > o2.itemCategoryId) {
                return 1
            }
            return 0
        }
    }

    fun equals(a: Any?, b: Any): Boolean {
        return a != null && a == b
    }

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeLong(itemCategoryId)
        parcel.writeString(description)
        parcel.writeByte(if (active) 1 else 0)
        parcel.writeLong(parentId ?: 0L)

        parcel.writeByte(if (dataRead) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ItemCategory> {
        override fun createFromParcel(parcel: android.os.Parcel): ItemCategory {
            return ItemCategory(parcel)
        }

        override fun newArray(size: Int): Array<ItemCategory?> {
            return arrayOfNulls(size)
        }

        fun add(
            description: String,
            active: Boolean,
            parentId: Long,
        ): ItemCategory? {
            if (description.isEmpty()) {
                return null
            }

            val i = ItemCategoryDbHelper()

            val newId = i.insert(
                description,
                active,
                parentId
            )

            return if (newId < 1) {
                null
            } else {
                i.selectById(newId)
            }
        }
    }
}