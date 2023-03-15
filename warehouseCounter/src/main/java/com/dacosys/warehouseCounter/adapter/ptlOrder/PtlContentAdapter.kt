package com.dacosys.warehouseCounter.adapter.ptlOrder

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.dto.ptlOrder.PtlContent
import com.dacosys.warehouseCounter.dto.ptlOrder.PtlItem
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.MakeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.Colors
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.ui.views.AutoResizeTextView
import java.util.*


/**
 * Created by Agustin on 10/03/2023.
 */

class PtlContentAdapter(
    private val recyclerView: RecyclerView,
    val fullList: ArrayList<PtlContent>,
    var checkedIdArray: ArrayList<Long> = ArrayList(),
    var multiSelect: Boolean = false,
    var allowEditQty: Boolean = false,
    var showQtyPanel: Boolean = false,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

    private var currentIndex = RecyclerView.NO_POSITION
    private var dataSetChangedListener: DataSetChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null
    private var editQtyListener: EditQtyListener? = null

    var filteredList: ArrayList<PtlContent> = fullList
        @SuppressLint("NotifyDataSetChanged") set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun refreshListeners(
        checkedChangedListener: CheckedChangedListener? = null,
        dataSetChangedListener: DataSetChangedListener? = null,
        editQtyListener: EditQtyListener? = null,
    ) {
        this.editQtyListener = editQtyListener
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

    interface EditQtyListener {
        fun onEditQtyRequired(
            position: Int,
            content: PtlContent,
            initialQty: Double,
            minValue: Double,
            maxValue: Double,
            multiplier: Int,
        )
    }

    companion object {
        // Aquí definimos dos constantes para identificar los dos diseños diferentes
        const val SELECTED_VIEW_TYPE = 1
        const val UNSELECTED_VIEW_TYPE = 2

        // region COLORS
        private var selectedForeColor: Int = 0
        private var collQtyEqualForeColor: Int = 0
        private var collQtyLessForeColor: Int = 0
        private var collQtyMoreForeColor: Int = 0
        private var defaultForeColor: Int = 0

        private fun setupColors() {
            selectedForeColor =
                ResourcesCompat.getColor(context.resources, R.color.text_light, null)

            collQtyEqualForeColor = Colors.getBestContrastColor("#FF009688")
            collQtyLessForeColor = Colors.getBestContrastColor("#FFE91E63")
            collQtyMoreForeColor = Colors.getBestContrastColor("#FF2196F3")
            defaultForeColor = Colors.getBestContrastColor("#FFDFDFDF")
        }
        // endregion
    }

    // El método onCreateViewHolder infla los diseños para cada tipo de vista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            SELECTED_VIEW_TYPE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.ptl_item_row_expanded, parent, false)
                SelectedViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.ptl_item_row, parent, false)
                UnselectedViewHolder(view)
            }
        }
    }

    // El método onBindViewHolder establece los valores de las vistas en función de los datos
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Aquí puedes establecer los valores para cada elemento
        holder.itemView.setOnClickListener {

            // Si el elemento ya está seleccionado, deselecciónalo
            if (currentIndex == holder.bindingAdapterPosition) {
                currentIndex = RecyclerView.NO_POSITION
                notifyItemChanged(holder.bindingAdapterPosition)
            } else {
                val previousSelectedItemPosition = currentIndex
                currentIndex = holder.bindingAdapterPosition
                notifyItemChanged(currentIndex)

                if (previousSelectedItemPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousSelectedItemPosition)
                }
            }

            // Seleccionamos el ítem
            holder.itemView.isSelected = currentIndex == position

            // Notificamos al Listener superior
            dataSetChangedListener?.onDataSetChanged()
        }

        // Actualiza la vista según el estado de selección del elemento
        if (currentIndex == position) {
            // Establece el estado seleccionado
            setSelectedHolder(holder as SelectedViewHolder, position)
        } else {
            // Establece el estado no seleccionado
            setUnselectedHolder(holder as UnselectedViewHolder, position)
        }
    }

    private fun setSelectedHolder(holder: SelectedViewHolder, position: Int) {
        val content = filteredList[position]
        val v = holder.itemView

        holder.bind(content)

        v.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            getColorWithAlpha(colorId = R.color.lightslategray, alpha = 240),
            BlendModeCompat.MODULATE
        )

        if (multiSelect) {
            setCheckBoxLogic(holder.checkBox, content, position)
        } else {
            holder.checkBox.visibility = View.GONE
        }

        if (allowEditQty) {
            setQtyEditLogic(holder.editQty, content, position)
            holder.editQty.visibility = View.VISIBLE
        } else {
            holder.editQty.visibility = View.GONE
        }

        if (showQtyPanel) {
            holder.qtyReqPanel.visibility = View.VISIBLE
            holder.eanDivider.visibility = View.VISIBLE
        } else {
            holder.qtyReqPanel.visibility = View.GONE
            holder.eanDivider.visibility = View.GONE
        }
    }

    private fun setUnselectedHolder(holder: UnselectedViewHolder, position: Int) {
        val content = filteredList[position]
        val v = holder.itemView

        holder.bind(content)

        v.background.colorFilter = null

        if (multiSelect) {
            setCheckBoxLogic(holder.checkBox, content, position)
        } else {
            holder.checkBox.visibility = View.GONE
        }

        if (showQtyPanel) {
            holder.qtyReqPanel.visibility = View.VISIBLE
            holder.eanDivider.visibility = View.VISIBLE
        } else {
            holder.qtyReqPanel.visibility = View.GONE
            holder.eanDivider.visibility = View.GONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setQtyEditLogic(editQty: AppCompatImageView, content: PtlContent, position: Int) {
        editQty.setOnTouchListener { _, _ ->
            editQtyListener?.onEditQtyRequired(
                position = position,
                content = content,
                initialQty = content.qtyCollected,
                minValue = 0.0,
                maxValue = 999999.0,
                multiplier = settingViewModel.scanMultiplier
            )
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setCheckBoxLogic(checkBox: CheckBox, content: PtlContent, position: Int) {
        checkBox.visibility = View.VISIBLE

        var isSpeakButtonLongPressed = false

        val checkChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            setChecked(
                content = content, isChecked = isChecked, suspendRefresh = true
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
                        checkBox.setOnCheckedChangeListener(null)
                        val newState = !checkBox.isChecked
                        setChecked(filteredList, newState)
                    }
                    isSpeakButtonLongPressed = false
                }
            }
            return@OnTouchListener true
        }

        //Important to remove previous checkedChangedListener before calling setChecked
        checkBox.setOnCheckedChangeListener(null)
        checkBox.isChecked = checkedIdArray.contains(content.id)

        checkBox.tag = position
        checkBox.setOnLongClickListener(pressHoldListener)
        checkBox.setOnTouchListener(pressTouchListener)
        checkBox.setOnCheckedChangeListener(checkChangeListener)
    }

    // El método getItemCount devuelve el número de elementos en la lista
    override fun getItemCount(): Int {
        return filteredList.size
    }

    // El método getItemViewType devuelve el tipo de vista que se usará para el elemento en la posición dada
    override fun getItemViewType(position: Int): Int {
        return if (currentIndex == position) {
            SELECTED_VIEW_TYPE
        } else {
            UNSELECTED_VIEW_TYPE
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val r: ArrayList<PtlContent> = ArrayList()

                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.ROOT)
                    var filterableItem: PtlItem

                    for (i in 0 until fullList.size) {
                        filterableItem = fullList[i].item.first()
                        filterableItem.description.lowercase(Locale.ROOT)
                            .contains(filterString) || filterableItem.ean.lowercase(
                            Locale.ROOT
                        ).contains(filterString)
                    }
                }

                results.values = r.sortBy { it.item.first().description }
                results.count = r.count()
                return results
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?, results: FilterResults?,
            ) {
                filteredList = results?.values as ArrayList<PtlContent>
                notifyDataSetChanged()

                // Notificamos al Listener superior
                dataSetChangedListener?.onDataSetChanged()
            }
        }
    }

    private var showAllOnFilterEmpty = false

    @Suppress("unused")
    fun refreshFilter(s: String, showAllOnFilterEmpty: Boolean) {
        this.showAllOnFilterEmpty = showAllOnFilterEmpty
        filter.filter(s)
    }

    fun add(content: PtlContent, position: Int) {
        fullList.add(position, content)

        notifyItemInserted(position)

        // Notificamos al Listener superior
        dataSetChangedListener?.onDataSetChanged()

        selectItem(position)
    }

    fun remove(position: Int) {
        val id = getItemId(position)
        checkedIdArray.remove(id)

        fullList.removeAt(position)

        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemCount)

        // Notificamos al Listener superior
        dataSetChangedListener?.onDataSetChanged()
    }

    fun updateQtyCollected(itemId: Long, qtyCollected: Double) {
        val item = getContentByItemId(itemId) ?: return
        val index = getIndexByItemId(itemId)

        // ¿La cantidad es mayor a la cantidad solicitada?
        if (item.qtyRequested >= qtyCollected) {
            item.qtyCollected = qtyCollected

            notifyItemChanged(index)

            // Mostrar un cartelito, si jode sacarlo.
            reportQtyCollectedChange(item)
        } else {
            // Mostrar un cartelito, si jode sacarlo.
            reportQtyRequestedReached(item)
        }

        dataSetChangedListener?.onDataSetChanged()

        selectItem(index)
    }

    fun setSelectItemAndScrollPos(a: PtlContent?) {
        var pos = RecyclerView.NO_POSITION
        if (a != null) pos = getIndex(a)
        selectItem(pos)
    }

    private fun selectItem(pos: Int) {
        if (currentIndex != pos) {
            recyclerView.layoutManager?.scrollToPosition(pos)
            currentIndex = pos
        }
    }

    private fun reportQtyCollectedChange(content: PtlContent) {
        val itemCode = content.item.firstOrNull()?.ean ?: context.getString(R.string.no_ean)
        val decimalPlaces = 0

        val res = "${itemCode}: ${Statics.roundToString(content.qtyRequested, decimalPlaces)}"

        MakeText.makeText(recyclerView, res, SnackBarType.ADD)
    }

    private fun reportQtyRequestedReached(content: PtlContent) {
        val itemCode = content.item.firstOrNull()?.ean ?: context.getString(R.string.no_ean)

        val res = "${itemCode}: ${context.getString(R.string.maximum_reached)}"

        MakeText.makeText(recyclerView, res, SnackBarType.UPDATE)
    }

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun getIndexById(id: Long): Int {
        return filteredList.indexOfFirst { it.id == id }
    }

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    private fun getIndex(content: PtlContent): Int {
        return filteredList.indexOf(content)
    }

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun getIndexByItemId(itemId: Long): Int {
        return filteredList.indexOfFirst { it.itemId == itemId }
    }

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun getContent(item: PtlItem): PtlContent? {
        return filteredList.firstOrNull { it.itemId == item.id }
    }

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun getContentByItemId(itemId: Long): PtlContent? {
        return filteredList.firstOrNull { it.itemId == itemId }
    }

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun getContentById(id: Long): PtlContent? {
        return filteredList.firstOrNull { it.id == id }
    }

    fun getAllChecked(): ArrayList<PtlContent> {
        val items = ArrayList<PtlContent>()
        checkedIdArray.mapNotNullTo(items) { getContentById(it) }
        return items
    }

    @Suppress("unused")
    fun qtyRequestedTotal() = filteredList.sumOf { it.qtyRequested }

    @Suppress("unused")
    fun qtyCollectedTotal() = filteredList.sumOf { it.qtyCollected }

    fun currentContent(): PtlContent? {
        if (currentIndex == RecyclerView.NO_POSITION) return null
        return if (filteredList.any() && filteredList.count() > currentIndex) filteredList[currentIndex]
        else null
    }

    fun countChecked(): Int {
        return checkedIdArray.count()
    }

    fun firstVisiblePos(): Int {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        return layoutManager.findFirstVisibleItemPosition()
    }

    fun setChecked(content: PtlContent, isChecked: Boolean, suspendRefresh: Boolean = false) {
        val pos = getIndexById(content.id)
        if (isChecked) {
            if (!checkedIdArray.contains(content.id)) {
                checkedIdArray.add(content.id)
            }
        } else {
            checkedIdArray.remove(content.id)
        }

        checkedChangedListener?.onCheckedChanged(isChecked, pos)

        // Notificamos al Listener superior
        if (!suspendRefresh) dataSetChangedListener?.onDataSetChanged()
    }

    private var isFilling = false
    fun setChecked(items: ArrayList<PtlContent>, isChecked: Boolean) {
        if (isFilling) return
        isFilling = true

        for (i in items) {
            setChecked(
                content = i, isChecked = isChecked, suspendRefresh = true
            )
        }

        isFilling = false

        // Notificamos al Listener superior
        dataSetChangedListener?.onDataSetChanged()
    }

    // Aquí creamos dos ViewHolder, uno para cada tipo de vista
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    class SelectedViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val descriptionTextView: AutoResizeTextView = v.findViewById(R.id.descriptionTextView)
        val eanTextView: AutoResizeTextView = v.findViewById(R.id.eanTextView)
        val itemIdTextView: CheckedTextView = v.findViewById(R.id.itemIdCheckedTextView)
        val extIdTextView: CheckedTextView = v.findViewById(R.id.extIdCheckedTextView)
        val lotIdTextView: CheckedTextView = v.findViewById(R.id.lotIdCheckedTextView)
        val itemIdLabelTextView: TextView = v.findViewById(R.id.itemIdLabelTextView)
        val extIdLabelTextView: TextView = v.findViewById(R.id.extIdLabelTextView)
        val lotIdLabelTextView: TextView = v.findViewById(R.id.lotIdLabelTextView)
        val extId2LabelTextView: TextView = v.findViewById(R.id.extId2LabelTextView)
        val priceLabelTextView: TextView = v.findViewById(R.id.priceLabelTextView)
        val qtyPanel: ConstraintLayout = v.findViewById(R.id.qtyPanel)
        val qtyReqPanel: ConstraintLayout = v.findViewById(R.id.qtyReqPanel)
        val qtyReqTitle: TextView = v.findViewById(R.id.qtyReqTitleTextView)
        val qtyTitle: TextView = v.findViewById(R.id.qtyTitleTextView)
        val eanDivider: View = v.findViewById(R.id.eanDivider)
        val qtyCollectedTextView: TextView = v.findViewById(R.id.qtyCollectedTextView)
        val qtyRequestedTextView: TextView = v.findViewById(R.id.qtyRequestedTextView)
        val extId2TextView: AutoResizeTextView = v.findViewById(R.id.extId2TextView)
        val priceTextView: AutoResizeTextView = v.findViewById(R.id.priceTextView)
        val checkBox: CheckBox = v.findViewById(R.id.checkBox)
        val editQty: AppCompatImageView = v.findViewById(R.id.editQty)
        val extId2ConstraintLayout: ConstraintLayout = v.findViewById(R.id.extId2ConstraintLayout)

        fun bind(content: PtlContent) {
            val item = content.item.first()

            descriptionTextView.text = item.description
            eanTextView.text = item.ean
            qtyCollectedTextView.text = content.qtyCollected.toString()
            qtyRequestedTextView.text = content.qtyRequested.toString()
            itemIdTextView.text = item.id.toString()
            extIdTextView.text = content.externalId ?: 0.toString()
            lotIdTextView.text = content.lotId.toString()
            extId2ConstraintLayout.visibility = View.VISIBLE
            extId2TextView.text = item.externalId2.toString()
            priceTextView.text = String.format("$ %s", Statics.roundToString(item.price, 2))

            setStyle(content)
        }

        private fun setStyle(content: PtlContent) {
            val v = itemView

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
                    foreColor = selectedForeColor
                }
                content.qtyCollected > content.qtyRequested -> {
                    backColor = collQtyMoreBackColor
                    foreColor = selectedForeColor
                }
                else -> {
                    backColor = collQtyLessBackColor
                    foreColor = selectedForeColor
                }
            }

            val titleForeColor: Int = Colors.manipulateColor(foreColor, 0.8f)

            v.background = backColor
            descriptionTextView.setTextColor(foreColor)
            eanTextView.setTextColor(foreColor)
            extId2TextView.setTextColor(foreColor)
            priceTextView.setTextColor(foreColor)
            qtyCollectedTextView.setTextColor(foreColor)
            qtyRequestedTextView.setTextColor(foreColor)
            itemIdTextView.setTextColor(foreColor)
            extIdTextView.setTextColor(foreColor)
            lotIdTextView.setTextColor(foreColor)
            checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)

            qtyReqTitle.setTextColor(titleForeColor)
            qtyTitle.setTextColor(titleForeColor)
            itemIdLabelTextView.setTextColor(titleForeColor)
            extIdLabelTextView.setTextColor(titleForeColor)
            lotIdLabelTextView.setTextColor(titleForeColor)
            extId2LabelTextView.setTextColor(titleForeColor)
            priceLabelTextView.setTextColor(titleForeColor)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    internal class UnselectedViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val descriptionTextView: AutoResizeTextView = v.findViewById(R.id.descriptionTextView)
        val eanTextView: AutoResizeTextView = v.findViewById(R.id.eanTextView)
        val qtyReqTitleTextView: TextView = v.findViewById(R.id.qtyReqTitleTextView)
        val qtyTitleTextView: TextView = v.findViewById(R.id.qtyTitleTextView)
        val qtyPanel: ConstraintLayout = v.findViewById(R.id.qtyPanel)
        val qtyReqPanel: ConstraintLayout = v.findViewById(R.id.qtyReqPanel)
        val eanDivider: View = v.findViewById(R.id.eanDivider)
        val qtyCollectedTextView: AutoResizeTextView = v.findViewById(R.id.qtyCollectedTextView)
        val qtyRequestedTextView: AutoResizeTextView = v.findViewById(R.id.qtyRequestedTextView)
        val checkBox: CheckBox = v.findViewById(R.id.checkBox)

        fun bind(content: PtlContent) {
            val item = content.item.first()

            descriptionTextView.text = item.description
            eanTextView.text = item.ean
            qtyCollectedTextView.text = content.qtyCollected.toString()
            qtyRequestedTextView.text = content.qtyRequested.toString()
            checkBox.isChecked = false

            setStyle(content)
        }

        private fun setStyle(content: PtlContent) {
            val v = itemView

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
                    foreColor = selectedForeColor
                }
                content.qtyCollected > content.qtyRequested -> {
                    backColor = collQtyMoreBackColor
                    foreColor = selectedForeColor
                }
                else -> {
                    backColor = collQtyLessBackColor
                    foreColor = selectedForeColor
                }
            }

            val titleForeColor: Int = Colors.manipulateColor(foreColor, 1.4f)

            v.background = backColor
            descriptionTextView.setTextColor(foreColor)
            eanTextView.setTextColor(foreColor)
            qtyCollectedTextView.setTextColor(foreColor)
            qtyRequestedTextView.setTextColor(foreColor)
            checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)
            qtyReqTitleTextView.setTextColor(titleForeColor)
            qtyTitleTextView.setTextColor(titleForeColor)
        }
    }

    init {
        setupColors()

        // Por el momento no queremos animaciones, ni transiciones ante cambios en el DataSet
        recyclerView.itemAnimator = null
    }
}