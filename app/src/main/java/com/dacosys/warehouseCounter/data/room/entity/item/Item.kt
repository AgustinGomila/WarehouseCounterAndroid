package com.dacosys.warehouseCounter.data.room.entity.item

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import androidx.room.ColumnInfo.Companion.NOCASE
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestContent
import com.dacosys.warehouseCounter.ui.adapter.generic.HasUUID
import com.dacosys.warehouseCounter.ui.adapter.item.ItemStatus
import com.dacosys.warehouseCounter.ui.utils.toVersion5UUID
import java.util.*
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.Item as ItemKtor
import com.dacosys.warehouseCounter.data.room.entity.item.ItemEntry as Entry

@Entity(
    tableName = Entry.TABLE_NAME,
    indices = [
        Index(value = [Entry.EAN], name = "IDX_${Entry.TABLE_NAME}_${Entry.EAN}"),
        Index(
            value = [Entry.ITEM_CATEGORY_ID],
            name = "IDX_${Entry.TABLE_NAME}_${Entry.ITEM_CATEGORY_ID}"
        ),
        Index(value = [Entry.EXTERNAL_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.EXTERNAL_ID}"),
        Index(value = [Entry.ACTIVE], name = "IDX_${Entry.TABLE_NAME}_${Entry.ACTIVE}"),
    ]
)
data class Item(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Entry.ITEM_ID) var itemId: Long = 0L,
    @ColumnInfo(name = Entry.DESCRIPTION) var description: String = "",
    @ColumnInfo(name = Entry.ACTIVE) var active: Int = 1,
    @ColumnInfo(name = Entry.PRICE) var price: Float? = 0f,
    @ColumnInfo(name = Entry.EAN, collate = NOCASE) var ean: String = "",
    @ColumnInfo(name = Entry.ITEM_CATEGORY_ID) var itemCategoryId: Long = 1L,
    @ColumnInfo(name = Entry.EXTERNAL_ID) var externalId: String? = null,
    @ColumnInfo(name = Entry.LOT_ENABLED, defaultValue = "0") var lotEnabled: Int = 0,
    @ColumnInfo(name = Entry.ITEM_CATEGORY_STR)
    @Ignore var itemCategoryStr: String = "",
    @Ignore val itemStatus: ItemStatus =
        if (lotEnabled == 1 && active == 1) ItemStatus.ACTIVE_LOT_ENABLED
        else if (lotEnabled != 1 && active == 1) ItemStatus.ACTIVE_LOT_DISABLED
        else if (lotEnabled == 1) ItemStatus.INACTIVE_LOT_ENABLED
        else ItemStatus.INACTIVE_LOT_DISABLED
) : Parcelable, HasUUID {
    constructor(parcel: Parcel) : this(
        itemId = parcel.readLong(),
        description = parcel.readString() ?: "",
        active = parcel.readInt(),
        price = parcel.readValue(Float::class.java.classLoader) as? Float,
        ean = parcel.readString() ?: "",
        itemCategoryId = parcel.readLong(),
        externalId = parcel.readString(),
        lotEnabled = parcel.readInt(),
        itemCategoryStr = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(itemId)
        parcel.writeString(description)
        parcel.writeInt(active)
        parcel.writeValue(price)
        parcel.writeString(ean)
        parcel.writeLong(itemCategoryId)
        parcel.writeString(externalId)
        parcel.writeInt(lotEnabled)
        parcel.writeString(itemCategoryStr)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Item

        return itemId == other.itemId
    }

    override fun hashCode(): Int {
        var result = itemId.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + active
        result = 31 * result + (price?.hashCode() ?: 0)
        result = 31 * result + ean.hashCode()
        result = 31 * result + itemCategoryId.hashCode()
        result = 31 * result + (externalId?.hashCode() ?: 0)
        result = 31 * result + lotEnabled
        return result
    }

    fun toOrderRequestContent(codeRead: String): OrderRequestContent {
        return OrderRequestContent(
            codeRead = codeRead,
            ean = ean,
            externalId = externalId,
            itemActive = active == 1,
            itemCategoryId = itemCategoryId,
            itemDescription = description,
            itemId = itemId,
            lotEnabled = lotEnabled == 1,
            price = price?.toDouble() ?: 0.0,
            qtyCollected = 0.0,
            qtyRequested = 0.0
        )
    }

    fun toKtor(): ItemKtor {
        return ItemKtor(
            active = active == 1,
            description = description,
            ean = ean,
            externalId = externalId,
            id = itemId,
            uuid = "",
            itemCategoryId = itemCategoryId,
            lotEnabled = lotEnabled == 1,
            price = price?.toDouble() ?: 0.0,
        )
    }

    @Ignore
    override val uuid: UUID = funUUID()
    private fun funUUID(): UUID {
        return itemId.toVersion5UUID()
    }

    @Ignore
    override val parentUuid: UUID? = null

    companion object CREATOR : Parcelable.Creator<Item> {
        override fun createFromParcel(parcel: Parcel): Item {
            return Item(parcel)
        }

        override fun newArray(size: Int): Array<Item?> {
            return arrayOfNulls(size)
        }
    }
}
