package com.example.warehouseCounter.ui.adapter.location

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CheckedTextView
import android.widget.CompoundButton
import android.widget.Filter
import android.widget.Filterable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.example.warehouseCounter.BuildConfig
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.data.ktor.v2.dto.location.Warehouse
import java.util.*

class WarehouseAdapter(
    private var activity: AppCompatActivity,
    private var resource: Int,
    private var warehouseArray: ArrayList<Warehouse>,
    private var suggestedList: ArrayList<Warehouse> = ArrayList(),
) : ArrayAdapter<Warehouse>(WarehouseCounterApp.context, resource, suggestedList), Filterable {

    private var dataSetChangedListener: DataSetChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null
    private var multiSelect: Boolean = false
    private var checkedIdArray: ArrayList<Long> = ArrayList()

    fun refreshListeners(
        checkedChangedListener: CheckedChangedListener?,
        dataSetChangedListener: DataSetChangedListener?,
    ) {
        this.checkedChangedListener = checkedChangedListener
        this.dataSetChangedListener = dataSetChangedListener
    }

    interface DataSetChangedListener {
        fun onDataSetChanged()
    }

    interface CheckedChangedListener {
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

    private fun getIndex(warehouse: Warehouse): Int {
        for (i in 0 until count) {
            val t = (getItem(i) as Warehouse)
            if (t == warehouse) {
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
                r.add(it.id)
            }
        }
        return r
    }

    fun getAll(): ArrayList<Warehouse> {
        val r: ArrayList<Warehouse> = ArrayList()
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
    fun setChecked(warehouses: ArrayList<Warehouse>, isChecked: Boolean) {
        if (isFilling) return
        isFilling = true

        for (i in warehouses) {
            setChecked(i, isChecked)
        }

        isFilling = false
        refresh()
    }

    fun setChecked(
        warehouse: Warehouse,
        isChecked: Boolean,
        suspendRefresh: Boolean = false,
    ) {
        val position = getIndex(warehouse)
        if (isChecked) {
            if (!checkedIdArray.contains(warehouse.id)) {
                checkedIdArray.add(warehouse.id)
            }
        } else {
            checkedIdArray.remove(warehouse.id)
        }

        if (checkedChangedListener != null) {
            checkedChangedListener!!.onCheckedChanged(isChecked, position)
        }

        if (!suspendRefresh) {
            refresh()
        }
    }

    override fun sort(comparator: Comparator<in Warehouse>) {
        super.sort(customComparator)
    }

    private val customComparator = Comparator { o1: Warehouse?, o2: Warehouse? ->
        WarehouseComparator().compareNullable(o1, o2)
    }

    fun refresh() {
        activity.runOnUiThread { notifyDataSetChanged() }
        dataSetChangedListener?.onDataSetChanged()
    }

    fun setChecked(checkedItems: ArrayList<Warehouse>) {
        checkedItems.clear()
        setChecked(checkedItems, true)
    }

    private fun clearChecked() {
        checkedIdArray.clear()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        var alreadyExists = true

        // Seleccionamos el layout dependiendo si es
        // un row visible u oculto según su AsseStatus.

        val currentLayout: Int = R.layout.warehouse_row

        if (v == null || v.tag == null) {
            // El view todavía no fue creado, crearlo con el layout correspondiente.
            val vi = LayoutInflater.from(context)
            v = vi.inflate(currentLayout, parent, false)
            alreadyExists = false
        }

        fillSimpleView(position, v!!, alreadyExists)

        val h = if (v.height > 0) v.height else v.minimumHeight
        settingsVm.locationViewHeight = h
        if (BuildConfig.DEBUG) Log.d(this::class.java.simpleName, "-------{RES: $resource Height:${h}}-------")

        return v
    }

    private fun createSimpleViewHolder(v: View, holder: SimpleViewHolder) {
        // Holder para los rows de dropdown.
        holder.checkBox = v.findViewById(R.id.checkBox)
        holder.warehouseTextView = v.findViewById(R.id.warehouse)
        holder.extIdTextView = v.findViewById(R.id.extId)

        if (multiSelect) {
            holder.checkBox?.visibility = View.VISIBLE
        } else {
            holder.checkBox?.visibility = View.GONE
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
            val warehouse = getItem(position)

            if (warehouse != null) {
                holder.warehouseTextView?.text = warehouse.description
                holder.extIdTextView?.text = warehouse.externalId

                if (holder.checkBox != null) {
                    var isSpeakButtonLongPressed = false

                    val checkChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                        this.setChecked(warehouse, isChecked, true)
                    }

                    val pressHoldListener = View.OnLongClickListener { // Do something when your hold starts here.
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
                    holder.checkBox?.isChecked = checkedIdArray.contains(warehouse.id)

                    holder.checkBox?.tag = position
                    holder.checkBox?.setOnLongClickListener(pressHoldListener)
                    holder.checkBox?.setOnTouchListener(pressTouchListener)
                    holder.checkBox?.setOnCheckedChangeListener(checkChangeListener)
                }

                // Background colors
                val white = ResourcesCompat.getColor(context.resources, R.color.text_light, null)

                // Font colors
                val black = ResourcesCompat.getColor(context.resources, R.color.text_dark, null)

                v.setBackgroundColor(white)
                holder.warehouseTextView?.setTextColor(black)
                holder.extIdTextView?.setTextColor(black)
            }
        }

        return v
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val r: ArrayList<Warehouse> = ArrayList()

                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    var filterableItem: Warehouse

                    for (i in 0 until warehouseArray.size) {
                        filterableItem = warehouseArray[i]
                        if (filterableItem.description.lowercase(Locale.getDefault())
                                .contains(filterString) || (filterableItem.externalId.lowercase(
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
                suggestedList.addAll(results?.values as ArrayList<Warehouse>)
                if (results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    companion object {
        class WarehouseComparator : Comparator<Warehouse> {
            fun compareNullable(o1: Warehouse?, o2: Warehouse?): Int {
                return if (o1 == null || o2 == null) {
                    -1
                } else {
                    compare(o1, o2)
                }
            }

            override fun compare(o1: Warehouse, o2: Warehouse): Int {
                return try {
                    val descComp = o1.description.compareTo(o2.description, true)
                    val extIdComp = o1.externalId.compareTo(
                        o2.externalId, true
                    )

                    // Orden natural: code, externalId
                    when (descComp) {
                        0 -> extIdComp
                        else -> descComp
                    }
                } catch (ex: Exception) {
                    0
                }
            }
        }
    }

    internal inner class SimpleViewHolder {
        var warehouseTextView: CheckedTextView? = null
        var extIdTextView: CheckedTextView? = null
        var checkBox: CheckBox? = null
    }
}
