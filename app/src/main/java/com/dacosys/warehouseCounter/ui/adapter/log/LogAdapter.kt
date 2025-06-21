package com.dacosys.warehouseCounter.ui.adapter.log


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
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.Log
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.LogStatus
import com.dacosys.warehouseCounter.databinding.LogContentRowBinding
import com.dacosys.warehouseCounter.misc.utils.NumberUtils.Companion.roundToString
import com.dacosys.warehouseCounter.ui.adapter.FilterOptions
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.manipulateColor
import java.util.*

class LogAdapter private constructor(builder: Builder) :
    ListAdapter<Log, ViewHolder>(LogContentDiffUtilCallback), Filterable {

    private val recyclerView: RecyclerView
    var fullList: ArrayList<Log> = ArrayList()
    private var visibleStatus: ArrayList<LogStatus> = ArrayList(LogStatus.values().toList())
    private var filterOptions: FilterOptions = FilterOptions()

    // Este Listener debe usarse para los cambios de cantidad o de ítems marcados de la lista,
    // ya que se utiliza para actualizar los valores sumarios en la actividad.
    private var dataSetChangedListener: DataSetChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null

    // Posición del ítem seleccionado
    private var currentIndex = NO_POSITION

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
                val previousSelectedPosition = currentIndex
                currentIndex = holder.bindingAdapterPosition
                notifyItemChanged(currentIndex)

                if (previousSelectedPosition != NO_POSITION) {
                    notifyItemChanged(previousSelectedPosition)
                }

                // Scroll para asegurarnos que se vea completamente el ítem
                holder.itemView.post { scrollToPos(currentIndex) }
            }

            // Seleccionamos el ítem
            holder.itemView.isSelected = currentIndex == position
        }

        // Establece el holder
        setHolder(holder as AdapterViewHolder, position)
    }

    private fun setHolder(holder: AdapterViewHolder, position: Int) {
        val logContent = getItem(position)
        val isSelected = currentIndex == position

        // Perform a full update
        holder.bind(logContent = logContent, isSelected = isSelected)

        holder.itemView.background.colorFilter =
            if (isSelected)
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
            var selected: Log? = null
            var firstVisible: Log? = null

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                // Guardamos el item seleccionado y la posición del scroll
                selected = currentItem()
                var scrollPos =
                    (recyclerView.layoutManager as LinearLayoutManager?)?.findFirstVisibleItemPosition()
                        ?: NO_POSITION

                if (scrollPos != NO_POSITION && itemCount > scrollPos) {
                    var currentScrolled = getItem(scrollPos)

                    // Comprobamos si es visible el ítem del Scroll
                    if (currentScrolled.logStatus in visibleStatus)
                        firstVisible = currentScrolled
                    else {
                        // Si no es visible, intentar encontrar el próximo visible.
                        while (firstVisible == null) {
                            scrollPos++
                            if (itemCount > scrollPos) {
                                currentScrolled = getItem(scrollPos)
                                if (currentScrolled.logStatus in visibleStatus)
                                    firstVisible = currentScrolled
                            } else break
                        }
                    }
                }

                // Filtramos los resultados
                val results = FilterResults()
                var r: ArrayList<Log> = ArrayList()
                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    if (filterString.isNotEmpty()) {
                        var filterableItem: Log

                        for (i in 0 until fullList.size) {
                            filterableItem = fullList[i]

                            // Descartamos aquellos que no debe ser visibles
                            if (filterableItem.logStatus !in visibleStatus) continue

                            if (isFilterable(filterableItem, filterString)) {
                                r.add(filterableItem)
                            }
                        }
                    } else if (filterOptions.showAllOnFilterEmpty) {
                        r = ArrayList(fullList.mapNotNull { if (it.logStatus in visibleStatus) it else null })
                    }
                }

                results.values = r
                results.count = r.size
                return results
            }

            fun isFilterable(filterableLogContent: Log, filterString: String): Boolean =
                filterableLogContent.itemCode.contains(filterString, true) ||
                        filterableLogContent.itemDescription.contains(filterString, true)

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?, results: FilterResults?,
            ) {
                submitList(results?.values as ArrayList<Log>) {
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

    private fun sortItems(originalList: MutableList<Log>): ArrayList<Log> {
        return ArrayList(
            originalList.sortedWith(
                compareBy { it.date }
            ).toList()
        )
    }

    fun refreshFilter(options: FilterOptions) {
        filterOptions = options
        filter.filter(filterOptions.filterString)
    }

    @Suppress("unused")
    private fun refreshFilter() {
        refreshFilter(filterOptions)
    }

    fun selectItem(a: Log?, scroll: Boolean = true) {
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

    private fun getIndex(logContent: Log): Int {
        return currentList.indexOfFirst { it == logContent }
    }

    fun currentItem(): Log? {
        if (currentIndex == NO_POSITION) return null
        return if (currentList.any() && itemCount > currentIndex) getItem(currentIndex)
        else null
    }

    fun firstVisiblePos(): Int {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        return layoutManager.findFirstVisibleItemPosition()
    }

    internal class AdapterViewHolder(val binding: LogContentRowBinding) : ViewHolder(binding.root) {
        fun bind(logContent: Log, isSelected: Boolean) {

            binding.descriptionAutoResizeTextView.text = logContent.itemDescription
            binding.eanAutoResizeTextView.text = logContent.itemCode
            binding.variationQtyTextView.text =
                roundToString(logContent.variationQty ?: 0.toDouble(), settingsVm.decimalPlaces)
            binding.finalQtyTextView.text =
                roundToString(logContent.finalQty ?: 0.toDouble(), settingsVm.decimalPlaces)

            setStyle(logContent, isSelected)
        }

        private fun setStyle(logContent: Log, isSelected: Boolean = false) {
            val v = itemView

            val someQty = getDrawable(context.resources, R.drawable.layout_thin_border_red, null)
            val qtyZero = getDrawable(context.resources, R.drawable.layout_thin_border, null)

            val backColor: Drawable
            val foreColor: Int
            when (logContent.logStatus) {
                LogStatus.SOME_QTY -> {
                    backColor = someQty!!
                    foreColor =
                        if (isSelected) someQtySelectedForeColor
                        else someQtyForeColor
                }

                LogStatus.QTY_ZERO -> {
                    backColor = qtyZero!!
                    foreColor =
                        if (isSelected) qtyZeroSelectedForeColor
                        else qtyZeroForeColor
                }
            }

            v.background = backColor
            binding.descriptionAutoResizeTextView.setTextColor(foreColor)
            binding.eanAutoResizeTextView.setTextColor(foreColor)
            binding.variationQtyTextView.setTextColor(foreColor)
            binding.finalQtyTextView.setTextColor(foreColor)
        }
    }

    private object LogContentDiffUtilCallback : DiffUtil.ItemCallback<Log>() {
        override fun areItemsTheSame(oldItem: Log, newItem: Log): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: Log, newItem: Log): Boolean {
            if (oldItem.itemId != newItem.itemId) return false
            if (oldItem.itemDescription != newItem.itemDescription) return false
            if (oldItem.itemCode != newItem.itemCode) return false
            if (oldItem.scannedCode != newItem.scannedCode) return false
            if (oldItem.variationQty != newItem.variationQty) return false
            if (oldItem.finalQty != newItem.finalQty) return false
            if (oldItem.clientId != newItem.clientId) return false
            if (oldItem.userId != newItem.userId) return false
            return oldItem.date == newItem.date
        }
    }

    companion object {
        // region COLORS
        private var someQtyForeColor: Int = 0
        private var qtyZeroForeColor: Int = 0

        private var someQtySelectedForeColor: Int = 0
        private var qtyZeroSelectedForeColor: Int = 0

        /**
         * Setup colors
         * Simplemente inicializamos algunas variables con los colores que vamos a usar para cada estado.
         */
        private fun setupColors() {
            // Color de los diferentes estados
            val someQty = getColor(context.resources, R.color.status_some_qty, null)
            val qtyZero = getColor(context.resources, R.color.status_qty_zero, null)

            // Mejor contraste para los ítems seleccionados
            someQtySelectedForeColor = getBestContrastColor(manipulateColor(someQty, 0.5f))
            qtyZeroSelectedForeColor = getBestContrastColor(manipulateColor(qtyZero, 0.5f))

            // Mejor contraste para los ítems no seleccionados
            someQtyForeColor = getBestContrastColor(someQty)
            qtyZeroForeColor = getBestContrastColor(qtyZero)
        }

        // endregion
    }

    init {
        // Set values from Builder
        recyclerView = builder.recyclerView
        fullList = builder.fullList
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
    private fun sortedVisibleList(list: MutableList<Log>?): MutableList<Log> {
        val croppedList =
            (list ?: mutableListOf()).mapNotNull { if (it.logStatus in visibleStatus) it else null }
        return sortItems(croppedList.toMutableList())
    }

    // Sobrecargamos estos métodos para suministrar siempre una lista ordenada y filtrada por estado de visibilidad
    override fun submitList(list: MutableList<Log>?) {
        super.submitList(sortedVisibleList(list))
    }

    override fun submitList(list: MutableList<Log>?, commitCallback: Runnable?) {
        super.submitList(sortedVisibleList(list), commitCallback)
    }

    class Builder {
        fun build(): LogAdapter {
            return LogAdapter(this)
        }

        internal lateinit var recyclerView: RecyclerView
        internal var fullList: ArrayList<Log> = ArrayList()
        internal var visibleStatus: ArrayList<LogStatus> = ArrayList(LogStatus.values().toList())
        internal var filterOptions: FilterOptions = FilterOptions()
        internal var dataSetChangedListener: DataSetChangedListener? = null
        internal var checkedChangedListener: CheckedChangedListener? = null

        // Setter methods for variables with chained methods
        fun recyclerView(`val`: RecyclerView): Builder {
            recyclerView = `val`
            return this
        }

        fun fullList(`val`: ArrayList<Log>): Builder {
            fullList = `val`
            return this
        }

        fun visibleStatus(`val`: ArrayList<LogStatus>): Builder {
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
