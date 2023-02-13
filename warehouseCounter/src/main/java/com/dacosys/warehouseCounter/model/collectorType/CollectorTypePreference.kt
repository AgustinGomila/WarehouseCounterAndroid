package com.dacosys.warehouseCounter.model.collectorType

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference

/**
 * Created by Agustin on 16/01/2017.
 */

class CollectorTypePreference
// ...

@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ListPreference(context, attrs) {
    init {
        entries = entries()
        entryValues = entryValues()

        if (entries.isNotEmpty() && entryValues.isNotEmpty()) {
            setValueIndex(initializeIndex())
        }
    }

    private fun entries(): Array<CharSequence> {
        //action to provide entry data in char sequence array for list
        val allCollector = CollectorType.getAll()
        val allDescription = allCollector.indices.map { allCollector[it].description }

        return allDescription.toTypedArray()
    }

    private fun entryValues(): Array<CharSequence> {
        //action to provide value data for list
        val allCollector = CollectorType.getAll()
        val allValues = allCollector.indices.map { allCollector[it].id.toString() }

        return allValues.toTypedArray()
    }

    private fun initializeIndex(): Int {
        //here you can provide the value to set (typically retrieved from the SharedPreferences)
        //...
        return 0
    }
}