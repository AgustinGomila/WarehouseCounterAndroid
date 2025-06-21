/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dacosys.warehouseCounter.scanners.devices.vh75

/**
 * Command header
 */
enum class Head(code: Byte) {
    SEND(0x40.toByte()),
    RECEIVE_OK(0xf0.toByte()),
    RECEIVE_FAIL(0xf4.toByte());

    var code: Byte = 0
        internal set

    init {
        this.code = code
    }
}
