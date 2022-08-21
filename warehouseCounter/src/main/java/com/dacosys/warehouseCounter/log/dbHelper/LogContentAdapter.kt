package com.dacosys.warehouseCounter.log.dbHelper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.Statics.Companion.decimalPlaces
import com.dacosys.warehouseCounter.Statics.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.log.`object`.LogContent
import com.dacosys.warehouseCounter.misc.AutoResizeTextView
import java.util.*


/**
 * Created by Agustin on 18/01/2017.
 */

class LogContentAdapter : ArrayAdapter<LogContent>, Filterable {
    constructor(
        activity: AppCompatActivity,
        resource: Int,
        logContArray: ArrayList<LogContent>,
        listView: ListView?,
    ) : super(Statics.WarehouseCounter.getContext(), resource, logContArray) {
        this.activity = activity
        this.listView = listView
        this.resource = resource
        this.logContArray = logContArray

        this.listView = listView
        if (this.listView != null) {
            this.listView!!.setOnItemClickListener { _, _, position, _ ->
                selectItem(position)
            }
        }
    }

    fun refresh() {
        activity.runOnUiThread { notifyDataSetChanged() }
    }

    private fun getIndex(`object`: LogContent): Int {
        for (i in 0 until count) {
            val t = (getItem(i) as LogContent)
            if (t == `object`) {
                return i
            }
        }
        return -1
    }

    fun getAll(): ArrayList<LogContent> {
        val r: ArrayList<LogContent> = ArrayList()
        for (i in 0 until count) {
            r.add(getItem(i) as LogContent)
        }
        return r
    }

    fun setSelectItemAndScrollPos(a: LogContent?, tScrollPos: Int?) {
        var pos = -1
        if (a != null) pos = getPosition(a)
        var scrollPos = -1
        if (tScrollPos != null) scrollPos = tScrollPos
        selectItem(pos, scrollPos, false)
    }

    fun selectItem(a: LogContent?) {
        var pos = -1
        if (a != null) pos = getPosition(a)
        selectItem(pos)
    }

    fun selectItem(pos: Int) {
        selectItem(pos, pos, true)
    }

    private fun selectItem(pos: Int, scrollPos: Int, smoothScroll: Boolean) {
        if (listView == null) {
            return
        }

        (listView ?: return).clearChoices()

        // Deseleccionar cuando:
        //   - Estaba previamente seleccionado
        //   - La posición es negativa
        //   - La cantidad de ítems es cero o menos

        activity.runOnUiThread {
            if (pos < 0 && lastSelectedPos < 0 && count > 0) {
                (listView ?: return@runOnUiThread).setItemChecked(0, true)
                (listView ?: return@runOnUiThread).setSelection(0)
            } else if (pos == lastSelectedPos || pos < 0 || count <= 0) {
                (listView ?: return@runOnUiThread).setItemChecked(-1, true)
                (listView ?: return@runOnUiThread).setSelection(-1)
            } else {
                (listView ?: return@runOnUiThread).setItemChecked(pos, true)
                (listView ?: return@runOnUiThread).setSelection(pos)
            }
        }

        lastSelectedPos = currentPos()

        activity.runOnUiThread {
            if (smoothScroll) {
                notifyDataSetChanged()
                (listView ?: return@runOnUiThread).smoothScrollToPosition(scrollPos)
            } else {
                refresh()
                (listView ?: return@runOnUiThread).setSelection(scrollPos)
            }
        }
    }

    fun currentLogCont(): LogContent? {
        return (0 until count)
            .firstOrNull { isSelected(it) }
            ?.let { getItem(it) }
    }

    fun currentPos(): Int {
        return (0 until count)
            .firstOrNull { isSelected(it) } ?: -1
    }

    fun firstVisiblePos(): Int {
        var pos = (listView ?: return -1).firstVisiblePosition
        if ((listView ?: return -1).childCount > 1 && (listView
                ?: return -1).getChildAt(0).top < 0
        ) pos++
        return pos
    }

    private fun isSelected(position: Int): Boolean {
        return position >= 0 && listView!!.isItemChecked(position)
    }

    var listView: ListView? = null
        set(newValue) {
            field = newValue
            activity.runOnUiThread {
                if (field != null) {
                    field!!.adapter = this
                }
            }
        }

    private var lastSelectedPos = -1
    private var activity: AppCompatActivity
    private var resource: Int = 0
    private var logContArray: ArrayList<LogContent> = ArrayList()
    private var suggestedList: ArrayList<LogContent> = ArrayList()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        var alreadyExists = true

        val currentLayout = resource
        if (v == null || v.tag == null) {
            // El view todavía no fue creado, crearlo con el layout correspondiente.
            val vi = LayoutInflater.from(context)
            v = vi.inflate(currentLayout, parent, false)

            alreadyExists = false
        }

        v = fillListView(position, v!!, alreadyExists)
        return v
    }

    private fun fillListView(position: Int, v: View, alreadyExists: Boolean): View {
        var holder = ViewHolder()
        if (alreadyExists) {
            holder = v.tag as ViewHolder
        } else {
            holder.descriptionAutoSizeTextView = v.findViewById(R.id.descriptionAutoResizeTextView)
            holder.eanAutoSizeTextView = v.findViewById(R.id.eanAutoResizeTextView)
            holder.variationQtyTextView = v.findViewById(R.id.variationQtyTextView)
            holder.finalQtyTextView = v.findViewById(R.id.finalQtyTextView)

            v.tag = holder
        }

        if (position >= 0) {
            val logContent = getItem(position)

            if (logContent != null) {
                holder.descriptionAutoSizeTextView?.text = logContent.itemStr
                holder.eanAutoSizeTextView?.text = logContent.itemCode
                holder.variationQtyTextView?.text =
                    Statics.roundToString(
                        logContent.variationQty
                            ?: 0.toDouble(), decimalPlaces
                    )
                holder.finalQtyTextView?.text =
                    Statics.roundToString(
                        logContent.finalQty
                            ?: 0.toDouble(), decimalPlaces
                    )

                // Background layouts
                val layoutRed =
                    ResourcesCompat.getDrawable(
                        Statics.WarehouseCounter.getContext().resources,
                        R.drawable.layout_thin_border_red,
                        null
                    )
                val layoutDefault = ResourcesCompat.getDrawable(
                    Statics.WarehouseCounter.getContext().resources,
                    R.drawable.layout_thin_border,
                    null
                )

                // Font colors
                val black = ResourcesCompat.getColor(
                    Statics.WarehouseCounter.getContext().resources,
                    R.color.black,
                    null
                )
                val whitesmoke = ResourcesCompat.getColor(
                    Statics.WarehouseCounter.getContext().resources,
                    R.color.whitesmoke,
                    null
                )

                var backColor = layoutDefault!!
                var foreColor = black

                if ((logContent.finalQty ?: 0.toDouble()) <= 0.toDouble()) {
                    backColor = layoutRed!!
                    foreColor = whitesmoke
                }

                v.background = backColor
                holder.descriptionAutoSizeTextView?.setTextColor(foreColor)
                holder.eanAutoSizeTextView?.setTextColor(foreColor)
                holder.variationQtyTextView?.setTextColor(foreColor)
                holder.finalQtyTextView?.setTextColor(foreColor)

                if (listView != null) {
                    if (listView!!.isItemChecked(position)) {
                        v.background.colorFilter =
                            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                                getColorWithAlpha(colorId = R.color.lightslategray, alpha = 240),
                                BlendModeCompat.MODULATE
                            )
                    } else {
                        v.background?.colorFilter = null
                    }
                }
            }
        }

        return v
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val r: ArrayList<LogContent> = ArrayList()

                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.ROOT)
                    var filterableLogContent: LogContent

                    for (i in 0 until logContArray.size) {
                        filterableLogContent = logContArray[i]
                        if (filterableLogContent.itemCode.lowercase(Locale.ROOT)
                                .contains(filterString) ||
                            filterableLogContent.itemStr.lowercase(Locale.ROOT).contains(
                                filterString
                            )
                        ) {
                            r.add(filterableLogContent)
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
                suggestedList.addAll(results?.values as ArrayList<LogContent>)
                if (results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    internal inner class ViewHolder {
        var descriptionAutoSizeTextView: AutoResizeTextView? = null
        var eanAutoSizeTextView: AutoResizeTextView? = null
        var variationQtyTextView: CheckedTextView? = null
        var finalQtyTextView: CheckedTextView? = null
    }
}