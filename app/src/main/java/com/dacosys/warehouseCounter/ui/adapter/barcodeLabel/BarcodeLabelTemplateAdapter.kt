package com.dacosys.warehouseCounter.ui.adapter.barcodeLabel

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
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelTemplate
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelType
import java.util.*


class BarcodeLabelTemplateAdapter(
    private var activity: AppCompatActivity,
    private var resource: Int,
    private var templateArray: ArrayList<BarcodeLabelTemplate>,
    private var suggestedList: ArrayList<BarcodeLabelTemplate> = arrayListOf()
) : ArrayAdapter<BarcodeLabelTemplate>(context, resource, suggestedList), Filterable {

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

    private fun getIndex(barcodeLabelTemplate: BarcodeLabelTemplate): Int {
        for (i in 0 until count) {
            val t = (getItem(i) as BarcodeLabelTemplate)
            if (t == barcodeLabelTemplate) {
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
                r.add(it.templateId)
            }
        }
        return r
    }

    fun getAll(): ArrayList<BarcodeLabelTemplate> {
        val r: ArrayList<BarcodeLabelTemplate> = ArrayList()
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
    fun setChecked(barcodeLabelTemplates: ArrayList<BarcodeLabelTemplate>, isChecked: Boolean) {
        if (isFilling) return
        isFilling = true

        for (i in barcodeLabelTemplates) {
            setChecked(i, isChecked)
        }

        isFilling = false
        refresh()
    }

    fun setChecked(
        barcodeLabelTemplate: BarcodeLabelTemplate,
        isChecked: Boolean,
        suspendRefresh: Boolean = false,
    ) {
        val position = getIndex(barcodeLabelTemplate)
        if (isChecked) {
            if (!checkedIdArray.contains(barcodeLabelTemplate.templateId)) {
                checkedIdArray.add(barcodeLabelTemplate.templateId)
            }
        } else {
            checkedIdArray.remove(barcodeLabelTemplate.templateId)
        }

        if (checkedChangedListener != null) {
            checkedChangedListener!!.onCheckedChanged(isChecked, position)
        }

        if (!suspendRefresh) {
            refresh()
        }
    }

    override fun sort(comparator: Comparator<in BarcodeLabelTemplate>) {
        super.sort(customComparator)
    }

    private val customComparator = Comparator { o1: BarcodeLabelTemplate?, o2: BarcodeLabelTemplate? ->
        BarcodeLabelTemplateComparator().compareNullable(o1, o2)
    }

    fun refresh() {
        activity.runOnUiThread { notifyDataSetChanged() }
        dataSetChangedListener?.onDataSetChanged()
    }

    fun setChecked(checkedItems: ArrayList<BarcodeLabelTemplate>) {
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

        val currentLayout: Int = R.layout.template_row

        if (v == null || v.tag == null) {
            // El view todavía no fue creado, crearlo con el layout correspondiente.
            val vi = LayoutInflater.from(context)
            v = vi.inflate(currentLayout, parent, false)
            alreadyExists = false
        }

        fillSimpleView(position, v!!, alreadyExists)

        val h = if (v.height > 0) v.height else v.minimumHeight
        settingsVm.templateViewHeight = h
        if (BuildConfig.DEBUG) Log.d(this::class.java.simpleName, "-------{RES: $resource Height:${h}}-------")

        return v
    }

    private fun createSimpleViewHolder(v: View, holder: SimpleViewHolder) {
        // Holder para los rows de dropdown.
        holder.checkBox = v.findViewById(R.id.checkBox)
        holder.descriptionTextView = v.findViewById(R.id.templateStr)
        holder.templateTypeTextView = v.findViewById(R.id.templateType)

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
            val barcodeLabelTemplate = getItem(position)

            if (barcodeLabelTemplate != null) {
                holder.descriptionTextView?.text = barcodeLabelTemplate.description
                holder.templateTypeTextView?.text =
                    BarcodeLabelType.getById(barcodeLabelTemplate.barcodeLabelTypeId).description
                if (holder.checkBox != null) {
                    var isSpeakButtonLongPressed = false

                    val checkChangeListener =
                        CompoundButton.OnCheckedChangeListener { _, isChecked ->
                            this.setChecked(barcodeLabelTemplate, isChecked, true)
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
                        checkedIdArray.contains(barcodeLabelTemplate.templateId)

                    holder.checkBox?.tag = position
                    holder.checkBox?.setOnLongClickListener(pressHoldListener)
                    holder.checkBox?.setOnTouchListener(pressTouchListener)
                    holder.checkBox?.setOnCheckedChangeListener(checkChangeListener)
                }

                // Background colors
                val lightgray = ResourcesCompat.getColor(context.resources, R.color.lightgray, null)
                val white = ResourcesCompat.getColor(context.resources, R.color.text_light, null)

                // Font colors
                val black = ResourcesCompat.getColor(context.resources, R.color.text_dark, null)
                val dimgray = ResourcesCompat.getColor(context.resources, R.color.dimgray, null)

                val colorText = when {
                    barcodeLabelTemplate.active != 1 -> dimgray
                    else -> black
                }

                val backColor = when {
                    barcodeLabelTemplate.active != 1 -> lightgray
                    else -> white
                }

                v.setBackgroundColor(backColor)
                holder.descriptionTextView?.setTextColor(colorText)
                holder.templateTypeTextView?.setTextColor(colorText)
            }
        }

        return v
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val r: ArrayList<BarcodeLabelTemplate> = ArrayList()

                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    var filterableItem: BarcodeLabelTemplate

                    for (i in 0 until templateArray.size) {
                        filterableItem = templateArray[i]
                        if (filterableItem.description.lowercase(Locale.getDefault()).contains(filterString)) {
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
                suggestedList.addAll(results?.values as ArrayList<BarcodeLabelTemplate>)
                if (results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    companion object {
        class BarcodeLabelTemplateComparator : Comparator<BarcodeLabelTemplate> {
            fun compareNullable(o1: BarcodeLabelTemplate?, o2: BarcodeLabelTemplate?): Int {
                return if (o1 == null || o2 == null) {
                    -1
                } else {
                    compare(o1, o2)
                }
            }

            override fun compare(o1: BarcodeLabelTemplate, o2: BarcodeLabelTemplate): Int {
                return try {
                    o1.description.compareTo(o2.description, true)
                } catch (ex: Exception) {
                    0
                }
            }
        }
    }

    internal inner class SimpleViewHolder {
        var descriptionTextView: CheckedTextView? = null
        var templateTypeTextView: CheckedTextView? = null
        var checkBox: CheckBox? = null
    }
}
