package com.example.warehouseCounter.scanners.collector

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class RfidType(
    val id: Int,
    private val description: String
) : Parcelable {

    override fun toString() = description

    override fun equals(other: Any?) = other is RfidType && id == other.id
    override fun hashCode() = id

    companion object {
        private val allTypes by lazy {
            listOf(
                RfidType(0, "Ninguno"),
                RfidType(1, "VH75")
            ).sortedBy { it.id }
        }

        val none get() = allTypes[0]
        val vh75 get() = allTypes[1]

        fun getAll() = allTypes
        fun getById(id: Int) = allTypes.firstOrNull { it.id == id } ?: none
    }
}