package com.example.warehouseCounter.scanners.devices.vh75

import java.util.*

object Utility {
    fun convert2HexArray(hexString: String): ByteArray {
        val len = hexString.length / 2
        val chars = hexString.toCharArray()
        val hexes = arrayOfNulls<String>(len)
        val bytes = ByteArray(len)
        var i = 0
        var j = 0
        while (j < len) {
            hexes[j] = "" + chars[i] + chars[i + 1]
            bytes[j] = hexes[j]!!.toInt(16).toByte()
            i += 2
            j++
        }
        return bytes
    }

    fun bytes2String(b: ByteArray?): String {
        return String(b!!).trim { it <= ' ' }
    }

    //byte Hexadecimal
    fun bytes2HexString(b: ByteArray, size: Int): String {
        val ret = StringBuilder()
        for (i in 0 until size) {
            var hex = Integer.toHexString(b[i].toInt() and 0xFF)
            if (hex.length == 1) {
                hex = "0$hex"
            }
            ret.append(hex.uppercase(Locale.getDefault()))
        }
        return ret.toString()
    }

    fun bytes2HexString(b: ByteArray): String {
        val ret = StringBuilder()
        for (aB in b) {
            var hex = Integer.toHexString(aB.toInt() and 0xFF)
            if (hex.length == 1) {
                hex = "0$hex"
            }
            ret.append(hex.uppercase(Locale.getDefault()))
        }
        return ret.toString()
    }

    fun bytes2HexStringWithSeparator(b: ByteArray): String {
        val ret = StringBuilder()
        for (i in b.indices) {
            var hex = Integer.toHexString(b[i].toInt() and 0xFF)
            if (hex.length == 1) {
                hex = "0$hex"
            }
            ret.append(hex.uppercase(Locale.getDefault()))
            if ((i + 1) % 4 == 0 && i + 1 != b.size) ret.append("-")
        }
        return ret.toString()
    }

    fun toByte(i: Int): Byte {
        return i.toByte()
    }

    /**
     * check whether the str is a hex str
     *
     * @param str  str
     * @param bits bits
     * @return true or false
     */
    fun isHexString(str: String, bits: Int): Boolean {
        val patten = "[abcdefABCDEF0123456789]*$bits}"
        return str.matches(patten.toRegex())
    }

    fun isHexString(str: String): Boolean {
        val patten = "[abcdefABCDEF0123456789]+"
        return str.matches(patten.toRegex())
    }

    fun isNumber(str: String): Boolean {
        val patten = "-?[0123456789]*"
        return str.matches(patten.toRegex())
    }
}
