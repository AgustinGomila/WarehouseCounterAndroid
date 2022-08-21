package com.dacosys.warehouseCounter.item.dbHelper

import android.database.Cursor
import android.database.SQLException
import com.dacosys.warehouseCounter.dataBaseHelper.StaticDbHelper
import com.dacosys.warehouseCounter.errorLog.ErrorLog
import com.dacosys.warehouseCounter.item.`object`.Item
import com.dacosys.warehouseCounter.item.dbHelper.ItemContract.ItemEntry.Companion.ACTIVE
import com.dacosys.warehouseCounter.item.dbHelper.ItemContract.ItemEntry.Companion.DESCRIPTION
import com.dacosys.warehouseCounter.item.dbHelper.ItemContract.ItemEntry.Companion.EAN
import com.dacosys.warehouseCounter.item.dbHelper.ItemContract.ItemEntry.Companion.EXTERNAL_ID
import com.dacosys.warehouseCounter.item.dbHelper.ItemContract.ItemEntry.Companion.ITEM_CATEGORY_ID
import com.dacosys.warehouseCounter.item.dbHelper.ItemContract.ItemEntry.Companion.ITEM_CATEGORY_STR
import com.dacosys.warehouseCounter.item.dbHelper.ItemContract.ItemEntry.Companion.ITEM_ID
import com.dacosys.warehouseCounter.item.dbHelper.ItemContract.ItemEntry.Companion.LOT_ENABLED
import com.dacosys.warehouseCounter.item.dbHelper.ItemContract.ItemEntry.Companion.PRICE
import com.dacosys.warehouseCounter.item.dbHelper.ItemContract.ItemEntry.Companion.TABLE_NAME
import com.dacosys.warehouseCounter.itemCategory.`object`.ItemCategory
import com.dacosys.warehouseCounter.itemCategory.dbHelper.ItemCategoryContract.ItemCategoryEntry

/**
 * Created by Agustin on 28/12/2016.
 */

class ItemDbHelper {

    private val minorId: Long
        get() {
            val sqLiteDatabase = StaticDbHelper.getReadableDb()

            return try {
                val mCount = sqLiteDatabase.rawQuery("SELECT MIN($ITEM_ID) FROM $TABLE_NAME", null)
                mCount.moveToFirst()
                val count = mCount.getInt(0).toLong()
                mCount.close()

                if (count >= 0L) {
                    -1L
                } else {
                    count - 1
                }
            } catch (ex: SQLException) {
                ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
                0L
            }
        }

    fun insert(
        description: String,
        ean: String,
        price: Double?,
        active: Boolean,
        itemCategoryId: Long,
        externalId: String?,
        lotEnabled: Boolean,
    ): Long? {
        val newId = minorId

        val sqLiteDatabase = StaticDbHelper.getWritableDb()
        val newItem = Item(
            itemId = newId,
            description = description,
            active = active,
            price = price,
            ean = ean,
            itemCategoryId = itemCategoryId,
            externalId = externalId,
            itemCategoryStr = "",
            lotEnabled = lotEnabled
        )

        return try {
            if (sqLiteDatabase.insertOrThrow(
                    TABLE_NAME,
                    null,
                    newItem.toContentValues()
                ) > 0
            ) {
                newId
            } else {
                0L
            }
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            return 0L
        }
    }

    fun insert(item: Item): Long {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        return try {
            if (sqLiteDatabase.insert(
                    TABLE_NAME,
                    null,
                    item.toContentValues()
                ) > 0
            ) {
                item.itemId
            } else {
                0L
            }
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            0L
        }
    }

    fun update(item: Item): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val selection = "$ITEM_ID = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(item.itemId.toString())

        return try {
            sqLiteDatabase.update(
                TABLE_NAME,
                item.toContentValues(),
                selection,
                selectionArgs
            ) > 0
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            false
        }
    }

    fun delete(item: Item): Boolean {
        return deleteById(item.itemId)
    }

    fun deleteById(id: Long?): Boolean {
        val sqLiteDatabase = StaticDbHelper.getWritableDb()

        val selection = "$ITEM_ID = ?" // WHERE id LIKE ?
        val selectionArgs = arrayOf(id!!.toString())

        return try {
            sqLiteDatabase.delete(
                TABLE_NAME,
                selection,
                selectionArgs
            ) > 0
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            false
        }
    }

    fun select(): ArrayList<Item> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        val rawQuery = "SELECT " +
                TABLE_NAME + "." + ITEM_ID + "," +
                TABLE_NAME + "." + DESCRIPTION + "," +
                TABLE_NAME + "." + ACTIVE + "," +
                TABLE_NAME + "." + PRICE + "," +
                TABLE_NAME + "." + EAN + "," +
                TABLE_NAME + "." + ITEM_CATEGORY_ID + "," +
                TABLE_NAME + "." + EXTERNAL_ID + "," +
                TABLE_NAME + "." + LOT_ENABLED + "," +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.DESCRIPTION + " AS " + ITEM_CATEGORY_STR +
                " FROM " + TABLE_NAME +
                " LEFT JOIN " + ItemCategoryEntry.TABLE_NAME + " ON " +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.ITEM_CATEGORY_ID + " = " + TABLE_NAME + "." + ITEM_CATEGORY_ID +
                " ORDER BY " + TABLE_NAME + "." + DESCRIPTION

        return try {
            val c = sqLiteDatabase.rawQuery(rawQuery, null)

            fromCursor(c)
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            ArrayList()
        }
    }

    fun selectById(id: Long?): Item? {
        // IDs negativos para ítems desconocidos
        if (id == null || id == 0L) {
            return null
        }

        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        val where = " WHERE $TABLE_NAME.$ITEM_ID = $id"

        val rawQuery = "SELECT " +
                TABLE_NAME + "." + ITEM_ID + "," +
                TABLE_NAME + "." + DESCRIPTION + "," +
                TABLE_NAME + "." + ACTIVE + "," +
                TABLE_NAME + "." + PRICE + "," +
                TABLE_NAME + "." + EAN + "," +
                TABLE_NAME + "." + ITEM_CATEGORY_ID + "," +
                TABLE_NAME + "." + EXTERNAL_ID + "," +
                TABLE_NAME + "." + LOT_ENABLED + "," +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.DESCRIPTION + " AS " + ITEM_CATEGORY_STR +
                " FROM " + TABLE_NAME +
                " LEFT JOIN " + ItemCategoryEntry.TABLE_NAME + " ON " +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.ITEM_CATEGORY_ID + " = " + TABLE_NAME + "." + ITEM_CATEGORY_ID +
                where +
                " ORDER BY " + TABLE_NAME + "." + DESCRIPTION

        return try {
            val c = sqLiteDatabase.rawQuery(rawQuery, null)

            val result = fromCursor(c)
            when {
                result.isNotEmpty() -> result[0]
                else -> null
            }
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            null
        }
    }

    fun selectCodes(onlyActive: Boolean): ArrayList<String> {
        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        val where: String = if (onlyActive) {
            " WHERE ${TABLE_NAME}.${ACTIVE} = 1"
        } else ""

        val rawQuery = "SELECT " + TABLE_NAME + "." + EAN +
                " FROM " + TABLE_NAME +
                where +
                " ORDER BY " + TABLE_NAME + "." + EAN

        sqLiteDatabase.beginTransaction()
        try {
            val c = sqLiteDatabase.rawQuery(rawQuery, null)
            val result: ArrayList<String> = ArrayList()
            c.use {
                if (it != null) {
                    while (it.moveToNext()) {
                        result.add(it.getString(it.getColumnIndexOrThrow(EAN)))
                    }
                }
            }
            sqLiteDatabase.setTransactionSuccessful()
            return result
        } catch (ex: SQLException) {
            ex.printStackTrace()
            ErrorLog.writeLog(null, this::class.java.simpleName, ex)
            return ArrayList()
        } finally {
            sqLiteDatabase.endTransaction()
        }
    }

    fun selectByDescriptionEanItemCategory(
        descEan: String,
        itemCategory: ItemCategory?,
    ): ArrayList<Item> {
        if (itemCategory == null) {
            return ArrayList()
        }

        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        val itemCategoryId = itemCategory.itemCategoryId

        var where = ""
        if (descEan.isNotEmpty()) {
            where =
                " WHERE $TABLE_NAME.$DESCRIPTION LIKE '$descEan%' OR $TABLE_NAME.$EAN LIKE '$descEan%'"
        }

        if (itemCategoryId > 0) {
            where = if (where.isNotEmpty()) "$where AND " else " WHERE "
            where += "$TABLE_NAME.$ITEM_CATEGORY_ID = $itemCategoryId"
        }

        val rawQuery = "SELECT " +
                TABLE_NAME + "." + ITEM_ID + "," +
                TABLE_NAME + "." + DESCRIPTION + "," +
                TABLE_NAME + "." + ACTIVE + "," +
                TABLE_NAME + "." + PRICE + "," +
                TABLE_NAME + "." + EAN + "," +
                TABLE_NAME + "." + ITEM_CATEGORY_ID + "," +
                TABLE_NAME + "." + EXTERNAL_ID + "," +
                TABLE_NAME + "." + LOT_ENABLED + "," +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.DESCRIPTION + " AS " + ITEM_CATEGORY_STR +
                " FROM " + TABLE_NAME +
                " LEFT JOIN " + ItemCategoryEntry.TABLE_NAME + " ON " +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.ITEM_CATEGORY_ID + " = " + TABLE_NAME + "." + ITEM_CATEGORY_ID +
                where +
                " ORDER BY " + TABLE_NAME + "." + DESCRIPTION

        return try {
            val c = sqLiteDatabase.rawQuery(rawQuery, null)
            fromCursor(c)
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            ArrayList()
        }
    }

    fun selectByDescriptionItemCategory(
        description: String,
        itemCategory: ItemCategory?,
    ): ArrayList<Item> {
        if (itemCategory == null) {
            return ArrayList()
        }

        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        val itemCategoryId = itemCategory.itemCategoryId

        var where = ""
        if (description.isNotEmpty()) {
            where = " WHERE $TABLE_NAME.$DESCRIPTION LIKE '$description%'"
        }

        if (itemCategoryId > 0) {
            where = if (where.isNotEmpty()) "$where AND " else " WHERE "
            where += "$TABLE_NAME.$ITEM_CATEGORY_ID = $itemCategoryId"
        }

        val rawQuery = "SELECT " +
                TABLE_NAME + "." + ITEM_ID + "," +
                TABLE_NAME + "." + DESCRIPTION + "," +
                TABLE_NAME + "." + ACTIVE + "," +
                TABLE_NAME + "." + PRICE + "," +
                TABLE_NAME + "." + EAN + "," +
                TABLE_NAME + "." + ITEM_CATEGORY_ID + "," +
                TABLE_NAME + "." + EXTERNAL_ID + "," +
                TABLE_NAME + "." + LOT_ENABLED + "," +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.DESCRIPTION + " AS " + ITEM_CATEGORY_STR +
                " FROM " + TABLE_NAME +
                " LEFT JOIN " + ItemCategoryEntry.TABLE_NAME + " ON " +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.ITEM_CATEGORY_ID + " = " + TABLE_NAME + "." + ITEM_CATEGORY_ID +
                where +
                " ORDER BY " + TABLE_NAME + "." + DESCRIPTION

        return try {
            val c = sqLiteDatabase.rawQuery(rawQuery, null)
            fromCursor(c)
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            ArrayList()
        }
    }

    fun selectByItemCategory(itemCategory: ItemCategory?): ArrayList<Item> {
        if (itemCategory == null) {
            return ArrayList()
        }

        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        val itemCategoryId = itemCategory.itemCategoryId

        var where = ""
        if (itemCategoryId > 0) {
            where = " WHERE $TABLE_NAME.$ITEM_CATEGORY_ID + $itemCategoryId"
        }

        val rawQuery = "SELECT " +
                TABLE_NAME + "." + ITEM_ID + "," +
                TABLE_NAME + "." + DESCRIPTION + "," +
                TABLE_NAME + "." + ACTIVE + "," +
                TABLE_NAME + "." + PRICE + "," +
                TABLE_NAME + "." + EAN + "," +
                TABLE_NAME + "." + ITEM_CATEGORY_ID + "," +
                TABLE_NAME + "." + EXTERNAL_ID + "," +
                TABLE_NAME + "." + LOT_ENABLED + "," +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.DESCRIPTION + " AS " + ITEM_CATEGORY_STR +
                " FROM " + TABLE_NAME +
                " LEFT JOIN " + ItemCategoryEntry.TABLE_NAME + " ON " +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.ITEM_CATEGORY_ID + " = " + TABLE_NAME + "." + ITEM_CATEGORY_ID +
                where +
                " ORDER BY " + TABLE_NAME + "." + DESCRIPTION

        return try {
            val c = sqLiteDatabase.rawQuery(rawQuery, null)
            fromCursor(c)
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            ArrayList()
        }
    }

    fun selectByDescriptionEan(descEan: String): ArrayList<Item> {
        if (descEan.isEmpty()) {
            return ArrayList()
        }

        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        val where =
            " WHERE $TABLE_NAME.$DESCRIPTION LIKE '$descEan%' OR $TABLE_NAME.$EAN LIKE '$descEan%'"

        val rawQuery = "SELECT " +
                TABLE_NAME + "." + ITEM_ID + "," +
                TABLE_NAME + "." + DESCRIPTION + "," +
                TABLE_NAME + "." + ACTIVE + "," +
                TABLE_NAME + "." + PRICE + "," +
                TABLE_NAME + "." + EAN + "," +
                TABLE_NAME + "." + ITEM_CATEGORY_ID + "," +
                TABLE_NAME + "." + EXTERNAL_ID + "," +
                TABLE_NAME + "." + LOT_ENABLED + "," +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.DESCRIPTION + " AS " + ITEM_CATEGORY_STR +
                " FROM " + TABLE_NAME +
                " LEFT JOIN " + ItemCategoryEntry.TABLE_NAME + " ON " +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.ITEM_CATEGORY_ID + " = " + TABLE_NAME + "." + ITEM_CATEGORY_ID +
                where +
                " ORDER BY " + TABLE_NAME + "." + DESCRIPTION

        return try {
            val c = sqLiteDatabase.rawQuery(rawQuery, null)
            fromCursor(c)
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            ArrayList()
        }
    }

    fun selectByDescription(description: String): ArrayList<Item> {
        if (description.isEmpty()) {
            return ArrayList()
        }

        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        val where = " WHERE $TABLE_NAME.$DESCRIPTION LIKE '$description%'"

        val rawQuery = "SELECT " +
                TABLE_NAME + "." + ITEM_ID + "," +
                TABLE_NAME + "." + DESCRIPTION + "," +
                TABLE_NAME + "." + ACTIVE + "," +
                TABLE_NAME + "." + PRICE + "," +
                TABLE_NAME + "." + EAN + "," +
                TABLE_NAME + "." + ITEM_CATEGORY_ID + "," +
                TABLE_NAME + "." + EXTERNAL_ID + "," +
                TABLE_NAME + "." + LOT_ENABLED + "," +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.DESCRIPTION + " AS " + ITEM_CATEGORY_STR +
                " FROM " + TABLE_NAME +
                " LEFT JOIN " + ItemCategoryEntry.TABLE_NAME + " ON " +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.ITEM_CATEGORY_ID + " = " + TABLE_NAME + "." + ITEM_CATEGORY_ID +
                where +
                " ORDER BY " + TABLE_NAME + "." + DESCRIPTION

        return try {
            val c = sqLiteDatabase.rawQuery(rawQuery, null)
            fromCursor(c)
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            ArrayList()
        }
    }

    fun selectByEan(ean: String): ArrayList<Item> {
        if (ean.isEmpty()) {
            return ArrayList()
        }

        val sqLiteDatabase = StaticDbHelper.getReadableDb()

        val where = " WHERE $TABLE_NAME.$EAN = '$ean'"

        val rawQuery = "SELECT " +
                TABLE_NAME + "." + ITEM_ID + "," +
                TABLE_NAME + "." + DESCRIPTION + "," +
                TABLE_NAME + "." + ACTIVE + "," +
                TABLE_NAME + "." + PRICE + "," +
                TABLE_NAME + "." + EAN + "," +
                TABLE_NAME + "." + ITEM_CATEGORY_ID + "," +
                TABLE_NAME + "." + EXTERNAL_ID + "," +
                TABLE_NAME + "." + LOT_ENABLED + "," +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.DESCRIPTION + " AS " + ITEM_CATEGORY_STR +
                " FROM " + TABLE_NAME +
                " LEFT JOIN " + ItemCategoryEntry.TABLE_NAME + " ON " +
                ItemCategoryEntry.TABLE_NAME + "." + ItemCategoryEntry.ITEM_CATEGORY_ID + " = " + TABLE_NAME + "." + ITEM_CATEGORY_ID +
                where +
                " ORDER BY " + TABLE_NAME + "." + EAN

        return try {
            val c = sqLiteDatabase.rawQuery(rawQuery, null)

            fromCursor(c)
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            ArrayList()
        }
    }

    private fun fromCursor(c: Cursor?): ArrayList<Item> {
        val result = ArrayList<Item>()
        c.use {
            if (it != null) {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(ITEM_ID))
                    val active = it.getInt(it.getColumnIndexOrThrow(ACTIVE)) == 1
                    val description = it.getString(it.getColumnIndexOrThrow(DESCRIPTION))
                    val itemCategoryId = it.getLong(it.getColumnIndexOrThrow(ITEM_CATEGORY_ID))
                    val ean = it.getString(it.getColumnIndexOrThrow(EAN))
                    val externalId = it.getString(it.getColumnIndexOrThrow(EXTERNAL_ID))
                    val price = it.getDouble(it.getColumnIndexOrThrow(PRICE))
                    val lotEnabled = it.getInt(it.getColumnIndexOrThrow(LOT_ENABLED)) == 1

                    var itemCategoryStr = ""
                    if (it.getColumnIndexOrThrow(ITEM_CATEGORY_STR) >= 0) {
                        if (it.getString(it.getColumnIndexOrThrow(ITEM_CATEGORY_STR)) != null) {
                            itemCategoryStr =
                                it.getString(it.getColumnIndexOrThrow(ITEM_CATEGORY_STR))
                        }
                    }

                    val temp = Item(
                        id,
                        description,
                        active,
                        price,
                        ean,
                        itemCategoryId,
                        externalId,
                        itemCategoryStr,
                        lotEnabled
                    )

                    result.add(temp)
                }
            }
        }
        return result
    }

    companion object {

        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS [" + TABLE_NAME + "] " +
                "( [" + ITEM_ID + "] BIGINT NOT NULL ," +
                " [" + DESCRIPTION + "] NVARCHAR(255) NOT NULL ," +
                " [" + ACTIVE + "] INT NOT NULL ," +
                " [" + PRICE + "] FLOAT NULL ," +
                " [" + EAN + "] NVARCHAR(45) NOT NULL ," +
                " [" + ITEM_CATEGORY_ID + "] BIGINT NOT NULL ," +
                " [" + EXTERNAL_ID + "] NVARCHAR(45) NULL ," +
                " [" + LOT_ENABLED + "] INT NOT NULL DEFAULT 0 ," +
                " CONSTRAINT [PK_" + ITEM_ID + "] PRIMARY KEY ([" + ITEM_ID + "]) )"

        val CREATE_INDEX: ArrayList<String> = arrayListOf(
            "DROP INDEX IF EXISTS [IDX_$EAN]",
            "CREATE INDEX [IDX_$EAN] ON [$TABLE_NAME] ([$EAN])"
        )
    }
}
