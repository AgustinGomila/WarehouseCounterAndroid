package com.example.warehouseCounter.printer

import androidx.fragment.app.FragmentActivity
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.ui.snackBar.SnackBarEventData
import com.example.warehouseCounter.ui.snackBar.SnackBarType

object Printer {
    interface PrintLabelListener {
        fun printLabel(printThis: String, qty: Int, onFinish: (Boolean) -> Unit)
    }

    object PrinterFactory {
        fun createPrinter(activity: FragmentActivity, onEvent: (SnackBarEventData) -> Unit): PrintLabelListener? {
            return when {
                settingsVm.useNetPrinter -> NetPrinter(onEvent)
                settingsVm.useBtPrinter -> BtPrinter(activity, onEvent)

                else -> {
                    val msg = context.getString(R.string.there_is_no_selected_printer)
                    onEvent(SnackBarEventData(msg, SnackBarType.ERROR))
                    null
                }
            }
        }
    }
}