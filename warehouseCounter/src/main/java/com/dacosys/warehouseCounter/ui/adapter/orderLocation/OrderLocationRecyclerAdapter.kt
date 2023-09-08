package com.dacosys.warehouseCounter.ui.adapter.orderLocation

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
import androidx.recyclerview.widget.RecyclerView.*
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderLocation
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderLocation.OrderLocationStatus
import com.dacosys.warehouseCounter.databinding.OrderLocationRowBinding
import com.dacosys.warehouseCounter.databinding.OrderLocationRowExpandedBinding
import com.dacosys.warehouseCounter.ui.adapter.FilterOptions
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.manipulateColor
import java.util.*


class OrderLocationRecyclerAdapter private constructor(builder: Builder) :
    ListAdapter<OrderLocation, ViewHolder>(ItemDiffUtilCallback), Filterable {

    private var recyclerView: RecyclerView
    var fullList: ArrayList<OrderLocation> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()
    private var multiSelect: Boolean = false
    var showCheckBoxes: Boolean = false
    private var showCheckBoxesChanged: (Boolean) -> Unit = { }
    private var visibleStatus: ArrayList<OrderLocationStatus> = ArrayList(OrderLocationStatus.values().toList())
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
        checkedIdArray.clear()

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
        fun onSelectedItemChanged(item: OrderLocation?)
    }

    // El método onCreateViewHolder infla los diseños para cada tipo de vista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            SELECTED_VIEW_TYPE -> {
                SelectedViewHolder(
                    OrderLocationRowExpandedBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            else -> {
                UnselectedViewHolder(
                    OrderLocationRowBinding.inflate(
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
                        (holder as SelectedViewHolder).bindCheckBoxState(checkedIdArray.contains(item.uniqueId))
                    else
                        (holder as UnselectedViewHolder).bindCheckBoxState(checkedIdArray.contains(item.uniqueId))
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
    private fun setCheckBoxLogic(checkBox: CheckBox, item: OrderLocation, position: Int) {
        val checkChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            setChecked(item = item, isChecked = isChecked, suspendRefresh = true)
        }

        val longClickListener = OnLongClickListener { _ ->
            checkBox.setOnCheckedChangeListener(null)

            // Notificamos los cambios solo a los ítems que cambian de estado.
            val newState = !checkBox.isChecked
            if (newState) {
                currentList.mapIndexed { pos, item ->
                    if (item.uniqueId !in checkedIdArray) {
                        checkedIdArray.add(item.uniqueId)
                        notifyItemChanged(pos, PAYLOADS.CHECKBOX_STATE)
                    }
                }
            } else {
                currentList.mapIndexed { pos, item ->
                    if (item.uniqueId in checkedIdArray) {
                        checkedIdArray.remove(item.uniqueId)
                        notifyItemChanged(pos, PAYLOADS.CHECKBOX_STATE)
                    }
                }
            }

            dataSetChangedListener?.onDataSetChanged()
            return@OnLongClickListener true
        }

        // Important to remove previous checkedChangedListener before calling setChecked
        checkBox.setOnCheckedChangeListener(null)

        checkBox.isChecked = checkedIdArray.contains(item.uniqueId)
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
            var selected: OrderLocation? = null
            var firstVisible: OrderLocation? = null

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                // Guardamos el item seleccionado y la posición del scroll
                selected = currentItem()
                var scrollPos =
                    (recyclerView.layoutManager as LinearLayoutManager?)?.findFirstVisibleItemPosition()
                        ?: NO_POSITION

                if (scrollPos != NO_POSITION && itemCount > scrollPos) {
                    var currentScrolled = getItem(scrollPos)

                    // Comprobamos si es visible el ítem del Scroll
                    if (currentScrolled.itemStatus in visibleStatus)
                        firstVisible = currentScrolled
                    else {
                        // Si no es visible, intentar encontrar el próximo visible.
                        while (firstVisible == null) {
                            scrollPos++
                            if (itemCount > scrollPos) {
                                currentScrolled = getItem(scrollPos)
                                if (currentScrolled.itemStatus in visibleStatus)
                                    firstVisible = currentScrolled
                            } else break
                        }
                    }
                }

                // Filtramos los resultados
                val results = FilterResults()
                var r: ArrayList<OrderLocation> = ArrayList()
                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    if (filterString.isNotEmpty()) {
                        var filterableItem: OrderLocation

                        for (i in 0 until fullList.size) {
                            filterableItem = fullList[i]

                            // Descartamos aquellos que no debe ser visibles
                            if (filterableItem.itemStatus !in visibleStatus) continue

                            if (isFilterable(filterableItem, filterString)) {
                                r.add(filterableItem)
                            }
                        }
                    } else if (filterOptions.showAllOnFilterEmpty) {
                        r = ArrayList(fullList.mapNotNull { if (it.itemStatus in visibleStatus) it else null })
                    }
                }

                results.values = r
                results.count = r.size
                return results
            }

            fun isFilterable(filterableItem: OrderLocation, filterString: String): Boolean =
                (filterableItem.itemDescription.contains(filterString, true) ||
                        filterableItem.itemEan.contains(filterString, true) ||
                        filterableItem.itemExternalId.toString().contains(filterString, true) ||
                        filterableItem.orderExternalId.contains(filterString, true) ||
                        filterableItem.orderDescription.contains(filterString, true))

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?, results: FilterResults?,
            ) {
                submitList(results?.values as ArrayList<OrderLocation>) {
                    // Notificamos al Listener superior
                    dataSetChangedListener?.onDataSetChanged()

                    // Recuperamos el item seleccionado y la posición del scroll
                    if (firstVisible != null)
                        scrollToPos(getIndexById(firstVisible?.uniqueId ?: -1), true)
                    if (selected != null)
                        selectItem(selected, false)
                }
            }
        }
    }

    private fun sortItems(originalList: MutableList<OrderLocation>): ArrayList<OrderLocation> {
        // Run the follow method on each of the roots
        return ArrayList(
            originalList.sortedWith(
                compareBy(
                    { it.itemDescription },
                    { it.itemEan },
                    { it.itemExternalId },
                    { it.orderExternalId },
                    { it.orderDescription }
                )
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

    fun add(item: OrderLocation, position: Int) {
        fullList.add(position, item)
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

    fun selectItem(a: OrderLocation?, scroll: Boolean = true) {
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
        var item: OrderLocation? = null
        if (pos != NO_POSITION) item = getItem(pos)
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

    fun getIndexById(id: Long): Int {
        return currentList.indexOfFirst { it.uniqueId == id }
    }

    private fun getIndex(item: OrderLocation): Int {
        return currentList.indexOfFirst { it == item }
    }

    private fun getItemById(id: Long): OrderLocation? {
        return fullList.firstOrNull { it.uniqueId == id }
    }

    fun getAllChecked(): ArrayList<OrderLocation> {
        val items = ArrayList<OrderLocation>()
        checkedIdArray.mapNotNullTo(items) { getItemById(it) }
        return items
    }

    fun currentItem(): OrderLocation? {
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

    fun setChecked(item: OrderLocation, isChecked: Boolean, suspendRefresh: Boolean = false) {
        val pos = getIndexById(item.uniqueId)
        if (isChecked) {
            if (!checkedIdArray.contains(item.uniqueId)) {
                checkedIdArray.add(item.uniqueId)
            }
        } else {
            checkedIdArray.remove(item.uniqueId)
        }

        checkedChangedListener?.onCheckedChanged(isChecked, pos)

        // Notificamos al Listener superior
        if (!suspendRefresh) dataSetChangedListener?.onDataSetChanged()
    }

    private var isFilling = false
    fun setChecked(items: ArrayList<OrderLocation>, isChecked: Boolean) {
        if (isFilling) return

        isFilling = true
        for (i in items) {
            setChecked(item = i, isChecked = isChecked, suspendRefresh = true)
        }
        isFilling = false

        // Notificamos al Listener superior
        dataSetChangedListener?.onDataSetChanged()
    }

    fun addVisibleStatus(status: OrderLocationStatus) {
        if (visibleStatus.contains(status)) return
        visibleStatus.add(status)

        refreshFilter()
    }

    fun removeVisibleStatus(status: OrderLocationStatus) {
        if (!visibleStatus.contains(status)) return
        visibleStatus.remove(status)

        // Quitamos los ítems con el estado seleccionado de la lista marcados.
        val uncheckedItems = ArrayList(fullList.mapNotNull { if (it.itemStatus == status) it.uniqueId else null })
        checkedIdArray.removeAll(uncheckedItems.toSet())

        refreshFilter()
    }

    internal class SelectedViewHolder(val binding: OrderLocationRowExpandedBinding) :
        ViewHolder(binding.root) {
        fun bindCheckBoxVisibility(checkBoxVisibility: Int = GONE) {
            binding.checkBoxConstraintLayout.visibility = checkBoxVisibility
        }

        fun bindCheckBoxState(checked: Boolean) {
            binding.checkBox.isChecked = checked
        }

        fun bind(item: OrderLocation, checkBoxVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)

            binding.eanTextView.text = item.itemEan
            binding.qtyTextView.text = item.qtyCollected?.toString()
            binding.descriptionTextView.text = item.itemDescription
            binding.orderNbrTextView.text = item.orderId.toString()
            binding.orderDescriptionTextView.text = item.orderDescription

            val extId = item.itemExternalId ?: ""
            if (extId.isNotEmpty()) {
                binding.extIdTextView.text = item.itemExternalId
                binding.extIdPanel.visibility = VISIBLE
            } else {
                binding.extIdTextView.text = ""
                binding.extIdPanel.visibility = GONE
            }

            binding.warehouseTextView.text = item.warehouseDescription
            binding.warehouseAreaTextView.text = item.warehouseAreaDescription
            binding.rackTextView.text = item.rackCode

            setStyle(item)
        }

        private fun setStyle(item: OrderLocation) {
            val v = itemView

            // Background layouts
            // Resalta por estado del ítem
            val layoutPicked = getDrawable(context.resources, R.drawable.layout_thin_border, null)
            val layoutNotPicked = getDrawable(context.resources, R.drawable.layout_thin_border_gray, null)

            val backColor: Drawable
            val foreColor: Int

            when (item.itemStatus) {
                OrderLocationStatus.PICKED -> {
                    backColor = layoutPicked!!
                    foreColor = pickedSelectedForeColor
                }

                OrderLocationStatus.NOT_PICKED -> {
                    backColor = layoutNotPicked!!
                    foreColor = notPickedSelectedForeColor
                }
            }

            val titleForeColor: Int = manipulateColor(foreColor, 0.8f)

            v.background = backColor
            binding.eanTextView.setTextColor(foreColor)
            binding.qtyTextView.setTextColor(foreColor)
            binding.descriptionTextView.setTextColor(foreColor)
            binding.orderNbrTextView.setTextColor(foreColor)
            binding.orderDescriptionTextView.setTextColor(foreColor)
            binding.extIdTextView.setTextColor(foreColor)
            binding.warehouseTextView.setTextColor(foreColor)
            binding.warehouseAreaTextView.setTextColor(foreColor)
            binding.rackTextView.setTextColor(foreColor)
            binding.checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)

            // Titles
            binding.orderLabelTextView.setTextColor(titleForeColor)
            binding.locationLabelTextView.setTextColor(titleForeColor)
            binding.descriptionLabelTextView.setTextColor(titleForeColor)
            binding.extIdLabelTextView.setTextColor(titleForeColor)
        }
    }

    internal class UnselectedViewHolder(val binding: OrderLocationRowBinding) :
        ViewHolder(binding.root) {
        fun bindCheckBoxVisibility(checkBoxVisibility: Int = GONE) {
            binding.checkBoxConstraintLayout.visibility = checkBoxVisibility
        }

        fun bindCheckBoxState(checked: Boolean) {
            binding.checkBox.isChecked = checked
        }

        fun bind(item: OrderLocation, checkBoxVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)

            binding.eanTextView.text = item.itemEan
            binding.qtyTextView.text = item.qtyCollected?.toString()
            binding.descriptionTextView.text = item.itemDescription
            binding.orderNbrTextView.text = item.orderId.toString()
            binding.orderDescriptionTextView.text = item.orderDescription

            setStyle(item)
        }

        private fun setStyle(item: OrderLocation) {
            val v = itemView

            // Background layouts
            // Resalta por estado del ítem
            val layoutPicked = getDrawable(context.resources, R.drawable.layout_thin_border, null)
            val layoutNotPicked = getDrawable(context.resources, R.drawable.layout_thin_border_gray, null)

            val backColor: Drawable
            val foreColor: Int
            val titleForeColor: Int = darkslategray

            when (item.itemStatus) {
                OrderLocationStatus.PICKED -> {
                    backColor = layoutPicked!!
                    foreColor = pickedForeColor
                }

                OrderLocationStatus.NOT_PICKED -> {
                    backColor = layoutNotPicked!!
                    foreColor = notPickedForeColor
                }
            }

            v.background = backColor
            binding.eanTextView.setTextColor(foreColor)
            binding.qtyTextView.setTextColor(foreColor)
            binding.descriptionTextView.setTextColor(foreColor)
            binding.orderNbrTextView.setTextColor(foreColor)
            binding.orderDescriptionTextView.setTextColor(foreColor)

            binding.checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)
            binding.orderLabelTextView.setTextColor(titleForeColor)
        }
    }

    private object ItemDiffUtilCallback : DiffUtil.ItemCallback<OrderLocation>() {
        override fun areItemsTheSame(oldItem: OrderLocation, newItem: OrderLocation): Boolean {
            return oldItem.uniqueId == newItem.uniqueId
        }

        override fun areContentsTheSame(oldItem: OrderLocation, newItem: OrderLocation): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    companion object {

        // Aquí definimos dos constantes para identificar los dos diseños diferentes
        const val SELECTED_VIEW_TYPE = 1
        const val UNSELECTED_VIEW_TYPE = 2

        // region COLORS
        private var pickedForeColor: Int = 0
        private var notPickedForeColor: Int = 0

        private var pickedSelectedForeColor: Int = 0
        private var notPickedSelectedForeColor: Int = 0

        private var darkslategray: Int = 0

        /**
         * Setup colors
         * Simplemente inicializamos algunas variables con los colores que vamos a usar para cada estado.
         */
        private fun setupColors() {
            // Color de los diferentes estados
            val picked = getColor(context.resources, R.color.status_lot_active, null)
            val notPicked = getColor(context.resources, R.color.status_lot_inactive, null)

            // Mejor contraste para los ítems seleccionados
            pickedSelectedForeColor = getBestContrastColor(manipulateColor(picked, 0.5f))
            notPickedSelectedForeColor = getBestContrastColor(manipulateColor(notPicked, 0.5f))

            // Mejor contraste para los ítems no seleccionados
            pickedForeColor = getBestContrastColor(picked)
            notPickedForeColor = getBestContrastColor(notPicked)

            // CheckBox color
            darkslategray = getColor(context.resources, R.color.darkslategray, null)
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
        selectedItemChangedListener = builder.selectedItemChangedListener
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
    private fun sortedVisibleList(list: MutableList<OrderLocation>?): MutableList<OrderLocation> {
        val croppedList = (list ?: mutableListOf()).mapNotNull { if (it.itemStatus in visibleStatus) it else null }
        return sortItems(croppedList.toMutableList())
    }

    // Sobrecargamos estos métodos para suministrar siempre una lista ordenada y filtrada por estado de visibilidad
    override fun submitList(list: MutableList<OrderLocation>?) {
        super.submitList(sortedVisibleList(list))
    }

    override fun submitList(list: MutableList<OrderLocation>?, commitCallback: Runnable?) {
        super.submitList(sortedVisibleList(list), commitCallback)
    }

    fun selectedLocations(): ArrayList<OrderLocation> {
        val item = currentItem()
        val countChecked = countChecked()
        var itemArray: ArrayList<OrderLocation> = ArrayList()

        if (!multiSelect && item != null) {
            itemArray = arrayListOf(item)
        } else if (multiSelect) {
            if (countChecked > 0 || item != null) {
                if (countChecked > 0) {
                    itemArray = getAllChecked()
                } else if (!showCheckBoxes) {
                    itemArray = arrayListOf(item!!)
                }
                itemArray.map { it } as ArrayList<OrderLocation>
            }
        }
        return itemArray
    }

    class Builder {
        fun build(): OrderLocationRecyclerAdapter {
            return OrderLocationRecyclerAdapter(this)
        }

        internal lateinit var recyclerView: RecyclerView
        internal var fullList: ArrayList<OrderLocation> = ArrayList()
        internal var checkedIdArray: ArrayList<Long> = ArrayList()
        internal var multiSelect: Boolean = false
        internal var showCheckBoxes: Boolean = false
        internal var showCheckBoxesChanged: (Boolean) -> Unit = { }
        internal var visibleStatus: ArrayList<OrderLocationStatus> = ArrayList(OrderLocationStatus.values().toList())
        internal var filterOptions: FilterOptions = FilterOptions()

        internal var dataSetChangedListener: DataSetChangedListener? = null
        internal var selectedItemChangedListener: SelectedItemChangedListener? = null
        internal var checkedChangedListener: CheckedChangedListener? = null

        @Suppress("unused")
        fun recyclerView(`val`: RecyclerView): Builder {
            recyclerView = `val`
            return this
        }

        @Suppress("unused")
        fun fullList(`val`: ArrayList<OrderLocation>): Builder {
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
        fun showCheckBoxes(`val`: Boolean, callback: (Boolean) -> Unit): Builder {
            showCheckBoxes = `val`
            showCheckBoxesChanged = callback
            return this
        }

        @Suppress("unused")
        fun visibleStatus(`val`: ArrayList<OrderLocationStatus>): Builder {
            visibleStatus = `val`
            return this
        }

        @Suppress("unused")
        fun filterOptions(`val`: FilterOptions): Builder {
            filterOptions = `val`
            return this
        }

        @Suppress("unused")
        fun dataSetChangedListener(`val`: DataSetChangedListener): Builder {
            dataSetChangedListener = `val`
            return this
        }

        @Suppress("unused")
        fun selectedItemChangedListener(`val`: SelectedItemChangedListener): Builder {
            selectedItemChangedListener = `val`
            return this
        }

        @Suppress("unused")
        fun checkedChangedListener(`val`: CheckedChangedListener): Builder {
            checkedChangedListener = `val`
            return this
        }
    }
}
