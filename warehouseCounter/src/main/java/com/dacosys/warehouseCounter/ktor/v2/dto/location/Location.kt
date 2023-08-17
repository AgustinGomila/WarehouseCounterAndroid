package com.dacosys.warehouseCounter.ktor.v2.dto.location

open class Location {
    open var locId: Long = 0
    open lateinit var desc: String
    open lateinit var locationType: LocationType
}
