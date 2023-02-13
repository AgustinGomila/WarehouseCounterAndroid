package com.dacosys.warehouseCounter.settings

import android.content.SharedPreferences

@Suppress("UNCHECKED_CAST")
class Preference(
    private val prefs: SharedPreferences,
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
        prefs.edit().putInt(key, value).apply()
    }

    override fun updateString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun updateFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    override fun updateLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun updateStringSet(key: String, value: Set<String>) {
        prefs.edit().putStringSet(key, value).apply()
    }

    override fun updateBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    override fun getString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return prefs.getFloat(key, defaultValue)
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return prefs.getLong(key, defaultValue)
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Set<String> {
        return prefs.getStringSet(key, defaultValue) ?: defaultValue
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }
}