package com.dacosys.warehouseCounter.printer

import androidx.fragment.app.FragmentActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType

open class Printer {
    fun printThis(printThis: String, qty: Int, onFinish: (Boolean) -> Unit) {
        val printer = printerDevice
        if (printer == null) {
            onFinish(false)
        } else {
            when (printer) {
                is BtPrinter -> printer.print(printThis, qty, onFinish)
                is NetPrinter -> printer.print(printThis, qty, onFinish)
                else -> onFinish(false)
            }
        }
    }

    constructor()

    constructor(activity: FragmentActivity, onEvent: (SnackBarEventData) -> Unit) {
        build(activity, onEvent)
    }

    private var printerDevice: Printer? = null

    private fun build(activity: FragmentActivity, onEvent: (SnackBarEventData) -> Unit): Printer? {
        printerDevice = when {
            settingViewModel.useBtPrinter -> BtPrinter(activity, onEvent)
            settingViewModel.useNetPrinter -> NetPrinter(onEvent)
            else -> {
                onEvent(SnackBarEventData(context.getString(R.string.there_is_no_selected_printer), SnackBarType.ERROR))
                null
            }
        }
        return printerDevice
    }
}