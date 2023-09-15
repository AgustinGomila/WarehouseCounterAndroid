package com.dacosys.warehouseCounter.ui.adapter.ptlOrder

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.GONE
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.VISIBLE
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlOrder
import com.dacosys.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlOrderType
import com.dacosys.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlOrderType.Type0
import com.dacosys.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlOrderType.Type1
import com.dacosys.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlOrderType.Type2
import com.dacosys.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlOrderType.Type3
import com.dacosys.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlOrderType.Type4
import com.dacosys.warehouseCounter.databinding.PtlOrderRowBinding
import com.dacosys.warehouseCounter.databinding.PtlOrderRowExpandedBinding
import com.dacosys.warehouseCounter.ui.adapter.FilterOptions
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.manipulateColor
import java.util.*

class PtlOrderAdapter private constructor(builder: Builder) :
    ListAdapter<PtlOrder, ViewHolder>(PtlOrderDiffUtilCallback), Filterable {

    private val recyclerView: RecyclerView
    var fullList: ArrayList<PtlOrder> = ArrayList()
    var checkedIdArray: ArrayList<Long> = ArrayList()
    private var multiSelect: Boolean = false
    var showCheckBoxes: Boolean = false
    private var showCheckBoxesChanged: (Boolean) -> Unit = { }
    private var visibleStatus: ArrayList<PtlOrderType> = ArrayList(PtlOrderType.values().toList())
    private var filterOptions: FilterOptions

    // Este Listener debe usarse para los cambios de cantidad o de ítems marcados de la lista,
    // ya que se utiliza para actualizar los valores sumarios en la actividad.
    private var dataSetChangedListener: DataSetChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null

    // Posición del ítem seleccionado
    private var currentIndex = NO_POSITION

    // Clase para distinguir actualizaciones parciales
    private enum class PAYLOADS {
        CHECKBOX_STATE,
        CHECKBOX_VISIBILITY
    }

    fun clear() {
        checkedIdArray.clear()

        fullList.clear()
        submitList(fullList)
    }

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
        fun onCheckedChanged(isChecked: Boolean, pos: Int)
    }

    // El método onCreateViewHolder infla los diseños para cada tipo de vista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            SELECTED_VIEW_TYPE -> {
                SelectedViewHolder(
                    PtlOrderRowExpandedBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            else -> {
                UnselectedViewHolder(PtlOrderRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }
    }

    // Sobrecarga del método onBindViewHolder para actualización parcial de las vistas
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            // No hay payload, realizar la vinculación completa
            super.onBindViewHolder(holder, position, payloads)
            return
        }

        // Hay payload, realizar la actualización parcial basada en el payload
        for (payload in payloads) {
            // Extraer el payload y utilizarlo para actualizar solo las vistas relevantes
            when (payload) {
                PAYLOADS.CHECKBOX_VISIBILITY -> {
                    if (position == currentIndex)
                        (holder as SelectedViewHolder).bindCheckBoxVisibility(if (showCheckBoxes) VISIBLE else GONE)
                    else
                        (holder as UnselectedViewHolder).bindCheckBoxVisibility(if (showCheckBoxes) VISIBLE else GONE)
                }

                PAYLOADS.CHECKBOX_STATE -> {
                    val ptlOrder = getItem(position)
                    if (position == currentIndex)
                        (holder as SelectedViewHolder).bindCheckBoxState(checkedIdArray.contains(ptlOrder.id))
                    else
                        (holder as UnselectedViewHolder).bindCheckBoxState(checkedIdArray.contains(ptlOrder.id))
                }
            }
        }
    }

    // El método onBindViewHolder establece los valores de las vistas en función de los datos
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Evento de clic sobre el ítem que controla el estado de Selección para seleccionar el diseño adecuado
        holder.itemView.setOnClickListener {

            // Si el elemento ya está seleccionado quitar la selección
            if (currentIndex == holder.bindingAdapterPosition) {
                currentIndex = NO_POSITION
                notifyItemChanged(holder.bindingAdapterPosition)
            } else {
                // Notificamos los cambios para los dos ítems cuyo diseño necesita cambiar
                val previousSelectedItemPosition = currentIndex
                currentIndex = holder.bindingAdapterPosition
                notifyItemChanged(currentIndex)

                if (previousSelectedItemPosition != NO_POSITION) {
                    notifyItemChanged(previousSelectedItemPosition)
                }

                // Scroll para asegurarnos que se vea completamente el ítem
                holder.itemView.post { scrollToPos(currentIndex) }
            }

            // Seleccionamos el ítem
            holder.itemView.isSelected = currentIndex == position
        }

        // Establecer los valores para cada elemento según su posición con el estilo correspondiente
        if (currentIndex == position) {
            // Establece el estado seleccionado
            setSelectedHolder(holder as SelectedViewHolder, position)
        } else {
            // Establece el estado no seleccionado
            setUnselectedHolder(holder as UnselectedViewHolder, position)
        }
    }

    private fun setSelectedHolder(holder: SelectedViewHolder, position: Int) {
        val ptlOrder = getItem(position)

        // Lógica de clic largo sobre el ítem
        setItemCheckBoxLogic(holder.itemView)

        holder.bind(
            ptlOrder = ptlOrder,
            checkBoxVisibility = if (showCheckBoxes) VISIBLE else GONE
        )

        holder.itemView.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            getColorWithAlpha(colorId = R.color.lightslategray, alpha = 220), BlendModeCompat.MODULATE
        )

        // Acciones del checkBox de marcado
        setCheckBoxLogic(holder.binding.checkBox, ptlOrder, position)
    }

    private fun setUnselectedHolder(holder: UnselectedViewHolder, position: Int) {
        val ptlOrder = getItem(position)

        // Lógica de clic largo sobre el ítem
        setItemCheckBoxLogic(holder.itemView)

        // Perform a full update
        holder.bind(
            ptlOrder = ptlOrder,
            checkBoxVisibility = if (showCheckBoxes) VISIBLE else GONE
        )

        holder.itemView.background.colorFilter = null

        // Acciones del checkBox de marcado
        setCheckBoxLogic(holder.binding.checkBox, ptlOrder, position)
    }

    /**
     * Lógica del evento de clic sostenido sobre el ítem que cambia la visibilidad de los CheckBox
     *
     * @param itemView Vista general del ítem
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setItemCheckBoxLogic(itemView: View) {
        if (!multiSelect) {
            showCheckBoxes = false
            return
        }

        val longClickListener = OnLongClickListener { _ ->
            showCheckBoxes = !showCheckBoxes
            showCheckBoxesChanged.invoke(showCheckBoxes)
            notifyItemRangeChanged(0, itemCount, PAYLOADS.CHECKBOX_VISIBILITY)
            return@OnLongClickListener true
        }

        itemView.isLongClickable = true
        itemView.setOnLongClickListener(longClickListener)
    }

    /**
     * Lógica del comportamiento del CheckBox de marcado de ítems cuando [multiSelect] es verdadero
     *
     * @param checkBox Control CheckBox para marcado del ítem
     * @param ptlOrder Datos del ítem
     * @param position Posición en el adaptador
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setCheckBoxLogic(checkBox: CheckBox, ptlOrder: PtlOrder, position: Int) {
        val checkChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            setChecked(ptlOrder = ptlOrder, isChecked = isChecked, suspendRefresh = true)
        }

        val longClickListener = OnLongClickListener { _ ->
            checkBox.setOnCheckedChangeListener(null)

            // Notificamos los cambios solo a los ítems que cambian de estado.
            val newState = !checkBox.isChecked
            if (newState) {
                currentList.mapIndexed { pos, ptlOrder ->
                    if (ptlOrder.id !in checkedIdArray) {
                        checkedIdArray.add(ptlOrder.id)
                        notifyItemChanged(pos, PAYLOADS.CHECKBOX_STATE)
                    }
                }
            } else {
                currentList.mapIndexed { pos, ptlOrder ->
                    if (ptlOrder.id in checkedIdArray) {
                        checkedIdArray.remove(ptlOrder.id)
                        notifyItemChanged(pos, PAYLOADS.CHECKBOX_STATE)
                    }
                }
            }

            dataSetChangedListener?.onDataSetChanged()
            return@OnLongClickListener true
        }

        // Important to remove previous checkedChangedListener before calling setChecked
        checkBox.setOnCheckedChangeListener(null)

        checkBox.isChecked = checkedIdArray.contains(ptlOrder.id)
        checkBox.isLongClickable = true
        checkBox.tag = position

        checkBox.setOnLongClickListener(longClickListener)
        checkBox.setOnCheckedChangeListener(checkChangeListener)
    }

    // El método getItemViewType devuelve el tipo de vista que se usará para el elemento en la posición dada
    override fun getItemViewType(position: Int): Int {
        return if (currentIndex == position) SELECTED_VIEW_TYPE
        else UNSELECTED_VIEW_TYPE
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            var selected: PtlOrder? = null
            var firstVisible: PtlOrder? = null

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                // Guardamos el item seleccionado y la posición del scroll
                selected = currentPtlOrder()
                var scrollPos =
                    (recyclerView.layoutManager as LinearLayoutManager?)?.findFirstVisibleItemPosition()
                        ?: NO_POSITION

                if (scrollPos != NO_POSITION && itemCount > scrollPos) {
                    var currentScrolled = getItem(scrollPos)

                    // Comprobamos si es visible el ítem del Scroll
                    if (currentScrolled.orderType in visibleStatus)
                        firstVisible = currentScrolled
                    else {
                        // Si no es visible, intentar encontrar el próximo visible.
                        while (firstVisible == null) {
                            scrollPos++
                            if (itemCount > scrollPos) {
                                currentScrolled = getItem(scrollPos)
                                if (currentScrolled.orderType in visibleStatus)
                                    firstVisible = currentScrolled
                            } else break
                        }
                    }
                }

                // Filtramos los resultados
                val results = FilterResults()
                var r: ArrayList<PtlOrder> = ArrayList()
                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    if (filterString.isNotEmpty()) {
                        var filterableItem: PtlOrder

                        for (i in 0 until fullList.size) {
                            filterableItem = fullList[i]

                            // Descartamos aquellos que no debe ser visibles
                            if (filterableItem.orderType !in visibleStatus) continue

                            if (isFilterable(filterableItem, filterString)) {
                                r.add(filterableItem)
                            }
                        }
                    } else if (filterOptions.showAllOnFilterEmpty) {
                        r = ArrayList(fullList.mapNotNull { if (it.orderType in visibleStatus) it else null })
                    }
                }

                results.values = r
                results.count = r.size
                return results
            }

            fun isFilterable(filterablePtlOrder: PtlOrder, filterString: String): Boolean =
                filterablePtlOrder.description.contains(filterString, true) ||
                        filterablePtlOrder.zone.contains(filterString, true)

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?, results: FilterResults?,
            ) {
                submitList(results?.values as ArrayList<PtlOrder>) {
                    // Notificamos al Listener superior
                    dataSetChangedListener?.onDataSetChanged()

                    // Recuperamos el item seleccionado y la posición del scroll
                    if (firstVisible != null)
                        scrollToPos(getIndexById(firstVisible?.id ?: -1), true)
                    if (selected != null)
                        selectItem(selected, false)
                }
            }
        }
    }

    private fun sortItems(originalList: MutableList<PtlOrder>): ArrayList<PtlOrder> {
        // Run the follow method on each of the roots
        return ArrayList(
            originalList.sortedWith(
                compareBy({ it.orderType },
                    { it.zone },
                    { it.description })
            ).toList()
        )
    }

    fun refreshFilter(options: FilterOptions) {
        filterOptions = options
        filter.filter(filterOptions.filterString)
    }

    private fun refreshFilter() {
        refreshFilter(filterOptions)
    }

    fun add(ptlOrder: PtlOrder, position: Int) {
        fullList.add(position, ptlOrder)
        submitList(fullList).apply {
            dataSetChangedListener?.onDataSetChanged()
            selectItem(position)
        }
    }

    fun remove(position: Int) {
        val id = getItemId(position)
        checkedIdArray.remove(id)

        fullList.removeAt(position)
        submitList(fullList).apply {
            dataSetChangedListener?.onDataSetChanged()
        }
    }

    fun selectItem(a: PtlOrder?, scroll: Boolean = true) {
        var pos = NO_POSITION
        if (a != null) pos = getIndex(a)
        selectItem(pos, scroll)
    }

    private fun selectItem(pos: Int, scroll: Boolean = true) {
        // Si la posición está fuera del rango válido, reseteamos currentIndex a NO_POSITION.
        currentIndex = if (pos < 0 || pos >= itemCount) NO_POSITION else pos
        notifyItemChanged(currentIndex)
        if (scroll) scrollToPos(currentIndex)
    }

    /**
     * Scrolls to the given position, making sure the item can be fully displayed.
     *
     * @param position
     * @param scrollToTop If it is activated, the item will scroll until it is at the top of the view
     */
    fun scrollToPos(position: Int, scrollToTop: Boolean = false) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager

        if (position < 0 || position >= itemCount) {
            // La posición está fuera del rango válido, no se puede realizar el scroll
            return
        }

        val selectedView = layoutManager.findViewByPosition(position)

        if (scrollToTop) {
            // Hacemos scroll hasta que el ítem quede en la parte superior
            layoutManager.scrollToPositionWithOffset(position, 0)
        } else {
            if (selectedView != null) {
                // El ítem es visible, realizar scroll para asegurarse de que se vea completamente
                scrollToVisibleItem(selectedView)
            } else {
                // El ítem no es visible, realizar scroll directo a la posición
                recyclerView.scrollToPosition(position)
                recyclerView.addOnScrollListener(object : OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        val selected = layoutManager.findViewByPosition(position)
                        if (selected != null) {
                            // El ítem se ha vuelto visible, realizar scroll para asegurarse de que se vea completamente
                            scrollToVisibleItem(selected)
                            recyclerView.removeOnScrollListener(this)
                        }
                    }
                })
            }
        }
    }

    /**
     * Scrolls to the given view, making sure the item can be fully displayed.
     *
     * @param selectedView
     */
    private fun scrollToVisibleItem(selectedView: View) {
        val recyclerViewHeight = recyclerView.height - recyclerView.paddingTop - recyclerView.paddingBottom
        val selectedViewHeight = selectedView.height + selectedView.marginTop + selectedView.marginBottom

        val selectedViewTop = selectedView.top - selectedView.marginTop
        val selectedViewBottom = selectedView.bottom + selectedView.marginBottom

        if (selectedViewTop < 0) {
            // El ítem está parcialmente oculto en la parte superior del RecyclerView
            recyclerView.smoothScrollBy(0, selectedViewTop)
        } else if (selectedViewBottom > recyclerViewHeight) {
            // El ítem está parcialmente oculto en la parte inferior del RecyclerView
            val visibleHeight = recyclerViewHeight - selectedViewTop
            recyclerView.smoothScrollBy(0, selectedViewHeight - visibleHeight)
        }
    }

    fun getIndexById(id: Long): Int {
        return currentList.indexOfFirst { it.id == id }
    }

    private fun getIndex(ptlOrder: PtlOrder): Int {
        return currentList.indexOfFirst { it == ptlOrder }
    }

    private fun getPtlOrderById(id: Long): PtlOrder? {
        return fullList.firstOrNull { it.id == id }
    }

    fun getAllChecked(): ArrayList<PtlOrder> {
        val items = ArrayList<PtlOrder>()
        checkedIdArray.mapNotNullTo(items) { getPtlOrderById(it) }
        return items
    }

    fun currentPtlOrder(): PtlOrder? {
        if (currentIndex == NO_POSITION) return null
        return if (currentList.any() && itemCount > currentIndex) getItem(currentIndex)
        else null
    }

    fun countChecked(): Int {
        return checkedIdArray.size
    }

    fun totalVisible(): Int {
        return itemCount
    }

    fun firstVisiblePos(): Int {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        return layoutManager.findFirstVisibleItemPosition()
    }

    fun setChecked(ptlOrder: PtlOrder, isChecked: Boolean, suspendRefresh: Boolean = false) {
        val pos = getIndexById(ptlOrder.id)
        if (isChecked) {
            if (!checkedIdArray.contains(ptlOrder.id)) {
                checkedIdArray.add(ptlOrder.id)
            }
        } else {
            checkedIdArray.remove(ptlOrder.id)
        }

        checkedChangedListener?.onCheckedChanged(isChecked, pos)

        // Notificamos al Listener superior
        if (!suspendRefresh) dataSetChangedListener?.onDataSetChanged()
    }

    private var isFilling = false
    fun setChecked(items: ArrayList<PtlOrder>, isChecked: Boolean) {
        if (isFilling) return

        isFilling = true
        for (i in items) {
            setChecked(ptlOrder = i, isChecked = isChecked, suspendRefresh = true)
        }
        isFilling = false

        // Notificamos al Listener superior
        dataSetChangedListener?.onDataSetChanged()
    }

    fun addVisibleStatus(status: PtlOrderType) {
        if (visibleStatus.contains(status)) return
        visibleStatus.add(status)

        refreshFilter()
    }

    fun removeVisibleStatus(status: PtlOrderType) {
        if (!visibleStatus.contains(status)) return
        visibleStatus.remove(status)

        // Quitamos los ítems con el estado seleccionado de la lista marcados.
        val uncheckedItems = ArrayList(fullList.mapNotNull { if (it.orderType == status) it.id else null })
        checkedIdArray.removeAll(uncheckedItems.toSet())

        refreshFilter()
    }

    internal class SelectedViewHolder(val binding: PtlOrderRowExpandedBinding) : ViewHolder(binding.root) {
        fun bindCheckBoxVisibility(checkBoxVisibility: Int = GONE) {
            binding.checkBoxConstraintLayout.visibility = checkBoxVisibility
        }

        fun bindCheckBoxState(checked: Boolean) {
            binding.checkBox.isChecked = checked
        }

        fun bind(ptlOrder: PtlOrder, checkBoxVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)

            binding.descriptionTextView.text =
                ptlOrder.description.ifEmpty { context.getString(R.string.without_description) }
            binding.startDateTextView.text = ptlOrder.startDate
            binding.finishDateTextView.text = ptlOrder.finishDate ?: context.getString(R.string.uncompleted)
            binding.zoneTextView.text = ptlOrder.zone.substringAfterLast('/')

            setStyle(ptlOrder)
        }

        private fun setStyle(ptlOrder: PtlOrder) {
            val v = itemView

            // Background layouts
            // Resalta por estado de la orden
            val default = getDrawable(context.resources, R.drawable.layout_thin_border, null)!!
            val prepareOrder = getDrawable(context.resources, R.drawable.layout_thin_border_green, null)!!
            val stockAuditFromDevice = getDrawable(context.resources, R.drawable.layout_thin_border_yellow, null)!!
            val stockAudit = getDrawable(context.resources, R.drawable.layout_thin_border_blue, null)!!
            val receptionAudit = getDrawable(context.resources, R.drawable.layout_thin_border_orange, null)!!
            val deliveryAudit = getDrawable(context.resources, R.drawable.layout_thin_border_green_2, null)!!

            val backColor: Drawable
            val foreColor: Int
            when (ptlOrder.orderType) {
                Type0 -> {
                    backColor = prepareOrder
                    foreColor = prepareOrderSelectedForeColor
                }

                Type1 -> {
                    backColor = stockAuditFromDevice
                    foreColor = stockAuditFromDeviceSelectedForeColor
                }

                Type2 -> {
                    backColor = stockAudit
                    foreColor = stockAuditSelectedForeColor
                }

                Type3 -> {
                    backColor = receptionAudit
                    foreColor = receptionAuditSelectedForeColor
                }

                Type4 -> {
                    backColor = deliveryAudit
                    foreColor = deliveryAuditSelectedForeColor
                }

                else -> {
                    backColor = default
                    foreColor = defaultSelectedForeColor
                }
            }

            val titleForeColor: Int = manipulateColor(foreColor, 0.8f)

            v.background = backColor
            binding.zoneTextView.setTextColor(foreColor)
            binding.descriptionTextView.setTextColor(foreColor)
            binding.startDateTextView.setTextColor(foreColor)
            binding.finishDateTextView.setTextColor(foreColor)
            binding.checkBox.buttonTintList = ColorStateList.valueOf(darkslategray)

            // Titles
            binding.startDateLabelTextView.setTextColor(titleForeColor)
            binding.finishDateLabelTextView.setTextColor(titleForeColor)
        }
    }

    internal class UnselectedViewHolder(val binding: PtlOrderRowBinding) : ViewHolder(binding.root) {
        fun bindCheckBoxVisibility(checkBoxVisibility: Int = GONE) {
            binding.checkBoxConstraintLayout.visibility = checkBoxVisibility
        }

        fun bindCheckBoxState(checked: Boolean) {
            binding.checkBox.isChecked = checked
        }

        fun bind(ptlOrder: PtlOrder, checkBoxVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)

            binding.descriptionTextView.text = ptlOrder.description.ifEmpty {
                context.getString(R.string.without_description)
            }
            binding.zoneTextView.text = ptlOrder.zone.substringAfterLast('/')
            binding.startDateTextView.text = ptlOrder.startDate

            setStyle(ptlOrder)
        }

        private fun setStyle(ptlOrder: PtlOrder) {
            val v = itemView

            // Background layouts
            // Resalta por estado de la orden
            val default = getDrawable(context.resources, R.drawable.layout_thin_border, null)!!
            val prepareOrder = getDrawable(context.resources, R.drawable.layout_thin_border_green, null)!!
            val stockAuditFromDevice = getDrawable(context.resources, R.drawable.layout_thin_border_yellow, null)!!
            val stockAudit = getDrawable(context.resources, R.drawable.layout_thin_border_blue, null)!!
            val receptionAudit = getDrawable(context.resources, R.drawable.layout_thin_border_orange, null)!!
            val deliveryAudit = getDrawable(context.resources, R.drawable.layout_thin_border_green_2, null)!!

            val backColor: Drawable
            val foreColor: Int
            when (ptlOrder.orderType) {
                Type0 -> {
                    backColor = prepareOrder
                    foreColor = prepareOrderForeColor
                }

                Type1 -> {
                    backColor = stockAuditFromDevice
                    foreColor = stockAuditFromDeviceForeColor
                }

                Type2 -> {
                    backColor = stockAudit
                    foreColor = stockAuditForeColor
                }

                Type3 -> {
                    backColor = receptionAudit
                    foreColor = receptionAuditForeColor
                }

                Type4 -> {
                    backColor = deliveryAudit
                    foreColor = deliveryAuditForeColor
                }

                else -> {
                    backColor = default
                    foreColor = defaultForeColor
                }
            }

            v.background = backColor
            binding.zoneTextView.setTextColor(foreColor)
            binding.descriptionTextView.setTextColor(foreColor)
            binding.startDateTextView.setTextColor(foreColor)
            binding.checkBox.buttonTintList = ColorStateList.valueOf(darkslategray)
        }
    }

    private object PtlOrderDiffUtilCallback : DiffUtil.ItemCallback<PtlOrder>() {
        override fun areItemsTheSame(oldItem: PtlOrder, newItem: PtlOrder): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PtlOrder, newItem: PtlOrder): Boolean {
            if (oldItem.orderTypeId != newItem.orderTypeId) return false
            if (oldItem.orderType != newItem.orderType) return false
            if (oldItem.externalId != newItem.externalId) return false
            if (oldItem.clientId != newItem.clientId) return false
            if (oldItem.client != newItem.client) return false
            if (oldItem.collectorId != newItem.collectorId) return false
            if (oldItem.collectorUserId != newItem.collectorUserId) return false
            if (oldItem.description != newItem.description) return false
            if (oldItem.zone != newItem.zone) return false
            if (oldItem.resultDiffQty != newItem.resultDiffQty) return false
            if (oldItem.resultDiffProduct != newItem.resultDiffProduct) return false
            if (oldItem.resultAllowDiff != newItem.resultAllowDiff) return false
            if (oldItem.resultAllowMod != newItem.resultAllowMod) return false
            if (oldItem.completed != newItem.completed) return false
            if (oldItem.startDate != newItem.startDate) return false
            if (oldItem.finishDate != newItem.finishDate) return false
            if (oldItem.receivedDate != newItem.receivedDate) return false
            if (oldItem.processed != newItem.processed) return false
            if (oldItem.dataReceptionId != newItem.dataReceptionId) return false
            if (oldItem.dataReceived != newItem.dataReceived) return false
            if (oldItem.processedDate != newItem.processedDate) return false
            return oldItem.statusId == newItem.statusId

            // if (oldItem.rowCreationDate != newItem.rowCreationDate) return false
            // if (oldItem.rowModificationDate != newItem.rowModificationDate) return false
        }
    }

    companion object {
        // Aquí definimos dos constantes para identificar los dos diseños diferentes
        const val SELECTED_VIEW_TYPE = 1
        const val UNSELECTED_VIEW_TYPE = 2

        // region COLORS
        private var prepareOrderForeColor: Int = 0
        private var stockAuditFromDeviceForeColor: Int = 0
        private var stockAuditForeColor: Int = 0
        private var receptionAuditForeColor: Int = 0
        private var deliveryAuditForeColor: Int = 0
        private var defaultForeColor: Int = 0

        private var prepareOrderSelectedForeColor: Int = 0
        private var stockAuditFromDeviceSelectedForeColor: Int = 0
        private var stockAuditSelectedForeColor: Int = 0
        private var receptionAuditSelectedForeColor: Int = 0
        private var deliveryAuditSelectedForeColor: Int = 0
        private var defaultSelectedForeColor: Int = 0

        private var darkslategray: Int = 0
        private var lightgray: Int = 0

        /**
         * Setup colors
         * Simplemente inicializamos algunas variables con los colores que vamos a usar para cada estado.
         */
        private fun setupColors() {
            // Color de los diferentes estados
            // Color de los diferentes estados
            val prepareOrder = getColor(context.resources, R.color.status_prepare_order, null)
            val stockAuditFromDevice = getColor(context.resources, R.color.status_stock_audit_from_device, null)
            val stockAudit = getColor(context.resources, R.color.status_stock_audit, null)
            val receptionAudit = getColor(context.resources, R.color.status_reception_audit, null)
            val deliveryAudit = getColor(context.resources, R.color.status_delivery_audit, null)
            val default = getColor(context.resources, R.color.status_default, null)

            // Mejor contraste para los pedidos seleccionados
            prepareOrderSelectedForeColor = getBestContrastColor(manipulateColor(prepareOrder, 0.5f))
            stockAuditFromDeviceSelectedForeColor = getBestContrastColor(manipulateColor(stockAuditFromDevice, 0.5f))
            stockAuditSelectedForeColor = getBestContrastColor(manipulateColor(stockAudit, 0.5f))
            receptionAuditSelectedForeColor = getBestContrastColor(manipulateColor(receptionAudit, 0.5f))
            deliveryAuditSelectedForeColor = getBestContrastColor(manipulateColor(deliveryAudit, 0.5f))
            defaultSelectedForeColor = getBestContrastColor(manipulateColor(default, 0.5f))

            // Mejor contraste para los pedidos no seleccionados
            prepareOrderForeColor = getBestContrastColor(prepareOrder)
            stockAuditFromDeviceForeColor = getBestContrastColor(stockAuditFromDevice)
            stockAuditForeColor = getBestContrastColor(stockAudit)
            receptionAuditForeColor = getBestContrastColor(receptionAudit)
            deliveryAuditForeColor = getBestContrastColor(deliveryAudit)
            defaultForeColor = getBestContrastColor(default)

            // CheckBox color
            darkslategray = getColor(context.resources, R.color.darkslategray, null)

            // Title color
            lightgray = getColor(context.resources, R.color.lightgray, null)
        }
        // endregion
    }

    init {
        // Set values from Builder
        recyclerView = builder.recyclerView
        fullList = builder.fullList
        checkedIdArray = builder.checkedIdArray
        multiSelect = builder.multiSelect
        showCheckBoxes = builder.showCheckBoxes
        showCheckBoxesChanged = builder.showCheckBoxesChanged
        visibleStatus = builder.visibleStatus
        filterOptions = builder.filterOptions
        dataSetChangedListener = builder.dataSetChangedListener
        checkedChangedListener = builder.checkedChangedListener

        // Configuramos variables de estilo que se van a reutilizar.
        setupColors()

        // Por el momento no queremos animaciones, ni transiciones ante cambios en el DataSet
        recyclerView.itemAnimator = null

        // Vamos a retener en el caché un [cacheFactor] por ciento de los ítems creados o un máximo de [maxCachedItems]
        val maxCachedItems = 50
        val cacheFactor = 0.10
        var cacheSize = (fullList.size * cacheFactor).toInt()
        if (cacheSize > maxCachedItems) cacheSize = maxCachedItems
        recyclerView.setItemViewCacheSize(cacheSize)
        recyclerView.recycledViewPool.setMaxRecycledViews(SELECTED_VIEW_TYPE, 0)
        recyclerView.recycledViewPool.setMaxRecycledViews(UNSELECTED_VIEW_TYPE, 0)

        // Ordenamiento natural de la lista completa para trabajar en adelante con una lista ordenada
        val tList = sortItems(fullList)
        fullList = tList

        // Suministramos la lista a publicar refrescando el filtro que recorre la lista completa y devuelve los resultados filtrados y ordenados
        refreshFilter(filterOptions)
    }

    /**
     * Return a sorted list of visible state items
     *
     * @param list
     * @return Lista ordenada con los estados visibles
     */
    private fun sortedVisibleList(list: MutableList<PtlOrder>?): MutableList<PtlOrder> {
        val croppedList = (list ?: mutableListOf()).mapNotNull { if (it.orderType in visibleStatus) it else null }
        return sortItems(croppedList.toMutableList())
    }

    // Sobrecargamos estos métodos para suministrar siempre una lista ordenada y filtrada por estado de visibilidad
    override fun submitList(list: MutableList<PtlOrder>?) {
        super.submitList(sortedVisibleList(list))
    }

    override fun submitList(list: MutableList<PtlOrder>?, commitCallback: Runnable?) {
        super.submitList(sortedVisibleList(list), commitCallback)
    }

    class Builder {
        fun build(): PtlOrderAdapter {
            return PtlOrderAdapter(this)
        }

        internal lateinit var recyclerView: RecyclerView
        internal var fullList: ArrayList<PtlOrder> = ArrayList()
        internal var checkedIdArray: ArrayList<Long> = ArrayList()
        internal var multiSelect: Boolean = false
        internal var showCheckBoxes: Boolean = false
        internal var showCheckBoxesChanged: (Boolean) -> Unit = { }
        internal var visibleStatus: ArrayList<PtlOrderType> = ArrayList(PtlOrderType.values().toList())
        internal var filterOptions: FilterOptions = FilterOptions()
        internal var dataSetChangedListener: DataSetChangedListener? = null
        internal var checkedChangedListener: CheckedChangedListener? = null

        // Setter methods for variables with chained methods
        @Suppress("unused")
        fun recyclerView(`val`: RecyclerView): Builder {
            recyclerView = `val`
            return this
        }

        @Suppress("unused")
        fun fullList(`val`: ArrayList<PtlOrder>): Builder {
            fullList = `val`
            return this
        }

        @Suppress("unused")
        fun checkedIdArray(`val`: ArrayList<Long>): Builder {
            checkedIdArray = `val`
            return this
        }

        @Suppress("unused")
        fun multiSelect(`val`: Boolean): Builder {
            multiSelect = `val`
            return this
        }

        @Suppress("unused")
        fun showCheckBoxes(`val`: Boolean, listener: (Boolean) -> Unit): Builder {
            showCheckBoxes = `val`
            showCheckBoxesChanged = listener
            return this
        }

        @Suppress("unused")
        fun visibleStatus(`val`: ArrayList<PtlOrderType>): Builder {
            visibleStatus = `val`
            return this
        }

        @Suppress("unused")
        fun filterOptions(`val`: FilterOptions): Builder {
            filterOptions = `val`
            return this
        }

        @Suppress("unused")
        fun dataSetChangedListener(listener: DataSetChangedListener?): Builder {
            dataSetChangedListener = listener
            return this
        }

        @Suppress("unused")
        fun checkedChangedListener(listener: CheckedChangedListener?): Builder {
            checkedChangedListener = listener
            return this
        }
    }
}
