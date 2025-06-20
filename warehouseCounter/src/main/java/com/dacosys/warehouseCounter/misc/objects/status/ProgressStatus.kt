package com.dacosys.warehouseCounter.misc.objects.status

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import java.util.*

class ProgressStatus(var id: Int, var description: String) {

    override fun toString(): String {
        return description
    }

    companion object {
        var unknown = ProgressStatus(0, context.getString(R.string.unknown))
        var starting = ProgressStatus(1, context.getString(R.string.starting))
        var running = ProgressStatus(2, context.getString(R.string.running))
        var success = ProgressStatus(3, context.getString(R.string.success))
        var canceled = ProgressStatus(4, context.getString(R.string.canceled))
        var crashed = ProgressStatus(5, context.getString(R.string.crashed))
        var finished = ProgressStatus(6, context.getString(R.string.finished))

        fun getAll(): ArrayList<ProgressStatus> {
            val allSections = ArrayList<ProgressStatus>()
            Collections.addAll(
                allSections, unknown, starting, running, success, canceled, crashed, finished
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getById(id: Int): ProgressStatus? {
            return getAll().firstOrNull { it.id == id }
        }

        fun getAllFinish(): ArrayList<ProgressStatus> {
            val allSections = ArrayList<ProgressStatus>()
            Collections.addAll(
                allSections,
                canceled,
                crashed,
                finished,
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }
    }
}
