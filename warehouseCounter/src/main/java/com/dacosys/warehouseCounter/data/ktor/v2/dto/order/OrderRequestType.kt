package com.dacosys.warehouseCounter.data.ktor.v2.dto.order

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class OrderRequestType(
    @SerialName(ORDER_TYPE_ID_KEY) var id: Long = 0,
    @SerialName(DESCRIPTION_KEY) var description: String = "",
) : Parcelable {
    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        description = parcel.readString() ?: ""
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    /** For fragment adapter */
    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderRequestType

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    companion object CREATOR : Parcelable.Creator<OrderRequestType> {
        const val ORDER_TYPE_ID_KEY = "order_type_id"
        const val DESCRIPTION_KEY = "order_type_description"

        override fun createFromParcel(parcel: Parcel): OrderRequestType {
            return OrderRequestType(parcel)
        }

        override fun newArray(size: Int): Array<OrderRequestType?> {
            return arrayOfNulls(size)
        }

        var notDefined = OrderRequestType(0, context.getString(R.string.not_defined))
        var prepareOrder = OrderRequestType(1, context.getString(R.string.order_preparation))
        var stockAudit = OrderRequestType(2, context.getString(R.string.warehouse_counter))
        var receptionAudit = OrderRequestType(3, context.getString(R.string.reception_control))
        var deliveryAudit = OrderRequestType(4, context.getString(R.string.delivery_control))
        var stockAuditFromDevice = OrderRequestType(5, context.getString(R.string.warehouse_counter_from_device))
        var packaging = OrderRequestType(6, context.getString(R.string.packaging))

        fun getAll(): ArrayList<OrderRequestType> {
            val allSections = ArrayList<OrderRequestType>()
            Collections.addAll(
                allSections,
                notDefined,
                prepareOrder,
                stockAudit,
                receptionAudit,
                deliveryAudit,
                stockAuditFromDevice,
                packaging
            )

            Collections.sort(allSections, CustomComparator())
            return allSections
        }

        fun getUnlimited(): ArrayList<OrderRequestType> {
            val allSections = ArrayList<OrderRequestType>()
            Collections.addAll(
                allSections,
                stockAudit,
                receptionAudit,
                deliveryAudit,
                stockAuditFromDevice,
                packaging
            )

            Collections.sort(allSections, CustomComparator())
            return allSections
        }

        class CustomComparator : Comparator<OrderRequestType> {
            override fun compare(o1: OrderRequestType, o2: OrderRequestType): Int {
                if (o1.id < o2.id) {
                    return -1
                } else if (o1.id > o2.id) {
                    return 1
                }
                return 0
            }
        }

        private fun getAllIdAsString(): ArrayList<String> {
            val allSections = ArrayList<String>()
            Collections.addAll(
                allSections,
                prepareOrder.id.toString(),
                stockAudit.id.toString(),
                receptionAudit.id.toString(),
                deliveryAudit.id.toString(),
                stockAuditFromDevice.id.toString(),
                packaging.id.toString()
            )

            return ArrayList(allSections.sortedWith(compareBy { it }))
        }

        fun getAllIdAsSet(): Set<String> {
            return getAllIdAsString().toHashSet()
        }

        fun getById(typeId: Int?): OrderRequestType {
            return getById(typeId?.toLong())
        }

        fun getById(typeId: Long?): OrderRequestType {
            return getAll().firstOrNull { it.id == typeId } ?: notDefined
        }
    }
}
