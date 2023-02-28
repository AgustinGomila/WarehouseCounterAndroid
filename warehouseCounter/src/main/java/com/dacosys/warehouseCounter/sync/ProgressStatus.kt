package com.dacosys.warehouseCounter.sync

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import java.util.*

class ProgressStatus(var id: Int, var description: String) {

    override fun toString(): String {
        return description
    }

    companion object {
        var unknown = ProgressStatus(0, WarehouseCounterApp.context.getString(R.string.unknown))
        var starting = ProgressStatus(1, WarehouseCounterApp.context.getString(R.string.starting))
        var running = ProgressStatus(2, WarehouseCounterApp.context.getString(R.string.running))
        var success = ProgressStatus(3, WarehouseCounterApp.context.getString(R.string.success))
        var canceled = ProgressStatus(4, WarehouseCounterApp.context.getString(R.string.canceled))
        var crashed = ProgressStatus(5, WarehouseCounterApp.context.getString(R.string.crashed))
        var finished = ProgressStatus(6, WarehouseCounterApp.context.getString(R.string.finished))

        fun getAll(): ArrayList<ProgressStatus> {
            val allSections = ArrayList<ProgressStatus>()
            Collections.addAll(
                allSections, unknown, starting, running, success, canceled, crashed, finished
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getEnded(): ArrayList<ProgressStatus> {
            val allSections = ArrayList<ProgressStatus>()
            Collections.addAll(
                allSections, success, canceled, crashed, finished
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getById(ProgressStatusId: Int): ProgressStatus? {
            return getAll().firstOrNull { it.id == ProgressStatusId }
        }
    }
}