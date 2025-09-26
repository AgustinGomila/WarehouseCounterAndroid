package com.example.warehouseCounter.data.settings.custom


import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.preference.ListPreference

class DevicePreference
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ListPreference(context, attrs) {
    init {
        entries = entries()
        entryValues = entryValues()

        if (entries != null && entries.isNotEmpty() && entryValues != null && entryValues.isNotEmpty()) {
            setValueIndex(initializeIndex())
        }
    }

    private fun entries(): Array<CharSequence>? {
        var allDescription: List<String>? = null
        try {
            //action to provide entry data in char sequence array for list
            val allCollector = getAll()
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                allDescription = allCollector.indices.map { allCollector[it].name }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return allDescription?.toTypedArray()
    }

    private fun entryValues(): Array<CharSequence>? {
        var allValues: List<String>? = null
        try {
            //action to provide value data for list
            val allCollector = getAll()
            allValues = allCollector.indices.map { allCollector[it].address }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return allValues?.toTypedArray()
    }

    private fun initializeIndex(): Int {
        //here you can provide the value to set (typically retrieved from the SharedPreferences)
        //...
        return 0
    }

    private fun getAll(): ArrayList<BluetoothDevice> {
        val itemArray: ArrayList<BluetoothDevice> = ArrayList()
        try {
            val bluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val mBluetoothAdapter = bluetoothManager.adapter
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val mPairedDevices = mBluetoothAdapter?.bondedDevices
                if (mPairedDevices != null && mPairedDevices.isNotEmpty()) {
                    for (mDevice in mPairedDevices) {
                        itemArray.add(mDevice)
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            Log.e(this::class.java.simpleName, ex.message ?: "")
        }

        return itemArray
    }
}
