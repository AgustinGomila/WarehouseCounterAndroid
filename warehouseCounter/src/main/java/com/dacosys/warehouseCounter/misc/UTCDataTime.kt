package com.dacosys.warehouseCounter.misc

import java.text.SimpleDateFormat
import java.util.*

class UTCDataTime {
    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"

        fun getUTCDateTimeAsString(): String {
            val sdf = SimpleDateFormat(DATE_FORMAT, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")

            val date = sdf.parse(sdf.format(Date())) ?: Calendar.getInstance()

            sdf.timeZone = TimeZone.getDefault()
            return sdf.format(date)
        }
    }
}