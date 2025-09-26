package com.example.warehouseCounter.data.ktor.v1.dto.orderRequest

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.imageControl.dto.Document
import com.example.warehouseCounter.data.ktor.v1.dto.log.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class OrderRequest() : Parcelable {
    var filename: String = ""

    @SerialName("orderRequestId")
    var orderRequestId: Long? = null

    @SerialName("externalId")
    var externalId: String = ""

    @SerialName("creationDate")
    var creationDate: String? = null

    @SerialName("description")
    var description: String = ""

    @SerialName("zone")
    var zone: String = ""

    @SerialName("orderRequestedType")
    var orderRequestedType: OrderRequestType? = null

    @SerialName("resultDiffQty")
    var resultDiffQty: Boolean? = null

    @SerialName("resultDiffProduct")
    var resultDiffProduct: Boolean? = null

    @SerialName("resultAllowDiff")
    var resultAllowDiff: Boolean? = null

    @SerialName("resultAllowMod")
    var resultAllowMod: Boolean? = null

    @SerialName("completed")
    var completed: Boolean? = null

    @SerialName("startDate")
    var startDate: String? = null

    @SerialName("finishDate")
    var finishDate: String? = null

    @SerialName("clientId")
    var clientId: Long? = null

    @SerialName("userId")
    var userId: Long? = null

    @SerialName("content")
    var content: List<OrderRequestContent> = listOf()

    @SerialName("log")
    var log: Log =
        Log()

    @SerialName("document")
    var docArray: List<Document> = listOf()

    constructor(parcel: Parcel) : this() {
        filename = parcel.readString() ?: ""
        orderRequestId = parcel.readValue(Long::class.java.classLoader) as? Long
        externalId = parcel.readString() ?: ""
        creationDate = parcel.readString()
        description = parcel.readString() ?: ""
        zone = parcel.readString() ?: ""
        orderRequestedType = parcel.readParcelable(OrderRequestType::class.java.classLoader)
        resultDiffQty = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        resultDiffProduct = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        resultAllowDiff = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        resultAllowMod = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        completed = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        startDate = parcel.readString()
        finishDate = parcel.readString()
        clientId = parcel.readValue(Long::class.java.classLoader) as? Long
        userId = parcel.readValue(Long::class.java.classLoader) as? Long
        content = parcel.createTypedArrayList(OrderRequestContent) ?: listOf()
        log = parcel.readParcelable(Log::class.java.classLoader) ?: Log()
        docArray = parcel.createTypedArrayList(Document) ?: listOf()
    }

    constructor(
        orderRequestId: Long?,
        clientId: Long?,
        userId: Long?,
        externalId: String,
        creationDate: String,
        description: String,
        zone: String,
        orderRequestedType: OrderRequestType,
        resultDiffQty: Boolean?,
        resultDiffProduct: Boolean?,
        resultAllowDiff: Boolean?,
        resultAllowMod: Boolean?,
        completed: Boolean?,
        startDate: String?,
        finishDate: String?,
        content: ArrayList<OrderRequestContent>,
        documents: ArrayList<Document>,
        log: Log,
    ) : this() {
        this.orderRequestId = orderRequestId!!
        this.clientId = clientId
        this.userId = userId
        this.externalId = externalId
        this.creationDate = creationDate
        this.description = description
        this.zone = zone
        this.orderRequestedType = orderRequestedType
        this.resultDiffQty = resultDiffQty
        this.resultDiffProduct = resultDiffProduct
        this.resultAllowDiff = resultAllowDiff
        this.resultAllowMod = resultAllowMod
        this.completed = completed
        this.startDate = startDate
        this.finishDate = finishDate

        this.log = log
        this.content = content
        this.docArray = documents
    }

    override fun toString(): String {
        return description
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderRequest

        return orderRequestId == other.orderRequestId
    }

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + (orderRequestId?.hashCode() ?: 0)
        result = 31 * result + externalId.hashCode()
        result = 31 * result + (creationDate?.hashCode() ?: 0)
        result = 31 * result + description.hashCode()
        result = 31 * result + zone.hashCode()
        result = 31 * result + (orderRequestedType?.hashCode() ?: 0)
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
        parcel.writeString(filename)
        parcel.writeValue(orderRequestId)
        parcel.writeString(externalId)
        parcel.writeString(creationDate)
        parcel.writeString(description)
        parcel.writeString(zone)
        parcel.writeParcelable(orderRequestedType, flags)
        parcel.writeValue(resultDiffQty)
        parcel.writeValue(resultDiffProduct)
        parcel.writeValue(resultAllowDiff)
        parcel.writeValue(resultAllowMod)
        parcel.writeValue(completed)
        parcel.writeString(startDate)
        parcel.writeString(finishDate)
        parcel.writeValue(clientId)
        parcel.writeValue(userId)
        parcel.writeTypedList(content)
        parcel.writeParcelable(log, flags)
        parcel.writeTypedList(docArray)
    }

    companion object CREATOR : Parcelable.Creator<OrderRequest> {
        override fun createFromParcel(parcel: Parcel): OrderRequest {
            return OrderRequest(parcel)
        }

        override fun newArray(size: Int): Array<OrderRequest?> {
            return arrayOfNulls(size)
        }
    }
}