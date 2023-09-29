package com.dacosys.warehouseCounter.data.ktor.v2.dto.order

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.getJsonFromFile
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.Log.CREATOR.LOG_LIST_KEY
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestContent.CREATOR.CONTENT_LIST_KEY
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderStatus.CREATOR.outOfStock
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.Package.CREATOR.PACKAGE_LIST_KEY
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.OrderRequest as OrderRequestRoom
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.OrderRequestContent as OrderRequestContentRoom

@Serializable
data class OrderRequest(
    @SerialName(CLIENT_ID_KEY) var clientId: Long? = null,
    @SerialName(COMPLETED_KEY) var completed: Boolean? = null,
    @SerialName(CREATION_DATE_KEY) var creationDate: String? = null,
    @SerialName(DESCRIPTION_KEY) var description: String = "",
    @SerialName(EXTERNAL_ID_KEY) var externalId: String = "",
    @SerialName(FINISH_DATE_KEY) var finishDate: String? = null,
    @SerialName(ORDER_REQUEST_ID_KEY) var orderRequestId: Long? = null,
    @SerialName(ORDER_TYPE_DESCRIPTION_KEY) var orderTypeDescription: String = "",
    @SerialName(ORDER_TYPE_ID_KEY) var orderTypeId: Long? = null,
    @SerialName(RESULT_ALLOW_DIFF_KEY) var resultAllowDiff: Boolean? = null,
    @SerialName(RESULT_ALLOW_MOD_KEY) var resultAllowMod: Boolean? = null,
    @SerialName(RESULT_DIFF_PRODUCT_KEY) var resultDiffProduct: Boolean? = null,
    @SerialName(RESULT_DIFF_QTY_KEY) var resultDiffQty: Boolean? = null,
    @SerialName(START_DATE_KEY) var startDate: String? = null,
    @SerialName(USER_ID_KEY) var userId: Long? = null,
    @SerialName(ZONE_KEY) var zone: String = "",

    @SerialName(CONTENT_LIST_KEY) var contents: List<OrderRequestContent> = listOf(),
    @SerialName(LOG_LIST_KEY) var logs: List<Log> = listOf(),
    @SerialName(PACKAGE_LIST_KEY) var packages: List<Package> = listOf(),

    @SerialName(ROOM_ID_KEY) var roomId: Long? = null,
    @SerialName(FILENAME_KEY) var filename: String = "",
) : Parcelable {

    val orderRequestType: OrderRequestType
        get() {
            return OrderRequestType.getById(this.orderTypeId ?: 0)
        }

    constructor(parcel: Parcel) : this(
        clientId = parcel.readValue(Long::class.java.classLoader) as? Long,
        completed = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        creationDate = parcel.readString(),
        description = parcel.readString() ?: "",
        externalId = parcel.readString() ?: "",
        finishDate = parcel.readString(),
        orderRequestId = parcel.readValue(Long::class.java.classLoader) as? Long,
        orderTypeDescription = parcel.readString() ?: "",
        orderTypeId = parcel.readValue(Long::class.java.classLoader) as? Long,
        resultAllowDiff = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        resultAllowMod = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        resultDiffProduct = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        resultDiffQty = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        startDate = parcel.readString(),
        userId = parcel.readValue(Long::class.java.classLoader) as? Long,
        zone = parcel.readString() ?: "",

        contents = parcel.createTypedArrayList(OrderRequestContent)?.toList() ?: listOf(),
        logs = parcel.createTypedArrayList(Log)?.toList() ?: listOf(),
        packages = parcel.createTypedArrayList(Package)?.toList() ?: listOf(),

        roomId = parcel.readValue(Long::class.java.classLoader) as? Long,
        filename = parcel.readString() ?: "",
    )

    val toRoom: OrderRequestRoom
        get() {
            return OrderRequestRoom(
                id = this.roomId ?: 0L,
                orderRequestId = this.orderRequestId ?: 0L,
                clientId = this.clientId ?: 0L,
                completed = if (this.completed == true) 1 else 0,
                creationDate = this.creationDate ?: "",
                description = this.description,
                externalId = this.externalId,
                finishDate = this.finishDate ?: "",
                orderTypeDescription = this.orderTypeDescription,
                orderTypeId = this.orderTypeId?.toInt() ?: 0,
                resultAllowDiff = if (this.resultAllowDiff == true) 1 else 0,
                resultAllowMod = if (this.resultAllowMod == true) 1 else 0,
                resultDiffProduct = if (this.resultDiffProduct == true) 1 else 0,
                resultDiffQty = if (this.resultDiffQty == true) 1 else 0,
                startDate = this.startDate ?: "",
                userId = this.userId ?: 0L,
                zone = this.zone,
            )
        }

    fun contentToRoom(): ArrayList<OrderRequestContentRoom> {
        val orId = this.orderRequestId ?: return ArrayList()
        val r: ArrayList<OrderRequestContentRoom> = ArrayList()
        contents.mapTo(r) { it.toRoom(orId) }
        return r
    }

    constructor(filename: String) : this() {
        val jsonString = getJsonFromFile(filename)

        try {
            val or = json.decodeFromString<OrderRequest>(jsonString)

            this.clientId = or.clientId
            this.completed = or.completed
            this.creationDate = or.creationDate
            this.description = or.description
            this.externalId = or.externalId
            this.finishDate = or.finishDate
            this.orderRequestId = or.orderRequestId
            this.orderTypeDescription = or.orderTypeDescription
            this.orderTypeId = or.orderTypeId
            this.resultAllowDiff = or.resultAllowDiff
            this.resultAllowMod = or.resultAllowMod
            this.resultDiffProduct = or.resultDiffProduct
            this.resultDiffQty = or.resultDiffQty
            this.startDate = or.startDate
            this.userId = or.userId
            this.zone = or.zone

            this.contents = or.contents
            this.logs = or.logs
            this.packages = or.packages

            this.roomId = or.roomId
            this.filename = filename
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    constructor(orderResponse: OrderResponse) : this() {
        this.clientId = orderResponse.clientId
        this.completed = orderResponse.completed.isNotEmpty()
        this.creationDate = orderResponse.receivedDate
        this.description = orderResponse.description
        this.externalId = orderResponse.externalId
        this.finishDate = orderResponse.finishDate
        this.orderRequestId = orderResponse.id
        this.orderTypeDescription = orderResponse.orderType.description
        this.orderTypeId = orderResponse.orderTypeId
        this.resultAllowDiff = orderResponse.resultAllowDiff
        this.resultAllowMod = orderResponse.resultAllowMod
        this.resultDiffProduct = orderResponse.resultDiffProduct
        this.resultDiffQty = orderResponse.resultDiffQty
        this.startDate = orderResponse.startDate
        this.userId = orderResponse.collectorUserId
        this.zone = orderResponse.zone

        this.contents = orderResponse.contents.map { OrderRequestContent(it) }
        this.logs = listOf()
        this.packages = listOf()

        this.roomId = 0L
        this.filename = ""
    }

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderRequest

        return orderRequestId == other.orderRequestId
    }

    override fun hashCode(): Int {
        var result = orderRequestId?.hashCode() ?: 0
        result = 31 * result + externalId.hashCode()
        result = 31 * result + (creationDate?.hashCode() ?: 0)
        result = 31 * result + description.hashCode()
        result = 31 * result + zone.hashCode()
        result = 31 * result + (orderTypeId?.hashCode() ?: 0)
        result = 31 * result + orderTypeDescription.hashCode()
        result = 31 * result + (resultDiffQty?.hashCode() ?: 0)
        result = 31 * result + (resultDiffProduct?.hashCode() ?: 0)
        result = 31 * result + (resultAllowDiff?.hashCode() ?: 0)
        result = 31 * result + (resultAllowMod?.hashCode() ?: 0)
        result = 31 * result + (completed?.hashCode() ?: 0)
        result = 31 * result + (startDate?.hashCode() ?: 0)
        result = 31 * result + (finishDate?.hashCode() ?: 0)
        result = 31 * result + (clientId?.hashCode() ?: 0)
        result = 31 * result + (userId?.hashCode() ?: 0)
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(clientId)
        parcel.writeValue(completed)
        parcel.writeString(creationDate)
        parcel.writeString(description)
        parcel.writeString(externalId)
        parcel.writeString(finishDate)
        parcel.writeValue(orderRequestId)
        parcel.writeString(orderTypeDescription)
        parcel.writeValue(orderTypeId)
        parcel.writeValue(resultAllowDiff)
        parcel.writeValue(resultAllowMod)
        parcel.writeValue(resultDiffProduct)
        parcel.writeValue(resultDiffQty)
        parcel.writeString(startDate)
        parcel.writeValue(userId)
        parcel.writeString(zone)

        parcel.writeTypedList(contents)
        parcel.writeTypedList(logs)
        parcel.writeTypedList(packages)

        parcel.writeValue(roomId)
        parcel.writeString(filename)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderRequest> {
        const val CLIENT_ID_KEY = "clientId"
        const val COMPLETED_KEY = "completed"
        const val CREATION_DATE_KEY = "creationDate"
        const val DESCRIPTION_KEY = "description"
        const val EXTERNAL_ID_KEY = "externalId"
        const val FINISH_DATE_KEY = "finishDate"
        const val ORDER_REQUEST_ID_KEY = "orderRequestId"
        const val ORDER_TYPE_DESCRIPTION_KEY = "orderTypeDescription"
        const val ORDER_TYPE_ID_KEY = "orderTypeId"
        const val RESULT_ALLOW_DIFF_KEY = "resultAllowDiff"
        const val RESULT_ALLOW_MOD_KEY = "resultAllowMod"
        const val RESULT_DIFF_PRODUCT_KEY = "resultDiffProduct"
        const val RESULT_DIFF_QTY_KEY = "resultDiffQty"
        const val START_DATE_KEY = "startDate"
        const val USER_ID_KEY = "userId"
        const val ZONE_KEY = "zone"

        const val FILENAME_KEY = "filename"
        const val ROOM_ID_KEY = "roomId"

        fun toUpdatePayload(or: OrderRequest): OrderUpdatePayload {
            return OrderUpdatePayload(
                externalId = or.externalId,
                description = or.description,
                statusId = outOfStock.id.toInt()
            )
        }

        override fun createFromParcel(parcel: Parcel): OrderRequest {
            return OrderRequest(parcel)
        }

        override fun newArray(size: Int): Array<OrderRequest?> {
            return arrayOfNulls(size)
        }
    }
}
