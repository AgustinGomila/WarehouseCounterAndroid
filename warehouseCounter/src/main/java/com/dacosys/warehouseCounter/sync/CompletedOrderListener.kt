package com.dacosys.warehouseCounter.sync

import com.dacosys.warehouseCounter.dto.orderRequest.OrderRequest

interface CompletedOrderListener {
    fun onCompletedOrderResult(
        status: ProgressStatus,
        itemArray: ArrayList<OrderRequest>,
        taskCode: Int,
        msg: String,
    )
}