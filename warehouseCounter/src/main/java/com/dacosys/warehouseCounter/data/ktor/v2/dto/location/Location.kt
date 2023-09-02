package com.dacosys.warehouseCounter.data.ktor.v2.dto.location

import android.os.Parcelable

interface Location : Parcelable {
    val locationId: Long
    val locationDescription: String
    val locationParentStr: String
    val locationExternalId: String
    val locationType: LocationType
    val locationActive: Boolean
    val locationStatus: Status?
    val hashCode: Int
    val location: Location
}
