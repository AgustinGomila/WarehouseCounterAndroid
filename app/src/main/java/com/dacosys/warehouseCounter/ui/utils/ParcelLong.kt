package com.dacosys.warehouseCounter.ui.utils

import android.os.Parcel
import android.os.Parcelable
import java.security.MessageDigest
import java.util.*

class ParcelLong() : Parcelable {
    var value: Long = 0L

    constructor(value: Long) : this() {
        this.value = value
    }

    constructor(parcel: Parcel) : this() {
        value = parcel.readLong()
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeLong(value)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR :
        Parcelable.Creator<ParcelLong> {
        override fun createFromParcel(parcel: Parcel): ParcelLong {
            return ParcelLong(
                parcel
            )
        }

        override fun newArray(size: Int): Array<ParcelLong?> {
            return arrayOfNulls(size)
        }
    }
}

fun Long.toVersion5UUID(): UUID {
    val namespace = UUID.nameUUIDFromBytes("myapp/".toByteArray())
    val input = namespace.toString().toByteArray() +
            byteArrayOf(
                (this shr 24).toByte(),
                (this shr 16).toByte(),
                (this shr 8).toByte(),
                this.toByte()
            )

    val sha1 = MessageDigest.getInstance("SHA-1").digest(input)

    // Ajustar bytes según estándar UUID v5
    sha1[6] = (sha1[6].toInt() and 0x0F or 0x50).toByte()  // Versión 5
    sha1[8] = (sha1[8].toInt() and 0x3F or 0x80).toByte()  // Variante RFC 4122

    val msb = (0..7).fold(0L) { acc, i -> acc or (sha1[i].toLong() and 0xFF shl 56 - 8 * i) }
    val lsb = (0..7).fold(0L) { acc, i -> acc or (sha1[8 + i].toLong() and 0xFF shl 56 - 8 * i) }

    return UUID(msb, lsb)
}