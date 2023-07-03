package com.dacosys.warehouseCounter.adapter.log


import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.dacosys.warehouseCounter.databinding.LogContentRowBinding
import com.dacosys.warehouseCounter.dto.log.LogContent
import com.dacosys.warehouseCounter.dto.log.LogContentStatus
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.decimalPlaces
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.manipulateColor
import java.util.*

class LogContentAdapter(
    private val recyclerView: RecyclerView,
    var fullList: ArrayList<LogContent> = ArrayList(),
    private var visibleStatus: ArrayList<LogContentStatus> = ArrayList(LogContentStatus.values().toList()),
    private var filterOptions: FilterOptions = FilterOptions("", true)
) : ListAdapter<LogContent, ViewHolder>(LogContentDiffUtilCallback), Filterable {

    // Posición del ítem seleccionado
    private var currentIndex = NO_POSITION

    // Este Listener debe usarse para los cambios de cantidad o de ítems marcados de la lista,
    // ya que se utiliza para actualizar los valores sumarios en la actividad.
    private var dataSetChangedListener: DataSetChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null

    fun clear() {
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
        return AdapterViewHolder(LogContentRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
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

            // Notificamos al Listener superior
            dataSetChangedListener?.onDataSetChanged()
        }

        // Establece el holder
        setHolder(holder as AdapterViewHolder, position)
    }

    private fun setHolder(holder: AdapterViewHolder, position: Int) {
        val logContent = getItem(position)

        // Perform a full update
        holder.bind(logContent = logContent)

        holder.itemView.background.colorFilter =
            if (holder.itemView.isSelected)
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    getColorWithAlpha(
                        colorId = R.color.lightslategray,
                        alpha = 220
                    ), BlendModeCompat.MODULATE
                )
            else null
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            var selected: LogContent? = null
            var firstVisible: LogContent? = null

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
                var r: ArrayList<LogContent> = ArrayList()
                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    if (filterString.isNotEmpty()) {
                        var filterableItem: LogContent

                        for (i in 0 until fullList.size) {
                            filterableItem = fullList[i]

                            // Descartamos aquellos que no debe ser visibles
                            if (filterableItem.contentStatus !in visibleStatus) continue

                            if (isFilterable(filterableItem, filterString)) {
                                r.add(filterableItem)
                            }
                        }
                    } else if (filterOptions.showAllOnFilterEmpty) {
                        r = ArrayList(fullList.mapNotNull { if (it.contentStatus in visibleStatus) it else null })
                    }
                }

                results.values = r
                results.count = r.size
                return results
            }

            fun isFilterable(filterableLogContent: LogContent, filterString: String): Boolean =
                filterableLogContent.itemCode.contains(filterString, true) ||
                        filterableLogContent.itemStr.contains(filterString, true)

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?, results: FilterResults?,
            ) {
                submitList(results?.values as ArrayList<LogContent>) {
                    run {
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
    }

    private fun sortItems(originalList: MutableList<LogContent>): ArrayList<LogContent> {
        // Run the follow method on each of the roots
        return ArrayList(
            originalList.sortedWith(
                compareBy({ it.itemCode },
                    { it.itemStr })
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

    fun add(logContent: LogContent, position: Int) {
        fullList.add(position, logContent)
        submitList(fullList) {
            run {
                // Notificamos al Listener superior
                dataSetChangedListener?.onDataSetChanged()

                selectItem(position)
            }
        }
    }

    fun remove(position: Int) {
        fullList.removeAt(position)
        submitList(fullList) {
            run {
                // Notificamos al Listener superior
                dataSetChangedListener?.onDataSetChanged()
            }
        }
    }

    fun selectItem(a: LogContent?, scroll: Boolean = true) {
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
        return currentList.indexOfFirst { it.itemId == id }
    }

    private fun getIndex(logContent: LogContent): Int {
        return currentList.indexOfFirst { it == logContent }
    }

    fun currentItem(): LogContent? {
        if (currentIndex == NO_POSITION) return null
        return if (currentList.any() && itemCount > currentIndex) getItem(currentIndex)
        else null
    }

    fun totalVisible(): Int {
        return itemCount
    }

    fun firstVisiblePos(): Int {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        return layoutManager.findFirstVisibleItemPosition()
    }

    internal class AdapterViewHolder(val binding: LogContentRowBinding) : ViewHolder(binding.root) {
        fun bind(logContent: LogContent) {

            binding.descriptionAutoResizeTextView.text = logContent.itemStr
            binding.eanAutoResizeTextView.text = logContent.itemCode
            binding.variationQtyTextView.text =
                Statics.roundToString(logContent.variationQty ?: 0.toDouble(), decimalPlaces)
            binding.finalQtyTextView.text = Statics.roundToString(logContent.finalQty ?: 0.toDouble(), decimalPlaces)

            setStyle(logContent)
        }

        private fun setStyle(logContent: LogContent) {
            val v = itemView

            // Background layouts
            // Resalta por estado de la orden
            val default = getDrawable(context.resources, R.drawable.layout_thin_border, null)!!
            val someQty = getDrawable(context.resources, R.drawable.layout_thin_border_red, null)
            val qtyZero = getDrawable(context.resources, R.drawable.layout_thin_border, null)

            val backColor: Drawable
            val foreColor: Int
            when (logContent.contentStatus) {
                LogContentStatus.SOME_QTY -> {
                    backColor = someQty!!
                    foreColor = someQtySelectedForeColor
                }

                LogContentStatus.QTY_ZERO -> {
                    backColor = qtyZero!!
                    foreColor = qtyZeroSelectedForeColor
                }

                else -> {
                    backColor = default
                    foreColor = defaultSelectedForeColor
                }
            }

            v.background = backColor
            binding.descriptionAutoResizeTextView.setTextColor(foreColor)
            binding.eanAutoResizeTextView.setTextColor(foreColor)
            binding.variationQtyTextView.setTextColor(foreColor)
            binding.finalQtyTextView.setTextColor(foreColor)
        }
    }

    private object LogContentDiffUtilCallback : DiffUtil.ItemCallback<LogContent>() {
        override fun areItemsTheSame(oldItem: LogContent, newItem: LogContent): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: LogContent, newItem: LogContent): Boolean {
            if (oldItem.itemId != newItem.itemId) return false
            if (oldItem.itemStr != newItem.itemStr) return false
            if (oldItem.itemCode != newItem.itemCode) return false
            if (oldItem.lotId != newItem.lotId) return false
            if (oldItem.lotCode != newItem.lotCode) return false
            if (oldItem.scannedCode != newItem.scannedCode) return false
            if (oldItem.variationQty != newItem.variationQty) return false
            if (oldItem.finalQty != newItem.finalQty) return false
            return oldItem.date == newItem.date
        }
    }

    companion object {
        // region COLORS
        private var someQtyForeColor: Int = 0
        private var qtyZeroForeColor: Int = 0
        private var defaultForeColor: Int = 0

        private var someQtySelectedForeColor: Int = 0
        private var qtyZeroSelectedForeColor: Int = 0
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
            val someQty = getColor(context.resources, R.color.status_some_qty, null)
            val qtyZero = getColor(context.resources, R.color.status_qty_zero, null)
            val default = getColor(context.resources, R.color.status_default, null)

            // Mejor contraste para los pedidos seleccionados
            someQtySelectedForeColor = getBestContrastColor(manipulateColor(someQty, 0.5f))
            qtyZeroSelectedForeColor = getBestContrastColor(manipulateColor(qtyZero, 0.5f))
            defaultSelectedForeColor = getBestContrastColor(manipulateColor(default, 0.5f))

            // Mejor contraste para los pedidos no seleccionados
            someQtyForeColor = getBestContrastColor(someQty)
            qtyZeroForeColor = getBestContrastColor(qtyZero)
            defaultForeColor = getBestContrastColor(default)

            // CheckBox color
            darkslategray = getColor(context.resources, R.color.darkslategray, null)

            // Title color
            lightgray = getColor(context.resources, R.color.lightgray, null)
        }

        // endregion
        // Parámetros del filtro
        data class FilterOptions(
            var filterString: String = "",
            var showAllOnFilterEmpty: Boolean = false,
        )
    }

    init {
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
        recyclerView.recycledViewPool.setMaxRecycledViews(0, 0)

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
    private fun sortedVisibleList(list: MutableList<LogContent>?): MutableList<LogContent> {
        val croppedList =
            (list ?: mutableListOf()).mapNotNull { if (it.contentStatus in visibleStatus) it else null }
        return sortItems(croppedList.toMutableList())
    }

    // Sobrecargamos estos métodos para suministrar siempre una lista ordenada y filtrada por estado de visibilidad
    override fun submitList(list: MutableList<LogContent>?) {
        super.submitList(sortedVisibleList(list))
    }

    override fun submitList(list: MutableList<LogContent>?, commitCallback: Runnable?) {
        super.submitList(sortedVisibleList(list), commitCallback)
    }
}