package com.dacosys.warehouseCounter.data.ktor.v2.dto.order

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class OrderStatus(
    @SerialName(STATUS_ID_KEY) var id: Long = 0,
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

        other as OrderStatus

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    companion object CREATOR : Parcelable.Creator<OrderStatus> {
        const val STATUS_ID_KEY = "statusId"
        const val DESCRIPTION_KEY = "description"

        override fun createFromParcel(parcel: Parcel): OrderStatus {
            return OrderStatus(parcel)
        }

        override fun newArray(size: Int): Array<OrderStatus?> {
            return arrayOfNulls(size)
        }

        var approved = OrderStatus(1, context.getString(R.string.status_approved))
        var draft = OrderStatus(2, context.getString(R.string.status_draft))
        var inProcess = OrderStatus(3, context.getString(R.string.status_in_process))
        var inTransit = OrderStatus(4, context.getString(R.string.status_in_transit))
        var delivered = OrderStatus(5, context.getString(R.string.status_delivered))
        var finished = OrderStatus(6, context.getString(R.string.status_finished))
        var pending = OrderStatus(7, context.getString(R.string.status_pending))
        var pendingDistribution = OrderStatus(8, context.getString(R.string.status_pending_distribution))
        var processed = OrderStatus(9, context.getString(R.string.status_processed))
        var noStatus = OrderStatus(10, context.getString(R.string.status_no_status))
        var active = OrderStatus(11, context.getString(R.string.status_active))
        var deactivated = OrderStatus(12, context.getString(R.string.status_deactivated))
        var flashing = OrderStatus(99, context.getString(R.string.status_flashing))
        var outOfStock = OrderStatus(101, context.getString(R.string.status_out_of_stock))

        fun getAll(): ArrayList<OrderStatus> {
            val allSections = ArrayList<OrderStatus>()
            Collections.addAll(
                allSections,
                approved,
                draft,
                inProcess,
                inTransit,
                delivered,
                finished,
                pending,
                pendingDistribution,
                processed,
                noStatus,
                active,
                deactivated,
                flashing,
                outOfStock
            )

            Collections.sort(allSections, CustomComparator())
            return allSections
        }

        class CustomComparator : Comparator<OrderStatus> {
            override fun compare(o1: OrderStatus, o2: OrderStatus): Int {
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
                approved.id.toString(),
                draft.id.toString(),
                inProcess.id.toString(),
                inTransit.id.toString(),
                delivered.id.toString(),
                finished.id.toString(),
                pending.id.toString(),
                pendingDistribution.id.toString(),
                processed.id.toString(),
                noStatus.id.toString(),
                active.id.toString(),
                deactivated.id.toString(),
                flashing.id.toString(),
                outOfStock.id.toString()
            )

            return ArrayList(allSections.sortedWith(compareBy { it }))
        }

        fun getAllIdAsSet(): Set<String> {
            return getAllIdAsString().toHashSet()
        }

        fun getById(typeId: Int?): OrderStatus {
            return getById(typeId?.toLong())
        }

        fun getById(typeId: Long?): OrderStatus {
            return getAll().firstOrNull { it.id == typeId } ?: noStatus
        }
    }
}
