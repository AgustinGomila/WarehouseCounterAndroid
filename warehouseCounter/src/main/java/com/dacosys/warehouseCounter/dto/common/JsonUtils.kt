package com.dacosys.warehouseCounter.dto.common

import android.util.Log
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.squareup.moshi.JsonDataException

class JsonUtils {
    companion object {
        inline fun <reified T> fromJson(s: String): T? {
            try {
                return moshi.adapter(T::class.java).fromJson(s)
            } catch (ignored: JsonDataException) { // No pasa nada
            } catch (ex: Exception) {
                Log.e(T::class.java.simpleName, ex.toString())
                ex.printStackTrace()
            }
            return null
        }

        inline fun <reified T> toJson(t: T): String? {
            try {
                return moshi.adapter(T::class.java).toJson(t)
            } catch (ex: Exception) {
                Log.e(T::class.java.simpleName, ex.toString())
                ex.printStackTrace()
            }
            return null
        }
    }
}