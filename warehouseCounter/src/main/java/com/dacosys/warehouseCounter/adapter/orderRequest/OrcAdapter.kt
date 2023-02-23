package com.dacosys.warehouseCounter.adapter.orderRequest

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.model.orderRequest.Item
import com.dacosys.warehouseCounter.model.orderRequest.OrderRequestContent
import com.dacosys.warehouseCounter.model.orderRequest.OrderRequestType
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.views.AutoResizeTextView
import java.lang.ref.WeakReference
import java.util.*


/**
 * Created by Agustin on 18/01/2017.
 */

class OrcAdapter : ArrayAdapter<OrderRequestContent>, Filterable {
    private var activity: AppCompatActivity
    private var resource: Int = 0
    private var orcArray: ArrayList<OrderRequestContent> = ArrayList()
    private var suggestedList: ArrayList<OrderRequestContent> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()

    private var lastSelectedPos = -1

    private var multiSelect: Boolean = false
    private var orType: OrderRequestType? = null
    private var allowEditQty: Boolean = false
    private var setQtyOnCheckedChanged: Boolean = false

    private var dataSetChangedListener: DataSetChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null
    private var editQtyListener: EditQtyListener? = null
    private var editDescriptionListener: EditDescriptionListener? = null

    constructor(
        activity: AppCompatActivity,
        resource: Int,
        orcs: ArrayList<OrderRequestContent>,
        listView: ListView?,
        checkedIdArray: ArrayList<Int>,
        multiSelect: Boolean,
        allowEditQty: Boolean,
        orType: OrderRequestType,
        setQtyOnCheckedChanged: Boolean,
    ) : super(context(), resource, orcs) {
        this.resource = resource
        this.activity = activity

        this.orType = orType
        this.multiSelect = multiSelect
        this.allowEditQty = allowEditQty
        this.setQtyOnCheckedChanged = setQtyOnCheckedChanged

        this.orcArray = orcs

        this.checkedIdArray.clear()
        for (c in checkedIdArray) {
            this.checkedIdArray.add(c.toLong())
        }

        this.listView = listView

        setupColors()
    }

    fun refreshListeners(
        checkedChangedListener: CheckedChangedListener?,
        dataSetChangedListener: DataSetChangedListener?,
        editQtyListener: EditQtyListener?,
        editDescriptionListener: EditDescriptionListener?,
    ) {
        this.editQtyListener = editQtyListener
        this.checkedChangedListener = checkedChangedListener
        this.dataSetChangedListener = dataSetChangedListener
        this.editDescriptionListener = editDescriptionListener
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
            orc: OrderRequestContent,
            initialQty: Double,
            minValue: Double,
            maxValue: Double,
            multiplier: Int,
        )
    }

    interface EditDescriptionListener {
        fun onEditDescriptionRequired(
            position: Int,
            orc: OrderRequestContent,
        )
    }

    override fun add(orc: OrderRequestContent?) {
        var someChanges = false

        activity.runOnUiThread {
            if (orc != null) {
                if (!getAll().contains(orc)) {
                    someChanges = true
                    super.add(orc)
                }
            }

            if (!someChanges) return@runOnUiThread

            refresh()

            if (orc != null) forceSelectItem(orc)
        }
    }

    fun add(orcs: ArrayList<OrderRequestContent>) {
        val orcsAdded: ArrayList<OrderRequestContent> = ArrayList()

        activity.runOnUiThread {
            for (orc in orcs) {
                if (!getAll().contains(orc)) {
                    orcsAdded.add(orc)
                    super.add(orc)
                }
            }

            if (!orcsAdded.any()) return@runOnUiThread

            reportOrcAdded(orcsAdded)

            sort(customComparator)
        }
    }

    override fun clear() {
        activity.runOnUiThread {
            super.clear()
            clearChecked()
        }
    }

    override fun remove(orc: OrderRequestContent?) {
        if (orc != null) {
            remove(arrayListOf(orc))
        }
    }

    fun remove(orcs: ArrayList<OrderRequestContent>) {
        val orcsRemoved: ArrayList<OrderRequestContent> = ArrayList()
        activity.runOnUiThread {
            for (w in orcs) {
                if (getAll().contains(w)) {
                    orcsRemoved.add(w)
                    checkedIdArray.remove(w.item?.itemId ?: 0)
                    super.remove(w)
                }
            }

            if (!orcsRemoved.any()) return@runOnUiThread

            reportOrcRemoved(orcsRemoved)

            sort(customComparator)

            refresh()
        }
    }

    fun updateDescription(itemId: Long, desc: String) {
        for (i in 0 until count) {
            val t = (getItem(i) as OrderRequestContent)

            if (t.item == null) continue

            if (t.item?.itemId == itemId && t.item?.itemDescription != desc) {
                t.item?.itemDescription = desc

                refresh()

                forceSelectItem(t)

                break
            }
        }
    }

    fun updateDescription(item: Item, desc: String) {
        for (i in 0 until count) {
            val t = (getItem(i) as OrderRequestContent)

            if (t.item == null) continue

            if (t.item == item && t.item?.itemDescription != desc) {
                t.item?.itemDescription = desc

                refresh()

                forceSelectItem(t)

                break
            }
        }
    }

    fun updateQtyCollected(orc: OrderRequestContent, qty: Double) {
        for (i in 0 until count) {
            val t = (getItem(i) as OrderRequestContent)

            if (t.qty == null) continue

            if (t == orc) {
                val qtyRequested = t.qty!!.qtyRequested ?: 0.toDouble()

                t.qty!!.qtyCollected = qty
                // ¿La cantidad es mayor a la cantidad solicitada?
                if (qtyRequested >= qty) {
                    // Mostrar un cartelito, si jode sacarlo.
                    reportOrcQtyCollectedChange(t)
                } else if (qtyRequested > 0) {
                    if (qtyRequested == qty) {
                        reportOrcQtyRequestedReached(t)
                    } else {
                        reportOrcQtyRequestedExceeded(t)
                    }
                }

                refresh()

                forceSelectItem(orc)

                break
            }
        }
    }

    private fun reportOrcQtyRequestedExceeded(orc: OrderRequestContent) {
        if (suspendReport) return
        if (orc.item == null) return
        val listView = listView ?: return

        val res = "${orc.item!!.ean}: ${
            context().getString(R.string.qty_requested_exceeded)
        }"
        makeText(listView, res, SnackBarType.UPDATE)
        Log.d(this::class.java.simpleName, res)
    }

    private fun reportOrcQtyRequestedReached(orc: OrderRequestContent) {
        if (suspendReport) return
        if (orc.item == null) return
        val listView = listView ?: return

        val res = "${orc.item!!.ean}: ${
            context().getString(R.string.qty_requested_reached)
        }"
        makeText(listView, res, SnackBarType.UPDATE)
        Log.d(this::class.java.simpleName, res)
    }

    private fun reportOrcQtyCollectedChange(orc: OrderRequestContent) {
        if (suspendReport) return
        if (orc.item == null || orc.qty == null) return
        val listView = listView ?: return

        val res = "${orc.item!!.ean}: ${
            Statics.roundToString(orc.qty!!.qtyCollected ?: 0.toDouble(), Statics.decimalPlaces)
        }"
        makeText(listView, res, SnackBarType.ADD)
        Log.d(this::class.java.simpleName, res)
    }

    /**
     * Muestra un mensaje en pantalla con los códigos agregados
     */
    private fun reportOrcAdded(orcArray: ArrayList<OrderRequestContent>) {
        if (suspendReport) return
        if (orcArray.isEmpty()) return
        val listView = listView ?: return

        var res = ""
        for (orc in orcArray) {
            if (orc.item != null) {
                res += "${orc.item!!.ean}, "
            }
        }

        if (res.endsWith(", ")) {
            res = res.substring(0, res.length - 2)
        }

        res += ": " + if (orcArray.count() > 1) " ${
            context().getString(R.string.added_plural)
        }" else " ${context().getString(R.string.added)}"

        makeText(listView, res, SnackBarType.ADD)
        Log.d(this::class.java.simpleName, res)
    }

    /**
     * Muestra un mensaje en pantalla con los códigos eliminados
     */
    private fun reportOrcRemoved(orcArray: ArrayList<OrderRequestContent>) {
        if (suspendReport) return
        if (orcArray.isEmpty()) return
        val listView = listView ?: return

        var res = ""
        for (orc in orcArray) {
            if (orc.item != null) {
                res += "${orc.item!!.ean}, "
            }
        }

        if (res.endsWith(", ")) {
            res = res.substring(0, res.length - 2)
        }

        res += ": " + if (orcArray.count() > 1) " ${
            context().getString(R.string.removed_plural)
        }" else " ${context().getString(R.string.removed)}"

        makeText(listView, res, SnackBarType.REMOVE)
        Log.d(this::class.java.simpleName, res)
    }

    private fun getIndex(orc: OrderRequestContent): Int {
        for (i in 0 until count) {
            val t = (getItem(i) as OrderRequestContent)
            if (t == orc) {
                return i
            }
        }
        return -1
    }

    fun count(): Int {
        return count
    }

    fun qtyRequestedTotal(): Double = getAll().sumOf { it.qty?.qtyRequested ?: 0.toDouble() }

    fun qtyCollectedTotal(): Double = getAll().sumOf { it.qty?.qtyCollected ?: 0.toDouble() }

    fun totalRequested(): Double {
        var total = 0.toDouble()
        for (i in 0 until count) {
            val t = (getItem(i) as OrderRequestContent)
            if (t.item != null && t.qty != null) {
                total += t.qty!!.qtyRequested ?: 0.toDouble()
            }
        }

        return total
    }

    fun totalCollected(): Double {
        var total = 0.toDouble()
        for (i in 0 until count) {
            val t = (getItem(i) as OrderRequestContent)
            if (t.item != null && t.qty != null) {
                total += t.qty!!.qtyCollected ?: 0.toDouble()
            }
        }

        return total
    }

    fun getAll(): ArrayList<OrderRequestContent> {
        val r: ArrayList<OrderRequestContent> = ArrayList()
        for (i in 0 until count) {
            r.add(getItem(i) as OrderRequestContent)
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

    fun getAllChecked(): ArrayList<OrderRequestContent> {
        val r: ArrayList<OrderRequestContent> = ArrayList()
        for (a in getAll()) {
            if (checkedIdArray.contains(a.item?.itemId ?: 0)) {
                r.add(a)
            }
        }
        return r
    }

    private var isFilling = false
    fun setChecked(items: ArrayList<OrderRequestContent>, isChecked: Boolean) {
        if (isFilling) return
        isFilling = true

        for (i in items) {
            setChecked(i, isChecked)
        }

        isFilling = false
        refresh()
    }

    fun setChecked(item: OrderRequestContent, isChecked: Boolean, suspendRefresh: Boolean = false) {
        val position = getIndex(item)
        if (isChecked) {
            if (!checkedIdArray.contains(item.item?.itemId ?: 0)) {
                checkedIdArray.add(item.item?.itemId ?: 0)
            }
        } else {
            checkedIdArray.remove(item.item?.itemId ?: 0)
        }

        if (checkedChangedListener != null) {
            checkedChangedListener!!.onCheckedChanged(isChecked, position)
        }

        if (!suspendRefresh) {
            refresh()
        }
    }

    fun setChecked(checkedItems: ArrayList<OrderRequestContent>) {
        checkedItems.clear()
        setChecked(checkedItems, true)
    }

    private fun clearChecked() {
        checkedIdArray.clear()
    }

    override fun sort(comparator: Comparator<in OrderRequestContent>) {
        super.sort(customComparator)
    }

    private val customComparator =
        Comparator { o1: OrderRequestContent?, o2: OrderRequestContent? ->
            OrcComparator().compareNullable(o1, o2)
        }

    private fun refresh() {
        activity.runOnUiThread { notifyDataSetChanged() }
        dataSetChangedListener?.onDataSetChanged()
    }

    fun setSelectItemAndScrollPos(a: OrderRequestContent?, tScrollPos: Int?) {
        var pos = -1
        if (a != null) pos = getPosition(a)
        var scrollPos = -1
        if (tScrollPos != null) scrollPos = tScrollPos
        selectItem(pos, scrollPos, false)
    }

    private fun forceSelectItem(a: OrderRequestContent) {
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

    fun selectItem(a: OrderRequestContent?) {
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

    fun currentOrc(): OrderRequestContent? {
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
            currentLayout = R.layout.orc_row_expanded
        }

        if (v == null || v.tag == null) {
            // El view todavía no fue creado, crearlo con el layout correspondiente.
            val vi = LayoutInflater.from(context)
            v = vi.inflate(currentLayout, parent, false)

            alreadyExists = false
        } else {
            // El view ya existe, comprobar que no necesite cambiar de layout.
            if (v.tag is ViewHolder && currentLayout == R.layout.orc_row_expanded || v.tag is ExpandedViewHolder && currentLayout == resource) {
                // Ya fue creado, si es un row normal que está siendo seleccionada
                // o un row expandido que está siendo deseleccionado
                // debe cambiar de layout, por lo tanto volver a crearse.
                val vi = LayoutInflater.from(context)
                v = vi.inflate(currentLayout, parent, false)

                alreadyExists = false
            }
        }

        v = if (currentLayout == R.layout.orc_row_expanded) {
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

        holder.qtyReqTitleTextView = v.findViewById(R.id.qtyReqTitleTextView)
        holder.qtyTitleTextView = v.findViewById(R.id.qtyTitleTextView)
        holder.itemIdLabelTextView = v.findViewById(R.id.itemIdLabelTextView)
        holder.extIdLabelTextView = v.findViewById(R.id.extIdLabelTextView)
        holder.lotIdLabelTextView = v.findViewById(R.id.lotIdLabelTextView)
        holder.categoryLabelTextView = v.findViewById(R.id.categoryLabelTextView)
        holder.priceLabelTextView = v.findViewById(R.id.priceLabelTextView)

        holder.qtyPanel = v.findViewById(R.id.qtyPanel)
        holder.qtyReqPanel = v.findViewById(R.id.qtyReqPanel)
        holder.qtyReqTitle = v.findViewById(R.id.qtyReqTitleTextView)
        holder.qtyTitle = v.findViewById(R.id.qtyTitleTextView)
        holder.eanDivider = v.findViewById(R.id.eanDivider)

        holder.qtyCollectedTextView = v.findViewById(R.id.qtyCollectedTextView)
        holder.qtyRequestedTextView = v.findViewById(R.id.qtyRequestedTextView)

        holder.categoryTextView = v.findViewById(R.id.categoryTextView)
        holder.priceTextView = v.findViewById(R.id.priceTextView)

        holder.checkBox = v.findViewById(R.id.checkBox)
        holder.editQty = v.findViewById(R.id.editQty)
        holder.editDescription = v.findViewById(R.id.editDescription)

        if (allowEditQty) {
            holder.editQty!!.visibility = VISIBLE
        } else {
            holder.editQty!!.visibility = GONE
        }

        if (editDescriptionListener != null) {
            holder.editDescription!!.visibility = VISIBLE
        } else {
            holder.editDescription!!.visibility = GONE
        }

        if (multiSelect) {
            holder.checkBox?.visibility = VISIBLE
        } else {
            holder.checkBox?.visibility = GONE
        }

        if (orType == OrderRequestType.stockAuditFromDevice) {
            holder.qtyReqPanel!!.visibility = GONE
            holder.eanDivider!!.visibility = GONE
        } else {
            holder.qtyReqPanel!!.visibility = VISIBLE
            holder.eanDivider!!.visibility = VISIBLE
        }

        holder.categoryConstraintLayout = v.findViewById(R.id.categoryConstraintLayout)

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
            val orc = getItem(position)
            val isSelected = isSelected(position)

            if (orc?.item != null) {
                val item = orc.item!!

                holder.descriptionTextView?.text = item.itemDescription
                holder.eanTextView?.text = item.ean
                if (orc.qty != null) {
                    val qty = orc.qty!!
                    holder.qtyCollectedTextView?.text =
                        Statics.roundToString(
                            qty.qtyCollected ?: 0.toDouble(),
                            Statics.decimalPlaces
                        )
                    holder.qtyRequestedTextView?.text =
                        Statics.roundToString(
                            qty.qtyRequested ?: 0.toDouble(),
                            Statics.decimalPlaces
                        )
                }

                holder.itemIdTextView?.text = item.itemId?.toString() ?: 0.toString()
                holder.extIdTextView?.text = item.externalId ?: 0.toString()
                holder.lotIdTextView?.text = orc.lot?.lotId.toString()

                // region Category
                val category = item.itemCategoryStr
                val price = item.price

                if (category.isEmpty() && price == null) {
                    holder.categoryConstraintLayout?.visibility = GONE
                } else {
                    holder.categoryConstraintLayout?.visibility = VISIBLE
                    holder.categoryTextView?.text = category
                    holder.priceTextView?.text =
                        String.format("$ %s", Statics.roundToString(price!!, 2))
                }
                // endregion

                if (holder.checkBox != null) {
                    var isSpeakButtonLongPressed = false

                    val checkChangeListener =
                        CompoundButton.OnCheckedChangeListener { _, isChecked ->
                            if (orc.qty != null) {
                                this.setChecked(
                                    orc,
                                    (orc.qty!!.qtyRequested ?: 0.toDouble()) > 0,
                                    isChecked
                                )
                            }
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
                    holder.checkBox?.isChecked = checkedIdArray.contains(item.itemId)

                    holder.checkBox?.tag = position
                    holder.checkBox?.setOnLongClickListener(pressHoldListener)
                    holder.checkBox?.setOnTouchListener(pressTouchListener)
                    holder.checkBox?.setOnCheckedChangeListener(checkChangeListener)
                }

                if (holder.editQty != null) {
                    holder.editQty!!.setOnTouchListener { _, _ ->
                        if (editQtyListener != null) {
                            if (orc.qty != null) {
                                editQtyListener!!.onEditQtyRequired(
                                    position,
                                    orc,
                                    orc.qty!!.qtyCollected ?: 0.toDouble(),
                                    0.toDouble(),
                                    999999.toDouble(),
                                    settingViewModel().scanMultiplier
                                )
                            }
                        }
                        true
                    }
                }

                if (item.itemId!! > 0) {
                    holder.editDescription?.visibility = GONE
                }
                holder.editDescription?.setOnTouchListener { _, _ ->
                    if (editDescriptionListener != null) {
                        if (orc.item != null) {
                            editDescriptionListener!!.onEditDescriptionRequired(position, orc)
                        }
                    }
                    true
                }

                // Background layouts
                val colorDefault =
                    getDrawable(context().resources, R.drawable.layout_thin_border, null)!!
                val collQtyEqualBackColor =
                    getDrawable(context().resources, R.drawable.layout_thin_border_green, null)!!
                val collQtyMoreBackColor =
                    getDrawable(context().resources, R.drawable.layout_thin_border_blue, null)!!
                val collQtyLessBackColor =
                    getDrawable(context().resources, R.drawable.layout_thin_border_red, null)!!

                var backColor = colorDefault
                var foreColor = if (isSelected) selectedForeColor else defaultForeColor

                if (orc.qty != null) {
                    val qty = orc.qty!!
                    if (qty.qtyRequested != null && qty.qtyCollected != null) {

                        when {
                            qty.qtyCollected == qty.qtyRequested -> {
                                backColor = collQtyEqualBackColor
                                foreColor = if (isSelected) selectedForeColor else selectedForeColor
                            }
                            (qty.qtyCollected ?: 0.0) > (qty.qtyRequested ?: 0.0) -> {
                                backColor = collQtyMoreBackColor
                                foreColor = if (isSelected) selectedForeColor else selectedForeColor
                            }
                            (qty.qtyCollected ?: 0.0) < (qty.qtyRequested ?: 0.0) -> {
                                backColor = collQtyLessBackColor
                                foreColor = if (isSelected) selectedForeColor else selectedForeColor
                            }
                        }
                    }
                }

                val darkerColor = when {
                    isSelected -> true
                    foreColor == Statics.textLightColor() -> true
                    else -> false
                }

                val titleForeColor: Int =
                    Statics.manipulateColor(foreColor, if (darkerColor) 0.8f else 1.4f)

                v.background = backColor
                holder.descriptionTextView?.setTextColor(foreColor)
                holder.eanTextView?.setTextColor(foreColor)
                holder.categoryTextView?.setTextColor(foreColor)
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
                holder.categoryLabelTextView?.setTextColor(titleForeColor)
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
            holder.checkBox?.visibility = VISIBLE
        } else {
            holder.checkBox?.visibility = GONE
        }

        if (orType == OrderRequestType.stockAuditFromDevice) {
            holder.qtyReqPanel!!.visibility = GONE
            holder.eanDivider!!.visibility = GONE
        } else {
            holder.qtyReqPanel!!.visibility = VISIBLE
            holder.eanDivider!!.visibility = VISIBLE
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
            val orc = getItem(position)

            if (orc?.item != null) {
                val item = orc.item!!

                holder.descriptionTextView?.text = item.itemDescription
                holder.eanTextView?.text = item.ean
                if (orc.qty != null) {
                    val qty = orc.qty!!
                    holder.qtyCollectedTextView?.text =
                        Statics.roundToString(
                            qty.qtyCollected ?: 0.toDouble(),
                            Statics.decimalPlaces
                        )
                    holder.qtyRequestedTextView?.text =
                        Statics.roundToString(
                            qty.qtyRequested ?: 0.toDouble(),
                            Statics.decimalPlaces
                        )
                }


                if (holder.checkBox != null) {
                    var isSpeakButtonLongPressed = false

                    val checkChangeListener =
                        CompoundButton.OnCheckedChangeListener { _, isChecked ->
                            if (orc.qty != null) {
                                this.setChecked(
                                    orc,
                                    (orc.qty!!.qtyRequested ?: 0.toDouble()) > 0,
                                    isChecked
                                )
                            }
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
                    holder.checkBox?.isChecked = checkedIdArray.contains(item.itemId)

                    holder.checkBox?.tag = position
                    holder.checkBox?.setOnLongClickListener(pressHoldListener)
                    holder.checkBox?.setOnTouchListener(pressTouchListener)
                    holder.checkBox?.setOnCheckedChangeListener(checkChangeListener)
                }

                // region Background layouts
                val colorDefault =
                    getDrawable(context().resources, R.drawable.layout_thin_border, null)!!
                val collQtyEqualBackColor =
                    getDrawable(context().resources, R.drawable.layout_thin_border_green, null)!!
                val collQtyMoreBackColor =
                    getDrawable(context().resources, R.drawable.layout_thin_border_blue, null)!!
                val collQtyLessBackColor =
                    getDrawable(context().resources, R.drawable.layout_thin_border_red, null)!!

                var backColor = colorDefault
                var foreColor = if (isSelected) selectedForeColor else defaultForeColor

                if (orc.qty != null) {
                    val qty = orc.qty!!
                    if (qty.qtyRequested != null && qty.qtyCollected != null) {

                        when {
                            qty.qtyCollected == qty.qtyRequested -> {
                                backColor = collQtyEqualBackColor
                                foreColor = if (isSelected) selectedForeColor else selectedForeColor
                            }
                            (qty.qtyCollected ?: 0.0) > (qty.qtyRequested ?: 0.0) -> {
                                backColor = collQtyMoreBackColor
                                foreColor = if (isSelected) selectedForeColor else selectedForeColor
                            }
                            (qty.qtyCollected ?: 0.0) < (qty.qtyRequested ?: 0.0) -> {
                                backColor = collQtyLessBackColor
                                foreColor = if (isSelected) selectedForeColor else selectedForeColor
                            }
                        }
                    }
                }

                val darkerColor = when {
                    isSelected -> true
                    foreColor == Statics.textLightColor() -> true
                    else -> false
                }

                val titleForeColor: Int =
                    Statics.manipulateColor(foreColor, if (darkerColor) 0.8f else 1.4f)

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
        selectedForeColor = getColor(context().resources, R.color.text_light, null)

        collQtyEqualForeColor = Statics.getBestContrastColor("#FF009688")
        collQtyLessForeColor = Statics.getBestContrastColor("#FFE91E63")
        collQtyMoreForeColor = Statics.getBestContrastColor("#2196F3")
        defaultForeColor = Statics.getBestContrastColor("#DFDFDF")
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val r: ArrayList<OrderRequestContent> = ArrayList()

                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.ROOT)
                    var filterableItem: OrderRequestContent

                    for (i in 0 until orcArray.size) {
                        filterableItem = orcArray[i]
                        filterableItem.item!!.itemDescription.lowercase(Locale.ROOT)
                            .contains(filterString) || filterableItem.item != null && filterableItem.item!!.ean.lowercase(
                            Locale.ROOT
                        ).contains(filterString)
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
                suggestedList.addAll(results?.values as ArrayList<OrderRequestContent>)
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
        var categoryLabelTextView: TextView? = null
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

        var categoryConstraintLayout: ConstraintLayout? = null
        var categoryTextView: AutoResizeTextView? = null
        var priceTextView: AutoResizeTextView? = null
    }

    companion object {

        private var suspendReport = false

        class OrcComparator : Comparator<OrderRequestContent> {
            fun compareNullable(o1: OrderRequestContent?, o2: OrderRequestContent?): Int {
                return if (o1 == null || o2 == null) {
                    -1
                } else {
                    compare(o1, o2)
                }
            }

            override fun compare(o1: OrderRequestContent, o2: OrderRequestContent): Int {
                // Ordena los items por código, serialNumber y uniqueLotId
                // Se podrían agregar más columnas
                if (o1.item == null && o2.item == null) {
                    return 0
                } else if (o1.item == null) {
                    return 1
                } else if (o2.item == null) {
                    return -1
                }

                val item1 = o1.item!!
                val item2 = o2.item!!

                try {
                    val eanComp = item1.ean.compareTo(item2.ean)
                    val descComp = (item1.itemDescription).compareTo(item2.itemDescription)
                    val itemIdComp = (item1.itemId ?: 0).compareTo(item2.itemId ?: 0)

                    // Orden natural: code, serialNumber, uniqueLotId
                    val result = when (eanComp) {
                        0 -> when (descComp) {
                            0 -> itemIdComp
                            else -> descComp
                        }
                        else -> eanComp
                    }

                    Log.d(
                        this::class.java.simpleName, when {
                            result < 0 -> " < "
                            result > 0 -> " > "
                            else -> " = "
                        }
                    )

                    return result
                } catch (ex: Exception) {
                    return 0
                }
            }
        }
    }
}