package com.dacosys.warehouseCounter.ktor.v2.dto.order

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.ktor.v2.dto.order.Log.CREATOR.LOG_LIST_KEY
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequestContent.CREATOR.CONTENT_LIST_KEY
import com.dacosys.warehouseCounter.ktor.v2.dto.order.Package.CREATOR.PACKAGE_LIST_KEY
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import com.dacosys.warehouseCounter.room.entity.orderRequest.OrderRequest as OrderRequestRoom
import com.dacosys.warehouseCounter.room.entity.orderRequest.OrderRequestContent as OrderRequestContentRoom

@Serializable
data class OrderRequest(
    @SerialName(ORDER_REQUEST_ID_KEY) var orderRequestId: Long? = null,
    @SerialName(EXTERNAL_ID_KEY) var externalId: String = "",
    @SerialName(CREATION_DATE_KEY) var creationDate: String? = null,
    @SerialName(DESCRIPTION_KEY) var description: String = "",
    @SerialName(ZONE_KEY) var zone: String = "",
    @SerialName(ORDER_TYPE_ID_KEY) var orderTypeId: Long? = null,
    @SerialName(ORDER_TYPE_DESCRIPTION_KEY) var orderTypeDescription: String = "",
    @SerialName(RESULT_DIFF_QTY_KEY) var resultDiffQty: Boolean? = null,
    @SerialName(RESULT_DIFF_PRODUCT_KEY) var resultDiffProduct: Boolean? = null,
    @SerialName(RESULT_ALLOW_DIFF_KEY) var resultAllowDiff: Boolean? = null,
    @SerialName(RESULT_ALLOW_MOD_KEY) var resultAllowMod: Boolean? = null,
    @SerialName(COMPLETED_KEY) var completed: Boolean? = null,
    @SerialName(START_DATE_KEY) var startDate: String? = null,
    @SerialName(FINISH_DATE_KEY) var finishDate: String? = null,
    @SerialName(CLIENT_ID_KEY) var clientId: Long? = null,
    @SerialName(USER_ID_KEY) var userId: Long? = null,
    @SerialName(CONTENT_LIST_KEY) var contents: List<OrderRequestContent> = listOf(),
    @SerialName(LOG_LIST_KEY) var logs: List<Log> = listOf(),
    @SerialName(PACKAGE_LIST_KEY) var packages: List<Package> = listOf(),
) : Parcelable {

    var filename: String = ""

    val orderRequestType: OrderRequestType
        get() {
            return OrderRequestType.getById(this.orderTypeId ?: 0)
        }

    constructor(parcel: Parcel) : this(
        orderRequestId = parcel.readValue(Long::class.java.classLoader) as? Long,
        externalId = parcel.readString() ?: "",
        creationDate = parcel.readString(),
        description = parcel.readString() ?: "",
        zone = parcel.readString() ?: "",
        orderTypeId = parcel.readValue(Long::class.java.classLoader) as? Long,
        orderTypeDescription = parcel.readString() ?: "",
        resultDiffQty = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        resultDiffProduct = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        resultAllowDiff = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        resultAllowMod = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        completed = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        startDate = parcel.readString(),
        finishDate = parcel.readString(),
        clientId = parcel.readValue(Long::class.java.classLoader) as? Long,
        userId = parcel.readValue(Long::class.java.classLoader) as? Long,

        contents = parcel.readParcelableArray(OrderRequestContent::class.java.classLoader)
            ?.mapNotNull { if (it != null) it as OrderRequestContent else null }
            ?: listOf(),
        logs = parcel.readParcelableArray(Log::class.java.classLoader)
            ?.mapNotNull { if (it != null) it as Log else null }
            ?: listOf(),
        packages = parcel.readParcelableArray(Package::class.java.classLoader)
            ?.mapNotNull { if (it != null) it as Package else null }
            ?: listOf(),
    ) {
        filename = parcel.readString() ?: ""
    }

    val toRoom: OrderRequestRoom
        get() {
            return OrderRequestRoom(
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

    fun contentToRoom(orderId: Long): ArrayList<OrderRequestContentRoom> {
        val r: ArrayList<OrderRequestContentRoom> = ArrayList()
        contents.mapTo(r) { it.toRoom(orderId) }
        return r
    }

    constructor(filename: String) : this() {
        val jsonString = Statics.getJsonFromFile(filename)

        try {
            val or = json.decodeFromString<OrderRequest>(jsonString)
            if (or.orderRequestId == null || or.orderRequestId!! <= 0L) return

            this.orderRequestId = or.orderRequestId
            this.externalId = or.externalId
            this.creationDate = or.creationDate
            this.description = or.description
            this.zone = or.zone
            this.orderTypeId = or.orderTypeId
            this.orderTypeDescription = or.orderTypeDescription
            this.resultDiffQty = or.resultDiffQty
            this.resultDiffProduct = or.resultDiffProduct
            this.resultAllowDiff = or.resultAllowDiff
            this.resultAllowMod = or.resultAllowMod
            this.completed = or.completed
            this.startDate = or.startDate
            this.finishDate = or.finishDate
            this.clientId = or.clientId
            this.userId = or.userId

            this.logs = or.logs
            this.contents = or.contents
            this.packages = or.packages

            this.filename = filename.substringAfterLast('/')
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    constructor(orderResponse: OrderResponse) : this() {
        this.orderRequestId = orderResponse.id
        this.externalId = orderResponse.externalId
        this.creationDate = orderResponse.receivedDate
        this.description = orderResponse.description
        this.zone = orderResponse.zone
        this.orderTypeId = orderResponse.orderTypeId
        this.orderTypeDescription = orderResponse.orderType.description
        this.resultDiffQty = orderResponse.resultDiffQty
        this.resultDiffProduct = orderResponse.resultDiffProduct
        this.resultAllowDiff = orderResponse.resultAllowDiff
        this.resultAllowMod = orderResponse.resultAllowMod
        this.completed = orderResponse.completed.isNotEmpty()
        this.startDate = orderResponse.startDate
        this.finishDate = orderResponse.finishDate
        this.clientId = orderResponse.clientId
        this.userId = orderResponse.collectorUserId

        this.contents = orderResponse.contents.map { OrderRequestContent(it) }
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
        parcel.writeValue(orderRequestId)
        parcel.writeString(externalId)
        parcel.writeString(creationDate)
        parcel.writeString(description)
        parcel.writeString(zone)
        parcel.writeValue(orderTypeId)
        parcel.writeString(orderTypeDescription)
        parcel.writeValue(resultDiffQty)
        parcel.writeValue(resultDiffProduct)
        parcel.writeValue(resultAllowDiff)
        parcel.writeValue(resultAllowMod)
        parcel.writeValue(completed)
        parcel.writeString(startDate)
        parcel.writeString(finishDate)
        parcel.writeValue(clientId)
        parcel.writeValue(userId)
        parcel.writeTypedList(contents)
        parcel.writeTypedList(logs)
        parcel.writeTypedList(packages)
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

        fun getPendingOrders(): ArrayList<OrderRequest> {
            return getOrders(Statics.getPendingPath())
        }

        fun getCompletedOrders(): ArrayList<OrderRequest> {
            return getOrders(Statics.getCompletedPath())
        }

        private fun getOrders(path: File): ArrayList<OrderRequest> {
            val orArray: ArrayList<OrderRequest> = ArrayList()
            if (Statics.isExternalStorageReadable) {
                // Get the directory for the user's public pictures' directory.
                val filesInFolder = Statics.getFiles(path.absolutePath)
                if (!filesInFolder.isNullOrEmpty()) {
                    for (filename in filesInFolder) {
                        val filePath = path.absolutePath + File.separator + filename
                        val tempOr = OrderRequest(filePath)
                        if (tempOr.orderRequestId != null && !orArray.contains(tempOr)) {
                            tempOr.filename = filename
                            orArray.add(tempOr)
                        }
                    }
                }
            }
            return orArray
        }

        fun removeCountFiles(successFiles: ArrayList<String>, sendEvent: (SnackBarEventData) -> Unit) {
            val isOk = removeOrders(successFiles)
            if (isOk) {
                sendEvent(SnackBarEventData(context.getString(R.string.ok), SnackBarType.SUCCESS))
            } else {
                sendEvent(
                    SnackBarEventData(
                        context.getString(R.string.an_error_occurred_while_deleting_counts),
                        SnackBarType.ERROR
                    )
                )
            }
        }

        private fun removeOrders(files: ArrayList<String>): Boolean {
            var isOk = true
            val path = Statics.getCompletedPath()

            if (Statics.isExternalStorageWritable) {
                for (f in files) {
                    val filePath = "${path.absolutePath}${File.separator}$f"
                    val fl = File(filePath)
                    if (fl.exists()) {
                        if (!fl.delete()) {
                            isOk = false
                            break
                        }
                    }
                }
            } else {
                isOk = false
            }

            return isOk
        }

        override fun createFromParcel(parcel: Parcel): OrderRequest {
            return OrderRequest(parcel)
        }

        override fun newArray(size: Int): Array<OrderRequest?> {
            return arrayOfNulls(size)
        }
    }
}
