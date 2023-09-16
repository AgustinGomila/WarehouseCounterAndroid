package com.dacosys.warehouseCounter.printer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.google.android.gms.common.api.CommonStatusCodes
import java.io.IOException

open class BtPrinter(private val activity: FragmentActivity, private val onEvent: (SnackBarEventData) -> Unit) :
    Printer(), Runnable {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null

    private var bluetoothDevice: BluetoothDevice? = null
        get() {
            if (field == null) {
                refreshBluetoothPrinter()
            }
            return field
        }

    private val tag: String = this::class.java.simpleName

    private fun initializePrinter() {
        val printer = bluetoothDevice ?: return

        val bluetoothManager =
            context.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager

        val mBluetoothAdapter = bluetoothManager.adapter

        if (mBluetoothAdapter == null) {
            sendEvent(context.getString(R.string.there_are_no_bluetooth_devices), SnackBarType.ERROR)
        } else {
            if (!mBluetoothAdapter.isEnabled) {
                val enablePrinter = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enablePrinter.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (ActivityCompat.checkSelfPermission(
                        activity, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        requestConnectPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                    return
                }
                resultForPrinterConnect.launch(enablePrinter)
            } else {
                connectToPrinter(printer.address)
            }
        }
    }

    private fun connectToPrinter(deviceAddress: String) {
        Log.v(tag, "Coming incoming address $deviceAddress")
        bluetoothDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress)

        bluetoothSocket?.close()

        val mBluetoothConnectThread = Thread(this)
        mBluetoothConnectThread.start()
    }

    @SuppressLint("MissingPermission")
    override fun run() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val device = bluetoothDevice ?: return

                bluetoothSocket = device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
                bluetoothAdapter?.cancelDiscovery()
                bluetoothSocket?.connect()

                sendEvent(activity.getString(R.string.device_connected), SnackBarType.INFO)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestConnectPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
                return
            }
        } catch (eConnectException: IOException) {
            Log.e(tag, "CouldNotConnectToSocket", eConnectException)
            bluetoothSocket?.close()

            sendEvent(context.getString(R.string.error_connecting_device), SnackBarType.ERROR)
            return
        }
    }

    private val requestConnectPermission =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.i("DEBUG", "permission granted")
            } else {
                Log.i("DEBUG", "permission denied")

                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.CAMERA
                )
            }
        }

    private val resultForPrinterConnect =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it?.resultCode == CommonStatusCodes.SUCCESS || it?.resultCode == CommonStatusCodes.SUCCESS_CACHE) {
                val device = bluetoothDevice ?: return@registerForActivityResult
                connectToPrinter(device.address)
            }
        }

    private fun sendEvent(message: String, type: SnackBarType) {
        onEvent(SnackBarEventData(message, type))
    }

    fun print(printThis: String, qty: Int, onFinish: (Boolean) -> Unit) {
        val t = object : Thread() {
            override fun run() {
                try {
                    val socket = bluetoothSocket
                    if (socket != null) {
                        val os = socket.outputStream
                        for (i in 0 until qty) {
                            os.write(printThis.toByteArray())
                        }
                        os.flush()
                        onFinish(true)
                    } else {
                        onFinish(false)
                    }
                } catch (e: Exception) {
                    ErrorLog.writeLog(activity, tag, "${context.getString(R.string.exception_error)}: " + e.message)
                    sendEvent(
                        "${context.getString(R.string.error_connecting_to)}: ${settingsVm.printerBtAddress}",
                        SnackBarType.ERROR
                    )
                    onFinish(false)
                }
            }
        }
        t.start()
    }

    private fun refreshBluetoothPrinter() {
        val sv = settingsVm
        if (sv.useBtPrinter) {
            val printerMacAddress = sv.printerBtAddress
            if (printerMacAddress.isEmpty()) {
                return
            }

            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter ?: return

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            @SuppressLint("MissingPermission") val mPairedDevices = bluetoothAdapter.bondedDevices

            this.bluetoothAdapter = bluetoothAdapter

            if (mPairedDevices.size > 0) {
                for (mDevice in mPairedDevices) {
                    if (mDevice.address == printerMacAddress) {
                        bluetoothDevice = mDevice
                        return
                    }
                }
            }
        }
    }

    init {
        initializePrinter()
    }
}