package com.dacosys.warehouseCounter.misc.objects.table

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import java.util.*

class Table(var tableId: Int, var tableName: String, var description: String) {

    override fun toString(): String {
        return description
    }

    companion object {
        var orderRequest = Table(6, "order_request", context.getString(R.string.order_request))

        fun getAll(): ArrayList<Table> {
            val allSections = ArrayList<Table>()
            Collections.addAll(
                allSections, orderRequest
            )

            return ArrayList(allSections.sortedWith(compareBy { it.tableId }))
        }

        fun getById(tableId: Int): Table? {
            return getAll().firstOrNull { it.tableId == tableId }
        }
    }
}