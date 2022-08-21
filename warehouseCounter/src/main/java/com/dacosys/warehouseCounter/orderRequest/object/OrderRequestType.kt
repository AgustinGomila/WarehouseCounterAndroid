package com.dacosys.warehouseCounter.orderRequest.`object`

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import com.google.gson.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
import java.util.*

class OrderRequestType :
    Parcelable,
    JsonDeserializer<OrderRequestType>,
    JsonSerializer<OrderRequestType> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): OrderRequestType? {
        if (json == null) {
            return null
        }

        val jsonObject = json.asJsonObject

        val orc = OrderRequestType()

        orc.id = (jsonObject.get("orderRequestTypeId")?.asLong) ?: 0L
        orc.description = (jsonObject.get("description")?.asString) ?: ""

        return orc
    }

    override fun serialize(
        src: OrderRequestType?,
        typeOfSrc: Type,
        context: JsonSerializationContext?,
    ): JsonElement {
        val jsonObject = JsonObject()
        if (src != null) {
            jsonObject.addProperty("orderRequestTypeId", src.id)
            jsonObject.addProperty("description", src.description)
        }

        return jsonObject
    }

    // region Variables de la clase
    @Expose
    @SerializedName("orderRequestTypeId")
    var id: Long = 0

    @Expose
    @SerializedName("description")
    var description: String = ""

    // Esta variable no va al json
    // endregion

    constructor()

    constructor(orderRequestTypeId: Long, description: String) {
        this.description = description
        this.id = orderRequestTypeId
    }

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is OrderRequestType) {
            false
        } else this.id == other.id

        // Custom equality check here.
    }

    override fun hashCode(): Int {
        return this.id.toInt()
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

    companion object CREATOR : Parcelable.Creator<OrderRequestType> {
        override fun createFromParcel(parcel: Parcel): OrderRequestType {
            return OrderRequestType(parcel)
        }

        override fun newArray(size: Int): Array<OrderRequestType?> {
            return arrayOfNulls(size)
        }

        var prepareOrder = OrderRequestType(
            1,
            Statics.WarehouseCounter.getContext().getString(R.string.order_preparation)
        )
        var stockAudit = OrderRequestType(
            2,
            Statics.WarehouseCounter.getContext().getString(R.string.warehouse_counter)
        )
        var receptionAudit = OrderRequestType(
            3,
            Statics.WarehouseCounter.getContext().getString(R.string.reception_control)
        )
        var deliveryAudit = OrderRequestType(
            4,
            Statics.WarehouseCounter.getContext().getString(R.string.delivery_control)
        )
        var stockAuditFromDevice = OrderRequestType(
            5,
            Statics.WarehouseCounter.getContext().getString(R.string.warehouse_counter_from_device)
        )

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

        fun getAllIdAsString(): ArrayList<String> {
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

        fun getById(orderRequestTypeId: Long): OrderRequestType? {
            return getAll().firstOrNull { it.id == orderRequestTypeId }
        }
    }
}