package com.dacosys.warehouseCounter.ui.adapter.order

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
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderStatus
import com.dacosys.warehouseCounter.databinding.OrderRowBinding
import com.dacosys.warehouseCounter.databinding.OrderRowExpandedBinding
import com.dacosys.warehouseCounter.ui.adapter.FilterOptions
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.manipulateColor
import java.util.*

class OrderAdapter private constructor(builder: Builder) :
    ListAdapter<OrderResponse, ViewHolder>(ItemDiffUtilCallback), Filterable {

    private val recyclerView: RecyclerView
    var fullList: ArrayList<OrderResponse> = ArrayList()
    var checkedHashArray: ArrayList<Int> = ArrayList()
    private var multiSelect: Boolean = false
    var showCheckBoxes: Boolean = false
    private var showCheckBoxesChanged: (Boolean) -> Unit = { }
    private var visibleStatus: ArrayList<OrderStatus> = ArrayList()
    private var filterOptions: FilterOptions = FilterOptions()

    // Este Listener debe usarse para los cambios de cantidad o de ítems marcados de la lista,
    // ya que se utiliza para actualizar los valores sumarios en la actividad.
    private var dataSetChangedListener: DataSetChangedListener? = null

    private var selectedItemChangedListener: SelectedItemChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null

    // Posición del ítem seleccionado
    private var currentIndex = NO_POSITION

    // Clase para distinguir actualizaciones parciales
    private enum class PAYLOADS {
        CHECKBOX_STATE,
        CHECKBOX_VISIBILITY,
        ITEM_SELECTED
    }

    fun clear() {
        checkedHashArray.clear()

        fullList.clear()
        submitList(fullList)
    }

    fun refreshListeners(
        checkedChangedListener: CheckedChangedListener? = null,
        dataSetChangedListener: DataSetChangedListener? = null,
        selectedItemChangedListener: SelectedItemChangedListener? = null,
    ) {
        this.checkedChangedListener = checkedChangedListener
        this.dataSetChangedListener = dataSetChangedListener
        this.selectedItemChangedListener = selectedItemChangedListener
    }

    interface DataSetChangedListener {
        fun onDataSetChanged()
    }

    interface CheckedChangedListener {
        fun onCheckedChanged(isChecked: Boolean, pos: Int)
    }

    interface SelectedItemChangedListener {
        fun onSelectedItemChanged(item: OrderResponse?)
    }

    // El método onCreateViewHolder infla los diseños para cada tipo de vista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            SELECTED_VIEW_TYPE -> {
                SelectedViewHolder(
                    OrderRowExpandedBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            else -> {
                UnselectedViewHolder(
                    OrderRowBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
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
                    val item = getItem(position)
                    if (position == currentIndex)
                        (holder as SelectedViewHolder).bindCheckBoxState(checkedHashArray.contains(item.hashCode))
                    else
                        (holder as UnselectedViewHolder).bindCheckBoxState(checkedHashArray.contains(item.hashCode))
                }

                PAYLOADS.ITEM_SELECTED -> {
                    // No regenerar la vista ante cambios de selección
                    // No está funcionando. La idea es usarlo para los cambios de selección.
                    // Pero por algún motivo los Payloads vienen vacíos luego de notifyItemChanged
                    super.onBindViewHolder(holder, position, payloads)
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
                notifyItemSelectedChanged(holder.bindingAdapterPosition)
            } else {
                // Notificamos los cambios para los dos ítems cuyo diseño necesita cambiar
                val previousSelectedItemPosition = currentIndex
                currentIndex = holder.bindingAdapterPosition
                notifyItemSelectedChanged(currentIndex)

                if (previousSelectedItemPosition != NO_POSITION) {
                    notifyItemSelectedChanged(previousSelectedItemPosition)
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
        val item = getItem(position)

        // Lógica de clic largo sobre el ítem
        setItemCheckBoxLogic(holder.itemView)

        holder.bind(
            item = item,
            checkBoxVisibility = if (showCheckBoxes) VISIBLE else GONE
        )

        holder.itemView.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            getColorWithAlpha(colorId = R.color.lightslategray, alpha = 220), BlendModeCompat.MODULATE
        )

        // Acciones del checkBox de marcado
        setCheckBoxLogic(holder.binding.checkBox, item, position)
    }

    private fun setUnselectedHolder(holder: UnselectedViewHolder, position: Int) {
        val item = getItem(position)

        // Lógica de clic largo sobre el ítem
        setItemCheckBoxLogic(holder.itemView)

        // Perform a full update
        holder.bind(
            item = item,
            checkBoxVisibility = if (showCheckBoxes) VISIBLE else GONE
        )

        holder.itemView.background.colorFilter = null

        // Acciones del checkBox de marcado
        setCheckBoxLogic(holder.binding.checkBox, item, position)
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
     * @param item Datos del ítem
     * @param position Posición en el adaptador
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setCheckBoxLogic(checkBox: CheckBox, item: OrderResponse, position: Int) {
        val checkChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            setChecked(item = item, isChecked = isChecked, suspendRefresh = true)
        }

        val longClickListener = OnLongClickListener { _ ->
            checkBox.setOnCheckedChangeListener(null)

            // Notificamos los cambios solo a los ítems que cambian de estado.
            val newState = !checkBox.isChecked
            if (newState) {
                currentList.mapIndexed { pos, item ->
                    if (item.hashCode !in checkedHashArray) {
                        checkedHashArray.add(item.hashCode)
                        notifyItemChanged(pos, PAYLOADS.CHECKBOX_STATE)
                    }
                }
            } else {
                currentList.mapIndexed { pos, item ->
                    if (item.hashCode in checkedHashArray) {
                        checkedHashArray.remove(item.hashCode)
                        notifyItemChanged(pos, PAYLOADS.CHECKBOX_STATE)
                    }
                }
            }

            dataSetChangedListener?.onDataSetChanged()
            return@OnLongClickListener true
        }

        // Important to remove previous checkedChangedListener before calling setChecked
        checkBox.setOnCheckedChangeListener(null)

        checkBox.isChecked = checkedHashArray.contains(item.hashCode)
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

    private fun mustBeVisible(it: OrderResponse): Boolean {
        return visibleStatus.any { v -> v == it.status }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            var selected: OrderResponse? = null
            var firstVisible: OrderResponse? = null

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                // Guardamos el item seleccionado y la posición del scroll
                selected = currentItem()
                var scrollPos =
                    (recyclerView.layoutManager as LinearLayoutManager?)?.findFirstVisibleItemPosition()
                        ?: NO_POSITION

                if (scrollPos != NO_POSITION && itemCount > scrollPos) {
                    var currentScrolled = getItem(scrollPos)

                    // Comprobamos si es visible el ítem del Scroll
                    if (mustBeVisible(currentScrolled))
                        firstVisible = currentScrolled
                    else {
                        // Si no es visible, intentar encontrar el próximo visible.
                        while (firstVisible == null) {
                            scrollPos++
                            if (itemCount > scrollPos) {
                                currentScrolled = getItem(scrollPos)
                                if (mustBeVisible(currentScrolled))
                                    firstVisible = currentScrolled
                            } else break
                        }
                    }
                }

                // Filtramos los resultados
                val results = FilterResults()
                var r: ArrayList<OrderResponse> = ArrayList()
                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    if (filterString.isNotEmpty()) {
                        var filterableItem: OrderResponse

                        for (i in 0 until fullList.size) {
                            filterableItem = fullList[i]

                            // Descartamos aquellos que no debe ser visibles
                            if (!mustBeVisible(filterableItem)) continue

                            if (isFilterable(filterableItem, filterString)) {
                                r.add(filterableItem)
                            }
                        }
                    } else if (filterOptions.showAllOnFilterEmpty) {
                        r = ArrayList(fullList.mapNotNull {
                            if (mustBeVisible(it)) it else null
                        })
                    }
                }

                results.values = r
                results.count = r.size
                return results
            }

            fun isFilterable(filterableItem: OrderResponse, filterString: String): Boolean =
                (filterableItem.description.contains(filterString, true) ||
                        filterableItem.externalId.contains(filterString, true) ||
                        filterableItem.zone.contains(filterString, true))

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?, results: FilterResults?,
            ) {
                submitList(results?.values as ArrayList<OrderResponse>) {
                    // Notificamos al Listener superior
                    dataSetChangedListener?.onDataSetChanged()

                    // Recuperamos el item seleccionado y la posición del scroll
                    if (firstVisible != null)
                        scrollToPos(getIndexByHashCode(firstVisible?.hashCode ?: -1), true)
                    if (selected != null)
                        selectItem(selected, false)
                }
            }
        }
    }

    private fun sortItems(originalList: MutableList<OrderResponse>): ArrayList<OrderResponse> {
        return ArrayList(
            originalList.sortedWith(
                compareBy({ it.description },
                    { it.externalId },
                    { it.zone })
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

    fun add(item: OrderResponse) {
        if (fullList.contains(item)) return

        val position = fullList.size
        fullList.add(item)
        checkedHashArray.add(item.hashCode)

        submitList(fullList).apply {
            dataSetChangedListener?.onDataSetChanged()
            selectItem(position)
        }
    }

    fun remove(position: Int) {
        val order = getItem(position)
        removeById(order.id.toString())
    }

    fun removeByIds(orderIdList: List<String>) {
        val allIndex: List<Int> =
            fullList.mapIndexed { index, order -> if (order.id.toString() in orderIdList) index else null }
                .filterNotNull()

        val allOrders: List<OrderResponse> = allIndex.map { getItem(it) }

        val allHashCodes = allOrders.map { it.hashCode }

        checkedHashArray.removeAll(allHashCodes.toSet())
        fullList.removeAll(allOrders.toSet())

        submitList(fullList).apply {
            dataSetChangedListener?.onDataSetChanged()
        }
    }

    private fun removeById(id: String) {
        removeByIds(listOf(id))
    }

    fun selectItem(a: OrderResponse?, scroll: Boolean = true) {
        var pos = NO_POSITION
        if (a != null) pos = getIndex(a)
        selectItem(pos, scroll)
    }

    fun selectItem(pos: Int, scroll: Boolean = true) {
        // Si la posición está fuera del rango válido, reseteamos currentIndex a NO_POSITION.
        currentIndex = if (pos < 0 || pos >= itemCount) NO_POSITION else pos
        notifyItemSelectedChanged(currentIndex)
        if (scroll) scrollToPos(currentIndex)
    }

    private fun notifyItemSelectedChanged(pos: Int) {
        notifyItemChanged(currentIndex)
        var item: OrderResponse? = null
        if (pos != NO_POSITION && pos < itemCount) item = getItem(pos)
        selectedItemChangedListener?.onSelectedItemChanged(item)
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

    fun getIndexByHashCode(hashCode: Int): Int {
        return currentList.indexOfFirst { it.hashCode == hashCode }
    }

    private fun getIndex(item: OrderResponse): Int {
        return currentList.indexOfFirst { it == item }
    }

    private fun getItemByHashCode(hashCode: Int): OrderResponse? {
        return fullList.firstOrNull { it.hashCode == hashCode }
    }

    fun getAllChecked(): ArrayList<OrderResponse> {
        val items = ArrayList<OrderResponse>()
        checkedHashArray.mapNotNullTo(items) { getItemByHashCode(it) }
        return items
    }

    fun currentItem(): OrderResponse? {
        if (currentIndex == NO_POSITION) return null
        return if (currentList.any() && itemCount > currentIndex) getItem(currentIndex)
        else null
    }

    fun countChecked(): Int {
        return checkedHashArray.size
    }

    fun totalVisible(): Int {
        return itemCount
    }

    fun firstVisiblePos(): Int {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        return layoutManager.findFirstVisibleItemPosition()
    }

    fun setChecked(item: OrderResponse, isChecked: Boolean, suspendRefresh: Boolean = false) {
        val pos = getIndexByHashCode(item.hashCode)
        if (isChecked) {
            if (!checkedHashArray.contains(item.hashCode)) {
                checkedHashArray.add(item.hashCode)
            }
        } else {
            checkedHashArray.remove(item.hashCode)
        }

        checkedChangedListener?.onCheckedChanged(isChecked, pos)

        // Notificamos al Listener superior
        if (!suspendRefresh) dataSetChangedListener?.onDataSetChanged()
    }

    private var isFilling = false
    fun setChecked(items: ArrayList<OrderResponse>, isChecked: Boolean) {
        if (isFilling) return

        isFilling = true
        for (i in items) {
            setChecked(item = i, isChecked = isChecked, suspendRefresh = true)
        }
        isFilling = false

        // Notificamos al Listener superior
        dataSetChangedListener?.onDataSetChanged()
    }

    fun addVisibleStatus(status: OrderStatus) {
        if (visibleStatus.contains(status)) return
        visibleStatus.add(status)

        refreshFilter()
    }

    fun removeVisibleStatus(status: OrderStatus) {
        if (!visibleStatus.contains(status)) return
        visibleStatus.remove(status)

        // Quitamos los ítems con el estado seleccionado de la lista marcados.
        val uncheckedItems = ArrayList(fullList.mapNotNull { if (it.status == status) it.hashCode else null })
        checkedHashArray.removeAll(uncheckedItems.toSet())

        refreshFilter()
    }

    internal class SelectedViewHolder(val binding: OrderRowExpandedBinding) :
        ViewHolder(binding.root) {
        fun bindCheckBoxVisibility(checkBoxVisibility: Int = GONE) {
            binding.checkBoxConstraintLayout.visibility = checkBoxVisibility
        }

        fun bindCheckBoxState(checked: Boolean) {
            binding.checkBox.isChecked = checked
        }

        fun bind(item: OrderResponse, checkBoxVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)

            binding.descriptionTextView.text =
                item.description.ifEmpty { context.getString(R.string.without_description) }

            val extId = item.externalId
            if (extId.isNotEmpty()) {
                binding.extIdTextView.text = extId
                binding.extIdPanel.visibility = VISIBLE
            } else {
                binding.extIdTextView.text = ""
                binding.extIdPanel.visibility = GONE
            }

            val status = item.status.description
            if (status.isNotEmpty()) {
                binding.statusTextView.text = status
                binding.statusPanel.visibility = VISIBLE
            } else {
                binding.statusTextView.text = ""
                binding.statusPanel.visibility = GONE
            }

            val type = item.orderType.description
            if (type.isNotEmpty()) {
                binding.orderTypeTextView.text = type
                binding.typePanel.visibility = VISIBLE
            } else {
                binding.orderTypeTextView.text = ""
                binding.typePanel.visibility = GONE
            }

            if (type.isEmpty() && status.isEmpty()) binding.orderTypeStatusPanel.visibility = GONE
            else binding.orderTypeStatusPanel.visibility = VISIBLE

            val zone = item.zone
            if (extId.isNotEmpty()) {
                binding.zoneTextView.text = zone
                binding.zonePanel.visibility = VISIBLE
            } else {
                binding.zoneTextView.text = ""
                binding.zonePanel.visibility = GONE
            }

            val startDate = item.startDate
            val finishDate = item.finishDate ?: ""

            if (!startDate.isNullOrEmpty() || finishDate.isNotEmpty()) binding.datesPanel.visibility = VISIBLE
            else binding.datesPanel.visibility = GONE

            binding.creationDateTextView.text = startDate
            binding.finishDateTextView.text = finishDate.ifEmpty { context.getString(R.string.uncompleted) }

            setStyle(item)
        }

        private fun setStyle(item: OrderResponse) {
            val v = itemView

            // Background layouts
            // Resalta por estado del ítem
            val layoutApproved = getDrawable(context.resources, R.drawable.layout_thin_border_status_approved, null)
            val layoutDraft = getDrawable(context.resources, R.drawable.layout_thin_border_status_draft, null)
            val layoutInProcess = getDrawable(context.resources, R.drawable.layout_thin_border_status_in_process, null)
            val layoutInTransit = getDrawable(context.resources, R.drawable.layout_thin_border_status_in_transit, null)
            val layoutDelivered = getDrawable(context.resources, R.drawable.layout_thin_border_status_delivered, null)
            val layoutFinished = getDrawable(context.resources, R.drawable.layout_thin_border_status_finished, null)
            val layoutPending = getDrawable(context.resources, R.drawable.layout_thin_border_status_pending, null)
            val layoutPendingDistribution =
                getDrawable(context.resources, R.drawable.layout_thin_border_status_pending_distribution, null)
            val layoutProcessed = getDrawable(context.resources, R.drawable.layout_thin_border_status_processed, null)
            val layoutNoStatus = getDrawable(context.resources, R.drawable.layout_thin_border_status_no_status, null)
            val layoutActive = getDrawable(context.resources, R.drawable.layout_thin_border_status_active, null)
            val layoutDeactivated =
                getDrawable(context.resources, R.drawable.layout_thin_border_status_deactivated, null)
            val layoutFlashing = getDrawable(context.resources, R.drawable.layout_thin_border_status_flashing, null)

            val backColor: Drawable
            val foreColor: Int

            when (item.status) {
                OrderStatus.approved -> {
                    backColor = layoutApproved!!
                    foreColor = approvedSelectedForeColor
                }

                OrderStatus.active -> {
                    backColor = layoutActive!!
                    foreColor = activeSelectedForeColor
                }

                OrderStatus.draft -> {
                    backColor = layoutDraft!!
                    foreColor = draftSelectedForeColor
                }

                OrderStatus.deactivated -> {
                    backColor = layoutDeactivated!!
                    foreColor = deactivatedSelectedForeColor
                }

                OrderStatus.inProcess -> {
                    backColor = layoutInProcess!!
                    foreColor = inProcessSelectedForeColor
                }

                OrderStatus.inTransit -> {
                    backColor = layoutInTransit!!
                    foreColor = inTransitSelectedForeColor
                }

                OrderStatus.delivered -> {
                    backColor = layoutDelivered!!
                    foreColor = deliveredSelectedForeColor
                }

                OrderStatus.finished -> {
                    backColor = layoutFinished!!
                    foreColor = finishedSelectedForeColor
                }

                OrderStatus.pending -> {
                    backColor = layoutPending!!
                    foreColor = pendingSelectedForeColor
                }

                OrderStatus.pendingDistribution -> {
                    backColor = layoutPendingDistribution!!
                    foreColor = pendingDistributionSelectedForeColor
                }

                OrderStatus.processed -> {
                    backColor = layoutProcessed!!
                    foreColor = processedSelectedForeColor
                }

                OrderStatus.flashing -> {
                    backColor = layoutFlashing!!
                    foreColor = flashingSelectedForeColor
                }

                else -> {
                    backColor = layoutNoStatus!!
                    foreColor = noStatusSelectedForeColor
                }
            }

            val titleForeColor: Int = manipulateColor(foreColor, 0.8f)

            v.background = backColor

            // Values
            binding.descriptionTextView.setTextColor(foreColor)

            binding.statusTextView.setTextColor(foreColor)
            binding.orderTypeTextView.setTextColor(foreColor)
            binding.extIdTextView.setTextColor(foreColor)
            binding.zoneTextView.setTextColor(foreColor)
            binding.creationDateTextView.setTextColor(foreColor)
            binding.finishDateTextView.setTextColor(foreColor)

            // Labels
            binding.statusLabelTextView.setTextColor(titleForeColor)
            binding.orderTypeLabelTextView.setTextColor(titleForeColor)
            binding.extIdLabelTextView.setTextColor(titleForeColor)
            binding.zoneLabelTextView.setTextColor(titleForeColor)
            binding.creationDateLabelTextView.setTextColor(titleForeColor)
            binding.finishDateLabelTextView.setTextColor(titleForeColor)

            // CheckBox
            binding.checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)
        }
    }

    internal class UnselectedViewHolder(val binding: OrderRowBinding) :
        ViewHolder(binding.root) {
        fun bindCheckBoxVisibility(checkBoxVisibility: Int = GONE) {
            binding.checkBoxConstraintLayout.visibility = checkBoxVisibility
        }

        fun bindCheckBoxState(checked: Boolean) {
            binding.checkBox.isChecked = checked
        }

        fun bind(item: OrderResponse, checkBoxVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)

            binding.descriptionTextView.text =
                item.description.ifEmpty { context.getString(R.string.without_description) }
            binding.extIdTextView.text = item.externalId
            binding.creationDateTextView.text = item.startDate

            setStyle(item)
        }

        private fun setStyle(item: OrderResponse) {
            val v = itemView

            val titleForeColor: Int = darkslategray

            // Background layouts
            // Resalta por estado del ítem
            val layoutApproved = getDrawable(context.resources, R.drawable.layout_thin_border_status_approved, null)
            val layoutDraft = getDrawable(context.resources, R.drawable.layout_thin_border_status_draft, null)
            val layoutInProcess = getDrawable(context.resources, R.drawable.layout_thin_border_status_in_process, null)
            val layoutInTransit = getDrawable(context.resources, R.drawable.layout_thin_border_status_in_transit, null)
            val layoutDelivered = getDrawable(context.resources, R.drawable.layout_thin_border_status_delivered, null)
            val layoutFinished = getDrawable(context.resources, R.drawable.layout_thin_border_status_finished, null)
            val layoutPending = getDrawable(context.resources, R.drawable.layout_thin_border_status_pending, null)
            val layoutPendingDistribution =
                getDrawable(context.resources, R.drawable.layout_thin_border_status_pending_distribution, null)
            val layoutProcessed = getDrawable(context.resources, R.drawable.layout_thin_border_status_processed, null)
            val layoutNoStatus = getDrawable(context.resources, R.drawable.layout_thin_border_status_no_status, null)
            val layoutActive = getDrawable(context.resources, R.drawable.layout_thin_border_status_active, null)
            val layoutDeactivated =
                getDrawable(context.resources, R.drawable.layout_thin_border_status_deactivated, null)
            val layoutFlashing = getDrawable(context.resources, R.drawable.layout_thin_border_status_flashing, null)

            val backColor: Drawable
            val foreColor: Int

            when (item.status) {
                OrderStatus.approved -> {
                    backColor = layoutApproved!!
                    foreColor = approvedForeColor
                }

                OrderStatus.active -> {
                    backColor = layoutActive!!
                    foreColor = activeForeColor
                }

                OrderStatus.draft -> {
                    backColor = layoutDraft!!
                    foreColor = draftForeColor
                }

                OrderStatus.deactivated -> {
                    backColor = layoutDeactivated!!
                    foreColor = deactivatedForeColor
                }

                OrderStatus.inProcess -> {
                    backColor = layoutInProcess!!
                    foreColor = inProcessForeColor
                }

                OrderStatus.inTransit -> {
                    backColor = layoutInTransit!!
                    foreColor = inTransitForeColor
                }

                OrderStatus.delivered -> {
                    backColor = layoutDelivered!!
                    foreColor = deliveredForeColor
                }

                OrderStatus.finished -> {
                    backColor = layoutFinished!!
                    foreColor = finishedForeColor
                }

                OrderStatus.pending -> {
                    backColor = layoutPending!!
                    foreColor = pendingForeColor
                }

                OrderStatus.pendingDistribution -> {
                    backColor = layoutPendingDistribution!!
                    foreColor = pendingDistributionForeColor
                }

                OrderStatus.processed -> {
                    backColor = layoutProcessed!!
                    foreColor = processedForeColor
                }

                OrderStatus.flashing -> {
                    backColor = layoutFlashing!!
                    foreColor = flashingForeColor
                }

                else -> {
                    backColor = layoutNoStatus!!
                    foreColor = noStatusForeColor
                }
            }

            v.background = backColor
            binding.extIdTextView.setTextColor(foreColor)
            binding.descriptionTextView.setTextColor(foreColor)
            binding.creationDateTextView.setTextColor(foreColor)
            binding.checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)
        }
    }

    private object ItemDiffUtilCallback : DiffUtil.ItemCallback<OrderResponse>() {
        override fun areItemsTheSame(oldItem: OrderResponse, newItem: OrderResponse): Boolean {
            return oldItem.hashCode == newItem.hashCode
        }

        override fun areContentsTheSame(oldItem: OrderResponse, newItem: OrderResponse): Boolean {
            if (oldItem.id != newItem.id) return false
            return oldItem.hashCode == newItem.hashCode
        }
    }

    companion object {
        // Aquí definimos dos constantes para identificar los dos diseños diferentes
        const val SELECTED_VIEW_TYPE = 1
        const val UNSELECTED_VIEW_TYPE = 2

        // region COLORS
        private var approvedForeColor: Int = 0
        private var draftForeColor: Int = 0
        private var inProcessForeColor: Int = 0
        private var inTransitForeColor: Int = 0
        private var deliveredForeColor: Int = 0
        private var finishedForeColor: Int = 0
        private var pendingForeColor: Int = 0
        private var pendingDistributionForeColor: Int = 0
        private var processedForeColor: Int = 0
        private var noStatusForeColor: Int = 0
        private var activeForeColor: Int = 0
        private var deactivatedForeColor: Int = 0
        private var flashingForeColor: Int = 0

        private var approvedSelectedForeColor: Int = 0
        private var draftSelectedForeColor: Int = 0
        private var inProcessSelectedForeColor: Int = 0
        private var inTransitSelectedForeColor: Int = 0
        private var deliveredSelectedForeColor: Int = 0
        private var finishedSelectedForeColor: Int = 0
        private var pendingSelectedForeColor: Int = 0
        private var pendingDistributionSelectedForeColor: Int = 0
        private var processedSelectedForeColor: Int = 0
        private var noStatusSelectedForeColor: Int = 0
        private var activeSelectedForeColor: Int = 0
        private var deactivatedSelectedForeColor: Int = 0
        private var flashingSelectedForeColor: Int = 0

        private var darkslategray: Int = 0
        private var lightgray: Int = 0

        /**
         * Setup colors
         * Simplemente inicializamos algunas variables con los colores que vamos a usar para cada estado.
         */
        private fun setupColors() {
            // Color de los diferentes estados
            val statusApproved = getColor(context.resources, R.color.status_approved, null)
            val statusDraft = getColor(context.resources, R.color.status_draft, null)
            val statusInProcess = getColor(context.resources, R.color.status_in_process, null)
            val statusInTransit = getColor(context.resources, R.color.status_in_transit, null)
            val statusDelivered = getColor(context.resources, R.color.status_delivered, null)
            val statusFinished = getColor(context.resources, R.color.status_finished, null)
            val statusPending = getColor(context.resources, R.color.status_pending, null)
            val statusPendingDistribution = getColor(context.resources, R.color.status_pending_distribution, null)
            val statusProcessed = getColor(context.resources, R.color.status_processed, null)
            val statusNoStatus = getColor(context.resources, R.color.status_no_status, null)
            val statusActive = getColor(context.resources, R.color.status_active, null)
            val statusDeactivated = getColor(context.resources, R.color.status_deactivated, null)
            val statusFlashing = getColor(context.resources, R.color.status_flashing, null)

            // Mejor contraste para los ítems seleccionados
            approvedSelectedForeColor = getBestContrastColor(manipulateColor(statusApproved, 0.5f))
            draftSelectedForeColor = getBestContrastColor(manipulateColor(statusDraft, 0.5f))
            inProcessSelectedForeColor = getBestContrastColor(manipulateColor(statusInProcess, 0.5f))
            inTransitSelectedForeColor = getBestContrastColor(manipulateColor(statusInTransit, 0.5f))
            deliveredSelectedForeColor = getBestContrastColor(manipulateColor(statusDelivered, 0.5f))
            finishedSelectedForeColor = getBestContrastColor(manipulateColor(statusFinished, 0.5f))
            pendingSelectedForeColor = getBestContrastColor(manipulateColor(statusPending, 0.5f))
            pendingDistributionSelectedForeColor =
                getBestContrastColor(manipulateColor(statusPendingDistribution, 0.5f))
            processedSelectedForeColor = getBestContrastColor(manipulateColor(statusProcessed, 0.5f))
            noStatusSelectedForeColor = getBestContrastColor(manipulateColor(statusNoStatus, 0.5f))
            activeSelectedForeColor = getBestContrastColor(manipulateColor(statusActive, 0.5f))
            deactivatedSelectedForeColor = getBestContrastColor(manipulateColor(statusDeactivated, 0.5f))
            flashingSelectedForeColor = getBestContrastColor(manipulateColor(statusFlashing, 0.5f))

            // Mejor contraste para los ítems no seleccionados
            approvedForeColor = getBestContrastColor(statusApproved)
            draftForeColor = getBestContrastColor(statusDraft)
            inProcessForeColor = getBestContrastColor(statusInProcess)
            inTransitForeColor = getBestContrastColor(statusInTransit)
            deliveredForeColor = getBestContrastColor(statusDelivered)
            finishedForeColor = getBestContrastColor(statusFinished)
            pendingForeColor = getBestContrastColor(statusPending)
            pendingDistributionForeColor = getBestContrastColor(statusPendingDistribution)
            processedForeColor = getBestContrastColor(statusProcessed)
            noStatusForeColor = getBestContrastColor(statusNoStatus)
            activeForeColor = getBestContrastColor(statusActive)
            deactivatedForeColor = getBestContrastColor(statusDeactivated)
            flashingForeColor = getBestContrastColor(statusFlashing)

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
        checkedHashArray = builder.checkedHashArray
        multiSelect = builder.multiSelect
        showCheckBoxes = builder.showCheckBoxes
        showCheckBoxesChanged = builder.showCheckBoxesChanged
        visibleStatus = builder.visibleStatus
        filterOptions = builder.filterOptions

        dataSetChangedListener = builder.dataSetChangedListener
        selectedItemChangedListener = builder.selectedItemChangedListener
        checkedChangedListener = builder.checkedChangedListener

        // Configuramos los estados visibles al usuario
        if (visibleStatus.isEmpty()) visibleStatus = OrderStatus.getAll()

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
    private fun sortedVisibleList(list: MutableList<OrderResponse>?): MutableList<OrderResponse> {
        val croppedList = (list ?: mutableListOf()).mapNotNull { if (mustBeVisible(it)) it else null }
        return sortItems(croppedList.toMutableList())
    }

    // Sobrecargamos estos métodos para suministrar siempre una lista ordenada y filtrada por estado de visibilidad
    override fun submitList(list: MutableList<OrderResponse>?) {
        super.submitList(sortedVisibleList(list))
    }

    override fun submitList(list: MutableList<OrderResponse>?, commitCallback: Runnable?) {
        super.submitList(sortedVisibleList(list), commitCallback)
    }

    /** Acá seleccionamos siguiendo estos criterios:
     * 1. Si NO es [multiSelect] tomamos el ítem seleccionado de forma simple.
     * 2. Si es [multiSelect] nos fijamos que o bien estén marcados algunos ítems o bien tengamos un ítem seleccionado de forma simple.
     *     1. Si es así, vamos a devolver los ítems marcados si existen como prioridad.
     *     2. Si no, nos fijamos que NO sean visibles los CheckBoxes, esto quiere decir que el usuario está seleccionado el ítem de forma simple y devolvemos este ítem.
     **/
    fun selectedOrders(): ArrayList<OrderResponse> {
        val item = currentItem()
        val countChecked = countChecked()
        var itemArray: ArrayList<OrderResponse> = ArrayList()

        if (!multiSelect && item != null) {
            itemArray = arrayListOf(item)
        } else if (multiSelect) {
            if (countChecked > 0 || item != null) {
                if (countChecked > 0) {
                    itemArray = getAllChecked()
                } else if (!showCheckBoxes) {
                    itemArray = arrayListOf(item!!)
                }
                itemArray.map { it } as ArrayList<OrderResponse>
            }
        }
        return itemArray
    }

    class Builder {
        fun build(): OrderAdapter {
            return OrderAdapter(this)
        }

        internal lateinit var recyclerView: RecyclerView
        internal var fullList: ArrayList<OrderResponse> = ArrayList()
        internal var checkedHashArray: ArrayList<Int> = ArrayList()
        internal var multiSelect: Boolean = false
        internal var showCheckBoxes: Boolean = false
        internal var showCheckBoxesChanged: (Boolean) -> Unit = { }
        internal var visibleStatus: ArrayList<OrderStatus> = ArrayList()
        internal var filterOptions: FilterOptions = FilterOptions()

        internal var dataSetChangedListener: DataSetChangedListener? = null
        internal var selectedItemChangedListener: SelectedItemChangedListener? = null
        internal var checkedChangedListener: CheckedChangedListener? = null

        // Setter methods for variables with chained methods
        @Suppress("unused")
        fun recyclerView(`val`: RecyclerView): Builder {
            recyclerView = `val`
            return this
        }

        @Suppress("unused")
        fun fullList(`val`: ArrayList<OrderResponse>): Builder {
            fullList = `val`
            return this
        }

        @Suppress("unused")
        fun checkedHashArray(`val`: ArrayList<Int>): Builder {
            checkedHashArray = `val`
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
        fun visibleStatus(`val`: ArrayList<OrderStatus>): Builder {
            visibleStatus = `val`
            return this
        }

        @Suppress("unused")
        fun filterOptions(`val`: FilterOptions): Builder {
            filterOptions = `val`
            return this
        }

        // Setter methods for variables with chained methods
        @Suppress("unused")
        fun dataSetChangedListener(listener: DataSetChangedListener?): Builder {
            dataSetChangedListener = listener
            return this
        }

        @Suppress("unused")
        fun selectedItemChangedListener(listener: SelectedItemChangedListener?): Builder {
            selectedItemChangedListener = listener
            return this
        }

        @Suppress("unused")
        fun checkedChangedListener(listener: CheckedChangedListener?): Builder {
            checkedChangedListener = listener
            return this
        }
    }
}
