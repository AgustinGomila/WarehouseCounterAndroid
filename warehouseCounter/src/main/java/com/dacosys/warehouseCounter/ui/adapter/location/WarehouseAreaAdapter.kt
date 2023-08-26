package com.dacosys.warehouseCounter.ui.adapter.location

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.ktor.v2.dto.location.Warehouse
import com.dacosys.warehouseCounter.ktor.v2.dto.location.WarehouseArea
import java.util.*

/**
 * Created by Agustin on 18/01/2017.
 */

@Suppress("SpellCheckingInspection")
class WarehouseAreaAdapter(
    private var activity: AppCompatActivity,
    private var resource: Int,
    private var filterWarehouse: Warehouse? = null,
    private var warehouseAreaArray: ArrayList<WarehouseArea>,
    private var suggestedList: ArrayList<WarehouseArea> = ArrayList()
) : ArrayAdapter<WarehouseArea>(WarehouseCounterApp.context, resource, suggestedList), Filterable {

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

    private fun getIndex(warehouseArea: WarehouseArea): Int {
        for (i in 0 until count) {
            val t = (getItem(i) as WarehouseArea)
            if (t == warehouseArea) {
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

    fun getAll(): ArrayList<WarehouseArea> {
        val r: ArrayList<WarehouseArea> = ArrayList()
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
    fun setChecked(warehouseAreas: ArrayList<WarehouseArea>, isChecked: Boolean) {
        if (isFilling) return
        isFilling = true

        for (i in warehouseAreas) {
            setChecked(i, isChecked)
        }

        isFilling = false
        refresh()
    }

    fun setChecked(
        warehouseArea: WarehouseArea,
        isChecked: Boolean,
        suspendRefresh: Boolean = false,
    ) {
        val position = getIndex(warehouseArea)
        if (isChecked) {
            if (!checkedIdArray.contains(warehouseArea.id)) {
                checkedIdArray.add(warehouseArea.id)
            }
        } else {
            checkedIdArray.remove(warehouseArea.id)
        }

        if (checkedChangedListener != null) {
            checkedChangedListener!!.onCheckedChanged(isChecked, position)
        }

        if (!suspendRefresh) {
            refresh()
        }
    }

    override fun sort(comparator: Comparator<in WarehouseArea>) {
        super.sort(customComparator)
    }

    private val customComparator = Comparator { o1: WarehouseArea?, o2: WarehouseArea? ->
        WarehouseAreaComparator().compareNullable(o1, o2)
    }

    fun refresh() {
        activity.runOnUiThread { notifyDataSetChanged() }
        dataSetChangedListener?.onDataSetChanged()
    }

    fun setChecked(checkedItems: ArrayList<WarehouseArea>) {
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

        val currentLayout: Int = R.layout.warehouse_area_row

        if (v == null || v.tag == null) {
            // El view todavía no fue creado, crearlo con el layout correspondiente.
            val vi = LayoutInflater.from(context)
            v = vi.inflate(currentLayout, parent, false)

            alreadyExists = false
        } else {
            // El view ya existe, comprobar que no necesite cambiar de layout.
            if (
            // Row null cambiando...
                v.tag is String && currentLayout == R.layout.warehouse_area_row) {
                // Ya fue creado, si es un row normal que está siendo seleccionada
                // o un row expandido que está siendo deseleccionado
                // debe cambiar de layout, por lo tanto, volver a crearse.
                val vi = LayoutInflater.from(context)
                v = vi.inflate(currentLayout, parent, false)

                alreadyExists = false
            }
        }

        fillSimpleView(position, v!!, alreadyExists)

        val h = if (v.height > 0) v.height else v.minimumHeight
        settingViewModel.locationViewHeight = h
        if (BuildConfig.DEBUG) Log.d(this::class.java.simpleName, "-------{RES: $resource Height:${h}}-------")

        return v
    }

    private fun createSimpleViewHolder(v: View, holder: SimpleViewHolder) {
        // Holder para los rows de dropdown.
        holder.checkBox = v.findViewById(R.id.checkBox)
        holder.areaTextView = v.findViewById(R.id.area)
        holder.warehouseTextView = v.findViewById(R.id.warehouse)

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
            val warehouseArea = getItem(position)

            if (warehouseArea != null) {
                holder.areaTextView?.text = warehouseArea.description
                holder.warehouseTextView?.text = warehouseArea.locationParentStr

                if (holder.checkBox != null) {
                    var isSpeakButtonLongPressed = false

                    val checkChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                        this.setChecked(warehouseArea, isChecked, true)
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
                    holder.checkBox?.isChecked = checkedIdArray.contains(warehouseArea.id)

                    holder.checkBox?.tag = position
                    holder.checkBox?.setOnLongClickListener(pressHoldListener)
                    holder.checkBox?.setOnTouchListener(pressTouchListener)
                    holder.checkBox?.setOnCheckedChangeListener(checkChangeListener)
                }

                // Background colors
                val white = ResourcesCompat.getColor(
                    WarehouseCounterApp.context.resources, R.color.text_light, null
                )

                // Font colors
                val black = ResourcesCompat.getColor(
                    WarehouseCounterApp.context.resources, R.color.text_dark, null
                )

                v.setBackgroundColor(white)
                holder.areaTextView?.setTextColor(black)
                holder.warehouseTextView?.setTextColor(black)
            }
        }

        return v
    }

    fun setFilterWarehouse(warehouse: Warehouse?) {
        filterWarehouse = warehouse
    }

    private val filterWarehouseId: Long?
        get() {
            return filterWarehouse?.id
        }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val r: ArrayList<WarehouseArea> = ArrayList()

                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    var filterableItem: WarehouseArea

                    for (i in 0 until warehouseAreaArray.size) {
                        filterableItem = warehouseAreaArray[i]
                        if (filterWarehouseId != null && filterWarehouseId != filterableItem.warehouseId) {
                            continue
                        }
                        if (filterableItem.description.lowercase(Locale.getDefault())
                                .contains(filterString) || (filterableItem.locationParentStr.lowercase(
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
                suggestedList.addAll(results?.values as ArrayList<WarehouseArea>)
                if (results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    companion object {
        class WarehouseAreaComparator : Comparator<WarehouseArea> {
            fun compareNullable(o1: WarehouseArea?, o2: WarehouseArea?): Int {
                return if (o1 == null || o2 == null) {
                    -1
                } else {
                    compare(o1, o2)
                }
            }

            override fun compare(o1: WarehouseArea, o2: WarehouseArea): Int {
                return try {
                    val nameComp = o1.description.compareTo(o2.description, true)
                    val warehouseDescriptionComp = o1.locationParentStr.compareTo(
                        o2.locationParentStr, true
                    )

                    // Orden natural: name, warehouseDescription, contactName
                    when (nameComp) {
                        0 -> warehouseDescriptionComp
                        else -> nameComp
                    }
                } catch (ex: Exception) {
                    0
                }
            }
        }
    }

    internal inner class SimpleViewHolder {
        var areaTextView: CheckedTextView? = null
        var warehouseTextView: CheckedTextView? = null
        var checkBox: CheckBox? = null
    }
}
