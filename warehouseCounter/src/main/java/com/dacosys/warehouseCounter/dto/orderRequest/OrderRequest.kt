@file:Suppress("UNCHECKED_CAST")

package com.dacosys.warehouseCounter.dto.orderRequest

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.imageControl.moshi.Document
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.dto.log.Log
import com.dacosys.warehouseCounter.misc.Statics
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.File


@JsonClass(generateAdapter = true)
class OrderRequest() : Parcelable {
    var filename: String = ""

    @Json(name = "orderRequestId")
    var orderRequestId: Long? = null

    @Json(name = "externalId")
    var externalId: String = ""

    @Json(name = "creationDate")
    var creationDate: String? = null

    @Json(name = "description")
    var description: String = ""

    @Json(name = "zone")
    var zone: String = ""

    @Json(name = "orderRequestedType")
    var orderRequestedType: OrderRequestType? = null

    @Json(name = "resultDiffQty")
    var resultDiffQty: Boolean? = null

    @Json(name = "resultDiffProduct")
    var resultDiffProduct: Boolean? = null

    @Json(name = "resultAllowDiff")
    var resultAllowDiff: Boolean? = null

    @Json(name = "resultAllowMod")
    var resultAllowMod: Boolean? = null

    @Json(name = "completed")
    var completed: Boolean? = null

    @Json(name = "startDate")
    var startDate: String? = null

    @Json(name = "finishDate")
    var finishDate: String? = null

    @Json(name = "clientId")
    var clientId: Long? = null

    @Json(name = "userId")
    var userId: Long? = null

    @Json(name = "content")
    var content: List<OrderRequestContent> = ArrayList<OrderRequestContent>().toList()

    @Json(name = "log")
    var log: Log = Log()

    @Json(name = "document")
    var docArray: List<Document> = ArrayList<Document>().toList()

    constructor(parcel: Parcel) : this() {
        filename = parcel.readString() ?: ""
        orderRequestId = parcel.readValue(Long::class.java.classLoader) as? Long
        externalId = parcel.readString() ?: ""
        creationDate = parcel.readString() ?: ""
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
        log = parcel.readParcelable(Log::class.java.classLoader) ?: Log()
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

        this.content = content
        this.docArray = documents
        this.log = log
    }

    constructor(filename: String) : this() {
        val completeJson = Statics.getJsonFromFile(filename)

        try {
            val or = moshi.adapter(OrderRequest::class.java).fromJson(completeJson) ?: return
            if (or.orderRequestId == null || or.orderRequestId!! <= 0L) return

            this.orderRequestId = or.orderRequestId
            this.externalId = or.externalId
            this.creationDate = or.creationDate
            this.description = or.description
            this.zone = or.zone
            this.orderRequestedType = or.orderRequestedType
            this.resultDiffQty = or.resultDiffQty
            this.resultDiffProduct = or.resultDiffProduct
            this.resultAllowDiff = or.resultAllowDiff
            this.resultAllowMod = or.resultAllowMod
            this.completed = or.completed
            this.startDate = or.startDate
            this.finishDate = or.finishDate
            this.clientId = or.clientId
            this.userId = or.userId

            this.content = or.content
            this.log = or.log
            this.docArray = or.docArray

            this.filename = filename.substringAfterLast('/')
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun toString(): String {
        return description
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
        parcel.writeParcelable(log, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderRequest

        if (orderRequestId != other.orderRequestId) return false

        return true
    }

    override fun hashCode(): Int {
        return orderRequestId?.hashCode() ?: 0
    }

    companion object CREATOR : Parcelable.Creator<OrderRequest> {
        override fun createFromParcel(parcel: Parcel): OrderRequest {
            return OrderRequest(parcel)
        }

        override fun newArray(size: Int): Array<OrderRequest?> {
            return arrayOfNulls(size)
        }

        fun getPendingOrders(): ArrayList<OrderRequest> {
            return getOrders(Statics.getPendingPath())
        }

        fun getCompletedOrders(): ArrayList<OrderRequest> {
            return getOrders(Statics.getCompletedPath())
        }

        private fun getOrders(path: File): ArrayList<OrderRequest> {
            val orArray: ArrayList<OrderRequest> = ArrayList()
            if (Statics.isExternalStorageReadable) {
                // Get the directory for the user's public pictures directory.
                val filesInFolder = Statics.getFiles(path.absolutePath)
                if (filesInFolder != null && filesInFolder.isNotEmpty()) {
                    for (filename in filesInFolder) {
                        val filePath = path.absolutePath + File.separator + filename
                        val tempOr = OrderRequest(filePath)
                        if (tempOr.orderRequestId != null && !orArray.contains(tempOr)) {
                            tempOr.filename = filename
                            orArray.add(tempOr)
                        }
                    }
                }
            } else {
                /*MakeText.makeText(
                    binding.root,
                    context().getString(R.string.error_external_storage_not_available_for_reading),
                    com.dacosys.warehouseCounter.misc.snackbar.SnackBarType.ERROR
                )*/
            }

            return orArray
        }
    }
}