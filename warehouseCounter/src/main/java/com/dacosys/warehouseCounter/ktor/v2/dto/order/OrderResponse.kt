package com.dacosys.warehouseCounter.ktor.v2.dto.order

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderResponseContent.CREATOR.CONTENT_RESPONSE_LIST_KEY
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    @SerialName(CLIENT_ID_KEY) var clientId: Long? = null,
    @SerialName(COMPLETED_KEY) var completed: String = "",
    @SerialName(DESCRIPTION_KEY) var description: String = "",
    @SerialName(EXTERNAL_ID_KEY) var externalId: String = "",
    @SerialName(FINISH_DATE_KEY) var finishDate: String? = null,
    @SerialName(ORDER_TYPE_ID_KEY) var orderTypeId: Long? = null,
    @SerialName(RESULT_ALLOW_DIFF_KEY) var resultAllowDiff: Boolean? = null,
    @SerialName(RESULT_ALLOW_MOD_KEY) var resultAllowMod: Boolean? = null,
    @SerialName(RESULT_DIFF_PRODUCT_KEY) var resultDiffProduct: Boolean? = null,
    @SerialName(RESULT_DIFF_QTY_KEY) var resultDiffQty: Boolean? = null,
    @SerialName(START_DATE_KEY) var startDate: String = "",
    @SerialName(ZONE_KEY) var zone: String = "",
    @SerialName(RECEIVED_DATE_KEY) var receivedDate: String = "",
    @SerialName(PROCESSED_KEY) var processed: String = "",
    @SerialName(PROCESSED_DATE_KEY) var processedDate: String = "",
    @SerialName(STATUS_ID_KEY) var statusId: Long? = null,
    @SerialName(ROW_CREATION_DATE_KEY) var rowCreationDate: String = "",
    @SerialName(ROW_MODIFICATION_DATE_KEY) var rowModificationDate: String = "",
    @SerialName(ID_KEY) var id: Long = 0L,
    @SerialName(COLLECTOR_USER_ID_KEY) var collectorUserId: Long? = null,
    @SerialName(COLLECTOR_ID_KEY) var collectorId: Long? = null,
    @SerialName(CONTENT_RESPONSE_LIST_KEY) var contents: List<OrderResponseContent> = listOf()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        clientId = parcel.readValue(Long::class.java.classLoader) as? Long,
        completed = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        externalId = parcel.readString() ?: "",
        finishDate = parcel.readString() ?: "",
        orderTypeId = parcel.readValue(Long::class.java.classLoader) as? Long,
        resultAllowDiff = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        resultAllowMod = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        resultDiffProduct = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        resultDiffQty = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        startDate = parcel.readString() ?: "",
        zone = parcel.readString() ?: "",
        receivedDate = parcel.readString() ?: "",
        processed = parcel.readString() ?: "",
        processedDate = parcel.readString() ?: "",
        statusId = parcel.readValue(Long::class.java.classLoader) as? Long,
        rowCreationDate = parcel.readString() ?: "",
        rowModificationDate = parcel.readString() ?: "",
        id = parcel.readLong(),
        collectorUserId = parcel.readValue(Long::class.java.classLoader) as? Long,
        collectorId = parcel.readValue(Long::class.java.classLoader) as? Long,
        contents = parcel.readParcelableArray(OrderResponseContent::class.java.classLoader)
            ?.mapNotNull { if (it != null) it as OrderResponseContent else null }
            ?: emptyList(),
    )

    val orderType: OrderRequestType
        get() {
            return OrderRequestType.getById(orderTypeId)
        }

    val status: OrderStatus
        get() {
            return OrderStatus.getById(statusId)
        }

    fun contentToKtor(): List<OrderRequestContent> {
        val r: ArrayList<OrderRequestContent> = ArrayList()
        contents.mapTo(r) { it.toKtor() }
        return r
    }

    val hashCode: Int
        get() {
            return hashCode()
        }

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderResponse

        return id == other.id
    }

    override fun hashCode(): Int {
        var result = clientId?.hashCode() ?: 0
        result = 31 * result + completed.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + externalId.hashCode()
        result = 31 * result + (finishDate?.hashCode() ?: 0)
        result = 31 * result + (orderTypeId?.hashCode() ?: 0)
        result = 31 * result + (resultAllowDiff?.hashCode() ?: 0)
        result = 31 * result + (resultAllowMod?.hashCode() ?: 0)
        result = 31 * result + (resultDiffProduct?.hashCode() ?: 0)
        result = 31 * result + (resultDiffQty?.hashCode() ?: 0)
        result = 31 * result + startDate.hashCode()
        result = 31 * result + zone.hashCode()
        result = 31 * result + receivedDate.hashCode()
        result = 31 * result + processed.hashCode()
        result = 31 * result + processedDate.hashCode()
        result = 31 * result + (statusId?.hashCode() ?: 0)
        result = 31 * result + rowCreationDate.hashCode()
        result = 31 * result + rowModificationDate.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + (collectorUserId?.hashCode() ?: 0)
        result = 31 * result + (collectorId?.hashCode() ?: 0)
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(clientId)
        parcel.writeString(completed)
        parcel.writeString(description)
        parcel.writeString(externalId)
        parcel.writeString(finishDate)
        parcel.writeValue(orderTypeId)
        parcel.writeValue(resultAllowDiff)
        parcel.writeValue(resultAllowMod)
        parcel.writeValue(resultDiffProduct)
        parcel.writeValue(resultDiffQty)
        parcel.writeString(startDate)
        parcel.writeString(zone)
        parcel.writeString(receivedDate)
        parcel.writeString(processed)
        parcel.writeString(processedDate)
        parcel.writeValue(statusId)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
        parcel.writeValue(id)
        parcel.writeValue(collectorUserId)
        parcel.writeValue(collectorId)
        parcel.writeTypedList(contents)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderResponse> {
        const val CLIENT_ID_KEY = "clientId"
        const val COMPLETED_KEY = "completed"
        const val DESCRIPTION_KEY = "description"
        const val EXTERNAL_ID_KEY = "externalId"
        const val FINISH_DATE_KEY = "finishDate"
        const val ORDER_TYPE_ID_KEY = "orderTypeId"
        const val RESULT_ALLOW_DIFF_KEY = "resultAllowDiff"
        const val RESULT_ALLOW_MOD_KEY = "resultAllowMod"
        const val RESULT_DIFF_PRODUCT_KEY = "resultDiffProduct"
        const val RESULT_DIFF_QTY_KEY = "resultDiffQty"
        const val START_DATE_KEY = "startDate"
        const val ZONE_KEY = "zone"
        const val RECEIVED_DATE_KEY = "receivedDate"
        const val PROCESSED_KEY = "processed"
        const val PROCESSED_DATE_KEY = "processedDate"
        const val STATUS_ID_KEY = "statusId"
        const val ROW_CREATION_DATE_KEY = "rowCreationDate"
        const val ROW_MODIFICATION_DATE_KEY = "rowModificationDate"
        const val ID_KEY = "id"
        const val COLLECTOR_USER_ID_KEY = "collectorUserId"
        const val COLLECTOR_ID_KEY = "collectorId"

        const val ORDER_RESPONSE_LIST_KEY = "orders"

        override fun createFromParcel(parcel: Parcel): OrderResponse {
            return OrderResponse(parcel)
        }

        override fun newArray(size: Int): Array<OrderResponse?> {
            return arrayOfNulls(size)
        }
    }
}
