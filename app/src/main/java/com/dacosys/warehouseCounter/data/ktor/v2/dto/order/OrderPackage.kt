package com.dacosys.warehouseCounter.data.ktor.v2.dto.order

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderPackage(
    @SerialName(ID_KEY) var id: Long? = null,
    @SerialName(CODE_KEY) var code: String = "",
    @SerialName(EXTERNAL_ID_KEY) var externalId: String = "",
    @SerialName(ORDER_ID_KEY) var orderId: Long? = null,
    @SerialName(ROW_CREATION_DATE_KEY) var rowCreationDate: String = "",
    @SerialName(ROW_MODIFICATION_DATE_KEY) var rowModificationDate: String = "",

    ) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readValue(Long::class.java.classLoader) as? Long,
        code = parcel.readString() ?: "",
        externalId = parcel.readString() ?: "",
        orderId = parcel.readValue(Long::class.java.classLoader) as? Long,
        rowCreationDate = parcel.readString() ?: "",
        rowModificationDate = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(code)
        parcel.writeString(externalId)
        parcel.writeValue(orderId)
        parcel.writeString(rowCreationDate)
        parcel.writeString(rowModificationDate)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderPackage> {
        const val ID_KEY = "id"
        const val EXTERNAL_ID_KEY = "externalId"
        const val CODE_KEY = "code"
        const val ORDER_ID_KEY = "orderId"
        const val ROW_CREATION_DATE_KEY = "rowCreationDate"
        const val ROW_MODIFICATION_DATE_KEY = "rowModificationDate"

        const val ORDER_PACKAGE_LIST_KEY = "orderPackages"

        override fun createFromParcel(parcel: Parcel): OrderPackage {
            return OrderPackage(parcel)
        }

        override fun newArray(size: Int): Array<OrderPackage?> {
            return arrayOfNulls(size)
        }
    }
}
