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
import com.dacosys.warehouseCounter.databinding.OrderRequestRowBinding
import com.dacosys.warehouseCounter.databinding.OrderRequestRowExpandedBinding
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.ui.adapter.FilterOptions
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.manipulateColor
import java.util.*

class OrderRequestAdapter private constructor(builder: Builder) :
    ListAdapter<OrderRequest, ViewHolder>(OrderRequestDiffUtilCallback), Filterable {

    private val recyclerView: RecyclerView
    var fullList: ArrayList<OrderRequest> = ArrayList()
    var checkedIdArray: ArrayList<Long> = ArrayList()
    private var multiSelect: Boolean = false
    var showCheckBoxes: Boolean = false
    private var showCheckBoxesChanged: (Boolean) -> Unit = { }
    var visibleStatus: ArrayList<OrderRequestType> = OrderRequestType.getAll()
    private var filterOptions: FilterOptions = FilterOptions()

    // Este Listener debe usarse para los cambios de cantidad o de pedidos marcados de la lista,
    // ya que se utiliza para actualizar los valores sumarios en la actividad.
    private var dataSetChangedListener: DataSetChangedListener? = null

    private var selectedOrderRequestChangedListener: SelectedOrderRequestChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null

    // Posición del pedido seleccionado
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
        selectedOrderRequestChangedListener: SelectedOrderRequestChangedListener? = null,
    ) {
        this.checkedChangedListener = checkedChangedListener
        this.dataSetChangedListener = dataSetChangedListener
        this.selectedOrderRequestChangedListener = selectedOrderRequestChangedListener
    }

    interface DataSetChangedListener {
        fun onDataSetChanged()
    }

    interface CheckedChangedListener {
        fun onCheckedChanged(isChecked: Boolean, pos: Int)
    }

    interface SelectedOrderRequestChangedListener {
        fun onSelectedOrderRequestChanged(orderRequest: OrderRequest?)
    }

    // El método onCreateViewHolder infla los diseños para cada tipo de vista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            SELECTED_VIEW_TYPE -> {
                SelectedViewHolder(
                    OrderRequestRowExpandedBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            else -> {
                UnselectedViewHolder(
                    OrderRequestRowBinding.inflate(
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
                    val orderRequest = getItem(position)
                    if (position == currentIndex)
                        (holder as SelectedViewHolder).bindCheckBoxState(checkedIdArray.contains(orderRequest.orderRequestId))
                    else
                        (holder as UnselectedViewHolder).bindCheckBoxState(checkedIdArray.contains(orderRequest.orderRequestId))
                }

                PAYLOADS.ITEM_SELECTED -> {
                    // TODO: No regenerar la vista ante cambios de selección
                    // No está funcionando.
                    // La idea es usarlo para los cambios de selección.
                    // Pero por algún motivo los Payloads vienen vacíos luego de notifyItemChanged
                    super.onBindViewHolder(holder, position, payloads)
                }
            }
        }
    }

    // El método onBindViewHolder establece los valores de las vistas en función de los datos
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Evento de clic sobre el pedido que controla el estado de Selección para seleccionar el diseño adecuado
        holder.itemView.setOnClickListener {

            // Si el elemento ya está seleccionado quitar la selección
            if (currentIndex == holder.bindingAdapterPosition) {
                currentIndex = NO_POSITION
                notifyItemSelectedChanged(holder.bindingAdapterPosition)
            } else {
                // Notificamos los cambios para los dos pedidos cuyo diseño necesita cambiar
                val previousSelectedOrderRequestPosition = currentIndex
                currentIndex = holder.bindingAdapterPosition
                notifyItemSelectedChanged(currentIndex)

                if (previousSelectedOrderRequestPosition != NO_POSITION) {
                    notifyItemSelectedChanged(previousSelectedOrderRequestPosition)
                }

                // Scroll para asegurarnos que se vea completamente el pedido
                holder.itemView.post { scrollToPos(currentIndex) }
            }

            // Seleccionamos el pedido
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
        val orderRequest = getItem(position)

        // Lógica de clic largo sobre el pedido
        setItemCheckBoxLogic(holder.itemView)

        holder.bind(
            orderRequest = orderRequest,
            checkBoxVisibility = if (showCheckBoxes) VISIBLE else GONE
        )

        holder.itemView.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            getColorWithAlpha(colorId = R.color.lightslategray, alpha = 220), BlendModeCompat.MODULATE
        )

        // Acciones del checkBox de marcado
        setCheckBoxLogic(holder.binding.checkBox, orderRequest, position)
    }

    private fun setUnselectedHolder(holder: UnselectedViewHolder, position: Int) {
        val orderRequest = getItem(position)

        // Lógica de clic largo sobre el pedido
        setItemCheckBoxLogic(holder.itemView)

        // Perform a full update
        holder.bind(
            orderRequest = orderRequest,
            checkBoxVisibility = if (showCheckBoxes) VISIBLE else GONE
        )

        holder.itemView.background.colorFilter = null

        // Acciones del checkBox de marcado
        setCheckBoxLogic(holder.binding.checkBox, orderRequest, position)
    }

    /**
     * Lógica del evento de clic sostenido sobre el pedido que cambia la visibilidad de los CheckBox
     *
     * @param itemView Vista general del pedido
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
     * Lógica del comportamiento del CheckBox de marcado de pedidos cuando [multiSelect] es verdadero
     *
     * @param checkBox Control CheckBox para marcado del pedido
     * @param orderRequest Datos del pedido
     * @param position Posición en el adaptador
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setCheckBoxLogic(checkBox: CheckBox, orderRequest: OrderRequest, position: Int) {
        val checkChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            setChecked(orderRequest = orderRequest, isChecked = isChecked, suspendRefresh = true)
        }

        val longClickListener = OnLongClickListener { _ ->
            checkBox.setOnCheckedChangeListener(null)

            // Notificamos los cambios solo a los pedidos que cambian de estado.
            val newState = !checkBox.isChecked
            if (newState) {
                currentList.mapIndexedNotNull { index, orderRequest ->
                    val orId = orderRequest.orderRequestId ?: -1
                    if (orId > 0 && orId !in checkedIdArray) {
                        checkedIdArray.add(orId)
                        notifyItemChanged(index, PAYLOADS.CHECKBOX_STATE)
                    }
                }
            } else {
                currentList.mapIndexed { pos, orderRequest ->
                    if (orderRequest.orderRequestId in checkedIdArray) {
                        checkedIdArray.remove(orderRequest.orderRequestId)
                        notifyItemChanged(pos, PAYLOADS.CHECKBOX_STATE)
                    }
                }
            }

            // Notificamos al Listener superior
            dataSetChangedListener?.onDataSetChanged()
            return@OnLongClickListener true
        }

        // Important to remove previous checkedChangedListener before calling setChecked
        checkBox.setOnCheckedChangeListener(null)

        checkBox.isChecked = checkedIdArray.contains(orderRequest.orderRequestId)
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
            var selected: OrderRequest? = null
            var firstVisible: OrderRequest? = null

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                // Guardamos el orderRequest seleccionado y la posición del scroll
                selected = currentItem()
                var scrollPos =
                    (recyclerView.layoutManager as LinearLayoutManager?)?.findFirstVisibleItemPosition()
                        ?: NO_POSITION

                if (scrollPos != NO_POSITION && itemCount > scrollPos) {
                    var currentScrolled = getItem(scrollPos)

                    // Comprobamos si es visible el pedido del Scroll
                    if (currentScrolled.orderRequestType in visibleStatus)
                        firstVisible = currentScrolled
                    else {
                        // Si no es visible, intentar encontrar el próximo visible.
                        while (firstVisible == null) {
                            scrollPos++
                            if (itemCount > scrollPos) {
                                currentScrolled = getItem(scrollPos)
                                if (currentScrolled.orderRequestType in visibleStatus)
                                    firstVisible = currentScrolled
                            } else break
                        }
                    }
                }

                // Filtramos los resultados
                val results = FilterResults()
                var r: ArrayList<OrderRequest> = ArrayList()
                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    if (filterString.isNotEmpty()) {
                        var filterableOrderRequest: OrderRequest

                        for (i in 0 until fullList.size) {
                            filterableOrderRequest = fullList[i]

                            // Descartamos aquellos que no debe ser visibles
                            if (filterableOrderRequest.orderRequestType !in visibleStatus) continue

                            if (isFilterable(filterableOrderRequest, filterString)) {
                                r.add(filterableOrderRequest)
                            }
                        }
                    } else if (filterOptions.showAllOnFilterEmpty) {
                        r =
                            ArrayList(fullList.mapNotNull { if (it.orderRequestType in visibleStatus) it else null })
                    }
                }

                results.values = r
                results.count = r.size
                return results
            }

            fun isFilterable(filterableOrder: OrderRequest, filterString: String): Boolean =
                (filterableOrder.description.contains(filterString, true) ||
                        filterableOrder.externalId.contains(filterString, true))

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?, results: FilterResults?,
            ) {
                submitList(results?.values as ArrayList<OrderRequest>) {
                    // Notificamos al Listener superior
                    dataSetChangedListener?.onDataSetChanged()

                    // Recuperamos el orderRequest seleccionado y la posición del scroll
                    if (firstVisible != null)
                        scrollToPos(getIndexById(firstVisible?.orderRequestId ?: -1), true)
                    if (selected != null)
                        selectItem(selected, false)
                }
            }
        }
    }

    private fun sortOrderRequests(originalList: MutableList<OrderRequest>): ArrayList<OrderRequest> {
        // Run the follow method on each of the roots
        return ArrayList(
            originalList.sortedWith(
                compareBy({ it.description },
                    { it.externalId })
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

    fun add(orderRequest: OrderRequest, position: Int) {
        fullList.add(position, orderRequest)
        submitList(fullList) {
            // Notificamos al Listener superior
            dataSetChangedListener?.onDataSetChanged()
            selectItem(position)
        }
    }

    fun remove(position: Int) {
        val id = getItemId(position)
        checkedIdArray.remove(id)

        fullList.removeAt(position)
        submitList(fullList) {
            /** Notificamos al Listener superior */
            dataSetChangedListener?.onDataSetChanged()
        }
    }

    /**
     * Se utiliza cuando se edita un pedido y necesita actualizarse
     */
    fun updateOrderRequest(orderRequest: OrderRequest, scrollToPos: Boolean = false) {
        val t = fullList.firstOrNull { it == orderRequest } ?: return

        t.orderRequestId = orderRequest.orderRequestId
        t.clientId = orderRequest.clientId
        t.userId = orderRequest.userId
        t.externalId = orderRequest.externalId
        t.creationDate = orderRequest.creationDate
        t.description = orderRequest.description
        t.zone = orderRequest.zone
        t.orderTypeId = orderRequest.orderTypeId
        t.orderTypeDescription = orderRequest.orderTypeDescription
        t.resultDiffQty = orderRequest.resultDiffQty
        t.resultDiffProduct = orderRequest.resultDiffProduct
        t.resultAllowDiff = orderRequest.resultAllowDiff
        t.resultAllowMod = orderRequest.resultAllowMod
        t.completed = orderRequest.completed
        t.startDate = orderRequest.startDate
        t.finishDate = orderRequest.finishDate
        t.contents = orderRequest.contents
        t.packages = orderRequest.packages
        t.logs = orderRequest.logs

        submitList(fullList) {
            // Notificamos al Listener superior
            dataSetChangedListener?.onDataSetChanged()

            // Seleccionamos el pedido y hacemos scroll hasta él.
            selectItem(orderRequest, scrollToPos)
        }
    }

    fun selectItem(a: OrderRequest?, scroll: Boolean = true) {
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
        var orderRequest: OrderRequest? = null
        if (pos != NO_POSITION) orderRequest = getItem(pos)
        selectedOrderRequestChangedListener?.onSelectedOrderRequestChanged(orderRequest)
    }

    /**
     * Scrolls to the given position, making sure the orderRequest can be fully displayed.
     *
     * @param position
     * @param scrollToTop If it is activated, the orderRequest will scroll until it is at the top of the view
     */
    fun scrollToPos(position: Int, scrollToTop: Boolean = false) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager

        if (position < 0 || position >= itemCount) {
            // La posición está fuera del rango válido, no se puede realizar el scroll
            return
        }

        val selectedView = layoutManager.findViewByPosition(position)

        if (scrollToTop) {
            // Hacemos scroll hasta que el pedido quede en la parte superior
            layoutManager.scrollToPositionWithOffset(position, 0)
        } else {
            if (selectedView != null) {
                // El pedido es visible, realizar scroll para asegurarse de que se vea completamente
                scrollToVisibleOrderRequest(selectedView)
            } else {
                // El pedido no es visible, realizar scroll directo a la posición
                recyclerView.scrollToPosition(position)
                recyclerView.addOnScrollListener(object : OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        val selected = layoutManager.findViewByPosition(position)
                        if (selected != null) {
                            // El pedido se ha vuelto visible, realizar scroll para asegurarse de que se vea completamente
                            scrollToVisibleOrderRequest(selected)
                            recyclerView.removeOnScrollListener(this)
                        }
                    }
                })
            }
        }
    }

    /**
     * Scrolls to the given view, making sure the orderRequest can be fully displayed.
     *
     * @param selectedView
     */
    private fun scrollToVisibleOrderRequest(selectedView: View) {
        val recyclerViewHeight = recyclerView.height - recyclerView.paddingTop - recyclerView.paddingBottom
        val selectedViewHeight = selectedView.height + selectedView.marginTop + selectedView.marginBottom

        val selectedViewTop = selectedView.top - selectedView.marginTop
        val selectedViewBottom = selectedView.bottom + selectedView.marginBottom

        if (selectedViewTop < 0) {
            // El pedido está parcialmente oculto en la parte superior del RecyclerView
            recyclerView.smoothScrollBy(0, selectedViewTop)
        } else if (selectedViewBottom > recyclerViewHeight) {
            // El pedido está parcialmente oculto en la parte inferior del RecyclerView
            val visibleHeight = recyclerViewHeight - selectedViewTop
            recyclerView.smoothScrollBy(0, selectedViewHeight - visibleHeight)
        }
    }

    fun getIndexById(id: Long): Int {
        return currentList.indexOfFirst { it.orderRequestId == id }
    }

    private fun getIndex(orderRequest: OrderRequest): Int {
        return currentList.indexOfFirst { it == orderRequest }
    }

    private fun getItemById(id: Long): OrderRequest? {
        return fullList.firstOrNull { it.orderRequestId == id }
    }

    fun getAllChecked(): ArrayList<OrderRequest> {
        val orderRequests = ArrayList<OrderRequest>()
        checkedIdArray.mapNotNullTo(orderRequests) { getItemById(it) }
        return orderRequests
    }

    fun currentItem(): OrderRequest? {
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

    fun setChecked(orderRequest: OrderRequest, isChecked: Boolean, suspendRefresh: Boolean = false) {
        val orId = orderRequest.orderRequestId ?: -1
        val pos = getIndexById(orId)
        if (isChecked) {
            if (orId > 0 && !checkedIdArray.contains(orId)) {
                checkedIdArray.add(orId)
            }
        } else {
            checkedIdArray.remove(orderRequest.orderRequestId)
        }

        checkedChangedListener?.onCheckedChanged(isChecked, pos)

        // Notificamos al Listener superior
        if (!suspendRefresh) dataSetChangedListener?.onDataSetChanged()
    }

    private var isFilling = false
    fun setChecked(orderRequests: ArrayList<OrderRequest>, isChecked: Boolean) {
        if (isFilling) return

        isFilling = true
        for (i in orderRequests) {
            setChecked(orderRequest = i, isChecked = isChecked, suspendRefresh = true)
        }
        isFilling = false

        // Notificamos al Listener superior
        dataSetChangedListener?.onDataSetChanged()
    }

    fun addVisibleStatus(status: OrderRequestType) {
        if (visibleStatus.contains(status)) return
        visibleStatus.add(status)

        refreshFilter()
    }

    fun removeVisibleStatus(status: OrderRequestType) {
        if (!visibleStatus.contains(status)) return
        visibleStatus.remove(status)

        // Quitamos los pedidos con el estado seleccionado de la lista marcados.
        val uncheckedOrderRequests =
            ArrayList(fullList.mapNotNull { if (it.orderRequestType == status) it.orderRequestId else null })
        checkedIdArray.removeAll(uncheckedOrderRequests.toSet())

        refreshFilter()
    }

    internal class SelectedViewHolder(val binding: OrderRequestRowExpandedBinding) :
        ViewHolder(binding.root) {
        fun bindCheckBoxVisibility(checkBoxVisibility: Int = GONE) {
            binding.checkBoxConstraintLayout.visibility = checkBoxVisibility
        }

        fun bindCheckBoxState(checked: Boolean) {
            binding.checkBox.isChecked = checked
        }

        fun bind(orderRequest: OrderRequest, checkBoxVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)

            binding.descriptionTextView.text =
                orderRequest.description.ifEmpty { context.getString(R.string.without_description) }
            binding.creationDateTextView.text = orderRequest.creationDate.toString()
            binding.finishDateTextView.text = orderRequest.finishDate ?: context.getString(R.string.uncompleted)
            binding.filenameTextView.text = orderRequest.filename.substringAfterLast('/')

            setStyle(orderRequest)
        }

        private fun setStyle(orderRequest: OrderRequest) {
            val v = itemView

            // Background layouts
            // Resalta por estado del pedido
            val default = getDrawable(context.resources, R.drawable.layout_thin_border, null)!!
            val prepareOrder = getDrawable(context.resources, R.drawable.layout_thin_border_green, null)!!
            val stockAuditFromDevice = getDrawable(context.resources, R.drawable.layout_thin_border_yellow, null)!!
            val stockAudit = getDrawable(context.resources, R.drawable.layout_thin_border_blue, null)!!
            val receptionAudit = getDrawable(context.resources, R.drawable.layout_thin_border_orange, null)!!
            val deliveryAudit = getDrawable(context.resources, R.drawable.layout_thin_border_green_2, null)!!

            val backColor: Drawable
            val foreColor: Int

            when (orderRequest.orderRequestType) {
                OrderRequestType.prepareOrder -> {
                    backColor = prepareOrder
                    foreColor = prepareOrderSelectedForeColor
                }

                OrderRequestType.stockAuditFromDevice -> {
                    backColor = stockAuditFromDevice
                    foreColor = stockAuditFromDeviceSelectedForeColor
                }

                OrderRequestType.stockAudit -> {
                    backColor = stockAudit
                    foreColor = stockAuditSelectedForeColor
                }

                OrderRequestType.receptionAudit -> {
                    backColor = receptionAudit
                    foreColor = receptionAuditSelectedForeColor
                }

                OrderRequestType.deliveryAudit -> {
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
            binding.filenameTextView.setTextColor(foreColor)
            binding.descriptionTextView.setTextColor(foreColor)
            binding.creationDateTextView.setTextColor(foreColor)
            binding.finishDateTextView.setTextColor(foreColor)
            binding.checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)

            binding.creationDateLabelTextView.setTextColor(titleForeColor)
            binding.finishDateLabelTextView.setTextColor(titleForeColor)
        }
    }

    internal class UnselectedViewHolder(val binding: OrderRequestRowBinding) :
        ViewHolder(binding.root) {
        fun bindCheckBoxVisibility(checkBoxVisibility: Int = GONE) {
            binding.checkBoxConstraintLayout.visibility = checkBoxVisibility
        }

        fun bindCheckBoxState(checked: Boolean) {
            binding.checkBox.isChecked = checked
        }

        fun bind(orderRequest: OrderRequest, checkBoxVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)

            binding.descriptionTextView.text =
                orderRequest.description.ifEmpty { context.getString(R.string.without_description) }
            binding.filenameTextView.text = orderRequest.filename.substringAfterLast('/')
            binding.creationDateTextView.text = orderRequest.creationDate

            setStyle(orderRequest)
        }

        private fun setStyle(orderRequest: OrderRequest) {
            val v = itemView

            // Background layouts
            // Resalta por estado del pedido
            val default = getDrawable(context.resources, R.drawable.layout_thin_border, null)!!
            val prepareOrder = getDrawable(context.resources, R.drawable.layout_thin_border_green, null)!!
            val stockAuditFromDevice = getDrawable(context.resources, R.drawable.layout_thin_border_yellow, null)!!
            val stockAudit = getDrawable(context.resources, R.drawable.layout_thin_border_blue, null)!!
            val receptionAudit = getDrawable(context.resources, R.drawable.layout_thin_border_orange, null)!!
            val deliveryAudit = getDrawable(context.resources, R.drawable.layout_thin_border_green_2, null)!!

            val backColor: Drawable
            val foreColor: Int

            when (orderRequest.orderRequestType) {
                OrderRequestType.prepareOrder -> {
                    backColor = prepareOrder
                    foreColor = prepareOrderForeColor
                }

                OrderRequestType.stockAuditFromDevice -> {
                    backColor = stockAuditFromDevice
                    foreColor = stockAuditFromDeviceForeColor
                }

                OrderRequestType.stockAudit -> {
                    backColor = stockAudit
                    foreColor = stockAuditForeColor
                }

                OrderRequestType.receptionAudit -> {
                    backColor = receptionAudit
                    foreColor = receptionAuditForeColor
                }

                OrderRequestType.deliveryAudit -> {
                    backColor = deliveryAudit
                    foreColor = deliveryAuditForeColor
                }

                else -> {
                    backColor = default
                    foreColor = defaultForeColor
                }
            }

            v.background = backColor
            binding.filenameTextView.setTextColor(foreColor)
            binding.descriptionTextView.setTextColor(foreColor)
            binding.creationDateTextView.setTextColor(foreColor)
            binding.checkBox.buttonTintList = ColorStateList.valueOf(darkslategray)
        }
    }

    private object OrderRequestDiffUtilCallback : DiffUtil.ItemCallback<OrderRequest>() {
        override fun areItemsTheSame(oldItem: OrderRequest, newItem: OrderRequest): Boolean {
            return oldItem.orderRequestId == newItem.orderRequestId
        }

        override fun areContentsTheSame(oldItem: OrderRequest, newItem: OrderRequest): Boolean {
            if (oldItem.orderRequestId != newItem.orderRequestId) return false
            if (oldItem.clientId != newItem.clientId) return false
            if (oldItem.userId != newItem.userId) return false
            if (oldItem.externalId != newItem.externalId) return false
            if (oldItem.creationDate != newItem.creationDate) return false
            if (oldItem.description != newItem.description) return false
            if (oldItem.zone != newItem.zone) return false
            if (oldItem.orderRequestType != newItem.orderRequestType) return false
            if (oldItem.resultDiffQty != newItem.resultDiffQty) return false
            if (oldItem.resultDiffProduct != newItem.resultDiffProduct) return false
            if (oldItem.resultAllowDiff != newItem.resultAllowDiff) return false
            if (oldItem.resultAllowMod != newItem.resultAllowMod) return false
            if (oldItem.completed != newItem.completed) return false
            if (oldItem.startDate != newItem.startDate) return false
            if (oldItem.finishDate != newItem.finishDate) return false
            if (oldItem.contents != newItem.contents) return false
            if (oldItem.packages != newItem.packages) return false
            return true
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

        // Vamos a retener en el caché un [cacheFactor] por ciento de los pedidos creados o un máximo de [maxCachedOrderRequests]
        val maxCachedOrderRequests = 50
        val cacheFactor = 0.10
        var cacheSize = (fullList.size * cacheFactor).toInt()
        if (cacheSize > maxCachedOrderRequests) cacheSize = maxCachedOrderRequests
        recyclerView.setItemViewCacheSize(cacheSize)
        recyclerView.recycledViewPool.setMaxRecycledViews(SELECTED_VIEW_TYPE, 0)
        recyclerView.recycledViewPool.setMaxRecycledViews(UNSELECTED_VIEW_TYPE, 0)

        // Ordenamiento natural de la lista completa para trabajar en adelante con una lista ordenada
        val tList = sortOrderRequests(fullList)
        fullList = tList

        // Suministramos la lista a publicar refrescando el filtro que recorre la lista completa y devuelve los resultados filtrados y ordenados
        refreshFilter(filterOptions)
    }

    /**
     * Return a sorted list of visible state orderRequests
     *
     * @param list
     * @return Lista ordenada con los estados visibles
     */
    private fun sortedVisibleList(list: MutableList<OrderRequest>?): MutableList<OrderRequest> {
        val croppedList =
            (list ?: mutableListOf()).mapNotNull { if (it.orderRequestType in visibleStatus) it else null }
        return sortOrderRequests(croppedList.toMutableList())
    }

    // Sobrecargamos estos métodos para suministrar siempre una lista ordenada y filtrada por estado de visibilidad
    override fun submitList(list: MutableList<OrderRequest>?) {
        super.submitList(sortedVisibleList(list))
    }

    override fun submitList(list: MutableList<OrderRequest>?, commitCallback: Runnable?) {
        super.submitList(sortedVisibleList(list), commitCallback)
    }

    class Builder {
        fun build(): OrderRequestAdapter {
            return OrderRequestAdapter(this)
        }

        internal lateinit var recyclerView: RecyclerView
        internal var fullList: ArrayList<OrderRequest> = ArrayList()
        internal var checkedIdArray: ArrayList<Long> = ArrayList()
        internal var multiSelect: Boolean = false
        internal var showCheckBoxes: Boolean = false
        internal var showCheckBoxesChanged: (Boolean) -> Unit = { }
        internal var visibleStatus: ArrayList<OrderRequestType> = OrderRequestType.getAll()
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
        fun fullList(`val`: ArrayList<OrderRequest>): Builder {
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
        fun visibleStatus(`val`: ArrayList<OrderRequestType>): Builder {
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