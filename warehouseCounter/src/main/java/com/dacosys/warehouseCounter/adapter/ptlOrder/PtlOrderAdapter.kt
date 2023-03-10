package com.dacosys.warehouseCounter.adapter.ptlOrder

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.dto.ptlOrder.PtlOrder
import com.dacosys.warehouseCounter.ui.utils.Colors
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.manipulateColor
import com.dacosys.warehouseCounter.ui.views.AutoResizeTextView
import java.lang.ref.WeakReference
import java.util.*


/**
 * Created by Agustin on 18/01/2017.
 */

class PtlOrderAdapter : ArrayAdapter<PtlOrder>, Filterable {
    private var activity: AppCompatActivity
    private var resource: Int = 0
    private var itemList: ArrayList<PtlOrder> = ArrayList()
    private var suggestedList: ArrayList<PtlOrder> = ArrayList()
    private var visibleStatus: ArrayList<Long> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()
    private var lastSelectedPos = -1
    private var multiSelect: Boolean = false
    private var dataSetChangedListener: DataSetChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null

    constructor(
        activity: AppCompatActivity,
        resource: Int,
        itemList: ArrayList<PtlOrder>,
        suggestedList: ArrayList<PtlOrder>,
        checkedIdArray: ArrayList<Long>,
        listView: ListView?,
        multiSelect: Boolean,
    ) : super(WarehouseCounterApp.context, resource, suggestedList) {
        this.activity = activity
        this.resource = resource
        this.multiSelect = multiSelect
        this.suggestedList = suggestedList
        this.itemList = itemList
        this.checkedIdArray = checkedIdArray
        this.listView = listView
        this.visibleStatus = getPrefVisibleStatus()

        setupColors()
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

    override fun add(item: PtlOrder?) {
        if (item != null) {
            if (!getAll().contains(item)) {
                activity.runOnUiThread {
                    super.add(item)
                }
            }
        }
        refresh()
    }

    override fun clear() {
        activity.runOnUiThread {
            super.clear()
            clearChecked()
        }
    }

    override fun remove(orderRequest: PtlOrder?) {
        if (orderRequest != null) {
            remove(arrayListOf(orderRequest))
        }
    }

    fun remove(items: ArrayList<PtlOrder>) {
        activity.runOnUiThread {
            for (w in items) {
                if (getAll().contains(w)) {
                    checkedIdArray.remove(w.id)
                    super.remove(w)
                }
            }
        }
        refresh()
    }

    private fun getIndex(orderRequest: PtlOrder): Int {
        for (i in 0 until count) {
            val t = (getItem(i) as PtlOrder)
            if (t == orderRequest) {
                return i
            }
        }
        return -1
    }

    fun count(): Int {
        return count
    }

    fun getAll(): ArrayList<PtlOrder> {
        val r: ArrayList<PtlOrder> = ArrayList()
        for (i in 0 until count) {
            r.add(getItem(i) as PtlOrder)
        }
        return r
    }

    fun getAllCheckedAsInt(): ArrayList<Int> {
        val intArray: ArrayList<Int> = ArrayList()
        for (c in checkedIdArray) {
            intArray.add(c.toInt())
        }
        return intArray
    }

    fun countChecked(): Int {
        return checkedIdArray.count()
    }

    fun getAllIdChecked(): ArrayList<Long> {
        return checkedIdArray
    }

    fun getAllChecked(): ArrayList<PtlOrder> {
        val r: ArrayList<PtlOrder> = ArrayList()
        for (a in getAll()) {
            if (checkedIdArray.contains(a.id)) {
                r.add(a)
            }
        }
        return r
    }

    private var isFilling = false
    fun setChecked(items: ArrayList<PtlOrder>, isChecked: Boolean) {
        if (isFilling) return
        isFilling = true

        for (i in items) {
            setChecked(i, isChecked)
        }

        isFilling = false
        refresh()
    }

    fun setChecked(item: PtlOrder, isChecked: Boolean, suspendRefresh: Boolean = false) {
        val position = getIndex(item)
        if (isChecked) {
            if (!checkedIdArray.contains(item.id)) {
                checkedIdArray.add(item.id)
            }
        } else {
            checkedIdArray.remove(item.id)
        }

        if (checkedChangedListener != null) {
            checkedChangedListener!!.onCheckedChanged(isChecked, position)
        }

        if (!suspendRefresh) {
            refresh()
        }
    }

    fun setChecked(checkedItems: ArrayList<PtlOrder>) {
        checkedItems.clear()
        setChecked(checkedItems, true)
    }

    private fun clearChecked() {
        checkedIdArray.clear()
    }

    private fun refresh() {
        activity.runOnUiThread { notifyDataSetChanged() }
        dataSetChangedListener?.onDataSetChanged()
    }

    fun setSelectItemAndScrollPos(a: PtlOrder?, tScrollPos: Int?) {
        var pos = -1
        if (a != null) pos = getPosition(a)
        var scrollPos = -1
        if (tScrollPos != null) scrollPos = tScrollPos
        selectItem(pos, scrollPos, false)
    }

    fun selectItem(a: PtlOrder?) {
        var pos = -1
        if (a != null) pos = getPosition(a)
        selectItem(pos)
    }

    private fun selectItem(pos: Int) {
        selectItem(pos, pos, true)
    }

    private fun selectItem(pos: Int, scrollPos: Int, smoothScroll: Boolean) {
        val listView = listView ?: return
        listView.clearChoices()

        // Deseleccionar cuando:
        //   - Estaba previamente seleccionado
        //   - La posición es negativa
        //   - La cantidad de ítems es cero o menos

        activity.runOnUiThread {
            if (pos < 0 && lastSelectedPos < 0 && count > 0) {
                listView.setItemChecked(0, true)
                listView.setSelection(0)
            } else if (pos == lastSelectedPos || pos < 0 || count <= 0) {
                listView.setItemChecked(-1, true)
                listView.setSelection(-1)
            } else {
                listView.setItemChecked(pos, true)
                listView.setSelection(pos)
            }
        }

        lastSelectedPos = currentPos()

        activity.runOnUiThread {
            if (smoothScroll) {
                notifyDataSetChanged()
                listView.smoothScrollToPosition(scrollPos)
            } else {
                refresh()
                listView.setSelection(scrollPos)
            }
        }
    }

    fun currentItem(): PtlOrder? {
        return (0 until count).firstOrNull { isSelected(it) }?.let { getItem(it) }
    }

    private fun currentPos(): Int {
        return (0 until count).firstOrNull { isSelected(it) } ?: -1
    }

    fun firstVisiblePos(): Int {
        val listView = listView ?: return -1
        var pos = listView.firstVisiblePosition
        if (listView.childCount > 1 && listView.getChildAt(0).top < 0) pos++
        return pos
    }

    private fun isSelected(position: Int): Boolean {
        return position >= 0 && (listView != null && listView!!.isItemChecked(position))
    }

    private var weakRefListView: WeakReference<ListView?>? = null
        set(newValue) {
            field = newValue
            val l = listView
            if (l != null) {
                activity.runOnUiThread {
                    l.adapter = this
                }

                l.setOnItemClickListener { _, _, position, _ ->
                    selectItem(position)
                }
            }
        }

    var listView: ListView?
        get() {
            return if (weakRefListView == null) null else weakRefListView!!.get() ?: return null
        }
        set(newValue) {
            weakRefListView = WeakReference(newValue)
        }

    fun addVisibleStatus(status: Long) {
        if (!visibleStatus.contains(status)) {
            visibleStatus.add(status)
            refresh()
        }
    }

    fun removeVisibleStatus(status: Long) {
        if (visibleStatus.contains(status)) {
            visibleStatus.remove(status)
            refresh()
        }
    }

    fun getVisibleStatus(): ArrayList<Long> {
        return visibleStatus
    }

    private var showAllOnFilterEmpty = false
    fun refreshFilter(s: String, showAllOnFilterEmpty: Boolean) {
        this.showAllOnFilterEmpty = showAllOnFilterEmpty
        filter.filter(s)
        refresh()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        var alreadyExists = true

        // Seleccionamos el layout dependiendo si es un row expandido o normal.
        // Los rows expandidos son los que están seleccionados.
        var currentLayout = resource
        if (isSelected(position)) {
            currentLayout = R.layout.ptl_order_row_expanded
        }

        if (v == null || v.tag == null) {
            // El view todavía no fue creado, crearlo con el layout correspondiente.
            val vi = LayoutInflater.from(context)
            v = vi.inflate(currentLayout, parent, false)

            alreadyExists = false
        } else {
            // El view ya existe, comprobar que no necesite cambiar de layout.
            if (v.tag is ListViewHolder && currentLayout == R.layout.ptl_order_row_expanded ||
                v.tag is ExpandedViewHolder && currentLayout == resource
            ) {
                // Ya fue creado, si es un row normal que está siendo seleccionada
                // o un row expandido que está siendo deseleccionado
                // debe cambiar de layout, por lo tanto volver a crearse.
                val vi = LayoutInflater.from(context)
                v = vi.inflate(currentLayout, parent, false)

                alreadyExists = false
            }
        }

        v = if (currentLayout == R.layout.ptl_order_row_expanded) {
            fillSelectedItemView(position, v!!, alreadyExists)
        } else {
            fillListView(position, v!!, alreadyExists)
        }
        return v
    }

    //region COLORS

    private var selectedForeColor: Int = 0

    private var prepareOrderForeColor: Int = 0
    private var stockAuditFromDeviceForeColor: Int = 0
    private var stockAuditForeColor: Int = 0
    private var receptionAuditForeColor: Int = 0
    private var defaultForeColor: Int = 0

    private fun setupColors() {
        selectedForeColor = getColor(context.resources, R.color.text_light, null)

        prepareOrderForeColor = Colors.getBestContrastColor("#FF009688")
        stockAuditFromDeviceForeColor = Colors.getBestContrastColor("#FFC107")
        stockAuditForeColor = Colors.getBestContrastColor("#2196F3")
        receptionAuditForeColor = Colors.getBestContrastColor("#FF5722")
        defaultForeColor = Colors.getBestContrastColor("#DFDFDF")
    }

    //endregion

    @SuppressLint("ClickableViewAccessibility", "ObsoleteSdkInt")
    private fun fillSelectedItemView(position: Int, v: View, alreadyExists: Boolean): View {
        var holder = ExpandedViewHolder()
        if (alreadyExists) {
            if (v.tag is ListViewHolder) {
                createExpandedViewHolder(v, holder)
            } else {
                holder = v.tag as ExpandedViewHolder
            }
        } else {
            createExpandedViewHolder(v, holder)
        }

        if (position >= 0) {
            val orderRequest = getItem(position)
            val isSelected = isSelected(position)

            if (orderRequest != null) {
                holder.descriptionTextView?.text = orderRequest.description.ifEmpty {
                    context.getString(R.string.without_description)
                }
                holder.startDateTextView?.text = orderRequest.startDate.toString()
                holder.finishDateTextView?.text =
                    orderRequest.finishDate ?: context.getString(R.string.uncompleted)
                holder.zoneTextView?.text = orderRequest.zone.substringAfterLast('/')

                if (holder.checkBox != null) {
                    var isSpeakButtonLongPressed = false

                    val checkChangeListener =
                        CompoundButton.OnCheckedChangeListener { _, isChecked ->
                            this.setChecked(orderRequest, isChecked, true)
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
                    holder.checkBox?.isChecked = checkedIdArray.contains(orderRequest.id)

                    holder.checkBox?.tag = position
                    holder.checkBox?.setOnLongClickListener(pressHoldListener)
                    holder.checkBox?.setOnTouchListener(pressTouchListener)
                    holder.checkBox?.setOnCheckedChangeListener(checkChangeListener)
                }

                val colorDefault =
                    getDrawable(context.resources, R.drawable.layout_thin_border, null)!!
                val pepareOrderBackColor =
                    getDrawable(context.resources, R.drawable.layout_thin_border_green, null)!!
                val stockAuditFromDeviceBackColor =
                    getDrawable(context.resources, R.drawable.layout_thin_border_yellow, null)!!
                val stockAuditBackColor =
                    getDrawable(context.resources, R.drawable.layout_thin_border_blue, null)!!
                val receptionAuditBackColor =
                    getDrawable(context.resources, R.drawable.layout_thin_border_orange, null)!!

                var backColor = colorDefault
                var foreColor = if (isSelected) selectedForeColor else defaultForeColor

                val orType = orderRequest.orderTypeId
                when (orType) {
                    0L -> {
                        backColor = pepareOrderBackColor
                        foreColor = if (isSelected) selectedForeColor else prepareOrderForeColor
                    }
                    1L -> {
                        backColor = stockAuditFromDeviceBackColor
                        foreColor =
                            if (isSelected) selectedForeColor else stockAuditFromDeviceForeColor
                    }
                    2L -> {
                        backColor = stockAuditBackColor
                        foreColor = if (isSelected) selectedForeColor else stockAuditForeColor
                    }
                    3L -> {
                        backColor = receptionAuditBackColor
                        foreColor = if (isSelected) selectedForeColor else receptionAuditForeColor
                    }
                }

                val darkerColor = when {
                    isSelected -> true
                    foreColor == Colors.textLightColor() -> true
                    else -> false
                }

                val titleForeColor: Int =
                    manipulateColor(foreColor, if (darkerColor) 0.8f else 1.4f)

                v.background = backColor
                holder.zoneTextView?.setTextColor(foreColor)
                holder.descriptionTextView?.setTextColor(foreColor)
                holder.startDateTextView?.setTextColor(foreColor)
                holder.finishDateTextView?.setTextColor(foreColor)
                holder.checkBox?.buttonTintList = ColorStateList.valueOf(titleForeColor)

                // VISIBILIDAD DEL VIEW SEGÚN ESTADO
                val parentLayout = v.findViewById<ConstraintLayout>(R.id.parentLayout)
                if (!visibleStatus.contains(orType)) {
                    parentLayout.visibility = GONE
                    parentLayout.layoutParams = ConstraintLayout.LayoutParams(0, 0)
                } else {
                    parentLayout.visibility = VISIBLE
                    parentLayout.layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                }
            }

            if (listView != null) {
                if (isSelected(position)) {
                    v.background.colorFilter =
                        BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                            getColorWithAlpha(colorId = R.color.lightslategray, alpha = 240),
                            BlendModeCompat.MODULATE
                        )
                } else {
                    v.background.colorFilter = null
                }
            }
        }

        return v
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val r: ArrayList<PtlOrder> = ArrayList()

                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.ROOT)
                    var filterableItem: PtlOrder

                    for (i in 0 until itemList.size) {
                        filterableItem = itemList[i]
                        if (filterableItem.description.lowercase(Locale.ROOT)
                                .contains(filterString)
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
                if (results?.values != null) suggestedList.addAll(results.values as ArrayList<PtlOrder>)

                if ((results?.count ?: 0) > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility", "ObsoleteSdkInt")
    private fun fillListView(position: Int, v: View, alreadyExists: Boolean): View {
        var holder = ListViewHolder()
        if (alreadyExists && v.tag is ListViewHolder) {
            holder = v.tag as ListViewHolder
        } else {
            createListViewHolder(v, holder)
        }

        if (position >= 0) {
            val orderRequest = getItem(position)
            if (orderRequest != null) {
                holder.descriptionTextView?.text = orderRequest.description.ifEmpty {
                    context.getString(R.string.without_description)
                }
                holder.zoneTextView?.text = orderRequest.zone.substringAfterLast('/')
                holder.startDateTextView?.text = orderRequest.startDate

                if (holder.checkBox != null) {
                    var isSpeakButtonLongPressed = false

                    val checkChangeListener =
                        CompoundButton.OnCheckedChangeListener { _, isChecked ->
                            this.setChecked(orderRequest, isChecked, true)
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
                    holder.checkBox?.isChecked = checkedIdArray.contains(orderRequest.id)

                    holder.checkBox?.tag = position
                    holder.checkBox?.setOnLongClickListener(pressHoldListener)
                    holder.checkBox?.setOnTouchListener(pressTouchListener)
                    holder.checkBox?.setOnCheckedChangeListener(checkChangeListener)
                }

                // Font colors
                val black = getColor(context.resources, R.color.black, null)
                val white = getColor(context.resources, R.color.white, null)

                // CheckBox color
                val darkslategray = getColor(context.resources, R.color.darkslategray, null)

                var backColor =
                    getDrawable(context.resources, R.drawable.layout_thin_border, null)!!
                var foreColor = black

                val orType = orderRequest.orderTypeId
                when (orType) {
                    0L -> {
                        backColor = getDrawable(
                            context.resources, R.drawable.layout_thin_border_green, null
                        )!!
                        foreColor = white
                    }
                    1L -> {
                        backColor = getDrawable(
                            context.resources, R.drawable.layout_thin_border_blue, null
                        )!!
                        foreColor = white
                    }
                    2L -> {
                        backColor = getDrawable(
                            context.resources, R.drawable.layout_thin_border_orange, null
                        )!!
                        foreColor = white
                    }
                    3L -> {
                        backColor = getDrawable(
                            context.resources, R.drawable.layout_thin_border_green_2, null
                        )!!
                        foreColor = white
                    }
                    4L -> {
                        backColor = getDrawable(
                            context.resources, R.drawable.layout_thin_border_yellow, null
                        )!!
                        foreColor = black
                    }
                }

                v.background = backColor
                holder.zoneTextView?.setTextColor(foreColor)
                holder.descriptionTextView?.setTextColor(foreColor)
                holder.startDateTextView?.setTextColor(foreColor)
                holder.checkBox?.buttonTintList = ColorStateList.valueOf(darkslategray)

                // VISIBILIDAD DEL VIEW SEGÚN ESTADO
                val parentLayout = v.findViewById<ConstraintLayout>(R.id.parentLayout)
                if (!visibleStatus.contains(orType)) {
                    parentLayout.visibility = GONE
                    parentLayout.layoutParams = ConstraintLayout.LayoutParams(0, 0)
                } else {
                    parentLayout.visibility = VISIBLE
                    parentLayout.layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                }
            }

            if (listView != null) {
                if (isSelected(position)) {
                    v.background.colorFilter =
                        BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                            getColorWithAlpha(colorId = R.color.lightslategray, alpha = 240),
                            BlendModeCompat.MODULATE
                        )
                } else {
                    v.background.colorFilter = null
                }
            }
        }

        return v
    }

    private fun createExpandedViewHolder(v: View, holder: ExpandedViewHolder) {
        // Holder para los rows expandidos
        holder.descriptionTextView = v.findViewById(R.id.descriptionTextView)
        holder.zoneTextView = v.findViewById(R.id.zoneTextView)

        holder.startDateTextView = v.findViewById(R.id.startDateTextView)
        holder.finishDateTextView = v.findViewById(R.id.finishDateTextView)

        holder.checkBox = v.findViewById(R.id.checkBox)

        if (multiSelect) {
            holder.checkBox?.visibility = VISIBLE
        } else {
            holder.checkBox?.visibility = GONE
        }

        v.tag = holder
    }

    private fun createListViewHolder(v: View, holder: ListViewHolder) {
        // Holder para los rows normales
        holder.descriptionTextView = v.findViewById(R.id.descriptionTextView)
        holder.startDateTextView = v.findViewById(R.id.startDateTextView)
        holder.zoneTextView = v.findViewById(R.id.zoneTextView)

        holder.checkBox = v.findViewById(R.id.checkBox)

        if (multiSelect) {
            holder.checkBox?.visibility = VISIBLE
        } else {
            holder.checkBox?.visibility = GONE
        }

        v.tag = holder
    }

    internal inner class ExpandedViewHolder {
        var descriptionTextView: AutoResizeTextView? = null
        var zoneTextView: AutoResizeTextView? = null

        var startDateTextView: AutoResizeTextView? = null
        var finishDateTextView: CheckedTextView? = null

        var checkBox: CheckBox? = null
    }

    internal inner class ListViewHolder {
        var descriptionTextView: AutoResizeTextView? = null
        var zoneTextView: AutoResizeTextView? = null

        var startDateTextView: AutoResizeTextView? = null

        var checkBox: CheckBox? = null
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun getPrefVisibleStatus(): ArrayList<Long> {
            val visibleStatusArray: ArrayList<Long> = ArrayList()
            //Retrieve the values
            val set = settingViewModel.orderRequestVisibleStatus
            for (i in set) {
                if (i.trim().isEmpty()) continue
                visibleStatusArray.add(i.toLong())
            }
            return visibleStatusArray
        }
    }
}