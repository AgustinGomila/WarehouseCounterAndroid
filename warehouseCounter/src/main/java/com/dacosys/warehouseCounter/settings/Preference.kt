package com.dacosys.warehouseCounter.settings

import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.sharedPreferences

@Suppress("UNCHECKED_CAST")
class Preference(
    val key: String,
    val default: Any,
    val description: String = "",
) : Repository {
    var value: Any
        get() = when (default) {
            is String -> getString(key, default)
            is Int -> getInt(key, default)
            is Boolean -> getBoolean(key, default)
            is Float -> getFloat(key, default)
            is Long -> getLong(key, default)
            is Set<*> -> getStringSet(key, default as Set<String>)
            else -> default
        }
        set(value) {
            when (default) {
                is String -> updateString(key, value.toString())
                is Int -> updateInt(key, value as Int)
                is Boolean -> updateBoolean(key, value as Boolean)
                is Float -> updateFloat(key, value as Float)
                is Long -> updateLong(key, value as Long)
                is Set<*> -> updateStringSet(key, value as Set<String>)
            }
        }

    override fun updateInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    override fun updateString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    override fun updateFloat(key: String, value: Float) {
        sharedPreferences.edit().putFloat(key, value).apply()
    }

    override fun updateLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    override fun updateStringSet(key: String, value: Set<String>) {
        sharedPreferences.edit().putStringSet(key, value).apply()
    }

    override fun updateBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    override fun getString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Set<String> {
        return sharedPreferences.getStringSet(key, defaultValue) ?: defaultValue
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }
}