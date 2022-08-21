package com.dacosys.warehouseCounter.mainButton

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import java.util.*

/**
 * Created by Agustin on 16/01/2017.
 */

class MainButton(mainButton: Long, description: String, iconResource: Int?) {
    var id: Long = 0
    var description: String = ""
    var iconResource: Int? = null

    init {
        this.description = description
        this.id = mainButton
        this.iconResource = iconResource
    }

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is MainButton) {
            false
        } else this.id == other.id

        // Custom equality check here.
    }

    override fun hashCode(): Int {
        return this.id.toInt()
    }

    class CustomComparator : Comparator<MainButton> {
        override fun compare(o1: MainButton, o2: MainButton): Int {
            if (o1.id < o2.id) {
                return -1
            } else if (o1.id > o2.id) {
                return 1
            }
            return 0
        }
    }

    companion object {
        var PendingCounts = MainButton(
            1,
            Statics.WarehouseCounter.getContext().getString(R.string.pending_counts),
            R.drawable.ic_review
        )
        var CompletedCounts = MainButton(
            2,
            Statics.WarehouseCounter.getContext().getString(R.string.completed_counts),
            R.drawable.ic_send
        )
        var NewCount = MainButton(
            3,
            Statics.WarehouseCounter.getContext().getString(R.string.new_count),
            R.drawable.ic_new_count
        )
        var CodeRead = MainButton(
            4,
            Statics.WarehouseCounter.getContext().getString(R.string.code_read),
            R.drawable.ic_coderead
        )
        var LinkItemCodes = MainButton(
            6,
            Statics.WarehouseCounter.getContext().getString(R.string.code_link),
            R.drawable.ic_barcode_link
        )
        var PrintItemLabel = MainButton(
            7,
            Statics.WarehouseCounter.getContext().getString(R.string.print_code),
            R.drawable.ic_printer
        )
        var Configuration = MainButton(
            100,
            Statics.WarehouseCounter.getContext().getString(R.string.configuration),
            R.drawable.ic_settings
        )

        fun getAll(): ArrayList<MainButton> {
            val allSections = ArrayList<MainButton>()
            Collections.addAll(
                allSections,
                NewCount,
                PendingCounts,
                CompletedCounts,
                CodeRead,
                LinkItemCodes,
                PrintItemLabel,
                Configuration
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getAllMain(): ArrayList<MainButton> {
            val allSections = ArrayList<MainButton>()
            Collections.addAll(
                allSections,
                NewCount,
                PendingCounts,
                CompletedCounts,
                CodeRead,
                LinkItemCodes,
                PrintItemLabel
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getById(mainButtonId: Long): MainButton? {
            return getAll().firstOrNull { it.id == mainButtonId }
        }
    }
}