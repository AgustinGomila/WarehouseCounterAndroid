package com.dacosys.warehouseCounter.ktor.v2.dto.location

abstract class Location {
    abstract fun id(): Long
    abstract fun description(): String
    abstract fun locationType(): LocationType
}
