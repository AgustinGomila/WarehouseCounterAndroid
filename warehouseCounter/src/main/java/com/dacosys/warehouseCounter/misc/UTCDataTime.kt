package com.dacosys.warehouseCounter.misc

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class UTCDataTime {
    companion object {
        private const val dateFormat = "yyyy-MM-dd HH:mm:ss"

        fun getUTCDateTimeAsDate(): Date? {
            //note: doesn't check for null
            return stringDateToDate(getUTCDateTimeAsString())
        }

        fun getUTCDateTimeAsString(): String {
            val sdf = SimpleDateFormat(dateFormat, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")

            val date = sdf.parse(sdf.format(Date())) ?: Calendar.getInstance()

            sdf.timeZone = TimeZone.getDefault()
            return sdf.format(date)
        }

        fun dateToStringDate(dateTime: Date): String? {
            val sdf = SimpleDateFormat(dateFormat, Locale.US)
            return try {
                val date = sdf.parse(sdf.format(dateTime)) ?: Calendar.getInstance()
                sdf.format(date)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        private fun stringDateToDate(StrDate: String): Date? {
            var dateToReturn: Date? = null
            val dateFormat = SimpleDateFormat(dateFormat, Locale.US)

            try {
                dateToReturn = dateFormat.parse(StrDate) as Date
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            return dateToReturn
        }
    }
}