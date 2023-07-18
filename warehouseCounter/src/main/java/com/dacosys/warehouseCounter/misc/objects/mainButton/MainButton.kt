package com.dacosys.warehouseCounter.misc.objects.mainButton

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
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

    companion object {
        var PendingCounts =
            MainButton(1, context.getString(R.string.pending_counts), R.drawable.ic_review)
        var CompletedCounts =
            MainButton(2, context.getString(R.string.completed_counts), R.drawable.ic_send)
        var NewCount =
            MainButton(3, context.getString(R.string.new_count), R.drawable.ic_new_count)
        var CodeRead =
            MainButton(4, context.getString(R.string.code_read), R.drawable.ic_coderead)
        var PtlOrder =
            MainButton(5, context.getString(R.string.ptl_order), R.drawable.ic_order)
        var LinkItemCodes =
            MainButton(6, context.getString(R.string.code_link), R.drawable.ic_barcode_link)
        var PrintItemLabel =
            MainButton(7, context.getString(R.string.print_code), R.drawable.ic_printer)
        var OrderLocationLabel =
            MainButton(8, context.getString(R.string.order_location), R.drawable.ic_order_location)
        var Configuration =
            MainButton(100, context.getString(R.string.configuration), R.drawable.ic_settings)

        fun getAll(): ArrayList<MainButton> {
            val allSections = ArrayList<MainButton>()
            Collections.addAll(
                allSections,
                NewCount,
                PendingCounts,
                CompletedCounts,
                CodeRead,
                PtlOrder,
                LinkItemCodes,
                PrintItemLabel,
                OrderLocationLabel,
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
                PtlOrder,
                CodeRead,
                LinkItemCodes,
                PrintItemLabel,
                OrderLocationLabel
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getById(mainButtonId: Long): MainButton? {
            return getAll().firstOrNull { it.id == mainButtonId }
        }
    }
}