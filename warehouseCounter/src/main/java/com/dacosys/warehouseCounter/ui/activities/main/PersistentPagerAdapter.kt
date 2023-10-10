package com.dacosys.warehouseCounter.ui.activities.main

import android.util.SparseArray
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

@Suppress("UNCHECKED_CAST")
abstract class PersistentPagerAdapter<T : Fragment?>(
    fragmentManager: FragmentManager?,
) : FragmentPagerAdapter(fragmentManager!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val registeredFragments = SparseArray<T>()
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as T
        registeredFragments.put(position, fragment)
        return fragment!!
    }

    override fun destroyItem(
        container: ViewGroup,
        position: Int,
        `object`: Any,
    ) {
        registeredFragments.remove(position)
        super.destroyItem(container, position, `object`)
    }

    fun getRegisteredFragment(container: ViewGroup, position: Int): T {
        val existingInstance = registeredFragments[position]
        return existingInstance ?: instantiateItem(container, position) as T
    }
}
