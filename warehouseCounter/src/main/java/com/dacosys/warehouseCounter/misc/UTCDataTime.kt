package com.dacosys.warehouseCounter.misc

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class UTCDataTime {
    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"

        fun getUTCDateTimeAsDate(): Date? {
            //note: doesn't check for null
            return stringDateToDate(getUTCDateTimeAsString())
        }

        fun getUTCDateTimeAsString(): String {
            val sdf = SimpleDateFormat(DATE_FORMAT, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")

            val date = sdf.parse(sdf.format(Date())) ?: Calendar.getInstance()

            sdf.timeZone = TimeZone.getDefault()
            return sdf.format(date)
        }

        fun dateToStringDate(dateTime: Date): String? {
            val sdf = SimpleDateFormat(DATE_FORMAT, Locale.US)
            return try {
                val date = sdf.parse(sdf.format(dateTime)) ?: Calendar.getInstance()
                sdf.format(date)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        private fun stringDateToDate(strDate: String): Date? {
            var dateToReturn: Date? = null
            val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US)

            try {
                dateToReturn = dateFormat.parse(strDate) as Date
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            return dateToReturn
        }
    }
}