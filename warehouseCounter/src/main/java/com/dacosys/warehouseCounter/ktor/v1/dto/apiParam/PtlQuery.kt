package com.dacosys.warehouseCounter.ktor.v1.dto.apiParam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PtlQuery() {
    @SerialName(ITEM_ID_KEY)
    var itemId: Long? = null

    @SerialName(WAREHOUSE_AREA_ID_KEY)
    var warehouseAreaId: Long? = null

    @SerialName(ORDER_ID_KEY)
    var orderId: Long? = null

    @SerialName(QTY_KEY)
    var qty: Int? = null

    constructor(
        itemId: Long? = null, warehouseAreaId: Long? = null, orderId: Long? = null, qty: Int? = null
    ) : this() {
        this.itemId = itemId
        this.warehouseAreaId = warehouseAreaId
        this.orderId = orderId
        this.qty = qty
    }

    companion object {
        const val ITEM_ID_KEY = "itemId"
        const val WAREHOUSE_AREA_ID_KEY = "warehouseAreaId"
        const val ORDER_ID_KEY = "orderId"
        const val QTY_KEY = "qty"
    }
}