package com.example.warehouseCounter.ui.adapter.generic

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.Filter
import android.widget.Filterable
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp
import java.util.*

// Adaptador Genérico
class GenericDropDownAdapter<T : HasUUID> private constructor(
    private val resource: Int,
    private val itemList: List<T>,
    private val suggestedList: ArrayList<T> = ArrayList(),
    private val multiSelect: Boolean = false,
    private val visibleStatus: ArrayList<GenericStatus> = ArrayList(),
    private val checkedChangedListener: CheckedChangedListener? = null,
    private val bindView: (View, T, Int) -> Unit,
    private val getStatus: (T) -> GenericStatus?,
    private val styleView: (View, GenericStatus?) -> Unit,
    private val filterPredicate: (T, String) -> Boolean,
    private val comparator: Comparator<T>? = null,
    private var filterParentUUID: UUID? = null,
    private val onReady: (() -> Unit)? = null
) : ArrayAdapter<T>(WarehouseCounterApp.context, resource, suggestedList), Filterable {

    interface CheckedChangedListener {
        fun onCheckedChanged(isChecked: Boolean, pos: Int)
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        onReady?.invoke()
    }

    private var checkedUuidArray: ArrayList<UUID> = ArrayList()
    private var isFilling = false

    @Suppress("unused")
    fun setFilterParentUuid(uUID: UUID?) {
        filterParentUUID = uUID
    }

    // Métodos para manejar estados visibles
    @Suppress("unused")
    fun getVisibleStatus(): ArrayList<GenericStatus> = visibleStatus

    fun addVisibleStatus(status: GenericStatus) {
        if (!visibleStatus.contains(status)) {
            visibleStatus.add(status)
        }
    }

    fun removeVisibleStatus(status: GenericStatus) {
        visibleStatus.remove(status)
    }

    override fun clear() {
        super.clear()
        checkedUuidArray.clear()
    }

    override fun remove(item: T?) {
        item?.let {
            checkedUuidArray.remove(it.uuid)
            super.remove(it)
        }
    }

    fun getAll(): ArrayList<T> = ArrayList<T>().apply {
        for (i in 0 until count) {
            getItem(i)?.let { add(it) }
        }
    }

    fun countChecked(): Int = checkedUuidArray.size

    fun getAllChecked(): ArrayList<UUID> = ArrayList(checkedUuidArray)

    fun setChecked(items: List<T>, isChecked: Boolean) {
        if (!isFilling) {
            isFilling = true
            items.forEach {
                setChecked(it.uuid, isChecked)
            }
            isFilling = false
        }
    }

    private fun setChecked(uuid: UUID, isChecked: Boolean) {
        val position = getIndexByUuid(uuid)
        if (isChecked) {
            checkedUuidArray.add(uuid)
        } else {
            checkedUuidArray.remove(uuid)
        }
        checkedChangedListener?.onCheckedChanged(isChecked, position)
    }

    @Suppress("unused")
    fun clearChecked() {
        checkedUuidArray.clear()
    }

    private fun getIndexByUuid(uuid: UUID): Int =
        (0 until count).firstOrNull {
            getItem(it)?.let { item -> item.uuid == uuid } == true
        } ?: -1

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        var alreadyExists = true

        if (v == null || v.tag == null) {
            val inflater = LayoutInflater.from(context)
            v = inflater.inflate(this.resource, parent, false)
            alreadyExists = false
        }

        v = fillView(position, v!!, alreadyExists)
        return v
    }

    @Suppress("UNCHECKED_CAST")
    private fun fillView(position: Int, v: View, alreadyExists: Boolean): View {
        var holder = ViewHolder<T>()
        if (alreadyExists) {
            holder = v.tag as ViewHolder<T>
        } else {
            holder.bindView = bindView

            setCheckBoxVisibility(v)

            v.tag = holder
        }

        if (position >= 0) {
            setupHolder(position, v, holder)
        }

        return v
    }

    @SuppressLint("SetTextI18n")
    private fun setupHolder(position: Int, v: View, holder: ViewHolder<T>) {
        val item = getItem(position) ?: return
        val status = getStatus(item)

        // Control de visibilidad basado en el estado
        setVisibility(v, status)

        // Aplicar estilos usando la lambda proporcionada
        styleView(v, status)

        // Manejo del Checkbox si es multi-select
        val checkBox: CheckBox? = v.findViewById(R.id.checkBox)
        if (multiSelect && checkBox != null) {
            setCheckBoxLogic(position, checkBox, item.uuid)
        }

        // Llenar la vista usando la lambda proporcionada
        holder.bindView.invoke(v, item, position)
    }

    private fun setCheckBoxVisibility(v: View) {
        val checkBox: CheckBox? = v.findViewById(R.id.checkBox)
        if (multiSelect)
            checkBox?.visibility = View.VISIBLE else
            checkBox?.visibility = View.GONE
    }

    private fun setVisibility(v: View, status: GenericStatus?) {
        val parentLayout: ConstraintLayout? = v.findViewById(R.id.parentLayout)
        val divider: View? = v.findViewById(R.id.dividerBottom)

        if (parentLayout != null && divider != null) {
            if (!visibleStatus.contains(status)) {
                divider.visibility = View.GONE
                parentLayout.visibility = View.GONE
                parentLayout.layoutParams = ConstraintLayout.LayoutParams(-1, 1)
            } else {
                divider.visibility = View.VISIBLE
                parentLayout.visibility = View.VISIBLE
                parentLayout.layoutParams =
                    ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setCheckBoxLogic(position: Int, checkBox: CheckBox, uuid: UUID) {
        var isLongPressed = false

        val checkChangeListener =
            CompoundButton.OnCheckedChangeListener { _, isChecked ->
                setChecked(uuid, isChecked)
            }

        val pressHoldListener =
            View.OnLongClickListener {
                isLongPressed = true
                true
            }

        val pressTouchListener = View.OnTouchListener { pView, pEvent ->
            pView.onTouchEvent(pEvent)
            if (pEvent.action == MotionEvent.ACTION_UP) {
                if (isLongPressed) {
                    if (!isFilling) {
                        checkBox.setOnCheckedChangeListener(null)
                        val newState = !checkBox.isChecked
                        setChecked(getAll(), newState)
                    }
                    isLongPressed = false
                }
            }
            true
        }

        checkBox.setOnCheckedChangeListener(null)
        checkBox.setOnLongClickListener(null)
        checkBox.setOnTouchListener(null)
        checkBox.isChecked = checkedUuidArray.contains(uuid)
        checkBox.tag = position
        checkBox.setOnLongClickListener(pressHoldListener)
        checkBox.setOnTouchListener(pressTouchListener)
        checkBox.setOnCheckedChangeListener(checkChangeListener)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val filteredList: ArrayList<T> = ArrayList()

                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    for (item in itemList) {
                        if (filterParentUUID != null && filterParentUUID != item.parentUuid) {
                            continue
                        }
                        if (filterPredicate(item, filterString)) {
                            filteredList.add(item)
                        }
                    }
                }

                comparator?.let {
                    filteredList.sortWith(it)
                }

                results.values = filteredList
                results.count = filteredList.size
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                suggestedList.clear()
                if (results != null && results.count > 0) {
                    suggestedList.addAll(results.values as ArrayList<T>)
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    internal class ViewHolder<T> {
        lateinit var bindView: (View, T, Int) -> Unit
    }

    companion object {
        fun <T : HasUUID> create(
            resource: Int,
            itemList: List<T>,
            multiSelect: Boolean = false,
            visibleStatus: List<GenericStatus> = listOf(),
            listener: CheckedChangedListener? = null,
            bindView: (View, T, Int) -> Unit,
            getStatus: (T) -> GenericStatus?,
            styleView: (View, GenericStatus?) -> Unit,
            filterPredicate: (T, String) -> Boolean,
            comparator: Comparator<T>? = null,
            filterParentUUID: UUID? = null,
            onReady: (() -> Unit)? = null
        ): GenericDropDownAdapter<T> {
            return GenericDropDownAdapter(
                resource = resource,
                itemList = itemList,
                multiSelect = multiSelect,
                visibleStatus = ArrayList(visibleStatus),
                checkedChangedListener = listener,
                bindView = bindView,
                getStatus = getStatus,
                styleView = styleView,
                filterPredicate = filterPredicate,
                comparator = comparator,
                filterParentUUID = filterParentUUID,
                onReady = onReady
            )
        }
    }
}

@Suppress("unused")
interface HasUUID {
    val uuid: UUID
    val parentUuid: UUID?
}

@Suppress("unused")
interface Describable {
    val description: String
}

fun <T> ArrayAdapter<T>.getViewTotalHeight(parent: ViewGroup, maxHeight: Int): Int {
    var totalHeight = 0
    for (i in 0 until count) {

        val view = getView(i, null, parent)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val itemHeight = view.measuredHeight
        val layoutParams = view.layoutParams as? ViewGroup.MarginLayoutParams
        val verticalMargins = (layoutParams?.topMargin ?: 0) + (layoutParams?.bottomMargin ?: 0)

        if (totalHeight + itemHeight + verticalMargins > maxHeight) {
            return totalHeight
        }
        totalHeight += itemHeight + verticalMargins
    }
    return totalHeight
}