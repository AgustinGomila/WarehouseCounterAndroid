package com.dacosys.warehouseCounter.adapter.item

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.room.entity.item.Item
import com.dacosys.warehouseCounter.ui.utils.Colors
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.views.AutoResizeTextView
import java.lang.ref.WeakReference
import java.util.*


/**
 * Created by Agustin on 18/01/2017.
 */

class ItemAdapter : ArrayAdapter<Item>, Filterable {

    fun refreshListeners(
        checkedChangedListener: CheckedChangedListener? = null,
        dataSetChangedListener: DataSetChangedListener? = null,
        selectedItemChangedListener: SelectedItemChangedListener? = null,
    ) {
        this.checkedChangedListener = checkedChangedListener
        this.dataSetChangedListener = dataSetChangedListener
        this.selectedItemChangedListener = selectedItemChangedListener
    }

    constructor(
        activity: AppCompatActivity,
        resource: Int,
        itemList: ArrayList<Item>,
        suggestedList: ArrayList<Item>,
        listView: ListView?,
        multiSelect: Boolean,
        checkedIdArray: ArrayList<Long>,
    ) : super(WarehouseCounterApp.context, resource, suggestedList) {
        this.activity = activity
        this.resource = resource
        this.multiSelect = multiSelect
        this.suggestedList = suggestedList
        this.itemList = itemList
        this.checkedIdArray = checkedIdArray
        this.listView = listView

        setupColors()
    }

    constructor(
        activity: AppCompatActivity,
        resource: Int,
        itemList: ArrayList<Item>,
        suggestedList: ArrayList<Item>,
    ) : super(WarehouseCounterApp.context, resource, suggestedList) {
        this.activity = activity
        this.resource = resource
        this.suggestedList = suggestedList
        this.itemList = itemList
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

    interface SelectedItemChangedListener {
        // Define data you like to return from AysncTask
        fun onSelectedItemChanged(
            item: Item?,
            pos: Int,
        )
    }

    fun add(items: ArrayList<Item>, scrollToPosition: Boolean) {
        val all = getAll()
        val itemsAdded: ArrayList<Item> = ArrayList()

        activity.runOnUiThread {
            for (w in items) {
                if (!all.contains(w)) {
                    itemsAdded.add(w)
                    all.add(w)
                }
            }

            if (!itemsAdded.any()) return@runOnUiThread

            refreshItems(all)

            if (scrollToPosition) forceSelectItem(itemsAdded[0])
        }
    }

    override fun clear() {
        activity.runOnUiThread {
            super.clear()
            clearChecked()
        }
    }

    override fun remove(item: Item?) {
        if (item != null) {
            remove(arrayListOf(item))
        }
    }

    fun remove(items: ArrayList<Item>) {
        if (items.isEmpty()) return

        val all = getAll()
        lastSelectedPos = currentPos()

        val itemsRemoved: ArrayList<Item> = ArrayList()
        activity.runOnUiThread {
            for (w in items) {
                if (all.contains(w)) {
                    itemsRemoved.add(w)
                    if (checkedIdArray.contains((w.itemId))) {
                        checkedIdArray.remove((w.itemId))
                    }
                }
            }

            if (!itemsRemoved.any()) return@runOnUiThread

            all.removeAll(itemsRemoved.toSet())

            refreshItems(all)

            Handler(Looper.getMainLooper()).postDelayed({ run { selectItem(lastSelectedPos) } }, 20)
        }
    }

    override fun sort(comparator: Comparator<in Item>) {
        super.sort(customComparator)
    }

    private val customComparator = Comparator { o1: Item?, o2: Item? ->
        ItemComparator().compareNullable(o1, o2)
    }

    private fun getIndex(item: Item): Int {
        for (i in 0 until count) {
            val t = (getItem(i) as Item)
            if (t == item) {
                return i
            }
        }
        return -1
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

    fun getAllCheckedAsInt(): ArrayList<Int> {
        val intArray: ArrayList<Int> = ArrayList()
        for (c in checkedIdArray) {
            intArray.add(c.toInt())
        }
        return intArray
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
        val position = getIndex(item)
        if (isChecked) {
            if (!checkedIdArray.contains(item.itemId)) {
                checkedIdArray.add(item.itemId)
            }
        } else {
            checkedIdArray.remove(item.itemId)
        }

        if (checkedChangedListener != null) {
            checkedChangedListener!!.onCheckedChanged(isChecked, position)
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
        dataSetChangedListener?.onDataSetChanged()
    }

    /**
     * Esta función es la que agrega realmente los items al adaptador.
     * Está separada para poder realizar antes el ordenamiento especial.
     * Recibe la lista, la ordena, limpia el adaptador, lo llena y llama al
     * refresh.
     * */
    private fun refreshItems(originalList: ArrayList<Item>) {
        val sortedList = sortItems(originalList)

        activity.runOnUiThread {
            super.clear()
            super.addAll(sortedList)
        }

        refresh()
    }

    override fun add(item: Item?) {
        val all = getAll()
        var someChanges = false

        activity.runOnUiThread {
            if (item != null) {
                if (!all.contains(item)) {
                    someChanges = true
                    all.add(item)
                }
            }

            if (!someChanges) return@runOnUiThread

            refreshItems(all)

            if (item != null) forceSelectItem(item)
        }
    }

    fun itemExists(item: Item): Boolean {
        return getPosition(item) >= 0
    }

    fun forceSelectItem(a: Item) {
        val listView = listView ?: return
        val pos = getPosition(a)

        listView.clearChoices()

        activity.runOnUiThread {
            listView.setItemChecked(pos, true)
            listView.setSelection(pos)
        }

        lastSelectedPos = currentPos()

        activity.runOnUiThread {
            notifyDataSetChanged()
            listView.smoothScrollToPosition(pos)
        }

        performItemChanged()
    }

    fun setSelectItemAndScrollPos(a: Item?, tScrollPos: Int?) {
        var pos = -1
        if (a != null) pos = getPosition(a)
        var scrollPos = -1
        if (tScrollPos != null) scrollPos = tScrollPos
        selectItem(pos, scrollPos, false)
    }

    fun selectItem(a: Item?) {
        var pos = -1
        if (a != null) pos = getPosition(a)
        selectItem(pos)
    }

    fun selectItem(pos: Int) {
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

        performItemChanged()
    }

    private fun performItemChanged() {
        selectedItemChangedListener?.onSelectedItemChanged(
            if (lastSelectedPos >= 0) getItem(
                lastSelectedPos
            ) else null, lastSelectedPos
        )
    }

    fun currentItem(): Item? {
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

    private var lastSelectedPos = -1
    private var multiSelect: Boolean = false

    private var dataSetChangedListener: DataSetChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null
    private var selectedItemChangedListener: SelectedItemChangedListener? = null
    private var activity: AppCompatActivity
    private var resource: Int = 0
    private var itemList: ArrayList<Item> = ArrayList()
    private var suggestedList: ArrayList<Item> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        var alreadyExists = true

        // Seleccionamos el layout dependiendo si es un row expandido o normal.
        // Los rows expandidos son los que están seleccionados.
        var currentLayout = resource
        if (isSelected(position)) {
            currentLayout = R.layout.item_row_expanded
        }

        if (v == null || v.tag == null) {
            // El view todavía no fue creado, crearlo con el layout correspondiente.
            val vi = LayoutInflater.from(context)
            v = vi.inflate(currentLayout, parent, false)

            alreadyExists = false
        } else {
            // El view ya existe, comprobar que no necesite cambiar de layout.
            if (v.tag is ListViewHolder && currentLayout == R.layout.item_row_expanded || v.tag is ExpandedViewHolder && currentLayout == resource) {
                // Ya fue creado, si es un row normal que está siendo seleccionada
                // o un row expandido que está siendo deseleccionado
                // debe cambiar de layout, por lo tanto volver a crearse.
                val vi = LayoutInflater.from(context)
                v = vi.inflate(currentLayout, parent, false)

                alreadyExists = false
            }
        }

        v = if (currentLayout == R.layout.item_row_simple) {
            fillDropDownListView(position, v!!, alreadyExists)
        } else {
            if (currentLayout == R.layout.item_row_expanded) {
                fillSelectedItemView(position, v!!, alreadyExists)
            } else {
                fillListView(position, v!!, alreadyExists)
            }
        }

        return v
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun fillDropDownListView(position: Int, v: View, alreadyExists: Boolean): View {
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
                    holder.checkBox!!.setOnCheckedChangeListener(null)
                    holder.checkBox!!.isChecked = checkedIdArray.contains(item.itemId)
                    holder.checkBox!!.tag = position
                    holder.checkBox!!.setOnLongClickListener(pressHoldListener)
                    holder.checkBox!!.setOnTouchListener(pressTouchListener)
                    holder.checkBox!!.setOnCheckedChangeListener(checkChangeListener)
                }

                // Background colors
                val lightgray = getColor(context.resources, R.color.lightgray, null)
                val whitesmoke = getColor(context.resources, R.color.whitesmoke, null)

                // Font colors
                val black = getColor(context.resources, R.color.black, null)
                val dimgray = getColor(context.resources, R.color.dimgray, null)

                when {
                    item.active != 1 -> {
                        v.setBackgroundColor(lightgray)
                        holder.descriptionTextView!!.setTextColor(dimgray)
                        holder.eanTextView!!.setTextColor(dimgray)
                    }
                    else -> {
                        v.setBackgroundColor(whitesmoke)
                        holder.descriptionTextView!!.setTextColor(dimgray)
                        holder.eanTextView!!.setTextColor(black)
                    }
                }
            }
        }

        if (v.height > 0) {
            viewHeightForDropDown = v.height
            Log.d(this::class.java.simpleName, "-------{RES: $resource Height:${v.height}}-------")
        }
        return v
    }

    //region COLORS

    private var selectedForeColor: Int = 0

    private var inactiveForeColor: Int = 0
    private var defaultForeColor: Int = 0

    private fun setupColors() {
        selectedForeColor = getColor(context.resources, R.color.text_light, null)

        inactiveForeColor = Colors.getBestContrastColor("#C7C7C7")
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
            val item = getItem(position)
            val isSelected = isSelected(position)

            if (item != null) {
                holder.descriptionTextView?.text = item.description
                holder.eanTextView?.text = item.ean
                holder.priceTextView?.text =
                    String.format("$ %s", Statics.roundToString(item.price!!, 2))
                holder.extIdTextView?.text = item.externalId ?: 0.toString()

                // region Category
                val category = item.itemCategoryStr
                if (category.isEmpty()) {
                    holder.categoryConstraintLayout?.visibility = GONE
                } else {
                    holder.categoryConstraintLayout?.visibility = VISIBLE
                    holder.categoryTextView?.text = category
                }
                // endregion

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
                    holder.checkBox!!.setOnCheckedChangeListener(null)
                    holder.checkBox!!.isChecked = checkedIdArray.contains(item.itemId)
                    holder.checkBox!!.tag = position
                    holder.checkBox!!.setOnLongClickListener(pressHoldListener)
                    holder.checkBox!!.setOnTouchListener(pressTouchListener)
                    holder.checkBox!!.setOnCheckedChangeListener(checkChangeListener)
                }

                val colorDefault =
                    getDrawable(context.resources, R.drawable.layout_thin_border, null)!!
                val inactiveBackColor =
                    getDrawable(context.resources, R.drawable.layout_thin_border_gray, null)!!

                var backColor = colorDefault
                var foreColor = if (isSelected) selectedForeColor else defaultForeColor

                if (item.active != 1) {
                    backColor = inactiveBackColor
                    foreColor = if (isSelected) selectedForeColor else inactiveForeColor
                }

                val darkerColor = when {
                    isSelected -> true
                    foreColor == Colors.textLightColor() -> true
                    else -> false
                }

                val titleForeColor: Int =
                    Colors.manipulateColor(foreColor, if (darkerColor) 0.8f else 1.4f)

                v.background = backColor
                holder.descriptionTextView?.setTextColor(foreColor)
                holder.eanTextView?.setTextColor(foreColor)
                holder.categoryTextView?.setTextColor(foreColor)
                holder.priceTextView?.setTextColor(foreColor)
                holder.extIdTextView?.setTextColor(foreColor)
                holder.checkBox?.buttonTintList = ColorStateList.valueOf(titleForeColor)
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

    @SuppressLint("ClickableViewAccessibility", "ObsoleteSdkInt")
    private fun fillListView(position: Int, v: View, alreadyExists: Boolean): View {
        var holder = ListViewHolder()
        if (alreadyExists && v.tag is ListViewHolder) {
            holder = v.tag as ListViewHolder
        } else {
            createListViewHolder(v, holder)
        }

        if (position >= 0) {
            val item = getItem(position)
            val isSelected = isSelected(position)

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
                    holder.checkBox!!.setOnCheckedChangeListener(null)
                    holder.checkBox!!.isChecked = checkedIdArray.contains(item.itemId)
                    holder.checkBox!!.tag = position
                    holder.checkBox!!.setOnLongClickListener(pressHoldListener)
                    holder.checkBox!!.setOnTouchListener(pressTouchListener)
                    holder.checkBox!!.setOnCheckedChangeListener(checkChangeListener)
                }

                val colorDefault =
                    getDrawable(context.resources, R.drawable.layout_thin_border, null)!!
                val inactiveBackColor =
                    getDrawable(context.resources, R.drawable.layout_thin_border_gray, null)!!

                var backColor = colorDefault
                var foreColor = if (isSelected) selectedForeColor else defaultForeColor

                if (item.active != 1) {
                    backColor = inactiveBackColor
                    foreColor = if (isSelected) selectedForeColor else inactiveForeColor
                }

                val darkerColor = when {
                    isSelected -> true
                    foreColor == Colors.textLightColor() -> true
                    else -> false
                }

                val titleForeColor: Int =
                    Colors.manipulateColor(foreColor, if (darkerColor) 0.8f else 1.4f)

                v.background = backColor
                holder.descriptionTextView?.setTextColor(foreColor)
                holder.eanTextView?.setTextColor(foreColor)
                holder.checkBox?.buttonTintList = ColorStateList.valueOf(titleForeColor)
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
        holder.eanTextView = v.findViewById(R.id.eanTextView)
        holder.priceTextView = v.findViewById(R.id.priceCheckedTextView)
        holder.extIdTextView = v.findViewById(R.id.extIdCheckedTextView)
        holder.categoryTextView = v.findViewById(R.id.categoryTextView)

        holder.checkBox = v.findViewById(R.id.checkBox)

        if (multiSelect) holder.checkBox?.visibility = VISIBLE
        else holder.checkBox?.visibility = GONE

        holder.categoryConstraintLayout = v.findViewById(R.id.categoryConstraintLayout)

        v.tag = holder
    }

    private fun createListViewHolder(v: View, holder: ListViewHolder) {
        // Holder para los rows normales.
        holder.descriptionTextView = v.findViewById(R.id.descriptionTextView)
        holder.eanTextView = v.findViewById(R.id.eanTextView)

        holder.checkBox = v.findViewById(R.id.checkBox)

        if (multiSelect) holder.checkBox?.visibility = VISIBLE
        else holder.checkBox?.visibility = GONE

        v.tag = holder
    }

    internal inner class SimpleViewHolder {
        var descriptionTextView: CheckedTextView? = null
        var eanTextView: CheckedTextView? = null
        var checkBox: CheckBox? = null
    }

    internal inner class ExpandedViewHolder {
        var descriptionTextView: AutoResizeTextView? = null
        var eanTextView: AutoResizeTextView? = null
        var priceTextView: CheckedTextView? = null
        var extIdTextView: CheckedTextView? = null

        var checkBox: CheckBox? = null

        var categoryConstraintLayout: ConstraintLayout? = null
        var categoryTextView: AutoResizeTextView? = null
    }

    internal inner class ListViewHolder {
        var descriptionTextView: AutoResizeTextView? = null
        var eanTextView: AutoResizeTextView? = null
        var checkBox: CheckBox? = null
    }

    private var showAllOnFilterEmpty = false
    fun refreshFilter(s: String, showAllOnFilterEmpty: Boolean) {
        this.showAllOnFilterEmpty = showAllOnFilterEmpty
        filter.filter(s)
        refresh()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                var r: ArrayList<Item> = ArrayList()
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
                    } else if (showAllOnFilterEmpty) {
                        r = itemList
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
        var viewHeightForDropDown = if (Screen.isTablet()) 251 else 143

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
            // Get all of the parent groups
            return ArrayList(originalList.sortedWith(compareBy({ it.description }, { it.ean })))
        }

        fun isFilterable(filterableItem: Item, filterString: String): Boolean =
            filterableItem.ean.contains(
                filterString,
                true
            ) || filterableItem.itemCategoryId.toString()
                .contains(filterString) || filterableItem.description.contains(filterString, true)
    }
}