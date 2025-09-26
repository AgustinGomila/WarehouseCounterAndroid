package com.example.warehouseCounter.scanners.collector

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class CollectorType(
    val id: Int,
    val description: String
) : Parcelable {

    override fun toString() = description

    override fun equals(other: Any?) = other is CollectorType && id == other.id
    override fun hashCode() = id

    companion object {
        private val allTypes by lazy {
            listOf(
                CollectorType(0, "No configurado"),
                CollectorType(1, "Honeywell"),
                CollectorType(2, "Honeywell (nativo)"),
                CollectorType(3, "Zebra"),
            ).sortedBy { it.id }
        }

        val none get() = allTypes[0]
        val honeywell get() = allTypes[1]
        val honeywellNative get() = allTypes[2]
        val zebra get() = allTypes[3]

        fun getAll() = allTypes
        fun getById(id: Int) = allTypes.firstOrNull { it.id == id } ?: none
    }
}