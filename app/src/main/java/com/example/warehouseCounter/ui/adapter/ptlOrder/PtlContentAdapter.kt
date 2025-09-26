package com.example.warehouseCounter.ui.adapter.ptlOrder

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
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.ResourcesCompat.getColor
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
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.ContentStatus
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlContent
import com.example.warehouseCounter.data.ktor.v1.dto.ptlOrder.PtlItem
import com.example.warehouseCounter.databinding.PtlItemRowBinding
import com.example.warehouseCounter.databinding.PtlItemRowExpandedBinding
import com.example.warehouseCounter.misc.utils.NumberUtils.Companion.roundToString
import com.example.warehouseCounter.ui.adapter.FilterOptions
import com.example.warehouseCounter.ui.snackBar.MakeText
import com.example.warehouseCounter.ui.snackBar.SnackBarType
import com.example.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import com.example.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.example.warehouseCounter.ui.utils.Colors.Companion.manipulateColor
import java.util.*

class PtlContentAdapter private constructor(builder: Builder) :
    ListAdapter<PtlContent, ViewHolder>(PtlContentDiffUtilCallback), Filterable {

    private val recyclerView: RecyclerView
    var fullList: ArrayList<PtlContent>
    var checkedIdArray: ArrayList<Long> = ArrayList()
    var multiSelect: Boolean = false
    private var allowEditQty: Boolean = false
    private var showQtyPanel: Boolean = false
    var showCheckBoxes: Boolean = false
    private var showCheckBoxesChanged: (Boolean) -> Unit = { }
    var visibleStatus: ArrayList<ContentStatus> = ArrayList(ContentStatus.values().toList())
    private var filterOptions: FilterOptions = FilterOptions()

    private var dataSetChangedListener: DataSetChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null
    private var editQtyListener: EditQtyListener? = null

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

    private object PtlContentDiffUtilCallback : DiffUtil.ItemCallback<PtlContent>() {
        override fun areItemsTheSame(oldItem: PtlContent, newItem: PtlContent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PtlContent, newItem: PtlContent): Boolean {
            if (oldItem.externalId != newItem.externalId) return false
            if (oldItem.orderId != newItem.orderId) return false
            if (oldItem.itemId != newItem.itemId) return false
            if (oldItem.item != newItem.item) return false
            if (oldItem.qtyRequested != newItem.qtyRequested) return false
            if (oldItem.qtyCollected != newItem.qtyCollected) return false
            return oldItem.lotId == newItem.lotId
        }
    }

    companion object {
        // Aquí definimos dos constantes para identificar los dos diseños diferentes
        const val SELECTED_VIEW_TYPE = 1
        const val UNSELECTED_VIEW_TYPE = 2

        // region COLORS
        private var collQtyEqualForeColor: Int = 0
        private var collQtyLessForeColor: Int = 0
        private var collQtyMoreForeColor: Int = 0
        private var defaultForeColor: Int = 0

        private var collQtyEqualSelectedForeColor: Int = 0
        private var collQtyLessSelectedForeColor: Int = 0
        private var collQtyMoreSelectedForeColor: Int = 0
        private var defaultSelectedForeColor: Int = 0

        private fun setupColors() {
            // Color de los diferentes estados
            collQtyEqualForeColor = getBestContrastColor(getColor(context.resources, R.color.qty_equal, null))
            collQtyLessForeColor = getBestContrastColor(getColor(context.resources, R.color.qty_less, null))
            collQtyMoreForeColor = getBestContrastColor(getColor(context.resources, R.color.qty_more, null))
            defaultForeColor = getBestContrastColor(getColor(context.resources, R.color.status_default, null))

            // Mejor contraste para los ítems seleccionados
            collQtyEqualSelectedForeColor = getBestContrastColor(manipulateColor(collQtyEqualForeColor, 0.5f))
            collQtyLessSelectedForeColor = getBestContrastColor(manipulateColor(collQtyLessForeColor, 0.5f))
            collQtyMoreSelectedForeColor = getBestContrastColor(manipulateColor(collQtyMoreForeColor, 0.5f))
            defaultSelectedForeColor = getBestContrastColor(manipulateColor(defaultForeColor, 0.5f))
        }
        // endregion
    }

    // El método onCreateViewHolder infla los diseños para cada tipo de vista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            SELECTED_VIEW_TYPE -> {
                SelectedViewHolder(
                    PtlItemRowExpandedBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            else -> {
                UnselectedViewHolder(
                    PtlItemRowBinding.inflate(
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
        // Aquí puedes establecer los valores para cada elemento
        holder.itemView.setOnClickListener {

            // Si el elemento ya está seleccionado, quitamos la selección.
            if (currentIndex == holder.bindingAdapterPosition) {
                currentIndex = NO_POSITION
                notifyItemChanged(holder.bindingAdapterPosition)
            } else {
                val previousSelectedItemPosition = currentIndex
                currentIndex = holder.bindingAdapterPosition
                notifyItemChanged(currentIndex)

                if (previousSelectedItemPosition != NO_POSITION) {
                    notifyItemChanged(previousSelectedItemPosition)
                }
            }

            // Seleccionamos el ítem
            holder.itemView.isSelected = currentIndex == position
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
        val ptlContent = getItem(position)

        // Lógica de clic largo sobre el ítem
        setItemCheckBoxLogic(holder.itemView)

        holder.bind(
            content = ptlContent,
            checkBoxVisibility = if (showCheckBoxes) VISIBLE else GONE
        )

        holder.itemView.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            getColorWithAlpha(colorId = R.color.lightslategray, alpha = 220), BlendModeCompat.MODULATE
        )

        // Acciones del checkBox de marcado
        setCheckBoxLogic(holder.binding.checkBox, ptlContent, position)

        if (allowEditQty) {
            setQtyEditLogic(holder.binding.editQty, ptlContent, position)
            holder.binding.editQty.visibility = VISIBLE
        } else {
            holder.binding.editQty.visibility = GONE
        }

        if (showQtyPanel) {
            holder.binding.qtyReqPanel.visibility = VISIBLE
            holder.binding.eanDivider.visibility = VISIBLE
        } else {
            holder.binding.qtyReqPanel.visibility = GONE
            holder.binding.eanDivider.visibility = GONE
        }
    }

    private fun setUnselectedHolder(holder: UnselectedViewHolder, position: Int) {
        val ptlContent = getItem(position)

        // Lógica de clic largo sobre el ítem
        setItemCheckBoxLogic(holder.itemView)

        // Perform a full update
        holder.bind(
            content = ptlContent,
            checkBoxVisibility = if (showCheckBoxes) VISIBLE else GONE
        )

        holder.itemView.background.colorFilter = null

        setCheckBoxLogic(holder.binding.checkBox, ptlContent, position)

        if (showQtyPanel) {
            holder.binding.qtyReqPanel.visibility = VISIBLE
            holder.binding.eanDivider.visibility = VISIBLE
        } else {
            holder.binding.qtyReqPanel.visibility = GONE
            holder.binding.eanDivider.visibility = GONE
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
                multiplier = settingsVm.scanMultiplier
            )
            true
        }
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
     * @param ptlContent Datos del ítem
     * @param position Posición en el adaptador
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setCheckBoxLogic(checkBox: CheckBox, ptlContent: PtlContent, position: Int) {
        val checkChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            setChecked(content = ptlContent, isChecked = isChecked, suspendRefresh = true)
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

        checkBox.isChecked = checkedIdArray.contains(ptlContent.id)
        checkBox.isLongClickable = true
        checkBox.tag = position

        checkBox.setOnLongClickListener(longClickListener)
        checkBox.setOnCheckedChangeListener(checkChangeListener)
    }

    // El método getItemCount devuelve el número de elementos en la lista
    override fun getItemCount(): Int {
        return currentList.size
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
        var selected: PtlContent? = null
        var firstVisible: PtlContent? = null

        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                // Guardamos el item seleccionado y la posición del scroll
                selected = currentItem()
                var scrollPos =
                    (recyclerView.layoutManager as LinearLayoutManager?)?.findFirstVisibleItemPosition()
                        ?: NO_POSITION

                if (scrollPos != NO_POSITION && itemCount > scrollPos) {
                    var currentScrolled = getItem(scrollPos)

                    // Comprobamos si es visible el ítem del Scroll
                    if (currentScrolled.contentStatus in visibleStatus)
                        firstVisible = currentScrolled
                    else {
                        // Si no es visible, intentar encontrar el próximo visible.
                        while (firstVisible == null) {
                            scrollPos++
                            if (itemCount > scrollPos) {
                                currentScrolled = getItem(scrollPos)
                                if (currentScrolled.contentStatus in visibleStatus)
                                    firstVisible = currentScrolled
                            } else break
                        }
                    }
                }

                // Filtramos los resultados
                val results = FilterResults()
                var r: ArrayList<PtlContent> = ArrayList()
                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    if (filterString.isNotEmpty()) {
                        var filterableItem: PtlContent

                        for (i in 0 until fullList.size) {
                            filterableItem = fullList[i]

                            // Descartamos aquellos que no debe ser visibles
                            if (filterableItem.contentStatus !in visibleStatus) continue

                            if (isFilterable(filterableItem.item.first(), filterString)) {
                                r.add(filterableItem)
                            }
                        }
                    } else if (filterOptions.showAllOnFilterEmpty) {
                        r = ArrayList(fullList.map { it })
                    }
                }

                results.values = r
                results.count = r.size
                return results
            }

            fun isFilterable(ptlItem: PtlItem, filterString: String): Boolean =
                ptlItem.description.contains(filterString, true) ||
                        ptlItem.ean.contains(filterString, true)

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?, results: FilterResults?,
            ) {
                submitList(results?.values as ArrayList<PtlContent>) {
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

    private fun sortItems(originalList: MutableList<PtlContent>): ArrayList<PtlContent> {
        return ArrayList(
            originalList.sortedWith(
                compareBy(
                    { it.id },
                    { it.itemId },
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

    fun add(content: PtlContent, position: Int) {
        fullList.add(position, content)
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

    /**
     * Se utiliza cuando se edita un contenido y necesita actualizarse
     */
    fun updateQtyCollected(itemId: Long, qtyCollected: Double, scrollToPos: Boolean = false) {
        val content = getContentByItemId(itemId) ?: return
        val index = getIndexByItemId(itemId)

        val t = fullList.firstOrNull { it == content } ?: return

        // ¿La cantidad es mayor a la cantidad solicitada?
        if (t.qtyRequested < qtyCollected) {
            // Mostrar un cartelito, si jode sacarlo.
            reportQtyRequestedReached(content)
            return
        }

        t.qtyCollected = qtyCollected

        submitList(fullList).apply {
            notifyItemChanged(index)

            reportQtyCollectedChange(content)

            dataSetChangedListener?.onDataSetChanged()
            selectItem(content, scrollToPos)
        }
    }

    fun selectItem(a: PtlContent?, scroll: Boolean = true) {
        var pos = NO_POSITION
        if (a != null) pos = getIndex(a)
        selectItem(pos, scroll)
    }

    private fun selectItem(pos: Int, scroll: Boolean = true) {
        // Si la posición está fuera del rango válido, reseteamos currentIndex a NO_POSITION.
        currentIndex = if (pos !in 0..<itemCount) NO_POSITION else pos
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

        if (position !in 0..<itemCount) {
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

    private fun reportQtyCollectedChange(content: PtlContent) {
        val itemCode = content.item.firstOrNull()?.ean ?: context.getString(R.string.no_ean)
        val decimalPlaces = 0

        val res = "${itemCode}: ${roundToString(content.qtyRequested, decimalPlaces)}"

        MakeText.makeText(recyclerView, res, SnackBarType.ADD)
    }

    private fun reportQtyRequestedReached(content: PtlContent) {
        val itemCode = content.item.firstOrNull()?.ean ?: context.getString(R.string.no_ean)

        val res = "${itemCode}: ${context.getString(R.string.maximum_reached)}"

        MakeText.makeText(recyclerView, res, SnackBarType.UPDATE)
    }

    fun getIndexById(id: Long): Int {
        return currentList.indexOfFirst { it.id == id }
    }

    private fun getIndex(content: PtlContent): Int {
        return currentList.indexOf(content)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun getIndexByItemId(itemId: Long): Int {
        return currentList.indexOfFirst { it.itemId == itemId }
    }

    @Suppress("unused")
    fun getContent(item: PtlItem): PtlContent? {
        return currentList.firstOrNull { it.itemId == item.id }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun getContentByItemId(itemId: Long): PtlContent? {
        return currentList.firstOrNull { it.itemId == itemId }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun getContentById(id: Long): PtlContent? {
        return currentList.firstOrNull { it.id == id }
    }

    fun getAllChecked(): ArrayList<PtlContent> {
        val items = ArrayList<PtlContent>()
        checkedIdArray.mapNotNullTo(items) { getContentById(it) }
        return items
    }

    fun qtyRequestedTotal() = currentList.sumOf { it.qtyRequested }

    fun qtyCollectedTotal() = currentList.sumOf { it.qtyCollected }

    fun currentItem(): PtlContent? {
        if (currentIndex == NO_POSITION) return null
        return if (currentList.any() && itemCount > currentIndex) getItem(currentIndex)
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

    fun addVisibleStatus(status: ContentStatus) {
        if (visibleStatus.contains(status)) return
        visibleStatus.add(status)

        refreshFilter()
    }

    fun removeVisibleStatus(status: ContentStatus) {
        if (!visibleStatus.contains(status)) return
        visibleStatus.remove(status)

        // Quitamos los ítems con el estado seleccionado de la lista marcados.
        val uncheckedItems = ArrayList(fullList.mapNotNull { if (it.contentStatus == status) it.itemId else null })
        checkedIdArray.removeAll(uncheckedItems.toSet())

        refreshFilter()
    }

    // Aquí creamos dos ViewHolder, uno para cada tipo de vista
    class SelectedViewHolder(val binding: PtlItemRowExpandedBinding) : ViewHolder(binding.root) {
        fun bindCheckBoxVisibility(checkBoxVisibility: Int = GONE) {
            binding.checkBoxConstraintLayout.visibility = checkBoxVisibility
        }

        fun bindCheckBoxState(checked: Boolean) {
            binding.checkBox.isChecked = checked
        }

        fun bind(content: PtlContent, checkBoxVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)

            val item = content.item.first()

            binding.descriptionTextView.text = item.description
            binding.eanTextView.text = item.ean
            binding.qtyCollectedTextView.text = content.qtyCollected.toString()
            binding.qtyRequestedTextView.text = content.qtyRequested.toString()
            binding.itemIdCheckedTextView.text = item.id.toString()
            binding.extIdCheckedTextView.text = content.externalId ?: 0.toString()
            binding.lotIdCheckedTextView.text = content.lotId?.toString()
            binding.extId2ConstraintLayout.visibility = VISIBLE
            binding.extId2TextView.text = item.externalId2
            binding.priceTextView.text = String.format("$ %s", roundToString(item.price, 2))

            setStyle(content)
        }

        private fun setStyle(content: PtlContent) {
            val v = itemView

            // Background layouts
            val collQtyEqualBackColor =
                ResourcesCompat.getDrawable(context.resources, R.drawable.layout_thin_border_green, null)!!
            val collQtyMoreBackColor =
                ResourcesCompat.getDrawable(context.resources, R.drawable.layout_thin_border_blue, null)!!
            val collQtyLessBackColor =
                ResourcesCompat.getDrawable(context.resources, R.drawable.layout_thin_border_red, null)!!

            val backColor: Drawable
            val foreColor: Int
            when (content.contentStatus) {
                ContentStatus.QTY_EQUAL -> {
                    backColor = collQtyEqualBackColor
                    foreColor = collQtyEqualSelectedForeColor
                }

                ContentStatus.QTY_MORE -> {
                    backColor = collQtyMoreBackColor
                    foreColor = collQtyMoreSelectedForeColor
                }

                else -> {
                    backColor = collQtyLessBackColor
                    foreColor = collQtyLessSelectedForeColor
                }
            }

            val titleForeColor: Int = manipulateColor(foreColor, 0.8f)

            v.background = backColor
            binding.descriptionTextView.setTextColor(foreColor)
            binding.eanTextView.setTextColor(foreColor)
            binding.extId2TextView.setTextColor(foreColor)
            binding.priceTextView.setTextColor(foreColor)
            binding.qtyCollectedTextView.setTextColor(foreColor)
            binding.qtyRequestedTextView.setTextColor(foreColor)
            binding.itemIdCheckedTextView.setTextColor(foreColor)
            binding.extIdCheckedTextView.setTextColor(foreColor)
            binding.lotIdCheckedTextView.setTextColor(foreColor)
            binding.checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)

            binding.qtyReqTitleTextView.setTextColor(titleForeColor)
            binding.qtyTitleTextView.setTextColor(titleForeColor)
            binding.itemIdLabelTextView.setTextColor(titleForeColor)
            binding.extIdLabelTextView.setTextColor(titleForeColor)
            binding.lotIdLabelTextView.setTextColor(titleForeColor)
            binding.extId2LabelTextView.setTextColor(titleForeColor)
            binding.priceLabelTextView.setTextColor(titleForeColor)
        }
    }

    internal class UnselectedViewHolder(val binding: PtlItemRowBinding) : ViewHolder(binding.root) {
        fun bindCheckBoxVisibility(checkBoxVisibility: Int = GONE) {
            binding.checkBoxConstraintLayout.visibility = checkBoxVisibility
        }

        fun bindCheckBoxState(checked: Boolean) {
            binding.checkBox.isChecked = checked
        }

        fun bind(content: PtlContent, checkBoxVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)

            val item = content.item.first()

            binding.descriptionTextView.text = item.description
            binding.eanTextView.text = item.ean
            binding.qtyCollectedTextView.text = content.qtyCollected.toString()
            binding.qtyRequestedTextView.text = content.qtyRequested.toString()
            binding.checkBox.isChecked = false

            setStyle(content)
        }

        private fun setStyle(content: PtlContent) {
            val v = itemView

            // region Background layouts
            val collQtyEqualBackColor =
                ResourcesCompat.getDrawable(context.resources, R.drawable.layout_thin_border_green, null)!!
            val collQtyMoreBackColor =
                ResourcesCompat.getDrawable(context.resources, R.drawable.layout_thin_border_blue, null)!!
            val collQtyLessBackColor =
                ResourcesCompat.getDrawable(context.resources, R.drawable.layout_thin_border_red, null)!!

            val backColor: Drawable
            val foreColor: Int
            when (content.contentStatus) {
                ContentStatus.QTY_EQUAL -> {
                    backColor = collQtyEqualBackColor
                    foreColor = collQtyEqualForeColor
                }

                ContentStatus.QTY_MORE -> {
                    backColor = collQtyMoreBackColor
                    foreColor = collQtyMoreForeColor
                }

                else -> {
                    backColor = collQtyLessBackColor
                    foreColor = collQtyLessForeColor
                }
            }

            val titleForeColor: Int = manipulateColor(foreColor, 1.4f)

            v.background = backColor
            binding.descriptionTextView.setTextColor(foreColor)
            binding.eanTextView.setTextColor(foreColor)
            binding.qtyCollectedTextView.setTextColor(foreColor)
            binding.qtyRequestedTextView.setTextColor(foreColor)
            binding.checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)
            binding.qtyReqTitleTextView.setTextColor(titleForeColor)
            binding.qtyTitleTextView.setTextColor(titleForeColor)
        }
    }

    init {
        // Set values from Builder
        recyclerView = builder.recyclerView
        fullList = builder.fullList
        checkedIdArray = builder.checkedIdArray
        multiSelect = builder.multiSelect
        allowEditQty = builder.allowEditQty
        showQtyPanel = builder.showQtyPanel
        showCheckBoxes = builder.showCheckBoxes
        showCheckBoxesChanged = builder.showCheckBoxesChanged
        visibleStatus = builder.visibleStatus
        filterOptions = builder.filterOptions

        dataSetChangedListener = builder.dataSetChangedListener
        checkedChangedListener = builder.checkedChangedListener
        editQtyListener = builder.editQtyListener

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
    private fun sortedVisibleList(list: MutableList<PtlContent>?): MutableList<PtlContent> {
        val croppedList = (list ?: mutableListOf()).mapNotNull { if (it.contentStatus in visibleStatus) it else null }
        return sortItems(croppedList.toMutableList())
    }

    // Sobrecargamos estos métodos para suministrar siempre una lista ordenada y filtrada por estado de visibilidad
    override fun submitList(list: MutableList<PtlContent>?) {
        super.submitList(sortedVisibleList(list))
    }

    override fun submitList(list: MutableList<PtlContent>?, commitCallback: Runnable?) {
        super.submitList(sortedVisibleList(list), commitCallback)
    }

    class Builder {
        fun build(): PtlContentAdapter {
            return PtlContentAdapter(this)
        }

        internal lateinit var recyclerView: RecyclerView
        internal var fullList: ArrayList<PtlContent> = arrayListOf()
        internal var checkedIdArray: ArrayList<Long> = arrayListOf()
        internal var multiSelect: Boolean = false
        internal var allowEditQty: Boolean = false
        internal var showQtyPanel: Boolean = false
        internal var showCheckBoxes: Boolean = false
        internal var showCheckBoxesChanged: (Boolean) -> Unit = { }
        internal var visibleStatus: ArrayList<ContentStatus> = ArrayList(ContentStatus.values().toList())
        internal var filterOptions: FilterOptions = FilterOptions()

        internal var dataSetChangedListener: DataSetChangedListener? = null
        internal var checkedChangedListener: CheckedChangedListener? = null
        internal var editQtyListener: EditQtyListener? = null

        // Setter methods for variables with chained methods
        fun recyclerView(`val`: RecyclerView): Builder {
            recyclerView = `val`
            return this
        }

        fun fullList(`val`: ArrayList<PtlContent>): Builder {
            fullList = `val`
            return this
        }

        fun checkedIdArray(`val`: ArrayList<Long>): Builder {
            checkedIdArray = `val`
            return this
        }

        fun multiSelect(`val`: Boolean): Builder {
            multiSelect = `val`
            return this
        }

        fun allowEditQty(`val`: Boolean, listener: EditQtyListener?): Builder {
            allowEditQty = `val`
            editQtyListener = listener
            return this
        }

        fun showQtyPanel(`val`: Boolean): Builder {
            showQtyPanel = `val`
            return this
        }

        fun showCheckBoxes(`val`: Boolean, listener: (Boolean) -> Unit): Builder {
            showCheckBoxes = `val`
            showCheckBoxesChanged = listener
            return this
        }

        fun visibleStatus(`val`: ArrayList<ContentStatus>): Builder {
            visibleStatus = `val`
            return this
        }

        fun filterOptions(`val`: FilterOptions): Builder {
            filterOptions = `val`
            return this
        }

        fun dataSetChangedListener(listener: DataSetChangedListener?): Builder {
            dataSetChangedListener = listener
            return this
        }

        fun checkedChangedListener(listener: CheckedChangedListener?): Builder {
            checkedChangedListener = listener
            return this
        }
    }
}
