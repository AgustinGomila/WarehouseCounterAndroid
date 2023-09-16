package com.dacosys.warehouseCounter.printer

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import java.io.IOException
import java.net.ConnectException
import java.net.Socket
import java.net.UnknownHostException

open class NetPrinter(private val onEvent: (SnackBarEventData) -> Unit) :
    Printer() {

    private val tag: String = this::class.java.simpleName
    fun print(printThis: String, qty: Int, onFinish: (Boolean) -> Unit) {
        val ipPrinter = settingViewModel.ipNetPrinter
        val portPrinter = settingViewModel.portNetPrinter

        Log.v(tag, "Printer IP: $ipPrinter ($portPrinter)")
        Log.v(tag, printThis)

        val t = object : Thread() {
            override fun run() {
                try {
                    val socket = Socket(ipPrinter, portPrinter)
                    val os = socket.outputStream
                    for (i in 0 until qty) {
                        os.write(printThis.toByteArray())
                    }
                    os.flush()
                    socket.close()
                    onFinish(true)
                } catch (e: UnknownHostException) {
                    e.printStackTrace()
                    sendEvent(
                        "${context.getString(R.string.unknown_host)}: $ipPrinter ($portPrinter)",
                        SnackBarType.ERROR
                    )
                    onFinish(false)
                } catch (e: ConnectException) {
                    e.printStackTrace()
                    sendEvent(
                        "${context.getString(R.string.error_connecting_to)}: $ipPrinter ($portPrinter)",
                        SnackBarType.ERROR
                    )
                    onFinish(false)
                } catch (e: IOException) {
                    e.printStackTrace()
                    sendEvent(
                        "${context.getString(R.string.error_printing_to)} $ipPrinter ($portPrinter)",
                        SnackBarType.ERROR
                    )
                    onFinish(false)
                }
            }
        }
        t.start()
    }

    private fun sendEvent(message: String, type: SnackBarType) {
        onEvent(SnackBarEventData(message, type))
    }
}