package com.dacosys.warehouseCounter.data.ktor.v2.dto.location

import android.os.Parcelable

interface Location : Parcelable {
    var locationId: Long
    var locationDescription: String
    var locationParentStr: String
    var locationExternalId: String
    var locationType: LocationType
    var locationActive: Boolean
    var locationStatus: Status?
    var hashCode: Int
    var location: Location
}
