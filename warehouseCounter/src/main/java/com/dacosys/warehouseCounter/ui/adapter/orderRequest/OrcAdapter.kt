package com.dacosys.warehouseCounter.ui.adapter.orderRequest

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
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.ContentStatus
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestContent
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.databinding.OrcRowBinding
import com.dacosys.warehouseCounter.databinding.OrcRowExpandedBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.adapter.FilterOptions
import com.dacosys.warehouseCounter.ui.snackBar.MakeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.manipulateColor
import java.util.*


/**
 * Created by Agustin on 10/03/2023.
 */

class OrcAdapter private constructor(builder: Builder) :
    ListAdapter<OrderRequestContent, ViewHolder>(OrderRequestContentDiffUtilCallback), Filterable {

    private val recyclerView: RecyclerView
    var fullList: ArrayList<OrderRequestContent>
    private var orType: OrderRequestType = OrderRequestType.notDefined
    var checkedIdArray: ArrayList<Long> = ArrayList()
    var multiSelect: Boolean = false
    private var allowEditQty: Boolean = false
    private var allowEditDescription: Boolean = false
    var showCheckBoxes: Boolean = false
    private var showCheckBoxesChanged: (Boolean) -> Unit = { }
    var visibleStatus: ArrayList<ContentStatus> = ArrayList(ContentStatus.values().toList())
    private var filterOptions: FilterOptions = FilterOptions()

    private var dataSetChangedListener: DataSetChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null
    private var editQtyListener: EditQtyListener? = null
    private var editDescriptionListener: EditDescriptionListener? = null

    private var currentIndex = NO_POSITION

    // Clase para distinguir actualizaciones parciales
    private enum class PAYLOADS {
        CHECKBOX_STATE,
        CHECKBOX_VISIBILITY
    }

    private val unlimitedMax by lazy { orType in OrderRequestType.getUnlimited() }

    fun clear() {
        checkedIdArray.clear()

        fullList.clear()
        submitList(fullList)
    }

    fun refreshListeners(
        checkedChangedListener: CheckedChangedListener? = null,
        dataSetChangedListener: DataSetChangedListener? = null,
        editQtyListener: EditQtyListener? = null,
        editDescriptionListener: EditDescriptionListener? = null,
    ) {
        this.editQtyListener = editQtyListener
        this.checkedChangedListener = checkedChangedListener
        this.dataSetChangedListener = dataSetChangedListener
        this.editDescriptionListener = editDescriptionListener
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
            content: OrderRequestContent,
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

    private object OrderRequestContentDiffUtilCallback : DiffUtil.ItemCallback<OrderRequestContent>() {
        override fun areItemsTheSame(oldItem: OrderRequestContent, newItem: OrderRequestContent): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: OrderRequestContent, newItem: OrderRequestContent): Boolean {
            if (oldItem.itemId != newItem.itemId) return false
            if (oldItem.itemDescription != newItem.itemDescription) return false
            if (oldItem.codeRead != newItem.codeRead) return false
            if (oldItem.ean != newItem.ean) return false
            if (oldItem.price != newItem.price) return false
            if (oldItem.itemActive != newItem.itemActive) return false
            if (oldItem.externalId != newItem.externalId) return false
            if (oldItem.itemCategoryId != newItem.itemCategoryId) return false
            if (oldItem.lotEnabled != newItem.lotEnabled) return false
            if (oldItem.lotId != newItem.lotId) return false
            if (oldItem.lotCode != newItem.lotCode) return false
            if (oldItem.lotActive != newItem.lotActive) return false
            if (oldItem.qtyRequested != newItem.qtyRequested) return false
            return oldItem.qtyCollected == newItem.qtyCollected
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
                    OrcRowExpandedBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            else -> {
                UnselectedViewHolder(
                    OrcRowBinding.inflate(
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
                        (holder as SelectedViewHolder).bindCheckBoxState(
                            checkedIdArray.contains(
                                ptlOrder.itemId ?: -1
                            )
                        )
                    else
                        (holder as UnselectedViewHolder).bindCheckBoxState(
                            checkedIdArray.contains(
                                ptlOrder.itemId ?: -1
                            )
                        )
                }
            }
        }
    }

    // El método onBindViewHolder establece los valores de las vistas en función de los datos
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Aquí puedes establecer los valores para cada elemento
        holder.itemView.setOnClickListener {

            // Si el elemento ya está seleccionado, quitar selección
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
        val orderRequestContent = getItem(position)

        // Lógica de clic largo sobre el ítem
        setItemCheckBoxLogic(holder.itemView)

        holder.bind(
            content = orderRequestContent,
            checkBoxVisibility = if (showCheckBoxes) VISIBLE else GONE
        )

        holder.itemView.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            getColorWithAlpha(colorId = R.color.lightslategray, alpha = 220), BlendModeCompat.MODULATE
        )

        // Acciones del checkBox de marcado
        setCheckBoxLogic(holder.binding.checkBox, orderRequestContent, position)

        if (allowEditQty) {
            setQtyEditLogic(holder.binding.editQty, orderRequestContent, position)
            holder.binding.editQty.visibility = View.VISIBLE
        } else {
            holder.binding.editQty.visibility = View.GONE
        }

        if (allowEditDescription) {
            setDescriptionEditLogic(holder.binding.editDescription, orderRequestContent, position)
            holder.binding.editDescription.visibility = View.VISIBLE
        } else {
            holder.binding.editDescription.visibility = View.GONE
        }

        if (orType == OrderRequestType.stockAuditFromDevice) {
            holder.binding.qtyReqPanel.visibility = GONE
            holder.binding.eanDivider.visibility = GONE
        } else {
            holder.binding.qtyReqPanel.visibility = VISIBLE
            holder.binding.eanDivider.visibility = VISIBLE
        }
    }

    private fun setUnselectedHolder(holder: UnselectedViewHolder, position: Int) {
        val orderRequestContent = getItem(position)

        // Lógica de clic largo sobre el ítem
        setItemCheckBoxLogic(holder.itemView)

        // Perform a full update
        holder.bind(
            content = orderRequestContent,
            checkBoxVisibility = if (showCheckBoxes) VISIBLE else GONE
        )

        holder.itemView.background.colorFilter = null

        setCheckBoxLogic(holder.binding.checkBox, orderRequestContent, position)

        if (orType == OrderRequestType.stockAuditFromDevice) {
            holder.binding.qtyReqPanel.visibility = View.GONE
            holder.binding.eanDivider.visibility = View.GONE
        } else {
            holder.binding.qtyReqPanel.visibility = View.VISIBLE
            holder.binding.eanDivider.visibility = View.VISIBLE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setDescriptionEditLogic(
        editDescription: AppCompatImageView,
        content: OrderRequestContent,
        position: Int,
    ) {
        val itemId = content.itemId ?: -1
        if (itemId > 0) {
            editDescription.visibility = GONE
        }
        editDescription.setOnTouchListener { _, _ ->
            editDescriptionListener?.onEditDescriptionRequired(position, content)
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setQtyEditLogic(editQty: AppCompatImageView, content: OrderRequestContent, position: Int) {
        editQty.setOnTouchListener { _, _ ->
            editQtyListener?.onEditQtyRequired(
                position = position,
                content = content,
                initialQty = content.qtyCollected ?: 0.toDouble(),
                minValue = 0.toDouble(),
                maxValue = 999999.toDouble(),
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
     * @param content Datos del ítem
     * @param position Posición en el adaptador
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setCheckBoxLogic(checkBox: CheckBox, content: OrderRequestContent, position: Int) {
        val checkChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            setChecked(content = content, isChecked = isChecked, suspendRefresh = true)
        }

        val longClickListener = OnLongClickListener { _ ->
            checkBox.setOnCheckedChangeListener(null)

            // Notificamos los cambios solo a los ítems que cambian de estado.
            val newState = !checkBox.isChecked
            if (newState) {
                currentList.mapIndexed { pos, ptlOrder ->
                    val id = ptlOrder.itemId ?: -1
                    if (id !in checkedIdArray) {
                        checkedIdArray.add(id)
                        notifyItemChanged(pos, PAYLOADS.CHECKBOX_STATE)
                    }
                }
            } else {
                currentList.mapIndexed { pos, ptlOrder ->
                    val id = ptlOrder.itemId ?: -1
                    if (id in checkedIdArray) {
                        checkedIdArray.remove(id)
                        notifyItemChanged(pos, PAYLOADS.CHECKBOX_STATE)
                    }
                }
            }

            dataSetChangedListener?.onDataSetChanged()
            return@OnLongClickListener true
        }

        // Important to remove previous checkedChangedListener before calling setChecked
        checkBox.setOnCheckedChangeListener(null)

        checkBox.isChecked = checkedIdArray.contains(content.itemId ?: -1)
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
        var selected: OrderRequestContent? = null
        var firstVisible: OrderRequestContent? = null

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
                var r: ArrayList<OrderRequestContent> = ArrayList()
                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    if (filterString.isNotEmpty()) {
                        var filterableItem: OrderRequestContent

                        for (i in 0 until fullList.size) {
                            filterableItem = fullList[i]

                            // Descartamos aquellos que no debe ser visibles
                            if (filterableItem.contentStatus !in visibleStatus) continue

                            val item = filterableItem
                            if (isFilterable(item, filterString)) {
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

            fun isFilterable(item: OrderRequestContent, filterString: String): Boolean =
                item.itemDescription.contains(filterString, true) ||
                        item.ean.contains(filterString, true)

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?, results: FilterResults?,
            ) {
                submitList(results?.values as ArrayList<OrderRequestContent>) {
                    // Notificamos al Listener superior
                    dataSetChangedListener?.onDataSetChanged()

                    // Recuperamos el item seleccionado y la posición del scroll
                    if (firstVisible != null)
                        scrollToPos(getIndexById(firstVisible?.itemId ?: -1), true)
                    if (selected != null)
                        selectItem(selected, false)
                }
            }
        }
    }

    private fun sortItems(originalList: MutableList<OrderRequestContent>): ArrayList<OrderRequestContent> {
        return ArrayList(
            originalList.sortedWith(
                compareBy(
                    { it.itemDescription },
                    { it.ean },
                )
            ).toList()
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun refreshFilter(options: FilterOptions) {
        filterOptions = options
        filter.filter(filterOptions.filterString)
    }

    private fun refreshFilter() {
        refreshFilter(filterOptions)
    }

    fun add(asset: OrderRequestContent, position: Int) {
        fullList.add(position, asset)
        submitList(fullList).apply {
            dataSetChangedListener?.onDataSetChanged()
            selectItem(position)
        }
    }


    fun add(orcs: ArrayList<OrderRequestContent>) {
        var lastIndex = fullList.lastIndex
        for (orc in orcs) {
            if (!fullList.contains(orc)) {
                lastIndex++
                fullList.add(lastIndex, orc)
            }
        }

        submitList(fullList).apply {
            dataSetChangedListener?.onDataSetChanged()
            selectItem(orcs.last())
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

    fun remove(orc: OrderRequestContent?) {
        if (orc == null) return
        remove(getIndex(orc))
    }

    fun remove(orcs: ArrayList<OrderRequestContent>) {
        for (w in orcs) {
            if (fullList.contains(w)) {
                fullList.remove(w)
                checkedIdArray.remove(w.itemId ?: -1)
            }
        }

        submitList(fullList).apply {
            dataSetChangedListener?.onDataSetChanged()
        }
    }

    fun updateDescription(item: OrderRequestContent, desc: String) {
        return updateDescription(item.itemId ?: -1, desc)
    }

    fun updateDescription(itemId: Long, desc: String) {
        val content = fullList.firstOrNull { it.itemId == itemId } ?: return
        val index = getIndexById(content.itemId ?: -1)

        if (content.itemDescription != desc) {
            content.itemDescription = desc

            submitList(fullList).apply {
                notifyItemChanged(index)
                selectItem(content)
            }
        }
    }

    fun updateLotCode(itemId: Long, lotCode: String) {
        val content = fullList.firstOrNull { it.itemId == itemId } ?: return
        val index = getIndexById(content.itemId ?: -1)

        val longLotId = try {
            lotCode.toLong()
        } catch (ex: Exception) {
            0L
        }

        content.lotCode = lotCode
        content.lotId = longLotId
        content.lotActive = true

        submitList(fullList).apply {
            notifyItemChanged(index)
            selectItem(content)
        }
    }

    fun updateQtyCollected(tempOrc: OrderRequestContent?, qtyCollected: Double) {
        val orc = tempOrc ?: return
        val content = fullList.firstOrNull { it == orc } ?: return
        val index = getIndexById(content.itemId ?: -1)
        val qtyRequested = content.qtyRequested ?: 0.0

        content.qtyCollected = qtyCollected

        submitList(fullList).apply {
            notifyItemChanged(index)

            if (qtyRequested == qtyCollected) {
                if (!unlimitedMax) reportQtyRequestedReached(content)
            } else if (qtyRequested < qtyCollected) {
                if (!unlimitedMax) reportQtyRequestedExceeded(content)
            } else {
                reportQtyCollectedChange(content)
            }

            dataSetChangedListener?.onDataSetChanged()
            selectItem(content)
        }
    }

    fun selectItem(orc: OrderRequestContent?, scroll: Boolean = true) {
        var pos = NO_POSITION
        if (orc != null) pos = getIndex(orc)
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

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        MakeText.makeText(recyclerView, text, snackBarType)
    }

    private fun reportQtyCollectedChange(content: OrderRequestContent) {
        val itemCode = content.ean
        val qtyRequested = content.qtyRequested ?: 0.0
        val decimalPlaces = 0

        val res = "${itemCode}: ${Statics.roundToString(qtyRequested, decimalPlaces)}"
        showSnackBar(res, SnackBarType.ADD)
    }

    private fun reportQtyRequestedReached(content: OrderRequestContent) {
        val itemCode = content.ean
        val res = "${itemCode}: ${context.getString(R.string.maximum_reached)}"
        showSnackBar(res, SnackBarType.UPDATE)
    }

    private fun reportQtyRequestedExceeded(orc: OrderRequestContent) {
        val res = "${orc.ean}: ${context.getString(R.string.qty_requested_exceeded)}"
        showSnackBar(res, SnackBarType.UPDATE)
    }

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun getIndexById(id: Long): Int {
        return currentList.indexOfFirst { it.itemId == id }
    }

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    private fun getIndex(content: OrderRequestContent): Int {
        return currentList.indexOf(content)
    }

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun getIndexByItemId(itemId: Long): Int {
        return currentList.indexOfFirst { it.itemId == itemId }
    }

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun getByItemId(itemId: Long): OrderRequestContent? {
        return currentList.firstOrNull { it.itemId == itemId }
    }

    fun getAllChecked(): ArrayList<OrderRequestContent> {
        val items = ArrayList<OrderRequestContent>()
        checkedIdArray.mapNotNullTo(items) { getByItemId(it) }
        return items
    }

    @Suppress("unused")
    fun qtyRequestedTotal() = currentList.sumOf { it.qtyRequested ?: 0.0 }

    @Suppress("unused")
    fun qtyCollectedTotal() = currentList.sumOf { it.qtyCollected ?: 0.0 }

    fun currentItem(): OrderRequestContent? {
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

    fun setChecked(content: OrderRequestContent, isChecked: Boolean, suspendRefresh: Boolean = false) {
        val id = content.itemId ?: -1
        val pos = getIndexById(id)
        if (isChecked) {
            if (!checkedIdArray.contains(id)) {
                checkedIdArray.add(id)
            }
        } else {
            checkedIdArray.remove(id)
        }

        checkedChangedListener?.onCheckedChanged(isChecked, pos)

        // Notificamos al Listener superior
        if (!suspendRefresh) dataSetChangedListener?.onDataSetChanged()
    }

    private var isFilling = false
    fun setChecked(items: ArrayList<OrderRequestContent>, isChecked: Boolean) {
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

    @Suppress("unused")
    fun addVisibleStatus(status: ContentStatus) {
        if (visibleStatus.contains(status)) return
        visibleStatus.add(status)

        refreshFilter()
    }

    @Suppress("unused")
    fun removeVisibleStatus(status: ContentStatus) {
        if (!visibleStatus.contains(status)) return
        visibleStatus.remove(status)

        // Quitamos los ítems con el estado seleccionado de la lista marcados.
        val uncheckedItems =
            ArrayList(fullList.mapNotNull { if (it.contentStatus == status) it.itemId else null })
        checkedIdArray.removeAll(uncheckedItems.toSet())

        refreshFilter()
    }

    // Aquí creamos dos ViewHolder, uno para cada tipo de vista
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    class SelectedViewHolder(val binding: OrcRowExpandedBinding) : ViewHolder(binding.root) {
        fun bindCheckBoxVisibility(checkBoxVisibility: Int = GONE) {
            binding.checkBoxConstraintLayout.visibility = checkBoxVisibility
        }

        fun bindCheckBoxState(checked: Boolean) {
            binding.checkBox.isChecked = checked
        }

        fun bind(content: OrderRequestContent, checkBoxVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)

            binding.descriptionTextView.text = content.itemDescription
            binding.eanTextView.text = content.ean

            binding.qtyCollectedTextView.text =
                Statics.roundToString(content.qtyCollected ?: 0.toDouble(), settingsVm.decimalPlaces)
            binding.qtyRequestedTextView.text =
                Statics.roundToString(content.qtyRequested ?: 0.toDouble(), settingsVm.decimalPlaces)

            binding.itemIdCheckedTextView.text =
                when {
                    (content.itemId ?: -1) < 0 -> context.getString(R.string.new_product)
                    content.itemId == 0L -> context.getString(R.string.without_id)
                    else -> content.itemId.toString()
                }
            binding.extIdCheckedTextView.text = content.externalId ?: ""
            binding.lotIdCheckedTextView.text = content.lotId?.toString() ?: ""

            // Category
            val category = content.itemCategoryId ?: 0L // TODO: Agregar description para categorías
            val price = content.price

            if (category <= 0 && price == null) {
                binding.categoryConstraintLayout.visibility = GONE
            } else {
                binding.categoryConstraintLayout.visibility = VISIBLE
                binding.categoryTextView.text = category.toString()
                binding.priceTextView.text = String.format("$ %s", Statics.roundToString(price!!, 2))
            }

            setStyle(content)
        }

        private fun setStyle(content: OrderRequestContent) {
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
            binding.categoryTextView.setTextColor(foreColor)
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
            binding.categoryLabelTextView.setTextColor(titleForeColor)
            binding.priceLabelTextView.setTextColor(titleForeColor)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    internal class UnselectedViewHolder(val binding: OrcRowBinding) : ViewHolder(binding.root) {
        fun bindCheckBoxVisibility(checkBoxVisibility: Int = GONE) {
            binding.checkBoxConstraintLayout.visibility = checkBoxVisibility
        }

        fun bindCheckBoxState(checked: Boolean) {
            binding.checkBox.isChecked = checked
        }

        fun bind(content: OrderRequestContent, checkBoxVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)

            binding.descriptionTextView.text = content.itemDescription
            binding.eanTextView.text = content.ean

            binding.qtyCollectedTextView.text =
                Statics.roundToString(content.qtyCollected ?: 0.toDouble(), settingsVm.decimalPlaces)
            binding.qtyRequestedTextView.text =
                Statics.roundToString(content.qtyRequested ?: 0.toDouble(), settingsVm.decimalPlaces)

            setStyle(content)
        }

        private fun setStyle(content: OrderRequestContent) {
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
        orType = builder.orType
        checkedIdArray = builder.checkedIdArray
        multiSelect = builder.multiSelect
        allowEditQty = builder.allowEditQty
        allowEditDescription = builder.allowEditDescription
        showCheckBoxes = builder.showCheckBoxes
        showCheckBoxesChanged = builder.showCheckBoxesChanged
        visibleStatus = builder.visibleStatus
        filterOptions = builder.filterOptions
        dataSetChangedListener = builder.dataSetChangedListener
        checkedChangedListener = builder.checkedChangedListener
        editQtyListener = builder.editQtyListener
        editDescriptionListener = builder.editDescriptionListener

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
    private fun sortedVisibleList(list: MutableList<OrderRequestContent>?): MutableList<OrderRequestContent> {
        val croppedList =
            (list ?: mutableListOf()).mapNotNull { if (it.contentStatus in visibleStatus) it else null }
        return sortItems(croppedList.toMutableList())
    }

    // Sobrecargamos estos métodos para suministrar siempre una lista ordenada y filtrada por estado de visibilidad
    override fun submitList(list: MutableList<OrderRequestContent>?) {
        super.submitList(sortedVisibleList(list))
    }

    override fun submitList(list: MutableList<OrderRequestContent>?, commitCallback: Runnable?) {
        super.submitList(sortedVisibleList(list), commitCallback)
    }

    class Builder {
        fun build(): OrcAdapter {
            return OrcAdapter(this)
        }

        internal lateinit var recyclerView: RecyclerView
        internal var fullList: ArrayList<OrderRequestContent> = arrayListOf()
        internal var orType: OrderRequestType = OrderRequestType.notDefined
        internal var checkedIdArray: ArrayList<Long> = ArrayList()
        internal var multiSelect: Boolean = false
        internal var allowEditQty: Boolean = false
        internal var allowEditDescription: Boolean = false
        internal var showCheckBoxes: Boolean = false
        internal var showCheckBoxesChanged: (Boolean) -> Unit = { }
        internal var visibleStatus: ArrayList<ContentStatus> = ArrayList(ContentStatus.values().toList())
        internal var filterOptions: FilterOptions = FilterOptions()

        internal var dataSetChangedListener: DataSetChangedListener? = null
        internal var checkedChangedListener: CheckedChangedListener? = null
        internal var editQtyListener: EditQtyListener? = null
        internal var editDescriptionListener: EditDescriptionListener? = null

        // Setter methods for variables with chained methods
        @Suppress("unused")
        fun recyclerView(`val`: RecyclerView): Builder {
            recyclerView = `val`
            return this
        }

        @Suppress("unused")
        fun fullList(`val`: ArrayList<OrderRequestContent>): Builder {
            fullList = `val`
            return this
        }

        @Suppress("unused")
        fun orType(`val`: OrderRequestType): Builder {
            orType = `val`
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
        fun allowEditQty(`val`: Boolean, listener: EditQtyListener?): Builder {
            allowEditQty = `val`
            editQtyListener = listener
            return this
        }

        @Suppress("unused")
        fun allowEditDescription(`val`: Boolean, listener: EditDescriptionListener?): Builder {
            allowEditDescription = `val`
            editDescriptionListener = listener
            return this
        }

        @Suppress("unused")
        fun showCheckBoxes(`val`: Boolean, listener: (Boolean) -> Unit): Builder {
            showCheckBoxes = `val`
            showCheckBoxesChanged = listener
            return this
        }

        @Suppress("unused")
        fun visibleStatus(`val`: ArrayList<ContentStatus>): Builder {
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
