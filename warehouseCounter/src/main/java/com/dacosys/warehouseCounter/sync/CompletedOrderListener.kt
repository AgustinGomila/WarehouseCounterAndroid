package com.dacosys.warehouseCounter.sync

import com.dacosys.warehouseCounter.model.orderRequest.OrderRequest

interface CompletedOrderListener {
    // Define data you like to return from AysncTask
    fun onCompletedOrderRequestResult(
        status: ProgressStatus,
        itemArray: ArrayList<OrderRequest>,
        TASK_CODE: Int,
        msg: String,
    )
}