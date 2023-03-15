package com.dacosys.warehouseCounter.misc

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.dacosys.warehouseCounter.WarehouseCounterApp

class BtPrinter {
    companion object {

        var printerBluetoothDevice: BluetoothDevice? = null
            get() {
                if (field == null) {
                    refreshBluetoothPrinter()
                }
                return field
            }

        private fun refreshBluetoothPrinter() {
            val sv = WarehouseCounterApp.settingViewModel
            if (sv.useBtPrinter) {
                val printerMacAddress = sv.printerBtAddress
                if (printerMacAddress.isEmpty()) {
                    return
                }

                val bluetoothManager =
                    WarehouseCounterApp.context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val mBluetoothAdapter = bluetoothManager.adapter

                if (ActivityCompat.checkSelfPermission(
                        WarehouseCounterApp.context, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }

                val mPairedDevices = mBluetoothAdapter!!.bondedDevices

                if (mPairedDevices.size > 0) {
                    for (mDevice in mPairedDevices) {
                        if (mDevice.address == printerMacAddress) {
                            printerBluetoothDevice = mDevice
                            return
                        }
                    }
                }
            }
        }
    }
}