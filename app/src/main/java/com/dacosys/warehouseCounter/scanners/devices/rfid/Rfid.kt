package com.dacosys.warehouseCounter.scanners.devices.rfid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.scanners.collector.RfidType
import com.dacosys.warehouseCounter.scanners.devices.vh75.Utility
import com.dacosys.warehouseCounter.scanners.devices.vh75.Vh75Bt
import kotlin.reflect.KClass

open class Rfid {
    interface RfidDeviceListener {
        fun onStateChanged(state: Int)

        fun onRead(data: ByteArray, bytes: Int) {
            val res = data.copyOfRange(0, bytes)

            Log.v(
                this::class.java.simpleName,
                "onRead Data (b: $bytes): " + Utility.bytes2HexStringWithSeparator(res)
            )
            if (vh75 != null) {
                Vh75Bt.processMessage(this, res, bytes)
            }
        }

        fun onWrite(data: ByteArray) {
            Log.v(
                this::class.java.simpleName,
                "onWrite Data: " + Utility.bytes2HexStringWithSeparator(data)
            )
        }

        fun onDeviceName(deviceName: String) {
            Log.v(this::class.java.simpleName, "onDeviceName deviceName: $deviceName")
        }

        fun onReadCompleted(scanCode: String)
        fun onWriteCompleted(isOk: Boolean)
        fun onGetBluetoothName(name: String)
    }

    companion object {
        private var rfidDevice: Rfid? = null

        val vh75: Vh75Bt?
            get() = rfidDevice as? Vh75Bt

        val vh75State: Int?
            get() = vh75?.state

        fun classImplementsInterface(clazz: KClass<*>, interfaceKClass: KClass<*>): Boolean {
            return interfaceKClass.java.isAssignableFrom(clazz.java)
        }

        fun appHasBluetoothPermission(): Boolean {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ActivityCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }

        private fun initRfidRequired(): Boolean {
            return isRfidEnabled() && (rfidDevice == null || vh75?.state == Vh75Bt.STATE_NONE)
        }

        private fun isRfidEnabled(): Boolean {
            if (!settingsVm.useRfid) return false
            return settingsVm.rfidBtAddress.isNotEmpty()
        }

        fun isRfidRequired(activity: AppCompatActivity): Boolean {
            return isRfidRequired(activity::class)
        }

        fun isRfidRequired(listener: KClass<*>): Boolean {
            if (!isRfidEnabled())
                return false

            return classImplementsInterface(listener, RfidDeviceListener::class)
        }

        fun resume(listener: RfidDeviceListener) {
            if (vh75 != null) {
                Log.v(this::class.java.simpleName, "RFID Listener: $listener")
                vh75?.setListener(null)
                vh75?.resume()
                vh75?.setListener(listener)
            }
        }

        fun pause() {
            if (vh75 != null) {
                Log.v(this::class.java.simpleName, "RFID Listener: NULL")
                vh75?.setListener(null)
                vh75?.pause()
            }
        }

        fun startScan() {
            if (rfidDevice != null) {
            }
        }

        fun stopScan() {
            if (rfidDevice != null) {
            }
        }

        fun destroy() {
            if (vh75 != null) {
                vh75?.setListener(null)
                vh75?.destroy()
            }
            rfidDevice = null
        }

        fun lockScanner(lock: Boolean) {
            if (rfidDevice != null) {
            }
        }

        fun getStatus(): Int {
            if (rfidDevice != null) {
            }
            return -1
        }

        fun setListener(listener: RfidDeviceListener, rfidType: RfidType) {
            if (initRfidRequired()) {
                build(listener, rfidType)
            } else {
                if (vh75 != null) {
                    vh75?.setListener(listener)
                }
            }
        }

        fun build(listener: RfidDeviceListener?, rfidType: RfidType): Rfid? {
            if (rfidType == RfidType.vh75) {
                rfidDevice = Vh75Bt(listener)
            }
            return rfidDevice
        }
    }
}