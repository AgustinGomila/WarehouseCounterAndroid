package com.dacosys.warehouseCounter.dto.ptlOrder

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PtlOrder(
    @SerialName(ID_KEY) val id: Long,
    @SerialName(ORDER_TYPE_ID_KEY) val orderTypeId: Long,
    @SerialName(ORDER_TYPE_KEY) val orderTypeKey: String,
    @SerialName(EXTERNAL_ID_KEY) val externalId: String,
    @SerialName(CLIENT_ID_KEY) val clientId: Long,
    @SerialName(CLIENT_KEY) val client: List<Client>,
    @SerialName(COLLECTOR_ID_KEY) val collectorId: Long?,
    @SerialName(COLLECTOR_USER_ID_KEY) val collectorUserId: Long?,
    @SerialName(DESCRIPTION_KEY) val description: String,
    @SerialName(ZONE_KEY) val zone: String,
    @SerialName(RESULT_DIFF_QTY_KEY) val resultDiffQty: Boolean,
    @SerialName(RESULT_DIFF_PRODUCT_KEY) val resultDiffProduct: Boolean,
    @SerialName(RESULT_ALLOW_DIFF_KEY) val resultAllowDiff: Boolean,
    @SerialName(RESULT_ALLOW_MOD_KEY) val resultAllowMod: Boolean,
    @SerialName(COMPLETED_KEY) val completed: Boolean,
    @SerialName(START_DATE_KEY) val startDate: String?,
    @SerialName(FINISH_DATE_KEY) val finishDate: String?,
    @SerialName(RECEIVED_DATE_KEY) val receivedDate: String?,
    @SerialName(PROCESSED_KEY) val processed: Boolean,
    @SerialName(DATA_RECEPTION_ID_KEY) val dataReceptionId: Long?,
    @SerialName(DATA_RECEIVED_KEY) val dataReceived: String?,
    @SerialName(PROCESSED_DATE_KEY) val processedDate: String?,
    @SerialName(STATUS_ID_KEY) val statusId: Long,
    @SerialName(ROW_CREATION_DATE_KEY) val rowCreationDate: String,
    @SerialName(ROW_MODIFICATION_DATE_KEY) val rowModificationDate: String,
) : Parcelable {
    constructor(parcel: Parcel) : this(id = parcel.readLong(),
        orderTypeId = parcel.readLong(),
        orderTypeKey = parcel.readString() ?: "",
        externalId = parcel.readString() ?: "",
        clientId = parcel.readLong(),
        client = parcel.readParcelableArray(Client::class.java.classLoader)?.map { it as Client } ?: emptyList(),
        collectorId = parcel.readValue(Long::class.java.classLoader) as? Long,
        collectorUserId = parcel.readValue(Long::class.java.classLoader) as? Long,
        description = parcel.readString() ?: "",
        zone = parcel.readString() ?: "",
        resultDiffQty = parcel.readByte() != 0.toByte(),
        resultDiffProduct = parcel.readByte() != 0.toByte(),
        resultAllowDiff = parcel.readByte() != 0.toByte(),
        resultAllowMod = parcel.readByte() != 0.toByte(),
        completed = parcel.readByte() != 0.toByte(),
        startDate = parcel.readString() ?: "",
        finishDate = parcel.readString() ?: "",
        receivedDate = parcel.readString() ?: "",
        processed = parcel.readByte() != 0.toByte(),
        dataReceptionId = parcel.readValue(Long::class.java.classLoader) as? Long,
        dataReceived = parcel.readString() ?: "",
        processedDate = parcel.readString() ?: "",
        statusId = parcel.readLong(),
        rowCreationDate = parcel.readString() ?: "",
        rowModificationDate = parcel.readString() ?: "")

    val orderType by lazy {
        PtlOrderType.values().firstOrNull { it.id == this.orderTypeId }
    }

    companion object CREATOR : Parcelable.Creator<PtlOrder> {
        override fun createFromParcel(parcel: Parcel): PtlOrder {
            return PtlOrder(parcel)
        }

        override fun newArray(size: Int): Array<PtlOrder?> {
            return arrayOfNulls(size)
        }

        const val ID_KEY = "id"
        const val ORDER_TYPE_ID_KEY = "order_type_id"
        const val ORDER_TYPE_KEY = "order_type"
        const val EXTERNAL_ID_KEY = "external_id"
        const val CLIENT_ID_KEY = "client_id"
        const val CLIENT_KEY = "client"
        const val COLLECTOR_ID_KEY = "collector_id"
        const val COLLECTOR_USER_ID_KEY = "collector_user_id"
        const val DESCRIPTION_KEY = "description"
        const val ZONE_KEY = "zone"
        const val RESULT_DIFF_QTY_KEY = "result_diff_qty"
        const val RESULT_DIFF_PRODUCT_KEY = "result_diff_product"
        const val RESULT_ALLOW_DIFF_KEY = "result_allow_diff"
        const val RESULT_ALLOW_MOD_KEY = "result_allow_mod"
        const val COMPLETED_KEY = "completed"
        const val START_DATE_KEY = "start_date"
        const val FINISH_DATE_KEY = "finish_date"
        const val RECEIVED_DATE_KEY = "received_date"
        const val PROCESSED_KEY = "processed"
        const val DATA_RECEPTION_ID_KEY = "data_reception_id"
        const val DATA_RECEIVED_KEY = "data_received"
        const val PROCESSED_DATE_KEY = "processed_date"
        const val STATUS_ID_KEY = "status_id"
        const val ROW_CREATION_DATE_KEY = "row_creation_date"
        const val ROW_MODIFICATION_DATE_KEY = "row_modification_date"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeLong(orderTypeId)
        parcel.writeString(orderTypeKey)
        parcel.writeString(externalId)
        parcel.writeLong(clientId)
        parcel.writeParcelableArray(client.toTypedArray(), flags)
        parcel.writeValue(collectorId)
        parcel.writeValue(collectorUserId)
        parcel.writeString(description)
        parcel.writeString(zone)
        parcel.writeByte(if (resultDiffQty) 1 else 0)
        parcel.writeByte(if (resultDiffProduct) 1 else 0)
        parcel.writeByte(if (resultAllowDiff) 1 else 0)
        parcel.writeByte(if (resultAllowMod) 1 else 0)
        parcel.writeByte(if (completed) 1 else 0)
        parcel.writeString(startDate)
        parcel.writeString(finishDate)
        parcel.writeString(receivedDate)
        parcel.writeByte(if (processed) 1 else 0)
        parcel.writeValue(dataReceptionId)
        parcel.writeString(dataReceived)
        parcel.writeString(processedDate)
        parcel.writeLong(statusId)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
    }

    override fun describeContents(): Int {
        return 0
    }
}