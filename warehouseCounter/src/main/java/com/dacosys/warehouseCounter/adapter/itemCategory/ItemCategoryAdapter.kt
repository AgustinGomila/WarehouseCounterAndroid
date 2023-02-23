package com.dacosys.warehouseCounter.adapter.itemCategory

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.room.entity.itemCategory.ItemCategory
import java.util.*


/**
 * Created by Agustin on 18/01/2017.
 */

@Suppress("SpellCheckingInspection")
class ItemCategoryAdapter : ArrayAdapter<ItemCategory>, Filterable {
    private var activity: AppCompatActivity
    private var resource: Int = 0

    private var dataSetChangedListener: DataSetChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null

    private var multiSelect: Boolean = false

    private var itemCategoryArray: ArrayList<ItemCategory> = ArrayList()
    private var suggestedList: ArrayList<ItemCategory> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()

    constructor(
        activity: AppCompatActivity,
        resource: Int,
        itemCategorys: ArrayList<ItemCategory>,
        suggestedList: ArrayList<ItemCategory>,
    ) : super(context(), resource, suggestedList) {
        this.activity = activity
        this.resource = resource
        this.itemCategoryArray = itemCategorys
        this.suggestedList = suggestedList
    }

    fun refreshListeners(
        checkedChangedListener: CheckedChangedListener?,
        dataSetChangedListener: DataSetChangedListener?,
    ) {
        this.checkedChangedListener = checkedChangedListener
        this.dataSetChangedListener = dataSetChangedListener
    }

    interface DataSetChangedListener {
        // Define data you like to return from AysncTask
        fun onDataSetChanged()
    }

    interface CheckedChangedListener {
        // Define data you like to return from AysncTask
        fun onCheckedChanged(
            isChecked: Boolean,
            pos: Int,
        )
    }

    override fun clear() {
        activity.runOnUiThread {
            super.clear()
            clearChecked()
        }
    }

    private fun getIndex(itemCategory: ItemCategory): Int {
        for (i in 0 until count) {
            val t = (getItem(i) as ItemCategory)
            if (t == itemCategory) {
                return i
            }
        }
        return -1
    }

    fun count(): Int {
        return count
    }

    fun getAllId(): ArrayList<Long> {
        val r: ArrayList<Long> = ArrayList()
        for (i in 0 until count) {
            val it = getItem(i)
            if (it != null) {
                r.add(it.itemCategoryId)
            }
        }
        return r
    }

    fun getAll(): ArrayList<ItemCategory> {
        val r: ArrayList<ItemCategory> = ArrayList()
        for (i in 0 until count) {
            val t = getItem(i) ?: continue
            r.add(t)
        }
        return r
    }

    fun countChecked(): Int {
        return checkedIdArray.count()
    }

    fun getAllChecked(): ArrayList<Long> {
        return checkedIdArray
    }

    private var isFilling = false
    fun setChecked(itemCategorys: ArrayList<ItemCategory>, isChecked: Boolean) {
        if (isFilling) return
        isFilling = true

        for (i in itemCategorys) {
            setChecked(i, isChecked)
        }

        isFilling = false
        refresh()
    }

    fun setChecked(
        itemCategory: ItemCategory,
        isChecked: Boolean,
        suspendRefresh: Boolean = false,
    ) {
        val position = getIndex(itemCategory)
        if (isChecked) {
            if (!checkedIdArray.contains(itemCategory.itemCategoryId)) {
                checkedIdArray.add(itemCategory.itemCategoryId)
            }
        } else {
            checkedIdArray.remove(itemCategory.itemCategoryId)
        }

        if (checkedChangedListener != null) {
            checkedChangedListener!!.onCheckedChanged(isChecked, position)
        }

        if (!suspendRefresh) {
            refresh()
        }
    }

    override fun sort(comparator: Comparator<in ItemCategory>) {
        super.sort(customComparator)
    }

    private val customComparator = Comparator { o1: ItemCategory?, o2: ItemCategory? ->
        ItemCategoryComparator().compareNullable(o1, o2)
    }

    fun refresh() {
        activity.runOnUiThread { notifyDataSetChanged() }
        dataSetChangedListener?.onDataSetChanged()
    }

    fun setChecked(checkedItems: ArrayList<ItemCategory>) {
        checkedItems.clear()
        setChecked(checkedItems, true)
    }

    fun clearChecked() {
        checkedIdArray.clear()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        var alreadyExists = true

        // Seleccionamos el layout dependiendo si es
        // un row visible u oculto según su AsseStatus.

        val currentLayout: Int = R.layout.item_category_row

        if (v == null || v.tag == null) {
            // El view todavía no fue creado, crearlo con el layout correspondiente.
            val vi = LayoutInflater.from(context)
            v = vi.inflate(currentLayout, parent, false)

            alreadyExists = false
        } else {
            // El view ya existe, comprobar que no necesite cambiar de layout.
            if (
            // Row null cambiando...
                v.tag is String && currentLayout == R.layout.item_category_row) {
                // Ya fue creado, si es un row normal que está siendo seleccionada
                // o un row expandido que está siendo deseleccionado
                // debe cambiar de layout, por lo tanto volver a crearse.
                val vi = LayoutInflater.from(context)
                v = vi.inflate(currentLayout, parent, false)

                alreadyExists = false
            }
        }

        fillSimpleView(position, v!!, alreadyExists)
        return v
    }

    private fun createSimpleViewHolder(v: View, holder: SimpleViewHolder) {
        // Holder para los rows de dropdown.
        holder.checkBox = v.findViewById(R.id.checkBox)
        holder.descriptionTextView = v.findViewById(R.id.itemCategoryStr)
        holder.parentCategoryTextView = v.findViewById(R.id.parentStr)

        if (multiSelect) {
            holder.checkBox?.visibility = VISIBLE
        } else {
            holder.checkBox?.visibility = GONE
        }

        v.tag = holder
    }

    @SuppressLint("ClickableViewAccessibility", "ObsoleteSdkInt")
    private fun fillSimpleView(position: Int, v: View, alreadyExists: Boolean): View {
        var holder = SimpleViewHolder()
        if (alreadyExists) {
            if (v.tag is SimpleViewHolder || v.tag is String) {
                createSimpleViewHolder(v, holder)
            } else {
                holder = v.tag as SimpleViewHolder
            }
        } else {
            createSimpleViewHolder(v, holder)
        }

        if (position >= 0) {
            val itemCategory = getItem(position)

            if (itemCategory != null) {
                holder.descriptionTextView?.text = itemCategory.description
                holder.parentCategoryTextView?.text = itemCategory.parentStr
                if (holder.checkBox != null) {
                    var isSpeakButtonLongPressed = false

                    val checkChangeListener =
                        CompoundButton.OnCheckedChangeListener { _, isChecked ->
                            this.setChecked(itemCategory, isChecked, true)
                        }

                    val pressHoldListener =
                        OnLongClickListener { // Do something when your hold starts here.
                            isSpeakButtonLongPressed = true
                            true
                        }

                    val pressTouchListener = OnTouchListener { pView, pEvent ->
                        pView.onTouchEvent(pEvent)
                        // We're only interested in when the button is released.
                        if (pEvent.action == MotionEvent.ACTION_UP) {
                            // We're only interested in anything if our speak button is currently pressed.
                            if (isSpeakButtonLongPressed) {
                                // Do something when the button is released.
                                if (!isFilling) {
                                    holder.checkBox?.setOnCheckedChangeListener(null)
                                    val newState = !(holder.checkBox?.isChecked ?: false)
                                    this.setChecked(getAll(), newState)
                                }
                                isSpeakButtonLongPressed = false
                            }
                        }
                        return@OnTouchListener true
                    }

                    //Important to remove previous checkedChangedListener before calling setChecked
                    holder.checkBox?.setOnCheckedChangeListener(null)
                    holder.checkBox?.isChecked =
                        checkedIdArray.contains(itemCategory.itemCategoryId)

                    holder.checkBox?.tag = position
                    holder.checkBox?.setOnLongClickListener(pressHoldListener)
                    holder.checkBox?.setOnTouchListener(pressTouchListener)
                    holder.checkBox?.setOnCheckedChangeListener(checkChangeListener)
                }

                // Background colors
                val lightgray =
                    ResourcesCompat.getColor(context().resources, R.color.lightgray, null)
                val white = ResourcesCompat.getColor(context().resources, R.color.text_light, null)

                // Font colors
                val black = ResourcesCompat.getColor(context().resources, R.color.text_dark, null)
                val dimgray = ResourcesCompat.getColor(context().resources, R.color.dimgray, null)

                val colorText = when {
                    itemCategory.active != 1 -> dimgray
                    else -> black
                }

                val backColor = when {
                    itemCategory.active != 1 -> lightgray
                    else -> white
                }

                v.setBackgroundColor(backColor)
                holder.descriptionTextView?.setTextColor(colorText)
                holder.parentCategoryTextView?.setTextColor(colorText)
            }
        }

        if (v.height > 0) {
            viewHeight = v.height
            Log.d(this::class.java.simpleName, "-------{RES: $resource Height:${v.height}}-------")
        }
        return v
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val r: ArrayList<ItemCategory> = ArrayList()

                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    var filterableItem: ItemCategory

                    for (i in 0 until itemCategoryArray.size) {
                        filterableItem = itemCategoryArray[i]
                        if (filterableItem.description.lowercase(Locale.getDefault())
                                .contains(filterString) || (filterableItem.parentId > 0 && filterableItem.parentStr.lowercase(
                                Locale.getDefault()
                            ).contains(filterString))
                        ) {
                            r.add(filterableItem)
                        }
                    }
                }

                results.values = r
                results.count = r.count()
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?, results: FilterResults?,
            ) {
                suggestedList.clear()
                suggestedList.addAll(results?.values as ArrayList<ItemCategory>)
                if (results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    companion object {

        var viewHeight = if (Statics.isTablet()) 186 else 107

        class ItemCategoryComparator : Comparator<ItemCategory> {
            fun compareNullable(o1: ItemCategory?, o2: ItemCategory?): Int {
                return if (o1 == null || o2 == null) {
                    -1
                } else {
                    compare(o1, o2)
                }
            }

            override fun compare(o1: ItemCategory, o2: ItemCategory): Int {
                return try {
                    val descComp = o1.description.compareTo(o2.description, true)
                    val parentComp = o1.parentStr.compareTo(o2.parentStr, true)

                    // Orden natural: name, taxNumber, contactName
                    when (descComp) {
                        0 -> parentComp
                        else -> descComp
                    }
                } catch (ex: Exception) {
                    0
                }
            }
        }
    }

    internal inner class SimpleViewHolder {
        var descriptionTextView: CheckedTextView? = null
        var parentCategoryTextView: CheckedTextView? = null
        var checkBox: CheckBox? = null
    }
}