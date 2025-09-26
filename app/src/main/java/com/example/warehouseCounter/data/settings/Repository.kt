package com.example.warehouseCounter.data.settings

interface Repository {
    fun updateInt(key: String, value: Int)
    fun updateString(key: String, value: String)
    fun updateFloat(key: String, value: Float)
    fun updateLong(key: String, value: Long)
    fun updateStringSet(key: String, value: Set<String>)
    fun updateBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int): Int
    fun getString(key: String, defaultValue: String): String
    fun getFloat(key: String, defaultValue: Float): Float
    fun getLong(key: String, defaultValue: Long): Long
    fun getStringSet(key: String, defaultValue: Set<String>): Set<String>
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
}
