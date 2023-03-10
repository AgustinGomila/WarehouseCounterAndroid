package com.dacosys.warehouseCounter.adapter.ptlItem


import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.dto.ptlOrder.PtlContent
import com.dacosys.warehouseCounter.dto.ptlOrder.PtlItem
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.MakeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.Colors
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.ui.views.AutoResizeTextView
import java.lang.ref.WeakReference
import java.util.*


/**
 * Created by Agustin on 18/01/2017.
 */

class PtlContentAdapter : ArrayAdapter<PtlContent>, Filterable {

    private var activity: AppCompatActivity
    private var resource: Int = 0
    private var itemArray: ArrayList<PtlContent> = ArrayList()
    private var suggestedList: ArrayList<PtlContent> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()
    private var lastSelectedPos = -1
    var multiSelect: Boolean = false
    private var showQtyPanel: Boolean = true
    private var allowEditQty: Boolean = false
    private var setQtyOnCheckedChanged: Boolean = false
    private var dataSetChangedListener: DataSetChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null
    private var editQtyListener: EditQtyListener? = null

    constructor(
        activity: AppCompatActivity,
        resource: Int,
        ptlContents: ArrayList<PtlContent>,
        listView: ListView?,
        checkedIdArray: ArrayList<Long>,
        multiSelect: Boolean,
        allowEditQty: Boolean,
        showQtyPanel: Boolean,
        setQtyOnCheckedChanged: Boolean,
    ) : super(WarehouseCounterApp.context, resource, ptlContents) {

        this.resource = resource
        this.activity = activity
        this.showQtyPanel = showQtyPanel
        this.multiSelect = multiSelect
        this.allowEditQty = allowEditQty
        this.setQtyOnCheckedChanged = setQtyOnCheckedChanged
        this.itemArray = ptlContents
        this.checkedIdArray = checkedIdArray
        this.listView = listView

        setupColors()
    }

    fun refreshListeners(
        checkedChangedListener: CheckedChangedListener?,
        dataSetChangedListener: DataSetChangedListener?,
        editQtyListener: EditQtyListener?,
    ) {
        this.editQtyListener = editQtyListener
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

    interface EditQtyListener {
        fun onEditQtyRequired(
            position: Int,
            item: PtlContent,
            initialQty: Double,
            minValue: Double,
            maxValue: Double,
            multiplier: Int,
        )
    }

    override fun add(item: PtlContent?) {
        var someChanges = false

        activity.runOnUiThread {
            if (item != null) {
                if (!getAll().contains(item)) {
                    someChanges = true
                    super.add(item)
                }
            }

            if (!someChanges) return@runOnUiThread

            refresh()

            if (item != null) forceSelectItem(item)
        }
    }

    fun add(items: ArrayList<PtlContent>) {
        val itemsAdded: ArrayList<PtlContent> = ArrayList()

        activity.runOnUiThread {
            for (item in items) {
                if (!getAll().contains(item)) {
                    itemsAdded.add(item)
                    super.add(item)
                }
            }

            if (!itemsAdded.any()) return@runOnUiThread

            sort(customComparator)
        }
    }

    override fun clear() {
        activity.runOnUiThread {
            super.clear()
            clearChecked()
        }
    }

    override fun remove(item: PtlContent?) {
        if (item != null) {
            remove(arrayListOf(item))
        }
    }

    fun remove(items: ArrayList<PtlContent>) {
        val itemsRemoved: ArrayList<PtlContent> = ArrayList()
        activity.runOnUiThread {
            for (w in items) {
                if (getAll().contains(w)) {
                    val id = w.item.first().id

                    itemsRemoved.add(w)
                    checkedIdArray.remove(id)
                    super.remove(w)
                }
            }

            if (!itemsRemoved.any()) return@runOnUiThread

            sort(customComparator)

            refresh()
        }
    }

    fun updateQtyCollected(itemId: Long, qtyCollected: Double) {
        for (i in 0 until count) {
            val t = (getItem(i) as PtlContent)
            if (t.itemId != itemId) continue

            // ¿La cantidad es mayor a la cantidad solicitada?
            if (t.qtyRequested >= qtyCollected) {
                activity.runOnUiThread {
                    t.qtyCollected = qtyCollected
                }
                // Mostrar un cartelito, si jode sacarlo.
                reportQtyCollectedChange(t)
            } else {
                // Mostrar un cartelito, si jode sacarlo.
                reportQtyRequestedReached(t)
            }

            dataSetChangedListener?.onDataSetChanged()

            val item = getItem(getIndex(itemId)) ?: break

            forceSelectItem(item)
            break
        }
    }

    private fun reportQtyCollectedChange(content: PtlContent) {
        val itemCode = content.item.firstOrNull()?.ean ?: context.getString(R.string.no_ean)
        val decimalPlaces = 0

        val res = "${itemCode}: ${Statics.roundToString(content.qtyRequested, decimalPlaces)}"

        MakeText.makeText(listView ?: return, res, SnackBarType.ADD)
        Log.d(this::class.java.simpleName, res)
    }

    private fun reportQtyRequestedReached(content: PtlContent) {
        val itemCode = content.item.firstOrNull()?.ean ?: context.getString(R.string.no_ean)

        val res = "${itemCode}: ${context.getString(R.string.maximum_reached)}"

        MakeText.makeText(listView ?: return, res, SnackBarType.UPDATE)
        Log.d(this::class.java.simpleName, res)
    }

    private fun getIndex(item: PtlContent): Int {
        for (i in 0 until count) {
            val t = (getItem(i) as PtlContent)
            if (t == item) {
                return i
            }
        }
        return -1
    }

    private fun getIndex(itemId: Long): Int {
        for (i in 0 until count) {
            val t = (getItem(i) as PtlContent)
            if (t.itemId == itemId) {
                return i
            }
        }
        return -1
    }

    fun count(): Int {
        return count
    }

    @Suppress("unused")
    fun qtyRequestedTotal() = getAll().sumOf { it.qtyRequested }

    @Suppress("unused")
    fun qtyCollectedTotal() = getAll().sumOf { it.qtyCollected }

    fun getAll(): ArrayList<PtlContent> {
        val r: ArrayList<PtlContent> = ArrayList()
        for (i in 0 until count) {
            r.add(getItem(i) as PtlContent)
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

    fun getAllChecked(): ArrayList<PtlContent> {
        val r: ArrayList<PtlContent> = ArrayList()
        for (a in getAll()) {
            val id = a.item.first().id
            if (checkedIdArray.contains(id)) {
                r.add(a)
            }
        }
        return r
    }

    private var isFilling = false
    fun setChecked(items: ArrayList<PtlContent>, isChecked: Boolean) {
        if (isFilling) return
        isFilling = true

        for (i in items) {
            setChecked(i, isChecked)
        }

        isFilling = false
        refresh()
    }

    fun setChecked(item: PtlContent, isChecked: Boolean, suspendRefresh: Boolean = false) {
        val position = getIndex(item)
        val id = item.item.first().id
        if (isChecked) {
            if (!checkedIdArray.contains(id)) {
                checkedIdArray.add(id)
            }
        } else {
            checkedIdArray.remove(id)
        }

        if (checkedChangedListener != null) {
            checkedChangedListener!!.onCheckedChanged(isChecked, position)
        }

        if (!suspendRefresh) {
            refresh()
        }
    }

    fun setChecked(checkedItems: ArrayList<PtlContent>) {
        checkedItems.clear()
        setChecked(checkedItems, true)
    }

    private fun clearChecked() {
        checkedIdArray.clear()
    }

    override fun sort(comparator: Comparator<in PtlContent>) {
        super.sort(customComparator)
    }

    private val customComparator = Comparator { o1: PtlContent?, o2: PtlContent? ->
        PtlItemComparator().compareNullable(o1, o2)
    }

    private fun refresh() {
        activity.runOnUiThread { notifyDataSetChanged() }
        dataSetChangedListener?.onDataSetChanged()
    }

    fun setSelectItemAndScrollPos(a: PtlContent?, tScrollPos: Int?) {
        var pos = -1
        if (a != null) pos = getPosition(a)
        var scrollPos = -1
        if (tScrollPos != null) scrollPos = tScrollPos
        selectItem(pos, scrollPos, false)
    }

    private fun forceSelectItem(a: PtlContent) {
        if (listView == null) return
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
    }

    fun selectItem(a: PtlContent?) {
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

    fun currentContent(): PtlContent? {
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
        return position >= 0 && listView!!.isItemChecked(position)
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

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        var alreadyExists = true

        // Seleccionamos el layout dependiendo si es un row expandido o normal.
        // Los rows expandidos son los que están seleccionados.
        var currentLayout = resource
        if (isSelected(position)) {
            currentLayout = R.layout.ptl_item_row_expanded
        }

        if (v == null || v.tag == null) {
            // El view todavía no fue creado, crearlo con el layout correspondiente.
            val vi = LayoutInflater.from(context)
            v = vi.inflate(currentLayout, parent, false)

            alreadyExists = false
        } else {
            // El view ya existe, comprobar que no necesite cambiar de layout.
            if (v.tag is ViewHolder && currentLayout == R.layout.ptl_item_row_expanded || v.tag is ExpandedViewHolder && currentLayout == resource) {
                // Ya fue creado, si es un row normal que está siendo seleccionada
                // o un row expandido que está siendo deseleccionado
                // debe cambiar de layout, por lo tanto volver a crearse.
                val vi = LayoutInflater.from(context)
                v = vi.inflate(currentLayout, parent, false)

                alreadyExists = false
            }
        }

        v = if (currentLayout == R.layout.ptl_item_row_expanded) {
            fillSelectedItemView(position, v!!, alreadyExists)
        } else {
            fillListView(position, v!!, alreadyExists)
        }

        return v
    }

    private fun createExpandedViewHolder(v: View, holder: ExpandedViewHolder) {
        // Holder para los rows expandidos
        holder.descriptionTextView = v.findViewById(R.id.descriptionTextView)
        holder.eanTextView = v.findViewById(R.id.eanTextView)
        holder.itemIdTextView = v.findViewById(R.id.itemIdCheckedTextView)
        holder.extIdTextView = v.findViewById(R.id.extIdCheckedTextView)
        holder.lotIdTextView = v.findViewById(R.id.lotIdCheckedTextView)

        holder.itemIdLabelTextView = v.findViewById(R.id.itemIdLabelTextView)
        holder.extIdLabelTextView = v.findViewById(R.id.extIdLabelTextView)
        holder.lotIdLabelTextView = v.findViewById(R.id.lotIdLabelTextView)
        holder.extId2LabelTextView = v.findViewById(R.id.extId2LabelTextView)
        holder.priceLabelTextView = v.findViewById(R.id.priceLabelTextView)

        holder.qtyPanel = v.findViewById(R.id.qtyPanel)
        holder.qtyReqPanel = v.findViewById(R.id.qtyReqPanel)
        holder.qtyReqTitle = v.findViewById(R.id.qtyReqTitleTextView)
        holder.qtyTitle = v.findViewById(R.id.qtyTitleTextView)
        holder.eanDivider = v.findViewById(R.id.eanDivider)

        holder.qtyCollectedTextView = v.findViewById(R.id.qtyCollectedTextView)
        holder.qtyRequestedTextView = v.findViewById(R.id.qtyRequestedTextView)

        holder.extId2TextView = v.findViewById(R.id.extId2TextView)
        holder.priceTextView = v.findViewById(R.id.priceTextView)

        holder.checkBox = v.findViewById(R.id.checkBox)
        holder.editQty = v.findViewById(R.id.editQty)
        holder.editDescription = v.findViewById(R.id.editDescription)

        if (allowEditQty) {
            holder.editQty!!.visibility = View.VISIBLE
        } else {
            holder.editQty!!.visibility = View.GONE
        }

        if (multiSelect) {
            holder.checkBox?.visibility = View.VISIBLE
        } else {
            holder.checkBox?.visibility = View.GONE
        }

        if (showQtyPanel) {
            holder.qtyReqPanel!!.visibility = View.VISIBLE
            holder.eanDivider!!.visibility = View.VISIBLE
        } else {
            holder.qtyReqPanel!!.visibility = View.GONE
            holder.eanDivider!!.visibility = View.GONE
        }

        holder.extId2ConstraintLayout = v.findViewById(R.id.extId2ConstraintLayout)

        v.tag = holder
    }

    @SuppressLint("ClickableViewAccessibility", "ObsoleteSdkInt")
    private fun fillSelectedItemView(position: Int, v: View, alreadyExists: Boolean): View {
        var holder = ExpandedViewHolder()
        if (alreadyExists) {
            if (v.tag is ViewHolder) {
                createExpandedViewHolder(v, holder)
            } else {
                holder = v.tag as ExpandedViewHolder
            }
        } else {
            createExpandedViewHolder(v, holder)
        }

        if (position >= 0) {
            val content = getItem(position)
            if (content != null) {
                val isSelected = isSelected(position)
                val item = content.item.first()

                holder.descriptionTextView?.text = item.description
                holder.eanTextView?.text = item.ean
                holder.qtyCollectedTextView?.text = content.qtyCollected.toString()
                holder.qtyRequestedTextView?.text = content.qtyRequested.toString()

                holder.itemIdTextView?.text = item.id.toString()
                holder.extIdTextView?.text = content.externalId ?: 0.toString()
                holder.lotIdTextView?.text = content.lotId.toString()

                holder.extId2ConstraintLayout?.visibility = View.VISIBLE
                holder.extId2TextView?.text = item.externalId2.toString()
                holder.priceTextView?.text =
                    String.format("$ %s", Statics.roundToString(item.price, 2))

                if (holder.checkBox != null) {
                    var isSpeakButtonLongPressed = false

                    val checkChangeListener =
                        CompoundButton.OnCheckedChangeListener { _, isChecked ->
                            this.setChecked(
                                content, content.qtyRequested > 0, isChecked
                            )
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
                    holder.checkBox?.isChecked = checkedIdArray.contains(item.id)

                    holder.checkBox?.tag = position
                    holder.checkBox?.setOnLongClickListener(pressHoldListener)
                    holder.checkBox?.setOnTouchListener(pressTouchListener)
                    holder.checkBox?.setOnCheckedChangeListener(checkChangeListener)
                }

                if (holder.editQty != null) {
                    holder.editQty!!.setOnTouchListener { _, _ ->
                        if (editQtyListener != null) {
                            editQtyListener!!.onEditQtyRequired(
                                position = position,
                                item = content,
                                initialQty = content.qtyCollected,
                                minValue = 0.0,
                                maxValue = 999999.0,
                                multiplier = settingViewModel.scanMultiplier
                            )
                        }
                        true
                    }
                }

                // Background layouts                
                val collQtyEqualBackColor = ResourcesCompat.getDrawable(
                    context.resources, R.drawable.layout_thin_border_green, null
                )!!
                val collQtyMoreBackColor = ResourcesCompat.getDrawable(
                    context.resources, R.drawable.layout_thin_border_blue, null
                )!!
                val collQtyLessBackColor = ResourcesCompat.getDrawable(
                    context.resources, R.drawable.layout_thin_border_red, null
                )!!

                val backColor: Drawable
                val foreColor: Int
                when {
                    content.qtyCollected == content.qtyRequested -> {
                        backColor = collQtyEqualBackColor
                        foreColor = if (isSelected) selectedForeColor else selectedForeColor
                    }
                    content.qtyCollected > content.qtyRequested -> {
                        backColor = collQtyMoreBackColor
                        foreColor = if (isSelected) selectedForeColor else selectedForeColor
                    }
                    else -> {
                        backColor = collQtyLessBackColor
                        foreColor = if (isSelected) selectedForeColor else selectedForeColor
                    }
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
                holder.extId2TextView?.setTextColor(foreColor)
                holder.priceTextView?.setTextColor(foreColor)
                holder.qtyCollectedTextView?.setTextColor(foreColor)
                holder.qtyRequestedTextView?.setTextColor(foreColor)
                holder.itemIdTextView?.setTextColor(foreColor)
                holder.extIdTextView?.setTextColor(foreColor)
                holder.lotIdTextView?.setTextColor(foreColor)
                holder.checkBox?.buttonTintList = ColorStateList.valueOf(titleForeColor)

                holder.qtyReqTitleTextView?.setTextColor(titleForeColor)
                holder.qtyTitleTextView?.setTextColor(titleForeColor)
                holder.itemIdLabelTextView?.setTextColor(titleForeColor)
                holder.extIdLabelTextView?.setTextColor(titleForeColor)
                holder.lotIdLabelTextView?.setTextColor(titleForeColor)
                holder.extId2LabelTextView?.setTextColor(titleForeColor)
                holder.priceLabelTextView?.setTextColor(titleForeColor)
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

    private fun createViewHolder(v: View, holder: ViewHolder) {
        // Holder para los rows normales.
        holder.descriptionTextView = v.findViewById(R.id.descriptionTextView)
        holder.eanTextView = v.findViewById(R.id.eanTextView)

        holder.qtyReqTitleTextView = v.findViewById(R.id.qtyReqTitleTextView)
        holder.qtyTitleTextView = v.findViewById(R.id.qtyTitleTextView)

        holder.qtyPanel = v.findViewById(R.id.qtyPanel)
        holder.qtyReqPanel = v.findViewById(R.id.qtyReqPanel)
        holder.eanDivider = v.findViewById(R.id.eanDivider)

        holder.qtyCollectedTextView = v.findViewById(R.id.qtyCollectedTextView)
        holder.qtyRequestedTextView = v.findViewById(R.id.qtyRequestedTextView)

        holder.checkBox = v.findViewById(R.id.checkBox)

        if (multiSelect) {
            holder.checkBox?.visibility = View.VISIBLE
        } else {
            holder.checkBox?.visibility = View.GONE
        }

        if (showQtyPanel) {
            holder.qtyReqPanel!!.visibility = View.VISIBLE
            holder.eanDivider!!.visibility = View.VISIBLE
        } else {
            holder.qtyReqPanel!!.visibility = View.GONE
            holder.eanDivider!!.visibility = View.GONE
        }

        v.tag = holder
    }

    @SuppressLint("ObsoleteSdkInt", "ClickableViewAccessibility")
    private fun fillListView(position: Int, v: View, alreadyExists: Boolean): View {
        var holder = ViewHolder()
        if (alreadyExists) {
            if (v.tag is ExpandedViewHolder) {
                createViewHolder(v, holder)
            } else {
                holder = v.tag as ViewHolder
            }
        } else {
            createViewHolder(v, holder)
        }

        if (position >= 0) {
            val isSelected = isSelected(position)
            val content = getItem(position)
            if (content != null) {
                val item = content.item.first()

                holder.descriptionTextView?.text = item.description
                holder.eanTextView?.text = item.ean
                holder.qtyCollectedTextView?.text = content.qtyCollected.toString()
                holder.qtyRequestedTextView?.text = content.qtyRequested.toString()

                if (holder.checkBox != null) {
                    var isSpeakButtonLongPressed = false

                    val checkChangeListener =
                        CompoundButton.OnCheckedChangeListener { _, isChecked ->
                            this.setChecked(
                                content, content.qtyRequested > 0, isChecked
                            )
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
                    holder.checkBox?.isChecked = checkedIdArray.contains(item.id)

                    holder.checkBox?.tag = position
                    holder.checkBox?.setOnLongClickListener(pressHoldListener)
                    holder.checkBox?.setOnTouchListener(pressTouchListener)
                    holder.checkBox?.setOnCheckedChangeListener(checkChangeListener)
                }

                // region Background layouts
                val collQtyEqualBackColor = ResourcesCompat.getDrawable(
                    context.resources, R.drawable.layout_thin_border_green, null
                )!!
                val collQtyMoreBackColor = ResourcesCompat.getDrawable(
                    context.resources, R.drawable.layout_thin_border_blue, null
                )!!
                val collQtyLessBackColor = ResourcesCompat.getDrawable(
                    context.resources, R.drawable.layout_thin_border_red, null
                )!!

                val backColor: Drawable
                val foreColor: Int
                when {
                    content.qtyCollected == content.qtyRequested -> {
                        backColor = collQtyEqualBackColor
                        foreColor = if (isSelected) selectedForeColor else selectedForeColor
                    }
                    content.qtyCollected > content.qtyRequested -> {
                        backColor = collQtyMoreBackColor
                        foreColor = if (isSelected) selectedForeColor else selectedForeColor
                    }
                    else -> {
                        backColor = collQtyLessBackColor
                        foreColor = if (isSelected) selectedForeColor else selectedForeColor
                    }
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
                holder.qtyCollectedTextView?.setTextColor(foreColor)
                holder.qtyRequestedTextView?.setTextColor(foreColor)
                holder.checkBox?.buttonTintList = ColorStateList.valueOf(titleForeColor)

                holder.qtyReqTitleTextView?.setTextColor(titleForeColor)
                holder.qtyTitleTextView?.setTextColor(titleForeColor)
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

    // region COLORS
    private var selectedForeColor: Int = 0
    private var collQtyEqualForeColor: Int = 0
    private var collQtyLessForeColor: Int = 0
    private var collQtyMoreForeColor: Int = 0
    private var defaultForeColor: Int = 0

    private fun setupColors() {
        selectedForeColor = ResourcesCompat.getColor(context.resources, R.color.text_light, null)

        collQtyEqualForeColor = Colors.getBestContrastColor("#FF009688")
        collQtyLessForeColor = Colors.getBestContrastColor("#FFE91E63")
        collQtyMoreForeColor = Colors.getBestContrastColor("#2196F3")
        defaultForeColor = Colors.getBestContrastColor("#DFDFDF")
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val r: ArrayList<PtlContent> = ArrayList()

                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.ROOT)
                    var filterableItem: PtlItem

                    for (i in 0 until itemArray.size) {
                        filterableItem = itemArray[i].item.first()
                        filterableItem.description.lowercase(Locale.ROOT)
                            .contains(filterString) || filterableItem.ean.lowercase(
                            Locale.ROOT
                        ).contains(filterString)
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
                suggestedList.addAll(results?.values as ArrayList<PtlContent>)
                if (results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    internal inner class ViewHolder {
        var descriptionTextView: AutoResizeTextView? = null
        var eanTextView: AutoResizeTextView? = null

        var qtyReqTitleTextView: TextView? = null
        var qtyTitleTextView: TextView? = null

        var qtyPanel: ConstraintLayout? = null
        var qtyReqPanel: ConstraintLayout? = null
        var eanDivider: View? = null

        var qtyCollectedTextView: AutoResizeTextView? = null
        var qtyRequestedTextView: AutoResizeTextView? = null

        var checkBox: CheckBox? = null
    }

    internal inner class ExpandedViewHolder {
        var descriptionTextView: AutoResizeTextView? = null
        var eanTextView: AutoResizeTextView? = null
        var itemIdTextView: CheckedTextView? = null
        var extIdTextView: CheckedTextView? = null
        var lotIdTextView: CheckedTextView? = null

        var qtyReqTitleTextView: TextView? = null
        var qtyTitleTextView: TextView? = null
        var itemIdLabelTextView: TextView? = null
        var extIdLabelTextView: TextView? = null
        var lotIdLabelTextView: TextView? = null
        var extId2LabelTextView: TextView? = null
        var priceLabelTextView: TextView? = null

        var qtyPanel: ConstraintLayout? = null
        var qtyReqPanel: ConstraintLayout? = null
        var qtyReqTitle: TextView? = null
        var qtyTitle: TextView? = null
        var eanDivider: View? = null

        var qtyCollectedTextView: AutoResizeTextView? = null
        var qtyRequestedTextView: AutoResizeTextView? = null

        var checkBox: CheckBox? = null
        var editQty: AppCompatImageView? = null
        var editDescription: AppCompatImageView? = null

        var extId2ConstraintLayout: ConstraintLayout? = null
        var extId2TextView: AutoResizeTextView? = null
        var priceTextView: AutoResizeTextView? = null
    }

    companion object {
        class PtlItemComparator : Comparator<PtlContent> {
            fun compareNullable(o1: PtlContent?, o2: PtlContent?): Int {
                return if (o1 == null || o2 == null) {
                    -1
                } else {
                    compare(o1, o2)
                }
            }

            override fun compare(o1: PtlContent, o2: PtlContent): Int {
                return try {
                    val item1 = o1.item.first()
                    val item2 = o2.item.first()
                    val eanComp = item1.ean.compareTo(item2.ean, true)
                    val descriptionComp = item1.description.compareTo(item2.description, true)

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

        fun sortItems(originalList: ArrayList<PtlContent>): ArrayList<PtlContent> {
            // Get all of the parent groups
            return ArrayList(
                originalList.sortedWith(
                    compareBy({ it.item.first().description },
                        { it.item.first().ean })
                )
            )
        }
    }
}