package com.dacosys.warehouseCounter.scanners.rfid

import android.content.Context
import android.util.Log
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.scanners.vh75.Utility
import com.dacosys.warehouseCounter.scanners.vh75.Vh75Bt

/**
 * Created by Agustin on 19/10/2017.
 */

open class Rfid {
    interface RfidDeviceListener {
        fun onStateChanged(state: Int) {}

        fun onRead(data: ByteArray, bytes: Int) {
            val res = data.copyOfRange(0, bytes)

            Log.v(
                this::class.java.simpleName,
                "onRead Data (b: $bytes): " + Utility.bytes2HexStringWithSperator(res)
            )
            if (rfidDevice != null) {
                if (rfidDevice is Vh75Bt) {
                    Vh75Bt.processMessage(this, res, bytes)
                }
            }
        }

        fun onWrite(data: ByteArray) {
            Log.v(
                this::class.java.simpleName,
                "onWrite Data: " + Utility.bytes2HexStringWithSperator(data)
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
        // region Public methods
        var rfidDevice: Rfid? = null

        fun resume(listener: RfidDeviceListener) {
            if (rfidDevice != null) {
                if (rfidDevice is Vh75Bt) {
                    Log.v(this::class.java.simpleName, "RFID Listener: $listener")
                    (rfidDevice as Vh75Bt).setListener(null)
                    (rfidDevice as Vh75Bt).resume()
                    (rfidDevice as Vh75Bt).setListener(listener)
                }
            }
        }

        fun pause() {
            if (rfidDevice != null) {
                if (rfidDevice is Vh75Bt) {
                    Log.v(this::class.java.simpleName, "RFID Listener: NULL")
                    (rfidDevice as Vh75Bt).setListener(null)
                    (rfidDevice as Vh75Bt).pause()
                }
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
            if (rfidDevice != null) {
                if (rfidDevice is Vh75Bt) {
                    (rfidDevice as Vh75Bt).setListener(null)
                    (rfidDevice as Vh75Bt).destroy()
                }
                rfidDevice = null
            }
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
        //endregion

        fun setListener(listener: RfidDeviceListener, rfidType: RfidType) {
            if (Statics.initRequired()) {
                build(listener, listener as Context, rfidType)
            } else {
                if (rfidDevice != null && rfidDevice is Vh75Bt) {
                    (rfidDevice as Vh75Bt).setListener(listener)
                }
            }
        }

        //endregion
        fun build(listener: RfidDeviceListener?, context: Context, rfidType: RfidType): Rfid? {
            if (rfidType == RfidType.vh75) {
                rfidDevice = Vh75Bt(listener, context)
            }
            return rfidDevice
        }
    }
}