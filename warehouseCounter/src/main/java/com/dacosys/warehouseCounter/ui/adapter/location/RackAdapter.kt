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
import com.dacosys.warehouseCounter.ktor.v2.dto.location.Rack
import com.dacosys.warehouseCounter.ktor.v2.dto.location.WarehouseArea
import java.util.*

/**
 * Created by Agustin on 18/01/2017.
 */

@Suppress("SpellCheckingInspection")
class RackAdapter(
    private var activity: AppCompatActivity,
    private var resource: Int,
    private var filterWarehouseArea: WarehouseArea? = null,
    private var rackArray: ArrayList<Rack>,
    private var suggestedList: ArrayList<Rack> = ArrayList()
) : ArrayAdapter<Rack>(WarehouseCounterApp.context, resource, suggestedList), Filterable {

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

    private fun getIndex(rack: Rack): Int {
        for (i in 0 until count) {
            val t = (getItem(i) as Rack)
            if (t == rack) {
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

    fun getAll(): ArrayList<Rack> {
        val r: ArrayList<Rack> = ArrayList()
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
    fun setChecked(racks: ArrayList<Rack>, isChecked: Boolean) {
        if (isFilling) return
        isFilling = true

        for (i in racks) {
            setChecked(i, isChecked)
        }

        isFilling = false
        refresh()
    }

    fun setChecked(
        rack: Rack,
        isChecked: Boolean,
        suspendRefresh: Boolean = false,
    ) {
        val position = getIndex(rack)
        if (isChecked) {
            if (!checkedIdArray.contains(rack.id)) {
                checkedIdArray.add(rack.id)
            }
        } else {
            checkedIdArray.remove(rack.id)
        }

        if (checkedChangedListener != null) {
            checkedChangedListener!!.onCheckedChanged(isChecked, position)
        }

        if (!suspendRefresh) {
            refresh()
        }
    }

    override fun sort(comparator: Comparator<in Rack>) {
        super.sort(customComparator)
    }

    private val customComparator = Comparator { o1: Rack?, o2: Rack? ->
        RackComparator().compareNullable(o1, o2)
    }

    fun refresh() {
        activity.runOnUiThread { notifyDataSetChanged() }
        dataSetChangedListener?.onDataSetChanged()
    }

    fun setChecked(checkedItems: ArrayList<Rack>) {
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

        val currentLayout: Int = R.layout.rack_row

        if (v == null || v.tag == null) {
            // El view todavía no fue creado, crearlo con el layout correspondiente.
            val vi = LayoutInflater.from(context)
            v = vi.inflate(currentLayout, parent, false)

            alreadyExists = false
        } else {
            // El view ya existe, comprobar que no necesite cambiar de layout.
            if (
            // Row null cambiando...
                v.tag is String && currentLayout == R.layout.rack_row) {
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
        holder.rackTextView = v.findViewById(R.id.rack)
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
            val rack = getItem(position)

            if (rack != null) {
                holder.rackTextView?.text = rack.code
                holder.extIdTextView?.text = rack.extId

                if (holder.checkBox != null) {
                    var isSpeakButtonLongPressed = false

                    val checkChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                        this.setChecked(rack, isChecked, true)
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
                    holder.checkBox?.isChecked = checkedIdArray.contains(rack.id)

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
                holder.rackTextView?.setTextColor(black)
                holder.extIdTextView?.setTextColor(black)
            }
        }

        return v
    }

    fun setFilterWarehouseArea(area: WarehouseArea?) {
        filterWarehouseArea = area
    }

    private val filterWarehouseAreaId: Long?
        get() {
            return filterWarehouseArea?.id
        }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val r: ArrayList<Rack> = ArrayList()

                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    var filterableItem: Rack

                    for (i in 0 until rackArray.size) {
                        filterableItem = rackArray[i]
                        if (filterWarehouseAreaId != null && filterWarehouseAreaId != filterableItem.warehouseAreaId) {
                            continue
                        }
                        if (filterableItem.code.lowercase(Locale.getDefault())
                                .contains(filterString) || (filterableItem.extId.lowercase(
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
                suggestedList.addAll(results?.values as ArrayList<Rack>)
                if (results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    companion object {
        class RackComparator : Comparator<Rack> {
            fun compareNullable(o1: Rack?, o2: Rack?): Int {
                return if (o1 == null || o2 == null) {
                    -1
                } else {
                    compare(o1, o2)
                }
            }

            override fun compare(o1: Rack, o2: Rack): Int {
                return try {
                    val codeComp = o1.code.compareTo(o2.code, true)
                    val extIdComp = o1.extId.compareTo(
                        o2.extId, true
                    )

                    // Orden natural: code, extId
                    when (codeComp) {
                        0 -> extIdComp
                        else -> codeComp
                    }
                } catch (ex: Exception) {
                    0
                }
            }
        }
    }

    internal inner class SimpleViewHolder {
        var rackTextView: CheckedTextView? = null
        var extIdTextView: CheckedTextView? = null
        var checkBox: CheckBox? = null
    }
}
