package com.dacosys.warehouseCounter.dto.apiParam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PtlQuery() {
    @SerialName(itemIdTag)
    var itemId: Long? = null

    @SerialName(warehouseAreaIdTag)
    var warehouseAreaId: Long? = null

    @SerialName(orderIdTag)
    var orderId: Long? = null

    @SerialName(qtyTag)
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
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val itemIdTag = "itemId"
        const val warehouseAreaIdTag = "warehouseAreaId"
        const val orderIdTag = "orderId"
        const val qtyTag = "qty"
    }
}