package com.dacosys.warehouseCounter.ui.adapter.item

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat.getColor
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.data.room.entity.item.Item
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import java.util.*


/**
 * Created by Agustin on 18/01/2017.
 */

class ItemAdapter(
    private var resource: Int,
    private var activity: AppCompatActivity,
    private var itemList: ArrayList<Item>,
    private var suggestedList: ArrayList<Item>
) : ArrayAdapter<Item>(context, R.layout.item_row_simple, suggestedList), Filterable {

    override fun clear() {
        activity.runOnUiThread {
            super.clear()
            clearChecked()
        }
    }

    override fun sort(comparator: Comparator<in Item>) {
        super.sort(customComparator)
    }

    private val customComparator = Comparator { o1: Item?, o2: Item? ->
        ItemComparator().compareNullable(o1, o2)
    }

    fun count(): Int {
        return count
    }

    fun countChecked(): Int {
        return checkedIdArray.count()
    }

    fun getAll(): ArrayList<Item> {
        val r: ArrayList<Item> = ArrayList()
        for (i in 0 until count) {
            r.add(getItem(i) as Item)
        }
        return r
    }

    fun getAllChecked(): ArrayList<Long> {
        return checkedIdArray
    }

    private var isFilling = false
    fun setChecked(items: ArrayList<Item>, isChecked: Boolean) {
        if (isFilling) return
        isFilling = true

        for (i in items) {
            setChecked(i, isChecked)
        }

        isFilling = false
        refresh()
    }

    fun setChecked(item: Item, isChecked: Boolean, suspendRefresh: Boolean = false) {
        if (isChecked) {
            if (!checkedIdArray.contains(item.itemId)) {
                checkedIdArray.add(item.itemId)
            }
        } else {
            checkedIdArray.remove(item.itemId)
        }

        if (!suspendRefresh) {
            refresh()
        }
    }

    fun setChecked(checkedItems: ArrayList<Item>) {
        checkedItems.clear()
        setChecked(checkedItems, true)
    }

    private fun clearChecked() {
        checkedIdArray.clear()
    }

    private fun refresh() {
        activity.runOnUiThread { notifyDataSetChanged() }
    }

    private var multiSelect: Boolean = false
    private var checkedIdArray: ArrayList<Long> = ArrayList()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        val alreadyExists: Boolean

        if (v == null || v.tag == null) {
            val vi = LayoutInflater.from(context)
            v = vi.inflate(this.resource, parent, false)
            alreadyExists = false
        } else {
            alreadyExists = true
        }

        createViewHolder(position, v!!, alreadyExists)

        val h = if (v.height > 0) v.height else v.minimumHeight
        settingViewModel.itemViewHeight = h
        if (BuildConfig.DEBUG) Log.d(this::class.java.simpleName, "-------{RES: $resource Height:${h}}-------")

        return v
    }

    private fun createViewHolder(position: Int, v: View, alreadyExists: Boolean): View {
        var holder = SimpleViewHolder()
        if (alreadyExists) {
            holder = v.tag as SimpleViewHolder
        } else {
            holder.descriptionTextView = v.findViewById(R.id.description)
            holder.eanTextView = v.findViewById(R.id.ean)
            holder.checkBox = v.findViewById(R.id.checkBox)

            if (multiSelect) {
                holder.checkBox?.visibility = VISIBLE
            } else {
                holder.checkBox?.visibility = GONE
            }

            v.tag = holder
        }

        return bind(position, v, holder)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bind(position: Int, v: View, holder: SimpleViewHolder): View {
        if (position >= 0) {
            val item = getItem(position)

            if (item != null) {
                holder.descriptionTextView?.text = item.description
                holder.eanTextView?.text = item.ean

                if (holder.checkBox != null) {
                    var isSpeakButtonLongPressed = false

                    val checkChangeListener =
                        CompoundButton.OnCheckedChangeListener { _, isChecked ->
                            this.setChecked(item, isChecked, true)
                        }

                    val pressHoldListener =
                        View.OnLongClickListener { // Do something when your hold starts here.
                            isSpeakButtonLongPressed = true
                            true
                        }

                    val pressTouchListener = View.OnTouchListener { pView, pEvent ->
                        pView.onTouchEvent(pEvent)
                        // We're only interested in when the button is released.
                        if (pEvent.action == MotionEvent.ACTION_UP) {
                            // We're only interested in anything if our speak button is currently pressed.
                            if (isSpeakButtonLongPressed) {
                                // Do something when the button is released.
                                if (!isFilling) {
                                    holder.checkBox!!.setOnCheckedChangeListener(null)
                                    val newState = !holder.checkBox!!.isChecked
                                    this.setChecked(getAll(), newState)
                                }
                                isSpeakButtonLongPressed = false
                            }
                        }
                        return@OnTouchListener true
                    }

                    //Important to remove previous checkedChangedListener before calling setChecked
                    holder.checkBox?.setOnCheckedChangeListener(null)
                    holder.checkBox?.isChecked = checkedIdArray.contains(item.itemId)
                    holder.checkBox?.tag = position
                    holder.checkBox?.setOnLongClickListener(pressHoldListener)
                    holder.checkBox?.setOnTouchListener(pressTouchListener)
                    holder.checkBox?.setOnCheckedChangeListener(checkChangeListener)
                }

                setStyle(item, v, holder)
            }
        }

        return v
    }

    private fun setStyle(item: Item, v: View, holder: SimpleViewHolder) {
        // Background colors
        val lightgray = getColor(context.resources, R.color.lightgray, null)
        val whitesmoke = getColor(context.resources, R.color.whitesmoke, null)

        when (item.active) {
            1 -> {
                v.setBackgroundColor(whitesmoke)
                holder.descriptionTextView?.setTextColor(defaultForeColor)
                holder.eanTextView?.setTextColor(defaultForeColor)
            }

            else -> {
                v.setBackgroundColor(lightgray)
                holder.descriptionTextView?.setTextColor(inactiveForeColor)
                holder.eanTextView?.setTextColor(inactiveForeColor)
            }
        }
    }

    //region COLORS

    private var selectedForeColor: Int = 0
    private var inactiveForeColor: Int = 0
    private var defaultForeColor: Int = 0

    private fun setupColors() {
        selectedForeColor = getColor(context.resources, R.color.text_light, null)
        inactiveForeColor = getBestContrastColor(getColor(context.resources, R.color.status_lot_inactive, null))
        defaultForeColor = getBestContrastColor(getColor(context.resources, R.color.status_default, null))
    }

    //endregion

    internal inner class SimpleViewHolder {
        var descriptionTextView: CheckedTextView? = null
        var eanTextView: CheckedTextView? = null
        var checkBox: CheckBox? = null
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val r: ArrayList<Item> = ArrayList()
                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    if (filterString.isNotEmpty()) {
                        var filterableItem: Item

                        for (i in 0 until itemList.size) {
                            filterableItem = itemList[i]
                            if (isFilterable(filterableItem, filterString)) {
                                r.add(filterableItem)
                            }
                        }
                    }
                }

                val s = sortItems(r)
                results.values = s
                results.count = s.count()
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?, results: FilterResults?,
            ) {
                suggestedList.clear()
                if (results != null && results.count > 0) {
                    suggestedList.addAll(results.values as ArrayList<Item>)
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    companion object {
        class ItemComparator : Comparator<Item> {
            fun compareNullable(o1: Item?, o2: Item?): Int {
                return if (o1 == null || o2 == null) {
                    -1
                } else {
                    compare(o1, o2)
                }
            }

            override fun compare(o1: Item, o2: Item): Int {
                return try {
                    val eanComp = o1.ean.compareTo(o2.ean, true)
                    val descriptionComp = o1.description.compareTo(o2.description, true)

                    // Orden natural: EAN, description,
                    when (eanComp) {
                        0 -> descriptionComp
                        else -> eanComp
                    }
                } catch (ex: Exception) {
                    0
                }
            }
        }

        fun sortItems(originalList: ArrayList<Item>): ArrayList<Item> {
            // Get all the parent groups
            return ArrayList(originalList.sortedWith(compareBy({ it.description }, { it.ean })))
        }

        fun isFilterable(filterableItem: Item, filterString: String): Boolean =
            filterableItem.ean.contains(
                filterString,
                true
            ) || filterableItem.itemCategoryId.toString()
                .contains(filterString) || filterableItem.description.contains(filterString, true)
    }

    init {
        setupColors()
    }
}
