package com.example.warehouseCounter.data.ktor.v1.dto.orderRequest

import android.os.Parcel
import android.os.Parcelable
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class OrderRequestType() : Parcelable {
    @SerialName("orderRequestTypeId")
    var id: Long = 0

    @SerialName("description")
    var description: String = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        description = parcel.readString() ?: ""
    }

    constructor(id: Long, description: String) : this() {
        this.id = id
        this.description = description
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
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

    companion object CREATOR : Parcelable.Creator<OrderRequestType> {
        override fun createFromParcel(parcel: Parcel): OrderRequestType {
            return OrderRequestType(parcel)
        }

        override fun newArray(size: Int): Array<OrderRequestType?> {
            return arrayOfNulls(size)
        }

        var prepareOrder = OrderRequestType(1, context.getString(R.string.order_preparation))
        var stockAudit = OrderRequestType(2, context.getString(R.string.warehouse_counter))
        var receptionAudit = OrderRequestType(3, context.getString(R.string.reception_control))
        var deliveryAudit = OrderRequestType(4, context.getString(R.string.delivery_control))
        var stockAuditFromDevice =
            OrderRequestType(5, context.getString(R.string.warehouse_counter_from_device))

        fun getAll(): ArrayList<OrderRequestType> {
            val allSections = ArrayList<OrderRequestType>()
            Collections.addAll(
                allSections,
                prepareOrder,
                stockAudit,
                receptionAudit,
                deliveryAudit,
                stockAuditFromDevice
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
                stockAuditFromDevice
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
                stockAuditFromDevice.id.toString()
            )

            return ArrayList(allSections.sortedWith(compareBy { it }))
        }

        fun getAllIdAsSet(): Set<String> {
            return getAllIdAsString().toHashSet()
        }

        fun getById(orderRequestTypeId: Long): OrderRequestType? {
            return getAll().firstOrNull { it.id == orderRequestTypeId }
        }
    }
}
